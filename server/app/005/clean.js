////////////////////////////////////////////////////////////////////////////////
// 
// CLEAN:
// SANITIZE & VALIDATE
//
////////////////////////////////////////////////////////////////////////////////

var Sanitizer = require('validator').sanitize, 
	Validator = require('validator').check,
	Config =	require('./config'),
	Log = 		require('./log');

// Sanitize post vars.  Safe sex.
exports.sanitize = function(dirty, response, testCallback, callback) {
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
		'shout_id': 1,
		're': 1
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
		'vote': 1,
		'lvl': 1,
		'hint': 1
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

	// Scores Request
	var param = 'scores';
	if (param in dirty) {
		var cleanArray = [];
		dirty[param] = JSON.parse(dirty[param]);
		var lengthCap = Math.min(dirty[param].length, Config.SCORE_REQUEST_LIMIT);
		for (var i = 0; i < lengthCap; i++) {
			var reqScoreId = dirty[param][i];
			reqScoreId = Sanitizer(reqScoreId).trim();
			reqScoreId = Sanitizer(reqScoreId).xss();
			reqScoreId = Sanitizer(reqScoreId).entityEncode();
			if (reqScoreId.length > 0) {
				cleanArray.push(reqScoreId);
			}
		}
		clean[param] = cleanArray;
	}

	// trusted params
	clean['ip'] = dirty['ip'];

	var routingObject = {
		'post': clean,
		'response': response,
		'testCallback': testCallback	
	};
	callback(routingObject);
};

// Strict whitelist validation.
exports.validate = function(dirty, response, testCallback, callback) {
	var clean = {};
	var param;

	// a
	param = 'a';
	if (param in dirty) {
		var validActions = {'create_account':1, 'ping':1, 'shout':1, 'vote':1};
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
			clean[param] = dirty[param];
		}
	}

	// scores
	param = 'scores';
	if (param in dirty) {
		var cleanArray = [];
		for (var i = 0; i < dirty[param].length; i++) {
			var reqScoreId = dirty[param][i];
			if (reqScoreId.length == 36) {
				if (Validator(reqScoreId).isUUID()) {
					cleanArray.push(reqScoreId);
				}
			}
		}
		clean[param] = cleanArray;
	}

	// lvl
	param = 'lvl';
	if (param in dirty) {
		if (Validator(dirty[param]).isNumeric() &&
		Validator(dirty[param]).min(0) && 
		Validator(dirty[param]).max(1000)) {
			clean[param] = dirty[param];
		}
	}

	// hint
	param = 'hint';
	if (param in dirty) {
		if (Validator(dirty[param]).isNumeric() &&
		Validator(dirty[param]).min(0) && 
		Validator(dirty[param]).max(6371000)) { // 6371000 = radius earth
			clean[param] = dirty[param];
		}
	}

	// re
	param = 're';
	if (param in dirty) {
		if (dirty[param].length == 36) {
			if (Validator(dirty[param]).isUUID()) {
				clean[param] = dirty[param];
			}
		}
	}

	// trusted params
	clean['ip'] = dirty['ip'];
	
	var routingObject = {
		'post': clean,
		'response': response,
		'testCallback': testCallback	
	};
	callback(routingObject);
};