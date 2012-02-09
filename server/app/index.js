////////////////////////////////////////////////////////////////////////////////
// Shoutbreak Server v2.0
// All code Copyright 2012 Virtuability, LLC.
// See shoutbreak.com for more info.
//
// Node order of appearance matters for many sections.
//
// Be careful old objects in cache won't reflect new capabilities.
//
// ps aux | grep node
//
// TODO:
// 1. Is Cache.set doing replace?
// 2. Better logging - based on UID.
// 3. Add secure random to generatePassword once it's released here:
// 		https://github.com/akdubya/rbytes
// 4. DDOS shield.
// 5. Cap number of auth challenges we'll send.
// 6. Make sure no 'new' uses create memory leak.
// 7. Uncalled callbacks can sit idle forever and hang server.
// 8. If auth fails, it's a character getting killed by xss() - remove it from genPW().
// 9. Don't let user keep asking for radius - cache limit that.
//
// RESOURCES:
// http://blog.mixu.net/2011/02/02/essential-node-js-patterns-and-snippets/
// http://nodejsmodules.org/tags/password
// http://docs.amazonwebservices.com/AWSRubySDK/latest/AWS/DynamoDB/AttributeCollection.html
// http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_GetItem.html
// http://www.paperplanes.de/2012/1/30/a-tour-of-amazons-dynamodb.html
////////////////////////////////////////////////////////////////////////////////

				/*
				var pendingLevelUp = user['pending_level_up'];
				if (pendingLevelUp) {
					var levelUpInfo = {'level': user['level'], 'pts': user['points']};
					// No callback
					Cache.set(Config.PRE_USER_PENDING_LEVEL_UP + uid, levelUpInfo, 
						Config.TIMEOUT_USER_PENDING_LEVEL_UP, null);
				}
				*/

// Includes ////////////////////////////////////////////////////////////////////

// Internal
var Log = 			require('./log'),
	Utils =			require('./utils'),
	Config =		require('./config'),
	User =			require('./user'),
	Shout =			require('./shout'),
	Storage =		require('./storage');

// External (Modules)
var Http = 			require('http'),
	QueryString = 	require('querystring'),
	// https://github.com/chriso/node-validator
	Sanitizer = 	require('validator').sanitize, 
	Validator = 	require('validator').check,
	Uuid = 			require('node-uuid'),
	Assert =		require('assert');

// Entry Methods ///////////////////////////////////////////////////////////////

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
	Utils.sleep(1, function() { 
		sanitize(dirty, response, testCallback); 
	});
};

// Sanitize post vars.  Safe sex.
var sanitize = function(dirty, response, testCallback) {
	var clean = {};
	
	// Strings
	var allowedStrings = {
		'a': 1,
		'uid': 1,
		'android_id': 1,
		'device_id': 1,
		'carrier': 1,
		'auth': 1,
		'txt': 1,
		'shout_id': 1
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
		'phone_num': 1,
		'radius': 1,
		'shoutreach': 1,
		'vote': 1
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
	var clean = {};
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
		if (dirty[param].length == 120 || dirty[param].length == 7) {
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

	// radius
	param = 'radius';
	if (param in dirty) {
		if (Validator(dirty[param]).isNumeric() && dirty[param] == 1) {
			clean[param] = 1;
		}
	}

	// android_id
	param = 'txt';
	if (param in dirty) {
		if (dirty[param].length <= Config.SHOUT_LENGTH_LIMIT) {
			clean[param] = dirty[param];
		}
	}

	// shoutreach
	param = 'shoutreach';
	if (param in dirty) {
		if (Validator(dirty[param]).isNumeric() &&
		Validator(dirty[param]).min(0) && 
		Validator(dirty[param]).max(Config.SHOUTREACH_LIMIT)) {
			clean[param] = dirty[param];
		}
	}

	// shout_id
	param = 'shout_id';
	if (param in dirty) {
		if (dirty[param].length == 36) {
			if (Validator(dirty[param]).isUUID()) {
				clean[param] = dirty[param];
			}
		}
	}

	// vote
	param = 'vote';
	if (param in dirty) {
		if (Validator(dirty[param]).isNumeric() && (dirty[param] == 1 || dirty[param] == -1)) {
			clean[param] = 1;
		}
	}

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
		case 'shout':
			shout(clean, response, testCallback);
			break;
		case 'vote':
			vote(clean, response, testCallback);
			break;
		default:
			break;
	}
};

// Action Logic ///////////////////////////////////////////////////////////////

var createAccount = function(clean, response, testCallback) {
	var userId = clean['uid'];
	var androidId = clean['android_id'];
	var deviceId = clean['device_id'];
	var phoneNum = clean['phone_num'];
	var carrier = clean['carrier'];
	var pw = Utils.generatePassword();
	if (typeof userId != 'undefined' &&
	typeof androidId != 'undefined' &&
	typeof deviceId != 'undefined' &&
	typeof phoneNum != 'undefined' &&
	typeof carrier != 'undefined') {
		var callback = function(getResult) {
			if (getResult) {
				var user = new User();
				var now = Utils.getNowISO();
				user.userId = userId;
				user.lastActivityTime = now;
				user.userPwSalt = Utils.generatePassword(16);
				user.userPwHash = Utils.hashSha512(pw + user.userPwSalt);
				user.androidId = androidId;
				user.deviceId = deviceId;
				user.phoneNum = phoneNum;
				user.carrier = carrier;
				user.creationTime = now;
				user.points = Config.USER_INITIAL_POINTS;
				user.level = Config.USER_INITIAL_LEVEL;
				user.pendingLevelUp = Config.USER_INITIAL_PENDING_LEVEL_UP;
				Storage.Users.addNewUser(user, callback2, 
					function() {
						var json = { 'code': 'error', 'txt': 'Could not add user to database.' };
						respond(json, response, testCallback);	
					}
				);
			}
		};
		var callback2 = function(addNewUserResult) {
			if (addNewUserResult) {
				// Same callback regardless of outcome.
				Storage.deleteAuth(userId, callback3, callback3);
			}				
		};
		var callback3 = function(deleteResult) {
			// We don't care about deleteResult.		
			var json = {
				'code': 'create_account_1', 
				'pw': pw
			};
			respond(json, response, testCallback);
		};
		Storage.getTempAccount(userId, callback,
			function() {
				var json = { 'code': 'error', 'txt': 'Could not find temp user id.'	};
				respond(json, response, testCallback);			
			}
		);
	} else {
		var tempUid = Uuid.v4();
		var callback = function(setResult) {
			if (setResult) {
				var json = {
					'code': 'create_account_0', 
					'uid': tempUid
				};
				respond(json, response, testCallback);
			}
		};
		Storage.setTempAccount(tempUid, callback, 
			function() {
				var json = { 'code': 'error', 'txt': 'Could not save temp user id.' };
				respond(json, response, testCallback);
			}
		);
	}
};

var ping = function(clean, response, testCallback) {
	var userId = clean['uid'];
	var auth = clean['auth'];
	var lat = clean['lat'];
	var lng = clean['lng'];
	var reqScores = clean['scores'];
	var reqRadius = clean['radius'];
	var level = clean['lvl'];
	var reqRadius = clean['radius'];
	if (typeof userId != 'undefined' &&
	typeof auth != 'undefined' &&
	typeof lat != 'undefined' &&
	typeof lng != 'undefined') {

		var json = {};

		var callback = function(authIsValidResult) {
			var validAuth = authIsValidResult['valid'];
			var json = authIsValidResult['json'];
			if (validAuth) {
				Storage.LiveUsers.putUserOnline(userId, lat, lng, callback2, 
					function() {
						var json = { 'code': 'error', 'txt': 'Could not put user online.' };
						respond(json, response, testCallback);
					}
				);
			} else {
				respond(json, response, testCallback);
			}
		};
		var callback2 = function(putUserOnlineResult) {
			json['code'] = 'ping_ok';
			Storage.Users.getUser(userId, callback3, 
				function() {
					var json = { 'code': 'error', 'txt': 'Could not get user.' };
					respond(json, response, testCallback);
				}
			);
		};
		var callback3 = function(getUserResult) {
			if (getUserResult) {
				// Check level up.
				var user = getUserResult;
				if (user.pendingLevelUp > 0) {
						// Begin here THursday

				}
				if (reqRadius == 1) {
					Storage.LiveUsers.calculateRadiusOrFindTargets(false, getUserResult, null, lat, lng, callback4, 
						function() {
							var json = { 'code': 'error', 'txt': 'Could not calculate radius for shoutreach.' };
							respond(json, response, testCallback);
						}
					);
				} else {
					callback4(false);
				}
			}
		};
		var callback4 = function(calculateRadiusOrFindTargetsResult) {
			if (calculateRadiusOrFindTargetsResult) {
				json['radius'] = calculateRadiusOrFindTargetsResult;	
			}
			Storage.Inbox.checkInbox(userId, callback5,
				function() {
					Log.e('Could not check inbox.');
				}
			);
		};
		var callback5 = function(inboxContent) {
			if (inboxContent) {
				var newShouts = [];
				var loop = function(index) {
					if (index < inboxContent.length) {
						Storage.Shouts.getShout(inboxContent[index], 
							function(shout) {
								newShouts.push(shout);
								loop(index + 1);
							},
							function() {
								var json = { 'code': 'error', 'txt': 'Could not get a shout from storage.' };
								respond(json, response, testCallback);
							}
						);
					} else {
						var sArray = [];
						for (var key in newShouts) {
							var shout = newShouts[key];
							Log.obj(shout);
							Log.obj(Utils.buildShoutJson(shout, userId));
							sArray.push(Utils.buildShoutJson(shout, userId));
						}
						json['shouts'] = sArray;
						callback6();
					}
				}
				loop(0);
			} else {
				callback6();
			}
		}
		var callback6 = function() {
			respond(json, response, testCallback);
		}
		
		authIsValid(userId, auth, callback);	
	} else {
		Log.l('invalid ping');
		Log.l(typeof userId + ' | ' + typeof auth + ' | ' + typeof lat  + ' | ' + typeof lng);
	}
};

var shout = function(clean, response, testCallback) {
	var userId = clean['uid'];
	var auth = clean['auth'];
	var lat = clean['lat'];
	var lng = clean['lng'];
	var text = clean['txt'];
	var shoutreach = clean['shoutreach'];
	var user;
	if (typeof userId != 'undefined' &&
	typeof auth != 'undefined' &&
	typeof lat != 'undefined' &&
	typeof lng != 'undefined' &&
	typeof text != 'undefined' &&
	typeof shoutreach != 'undefined') {
		var callback = function(authIsValidResult) {
			var validAuth = authIsValidResult['valid'];
			var json = authIsValidResult['json'];
			if (validAuth) {
				Storage.Users.getUser(userId, callback2, 
					function() {
						var json = { 'code': 'error', 'txt': 'Could not get user.' };
						respond(json, response, testCallback);
					}
				);
			} else {
				respond(json, response, testCallback);
			}
		};
		var callback4 = function(sendShoutResult) {
			var json = { 'code': 'shout_sent' };
			respond(json, response, testCallback);		
		};
		var callback3 = function(calculateRadiusOrFindTargetsResult) {
			Storage.Shouts.sendShout(user, calculateRadiusOrFindTargetsResult, shoutreach, lat, lng, text, callback4,
				function() {
					var json = { 'code': 'error', 'txt': 'Send shout failed.' };
					respond(json, response, testCallback);
				}
			);
		};
		var callback2 = function(getUserResult) {
			if (getUserResult) {
				user = getUserResult;
				if (user.level >= shoutreach) {
					calculateRadiusOrFindTargets(true, user, shoutreach, lat, lng, callback3, 
						function() {
							var json = { 'code': 'error', 'txt': 'Error finding targets.' };
							respond(json, response, testCallback);
						}
					); 
				} else {
					var json = { 'code': 'error', 'txt': 'User does not have requested shoutreach.' };
					respond(json, response, testCallback);
				}
			}	
		};
		authIsValid(userId, auth, callback);	
	} else {
		Log.l('Invalid shout.');
	}
};

var vote = function(clean, response, testCallback) {
	var shout = false;
	var userId = clean['uid'];
	var auth = clean['auth'];
	var shoutId = clean['shout_id'];
	var vote = clean['vote'];
	if (typeof userId != 'undefined' &&
	typeof auth != 'undefined' &&
	typeof shoutId != 'undefined' &&
	typeof vote != 'undefined') {


		var callback8 = function() {
			// update last activity time of shout
		};

		var callback7 = function() {
			
		};

		var callback6 = function(addNewVoteResult) {
			// Begin here Thursday.
			// We must update User's points
			// See if they leveled up...
			// Both shouter and voter.... fuck.

			// WARNING - we hardcoded in level 1 here.
			Storage.Users.givePoints(userId, Utils.pointsForVote(1), callback7, 
				function() {
					
				}
			);

			
			//var shout = Utils.makeShoutFromDynamoItem()
			
		};
		var callback5 = function(updateVoteCountResult) {
			Storage.Votes.addNewVote(userId, shoutId, vote, callback6,
				function() {
					var json = { 'code': 'error', 'txt': 'Could not create new vote.' };
					respond(json, response, testCallback);	
				}
			);
		};
		var callback4 = function(updateShoutResult) {		
			Storage.Shouts.updateVoteCount(shoutId, vote, callback5,
				function() {
					var json = { 'code': 'error', 'txt': 'Shout does not exist or could not update vote count.' };
					respond(json, response, testCallback);	
				}
			);
		};
		var callback3 = function(getShoutResult) {
			Log.e('callback3');
			Log.e('getShoutResult = ');
			Log.obj(getShoutResult);
			shout = getShoutResult;
			if (shout.open == 0) {
				var json = { 'code': 'error', 'txt': 'Cannot vote.  Shout is closed.'};
				respond(json, response, testCallback);	
			} else {
				Storage.Shouts.isExpired(shout, callback4, 
					function() {
						var json = { 'code': 'error', 'txt': 'Could not check shout expiration.' };
						respond(json, response, testCallback);			
					}
				);
			}
		};
		var callback2 = function(getVoteResult) {
			if (!getVoteResult) {
				Storage.Shouts.getShout(shoutId, callback3,
					function() {
						var json = { 'code': 'error', 'txt': 'Could not get shout to register vote.' };
						respond(json, response, testCallback);	
					}
				);
			} else {
				var json = { 'code': 'error', 'txt': 'User already voted on this shout.' };
				respond(json, response, testCallback);	
			}
		}
		var callback = function(authIsValidResult) {
			var validAuth = authIsValidResult['valid'];
			var json = authIsValidResult['json'];
			if (validAuth) {
				Storage.Votes.getVote(userId, shoutId, callback2, 
				function() {
					var json = { 'code': 'error', 'txt': 'Cannot check if vote exists.' };
					respond(json, response, testCallback);	
				}
			);
			} else {
				respond(json, response, testCallback);
			}
		};
		authIsValid(userId, auth, callback);
	} else {
		Log.l('Invalid vote.');
	}
};

var authIsValid = function(userId, auth, parentCallback) {
	var validAuth = false;
	var callback = function(getResult) {
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
					if (Utils.hashSha512(submittedPw + nonce + userId) == submittedHashChunk) {
						validAuth = true;
						var resultObject = {'valid': true};
						parentCallback(resultObject);
					} else {
						//Log.e('Utils.hashSha512(submittedPw + nonce + uid) != submittedHashChunk');
						//Log.e('Utils.hashSha512(' + submittedPw + ' + ' + nonce + ' + ' + uid + ') != ' + submittedHashChunk + ')');
					}
				} else {
					//Log.e('Utils.hashSha512(submittedPw + pwSalt) != pwHash');
					//Log.e('Utils.hashSha512(' + submittedPw + ' + ' + pwSalt + ') != ' + pwHash + ')');
				}
			}
		} else {
			// Do not send error.  This is ok.
			// It will fall to logic below.
		}
		if (!validAuth) {
			Storage.Users.generateNonce(userId, callback2, 
				function() {
					var resultObject = {
						'valid': false,
						json: {
							'code': 'invalid_uid'
						}
					};
					parentCallback(resultObject)
				}
			);
		}
	};
	var callback2 = function(generateNonceResult) {
		if (generateNonceResult) {
			var resultObject = {
				'valid': false,
				json: {
					'code': 'expired_auth', 
					'nonce': generateNonceResult
				}
			};
			parentCallback(resultObject)
		}
	};
	// We need to go to successCallback even if not found.
	Storage.getAuth(userId, callback, 
		function() {
			callback(false);
		}
	);
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

		var userId = null;
		var pw = null;
		var auth = null;
		var sentShoutId = null;

		var test1 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'create_account_0');
			userId = json['uid'];
			var post = {
				'a': 'create_account',
				'uid': json['uid'],
				'android_id': '0123456789abcdef',
				'device_id': '0123456789ABCD',
				'phone_num': 1234567890,
				'carrier': 'Test Wireless'
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test2);
		};

		var test2 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'create_account_1');
			pw = json['pw'];
			var post = {
				'a': 'user_ping',
				'uid': userId,
				'auth': 'default',
				'lat': 40.00000,
				'lng': -70.00000,
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test3);
		};

		var test3 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'expired_auth');
			var nonce = json['nonce'];
			Log.l(pw);
			auth = pw + Utils.hashSha512(pw + nonce + userId);
			var post = {
				'a': 'user_ping',
				'uid': userId,
				'auth': auth,
				'lat': 40.00000,
				'lng': -70.00000,
				'radius': 1
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test4);
		};

		var test4 = function(json) {
			Log.l(json);
			var post = {
				'a': 'shout',
				'uid': userId,
				'auth': auth,
				'lat': 40.00000,
				'lng': -70.00000,
				'txt': 'This is a test shout.',
				'shoutreach': 5
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test5);
		};

		var test5 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'shout_sent');
			var post = {
				'a': 'user_ping',
				'uid': userId,
				'auth': auth,
				'lat': 40.00000,
				'lng': -70.00000,
				'radius': 1
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test6);
		};

		var test6 = function(json) {
			Log.l(json);
			var newShouts = json['shouts'];
			var sentShoutId = newShouts[0]['shout_id'];
			Assert.equal(json['code'], 'ping_ok');
			var post = {
				'a': 'vote',
				'uid': userId,
				'auth': auth,
				'shout_id': sentShoutId,
				'vote': 1,
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test7);
		};

		var test7 = function(json) {
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