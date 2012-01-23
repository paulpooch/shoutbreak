///////////////////////////////////////////////////////////////////////////////
// Shoutbreak Server v2.0
// All code Copyright 2012 Virtuability, LLC.
// See shoutbreak.com for more info.
//
// Node order of appearance matters for many sections.
//
// TODO:
// 1. Is Cache.set doing replace?
// 2. Better logging - based on UID.
// 3. Add secure random to generatePassword once it's released here:
// 		https://github.com/akdubya/rbytes
// 4. DDOS shield.
// 5. Cap number or auth challenges we'll send.
// 6. Make sure no 'new' uses create memory leak.
//
// RESOURCES:
// http://blog.mixu.net/2011/02/02/essential-node-js-patterns-and-snippets/
// http://nodejsmodules.org/tags/password
// http://docs.amazonwebservices.com/AWSRubySDK/latest/AWS/DynamoDB/AttributeCollection.html
// http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_GetItem.html
///////////////////////////////////////////////////////////////////////////////

				/*
				var pendingLevelUp = user['pending_level_up'];
				if (pendingLevelUp) {
					var levelUpInfo = {'level': user['level'], 'pts': user['points']};
					// No callback
					Cache.set(Config.PRE_USER_PENDING_LEVEL_UP + uid, levelUpInfo, 
						Config.TIMEOUT_USER_PENDING_LEVEL_UP, null);
				}
				*/

// Config /////////////////////////////////////////////////////////////////////

var Config = (function() {

	// Settings
	this.USER_INITIAL_POINTS = 400;
	this.USER_INITIAL_LEVEL = 5;
	this.USER_INITIAL_PENDING_LEVEL_UP = 5;
	
	// AWS
	this.CACHE_URL = 'cache-001.ardkb4.0001.use1.cache.amazonaws.com:11211',
	this.AWS_CREDENTIALS = {
		AccessKeyId:'AKIAINHDEIZ3QVSHQ3PA', 
		SecretKey: 	'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'
	};
	
	// Cache Keys
	this.PRE_CREATE_ACCOUNT_USER_TEMP_ID = 		'tempuserid';
	this.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID = 	300; // 5 minutes
	this.PRE_ACTIVE_AUTH = 						'activeauth';
	this.TIMEOUT_ACTIVE_AUTH = 					1800; // 30 minutes
	
	// Tables
	this.TABLE_USERS = 'USERS';

	return this;
})();

// Includes ///////////////////////////////////////////////////////////////////

var Http = 			require('http'),
	QueryString = 	require('querystring'),
	DynamoDB = 		require('dynamoDB').DynamoDB(Config.AWS_CREDENTIALS),
	SimpleDB =		require('simpledb'),
	Memcached = 	require('memcached'),
	Sanitizer = 	require('validator').sanitize,
	Validator = 	require('validator').check,
	Uuid = 			require('node-uuid'),
	Crypto =		require('crypto'),
	Assert =		require('assert');


// Utils //////////////////////////////////////////////////////////////////////

// Add more robust logging here as needed
var Log = (function() {

	this.e = 	function(text) { console.error(text); };
	this.i = 	function(text) { console.info(text); };
	this.l = 	function(text) { console.log(text); };
	this.w = 	function(text) { console.warn(text); };
	this.obj = 	function(obj) { console.dir(obj); };

	return this;
})();

var Utils = (function() {

	this.generatePassword = function(pLength, pLevel) {
		var length = (typeof pLength == 'undefined') ? 32 : pLength;
		var level = (typeof pLevel == 'undefined') ? 3 : pLevel;
		var validChars = [
			'', // Level 0 undefined. 
			'0123456789abcdfghjkmnpqrstvwxyz',
			'0123456789abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ',
			'0123456789_!@#$%&*()-=+/abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
		];
		var password = '';
		var counter = 0;
		while (counter < length) {
			var rand = Math.round(Math.random() * (validChars[level].length - 1));
			var oneChar = validChars[level].substr(rand, 1);
			password += oneChar;
			counter++;
		}
		return password;
	};

	this.hashSha512 = function(text) {
		return Crypto.createHash('sha512').update(text).digest('base64');
	};

	this.sleep = function(seconds) {
		var startTime = new Date().getTime();
		while (new Date().getTime() < startTime + (seconds * 1000));
  	};

	return this;
})();

// Cache //////////////////////////////////////////////////////////////////////

var Cache = (function() {

	var memcached = new Memcached(Config.CACHE_URL);

	memcached.on('issue', function(issue) {
		Log.e('Issue occured on server ' + issue.server + ', ' + issue.retries  + 
		' attempts left untill failure');
	});

	memcached.on('failure', function(issue) {
		Log.e(issue.server + ' failed!');
	});

	memcached.on('reconnecting', function(issue) {
		Log.e('reconnecting to server: ' + issue.server + ' failed!');
	})

	this.get = function(key, callback) {
		memcached.get(key, function(err, result) {
			if (err) {
				Log.e(err);
			} else {
				callback(result);
			}
		});
	};

	// Returns _true_ if successful.
	this.set = function(key, value, lifetime, callback) {
		memcached.set(key, value, lifetime, function(err, result) {
			if (err) {
				Log.e(err);
			} else {
				callback(result);
			}
		});
	};

	this.delete = function(key, callback) {
		memcached.del(key, function(err, result) {
			if (err) {
				Log.e(err);
			} else {
				callback(result);
			}
		});
	};

	return this;
})();

// User ///////////////////////////////////////////////////////////////////////

var User = function() {
	this.uid = null;
	this.lastActivityTime = null;
	this.userPwHash = null;
	this.userPwSalt = null;
	this.androidId = null;
	this.deviceId = null;
	this.phoneNum = null;
	this.carrier = null;
	this.creationTime = null;
	this.points = null;
	this.level = null;
	this.pendingLevelUp = null;
}

// Database ///////////////////////////////////////////////////////////////////

var Database = (function() {

	// Users table
	this.Users = function() {

		this.add = function(user, parentCallback) {
			var data = {
				'TableName': Config.TABLE_USERS,
				'Item': {
					'user_id': 				{'S': user.uid},
					'last_activity_time': 	{'S': user.lastActivityTime},
					'user_pw_hash': 		{'S': user.userPwHash},
					'user_pw_salt': 		{'S': user.userPwSalt},
					'android_id': 			{'S': user.androidId},
					'device_id': 			{'S': user.deviceId},
					'phone_num': 			{'N': String(user.phoneNum)},
					'carrier': 				{'S': user.carrier},
					'creation_time': 		{'S': user.creationTime},
					'points': 				{'N': String(user.points)},
					'level': 				{'N': String(user.level)},
					'pending_level_up': 	{'N': String(user.pendingLevelUp)}
				}					
			};
			var callback = function(result) {
				result.on('data', function(chunk) {
					if (chunk['ConsumedCapacityUnits']) {
						parentCallback(true);
					} else {
						Log.e(String(chunk));
						parentCallback(false);
					}
				});
				result.on('ready', function(data) {
					Log.e(data.error);
					parentCallback(false);
				});
			};
			DynamoDB.putItem(data, callback);
		};

		this.getUser = function(uid, parentCallback) {
			Log.l("getUser = " + uid);
			var req = {
				'TableName': Config.TABLE_USERS,
				'Key': {
					'HashKeyElement': {'S': uid}
				},
				'AttributesToGet': [
					'user_id',
					'last_activity_time',
					'user_pw_hash',
					'user_pw_salt',
					'android_id',
					'device_id',
					'phone_num',
					'carrier',
					'creation_time',
					'points',
					'level',
					'pending_level_up'
				],
				'ConsistentRead': true
			};
			var callback = function(response) {
    			response.on('data', function(chunk) {
    				var item = JSON.parse(chunk);
    				if (item['Item']) {
    					item = item['Item'];
    					var user = new User();
    					user.uid = item['user_id']['S'];
						user.lastActivityTime = item['last_activity_time']['S'];
						user.userPwHash = item['user_pw_hash']['S'];
						user.userPwSalt = item['user_pw_salt']['S'];
						user.androidId = item['android_id']['S'];
						user.deviceId = item['device_id']['S'];
						user.phoneNum = parseInt(item['phone_num']['N']);
						user.carrier = item['carrier']['S'];
						user.creationTime = item['creation_time']['S'];
						user.points = parseInt(item['points']['N']);
						user.level = parseInt(item['level']['N']);
						user.pendingLevelUp = parseInt(item['pending_level_up']['N']);
       					parentCallback(user);
       				} else {
						Log.e(chunk);
						parentCallback(false);
       				}
    			});
    		};

			/*
			function(result) {
       			//result.on('data', parentCallback);
       			result.on('data', function(chunk) {
       				Log.l(chunk);
       			});
         	};*/
			DynamoDB.getItem(req, callback);
		};

		this.generateNonce = function(uid, parentCallback) {
			Log.l('generateNonce');
			var callback = function(user) {
				Log.l(user);
				if (user) {
					if (user.userPwHash && user.userPwSalt) {
						var nonce = Utils.generatePassword(40);
						var authInfo = {'pw_hash': user.userPwHash, 'pw_salt': user.userPwSalt, 'nonce': nonce};
						var callback2 = function(putResult) {
							var now = new Date().toISOString();
							var callback3 = function(updateResult) {
								parentCallback(nonce);
							};
							Database.Users.updateLastActivityTime(uid, now, callback3);
						};
						Cache.set(Config.PRE_ACTIVE_AUTH + uid, authInfo,
							Config.TIMEOUT_ACTIVE_AUTH, callback2);
					}
				} else {
					parentCallback(false);
				}
			};
			Database.Users.getUser(uid, callback);
		};

		this.updateLastActivityTime = function(uid, time, parentCallback) {
			var data = {
				'TableName': Config.TABLE_USERS,
				'Key': {
					'HashKeyElement': {'S': uid}
				},
				'AttributeUpdates': {
					'last_activity_time': time
				},
				'ReturnValues': 'NONE'
			};
			var callback = function(response, result) {
				response.on('data', parentCallback);
			};
			dynamoDB.updateItem(data, callback);
		};

		return this;
	}();

	return this;
})();

// Entry Methods //////////////////////////////////////////////////////////////

// Front door.
var init = function(request, response) {
	process(request, response);
};

// Parse POST variables.
var process = function(request, response) {
	if (request.method == 'POST') {
		var body = '';
		request.on('data', function(data) {
			body += data;
			if (body.length > 1e6) {
				// FLOOD ATTACK OR FAULTY CLIENT, NUKE REQUEST
				request.connection.destroy();
			}
		});
		request.on('end', function () {
			var POST = QueryString.parse(body);
			sanitize(POST, response);
        });
    }
};

// Used by tests to skip front door.
var fakePost = function(dirty, response, testCallback) {
	sanitize(dirty, response, testCallback);
};

// Sanitize post vars.  Safe sex.
var sanitize = function(dirty, response, testCallback) {
	Log.l(dirty);
	Log.l('sanitize');
	var clean = new Object();
	
	// Strings
	var allowedStrings = {
		'a': 1,
		'uid': 1,
		'android_id': 1,
		'device_id': 1,
		'carrier': 1,
		'auth': 1
	};
	for (var param in allowedStrings) {
		if (param in dirty) {
			clean[param] = Sanitizer(dirty[param]).trim();
			clean[param] = Sanitizer(clean[param]).xss();
			clean[param] = Sanitizer(clean[param]).entityEncode();
		}
	}
		
	// Ints
	var allowedInts = {
		'phone_num': 1
	};
	for (var param in allowedInts) {
		if (param in dirty) {
			clean[param] = Sanitizer(dirty[param]).toInt();
		}
	}

	// Floats
	var allowedFloats = {
		'lat': 1,
		'lng': 1	
	};
	for (var param in allowedFloats) {
		if (param in dirty) {
			clean[param] = Sanitizer(dirty[param]).toFloat();
		}
	}

	validate(clean, response, testCallback);
};

// Strict whitelist validation.
var validate = function(dirty, response, testCallback) {
	Log.l(dirty);
	Log.l('validate');
	var clean = new Object();
	var param;

	// a
	param = 'a';
	if (param in dirty) {
		var validActions = {'create_account':1, 'user_ping':1, 'shout':1, 'vote':1};
		if (dirty[param] in validActions) {
			clean[param] = dirty[param];
		}
	}

	// uid
	param = 'uid';
	if (param in dirty) {
		if (dirty[param].length == 36) {
			if (Validator(dirty[param]).isUUID()) {
				clean[param] = dirty[param];
			}
		}
	}

	// android_id
	param = 'android_id';
	if (param in dirty) {
		if (dirty[param].length == 16) {
			if (Validator(dirty[param]).regex(/[0-9A-Fa-f]{16}/)) {
				clean[param] = dirty[param];
			}
		}
	}

	// device_id
	param = 'device_id';
	if (param in dirty) {
		if (dirty[param].length < 65) {
			// We'll allow 8-64 hexchars.
			if (Validator(dirty[param]).regex(/[0-9A-Fa-f]{8,64}/)) {
				clean[param] = dirty[param];
			}
		}
	}

	// phone_num
	param = 'phone_num';
	if (param in dirty) {
		if (Validator(dirty[param]).isNumeric() &&
		Validator(dirty[param]).min(0) && 
		Validator(dirty[param]).max(9999999999)) {
			clean[param] = dirty[param];
		}
	}

	// carrier
	param = 'carrier';
	if (param in dirty) {
		if (dirty[param].length < 100) {
			clean[param] = dirty[param];
		}
	}

	// auth
	param = 'auth';
	if (param in dirty) {
		if (dirty[param].length == 160 || dirty[param].length == 7) {
			clean[param] = dirty[param];
		}
	}

	// lat
	param = 'lat';
	if (param in dirty) {
		if (Validator(dirty[param]).isFloat() &&
		Validator(dirty[param]).min(-90) && 
		Validator(dirty[param]).max(90)) {
			clean[param] = dirty[param];
		}
	}

	// lat
	param = 'lng';
	if (param in dirty) {
		if (Validator(dirty[param]).isFloat() &&
		Validator(dirty[param]).min(-180) && 
		Validator(dirty[param]).max(180)) {
			clean[param] = dirty[param];
		}
	}

	Log.l(clean);
	route(clean, response, testCallback);
};

var route = function(clean, response, testCallback) {
	var action = clean['a'];
	switch(action) {
		case 'create_account':
			createAccount(clean, response, testCallback);
			break;
		case 'user_ping':
			ping(clean, response, testCallback);
			break;
		default:
			break;
	}
};

// Action Logic ///////////////////////////////////////////////////////////////

var createAccount = function(clean, response, testCallback) {
	var uid = clean['uid'];
	var androidId = clean['android_id'];
	var deviceId = clean['device_id'];
	var phoneNum = clean['phone_num'];
	var carrier = clean['carrier'];
	var pw = Utils.generatePassword();
	if (uid && androidId && deviceId && phoneNum && carrier) {
		var callback = function(getResult) {
			if (getResult) {
				var user = new User();
				var now = new Date().toISOString();
				user.uid = uid;
				user.lastActivityTime = now;
				user.userPwSalt = Utils.generatePassword(16, 3);
				user.userPwHash = Utils.hashSha512(pw + User.userPwSalt);
				user.androidId = androidId;
				user.deviceId = deviceId;
				user.phoneNum = phoneNum;
				user.carrier = carrier;
				user.creationTime = now;
				user.points = Config.USER_INITIAL_POINTS;
				user.level = Config.USER_INITIAL_LEVEL;
				user.pendingLevelUp = Config.USER_INITIAL_PENDING_LEVEL_UP;
				Database.Users.add(user, callback2);
			}
		};
		var callback2 = function(result) {
			Cache.delete(Config.PRE_ACTIVE_AUTH + uid, callback3);					
		};
		var callback3 = function() {
			var json = {
				'code': 'create_account_1', 
				'pw': pw
			};
			respond(json, response, testCallback);			
		};
		Cache.get(Config.PRE_CREATE_ACCOUNT_USER_TEMP_ID + uid, callback);
	} else {
		var tempUid = Uuid.v4();
		var json = {
			'code': 'create_account_0', 
			'uid': tempUid
		};
		var callback = function(putResult) {
			respond(json, response, testCallback);
		};
		Cache.set(Config.PRE_CREATE_ACCOUNT_USER_TEMP_ID + tempUid, tempUid, 
			Config.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID, callback);
	}
};

var ping = function(clean, response, testCallback) {
	var uid = clean['uid'];
	var auth = clean['auth'];
	var lat = clean['lat'];
	var lng = clean['lng'];
	var reqScores = clean['scores'];
	var reqRadius = clean['radius'];
	var level = clean['lvl'];
	if (uid && auth && lat && lng) {
		var callback = function() {
			//	
		};
		authIsValid(uid, auth, callback);
	}
};

var authIsValid = function(uid, auth, parentCallback) {
	var validAuth = false;
	var callback = function(getResult) {
		Log.l('AUTH RESULT = ' + getResult);
		if (getResult) {
			if (auth.length > Config.PASSWORD_LENGTH) {
				var submittedPw = auth.substr(0, Config.PASSWORD_LENGTH);
				var submittedHashChunk = auth.substr(Config.PASSWORD_LENGTH, auth.length);
				var pwHash = getResult['pw_hash'];
				var pwSalt = getResult['pw_salt'];
				var nonce = getResult['nonce'];
				// Does password match?
				if (Utils.hashSha512(submittedPw + pwSalt) == pwHash) {
					// Does nonce match?
					if (submittedHashChunk == Utils.hashSha512(submittedPw + nonce + uid)) {
						validAuth = true;
						var resultObject = {'valid': true};
						parentCallback(resultObject);
					}
				}				
			}
		}
		if (!validAuth) {
			Database.Users.generateNonce(uid, callback2);
		}
	};
	var callback2 = function(nonce) {
		Log.l('NONCE = ' + nonce);
		if (nonce) {
			var resultObject = {
				'valid': false,
				json: {
					'code': 'expired_auth', 
					'nonce': nonce
				}
			};
			parentCallback(resultObject)
		} else {
			var resultObject = {
				'valid': false,
				json: {
					'code': 'invalid_uid'
				}
			};
			parentCallback(resultObject)
		}
	};
	Cache.get(Config.PRE_ACTIVE_AUTH + uid, callback);	
};

// Exit Methods ///////////////////////////////////////////////////////////////

var respond = function(json, response, testCallback) {
	if (response == null) {
		if (typeof testCallback != 'undefined') {
			testCallback(json);
		}
	} else {
		response.writeHead(200, {'Content-Type': 'application/json'});
		response.write(JSON.stringify(json));
		response.end();	
	}
};

// Test Suite /////////////////////////////////////////////////////////////////

var Tests = (function() {
	
	this.run = function() {

		var uid = null;
		var pw = null;

		var test1 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'create_account_0');
			uid = json['uid'];
			var post = {
				'a': 'create_account',
				'uid': json['uid'],
				'android_id': '0123456789abcdef',
				'device_id': '0123456789ABCD',
				'phone_num': 1234567890,
				'carrier': 'Test Wireless'
			};
			fakePost(post, null, test2);
		};

		var test2 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'create_account_1');
			pw = json['pw'];
			var post = {
				'a': 'user_ping',
				'uid': uid,
				'auth': 'default',
				'lat': 40.00000,
				'lng': -70.00000,
			};
			fakePost(post, null, test3);
		};

		var test3 = function(json) {
			Log.l(json);	
		};

		var post = {
			'a': 'create_account'
		};
		fakePost(post, null, test1);
	};

	return this;
})();

// Bootstrap //////////////////////////////////////////////////////////////////

Log.l('server online');
Http.createServer(init).listen(80);
Tests.run();


/*
var makeTable = function() {
	dynamoDB.createTable({
		'TableName': 'USERS',
		'KeySchema': {
			'HashKeyElement': {'AttributeName':'user_id', 'AttributeType':'S'},
			'RangeKeyElement': {'AttributeName':'user_id', 'AttributeType':'S'}
		},
		'ProvisionedThroughput': {
			'ReadCapacityUnits': 5, 'WriteCapacityUnits': 1
		}
	},
	function(result) {
		result.on('data', function(chunk) {
			console.log('' + chunk);
		});
	}
};
*/