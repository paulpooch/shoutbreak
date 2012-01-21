///////////////////////////////////////////////////////////////////////////////
// Shoutbreak Server v2.0
// All code Copyright 2012 Virtuability, LLC.
// See shoutbreak.com for more info.
//
// Node order of appearance matters for many sections.
//
// TODO:
// Is Cache.set doing replace?
// Better logging.
// 
///////////////////////////////////////////////////////////////////////////////

// Config /////////////////////////////////////////////////////////////////////

var Config = (function() {
	// Elasticache
	this.CACHE_URL = 'cache-001.ardkb4.0001.use1.cache.amazonaws.com:11211',
	// For simpleDB and dynamoDB
	this.AWS_CREDENTIALS = {
		AccessKeyId: 'AKIAINHDEIZ3QVSHQ3PA', 
		SecretKey: 'VNdRxsQNUAXYbps8YUAe3jjhTgnrG'
	};
	this.PRE_CREATE_ACCOUNT_USER_TEMP_ID = 		'tempuserid';
	this.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID = 	300; // 5 minutes
	return this;
})();

// Includes ///////////////////////////////////////////////////////////////////

var Http = 			require('http'),
	QueryString = 	require('querystring'),
	DynamoDB = 		require('dynamoDB').DynamoDB(Config.AWS_CREDENTIALS),
	Memcached = 	require('memcached'),
	Sanitizer = 	require('validator').sanitize,
	Validator = 	require('validator').check,
	Uuid = 			require('node-uuid');

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

// Cache //////////////////////////////////////////////////////////////////////

var Cache = (function() {
	var self = this;
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
		Log.l('GET:' + key);
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
	}

	return self;
})();

// Database ///////////////////////////////////////////////////////////////////

var Database = (function() {
	
})();

// Entry Methods //////////////////////////////////////////////////////////////

// Front door.
var init = function(request, response) {
	Log.l('init');
	process(request, response);
};

// Parse POST variables.
var process = function(request, response) {
	Log.l('process');
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

// Sanitize post vars.  Safe sex.
var sanitize = function(dirty, response) {
	Log.l(dirty);
	Log.l('sanitize');
	var clean = new Object();
	
	// Strings
	var allowedStrings = {
		'a': 1,
		'uid': 1,
		'android_id': 1,
		'device_id': 1,
		'carrier': 1
	};
	for (var param in allowedStrings) {
		Log.l(param);
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
		Log.l(param);
		if (param in dirty) {
			clean[param] = Sanitizer(dirty[param]).toInt();
		}
	}

	Log.l(clean);
	validate(clean, response);
};

// Strict whitelist validation.
var validate = function(dirty, response) {
	Log.l('validate');
	Log.l(dirty);
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

	Log.l(clean);
	route(clean, response);
};

var route = function(clean, response) {
	var action = clean['a'];
	switch(action) {
		case 'create_account':
			Log.l('create_account')
			createAccount(clean, response);
			break;
		default:
			break;
	}
};

// Action Logic ///////////////////////////////////////////////////////////////

var createAccount = function(clean, response) {
	Log.i('createAccount');
	var uid = clean['uid'];
	var androidId = clean['android_id'];
	var deviceId = clean['device_id'];
	var phoneNum = clean['phone_num'];
	var carrier = clean['carrier'];
	if (uid && androidId && deviceId && phoneNum && carrier) {
		var callback = function(hit) {
			if (hit) {
				


			} else {
				var tempUid = Uuid.v4();
				var json = {
					'code': 'create_account_0', 
					'uid': tempUid
				};
				var callback2 = function(put) {
					respond(json, response);
				};
				Cache.set(Config.PRE_CREATE_ACCOUNT_USER_TEMP_ID + tempUid, tempUid, 
					Config.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID, callback2);
			}
		};
		Cache.get(Config.PRE_CREATE_ACCOUNT_USER_TEMP_ID + uid, callback);
	}
};

// Exit Methods ///////////////////////////////////////////////////////////////

var respond = function(json, response) {
	if (response == null) {
		// We're in runTests()
		Log.l(JSON.stringify(json));
	} else {
		response.writeHead(200, {'Content-Type': 'application/json'});
		response.write(JSON.stringify(json));
		response.end();	
	}
};

// Test Suite /////////////////////////////////////////////////////////////////

var runTests = function() {
	// Create account.
	var post = {
		'a': 'create_account',
		'uid': 'abcd0123-ef45-6789-0000-abcdef012345',
		'android_id': '0123456789abcdef',
		'device_id': '0123456789ABCD',
		'phone_num': 1234567890,
		'carrier': 'Test Wireless'
	};
	sanitize(post, null);
};

// Bootstrap //////////////////////////////////////////////////////////////////

Log.l('server online'); 
Http.createServer(init).listen(80);
runTests();


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