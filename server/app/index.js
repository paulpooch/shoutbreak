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

// Config //////////////////////////////////////////////////////////////////////

var Config = (function() {

	// Settings
	this.USER_INITIAL_POINTS = 400;
	this.USER_INITIAL_LEVEL = 5;
	this.USER_INITIAL_PENDING_LEVEL_UP = 5;
	this.PASSWORD_LENGTH = 32;
	this.PAD_COORDS = 8;
	this.MULTIPLY_COORDS = 100000;
	this.OFFSET_LAT = 90;
	this.OFFSET_LNG = 180;
	this.SHOUTREACH_BUFFER_METERS = 200;
	this.SHOUTREACH_LIMIT = 500;
	this.SHOUT_LENGTH_LIMIT = 256;
	this.SHOUT_IDLE_TIMEOUT = 60; // minutes
	this.SIMPLEDB_MAX_NUMBER_OF_ITEMS = 2500;
	this.SHOUTBREAK_SCORING_WORK_AT_LEVEL_1 = 10;
	this.SHOUTBREAK_SCORING_COEFFECIENT = 1.09;
	 
	// AWS
	this.CACHE_URL = 'cache-001.ardkb4.0001.use1.cache.amazonaws.com:11211',
	this.DYNAMODB_CREDENTIALS = {
		AccessKeyId:'AKIAINHDEIZ3QVSHQ3PA', 
		SecretKey: 	'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'
	};
	this.SIMPLEDB_CREDENTIALS = {
		keyid: 		'AKIAINHDEIZ3QVSHQ3PA', 
		secret: 	'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'
	};
	
	// Cache Keys
	this.PRE_CREATE_ACCOUNT_USER_TEMP_ID = 		'tempuserid';
	this.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID = 	300; // 5 minutes
	this.PRE_ACTIVE_AUTH = 						'activeauth';
	this.TIMEOUT_ACTIVE_AUTH = 					1800; // 30 minutes
	this.PRE_USER =								'user';
	this.TIMEOUT_USER =							1800; // 30 minutes
	this.PRE_SHOUT =							'shout';
	this.TIMEOUT_SHOUT =						1800; // 30 minutes
	this.PRE_INBOX =							'inbox';
	this.TIMEOUT_INBOX =						1800; // 30 minutes
	this.PRE_VOTE =								'vote';
	this.TIMEOUT_VOTE = 						300; // 5 minutes
	
	// Tables
	this.TABLE_USERS = 'USERS';
	this.TABLE_LIVE = 'LIVE';
	this.TABLE_SHOUTS = 'SHOUTS';
	this.TABLE_VOTES = 'VOTES';

	return this;
})();

// Includes ////////////////////////////////////////////////////////////////////

var Http = 			require('http'),
	QueryString = 	require('querystring'),
	// https://github.com/xiepeng/dynamoDB
	DynamoDB = 		require('dynamoDB').DynamoDB(Config.DYNAMODB_CREDENTIALS),
	SimpleDB =		require('simpledb'),
	Memcached = 	require('memcached'),
	// https://github.com/chriso/node-validator
	Sanitizer = 	require('validator').sanitize, 
	Validator = 	require('validator').check,
	Uuid = 			require('node-uuid'),
	Crypto =		require('crypto'),
	Assert =		require('assert');

// Utils ///////////////////////////////////////////////////////////////////////

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
			'0123456789_!@#$%*()-=+/abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
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

	this.sleep = function(seconds, callback) {
		var startTime = new Date().getTime();
		while (new Date().getTime() < startTime + (seconds * 1000));
		callback();
  	};

  	this.getNowISO = function() {
  		return (new Date().toISOString());
  	};

  	this.formatLatForSimpleDB = function(lat) {
  		lat += Config.OFFSET_LAT;
  		return Utils.pad(Math.round(lat) * Config.MULTIPLY_COORDS, Config.PAD_COORDS);
  	};

	this.formatLngForSimpleDB = function(lng) {
  		lng += Config.OFFSET_LNG;
  		return Utils.pad(Math.round(lng) * Config.MULTIPLY_COORDS, Config.PAD_COORDS);
  	};

  	this.pad = function(val, digits) {
  		val = Math.round(val);
  		if (val.length > digits) {
  			if (val < 0) {
  				digits++;
  			}
  		}
 		while (val.length < digits) {
 			val = '0' + val;
 		}
 		return String(val);
  	};

 	if (typeof(Number.prototype.toRad) === 'undefined') {
  		Number.prototype.toRad = function() {
    		return this * Math.PI / 180;
  		};
	}

	// http://stackoverflow.com/questions/27928/how-do-i-calculate-distance-between-two-latitude-longitude-points
  	this.distanceBetween = function(lat1, lng1, lat2, lng2) {
  		var R = 6371; // Radius of the earth in km
		var dLat = (lat2 - lat1).toRad();  // Javascript functions in radians
		var dLon = (lng2 - lng1).toRad(); 
		var a = Math.sin(dLat / 2) * Math.sin( dLat / 2) +
			Math.cos(lat1.toRad()) * Math.cos(lat2.toRad()) * 
        	Math.sin(dLon / 2) * Math.sin(dLon / 2); 
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); 
		var d = R * c; // Distance in km
		return d;
  	};

  	// Adapter from simpledb.js
  	this.simpleDBLogger = function(date, type) {
  		var strs = ['simpledb: ', date.toISOString(), type];
  		for (var aI = 2; aI < arguments.length; aI++) {
  			var a = arguments[aI];
  			strs.push('object' == typeof(a) ? JSON.stringify(a) : '' + a);
  		}
  		//util.debug(strs.join(' '));
  		//Log.i(strs.join(' '));
  	};

  	this.buildShoutJson = function(shout, forUserId) {
		var sObj = {
			'shout_id': shout.shoutId,
			'txt': shout.text,
			'ts': shout.time,
			'hit': shout.hit
		};
		if (forUserId == shout.userId) {
			sObj['outbox'] = 1;
		}
		return sObj;
	};

	this.makeShoutFromDynamoItem = function(item) {
		var shout = new Shout();
		shout.shoutId = item['shout_id']['S'];
		shout.userId = item['user_id']['S'];
		shout.time = item['time']['S'];
		shout.lastActivityTime = item['last_activity_time']['S'];
		shout.text = item['text']['S'];
		shout.open = parseInt(item['open']['N']);
		shout.re = item['re']['S'];
		shout.hit = parseInt(item['hit']['N']);
		shout.power = parseInt(item['power']['N']);
		shout.ups = parseInt(item['ups']['N']);
		shout.downs = parseInt(item['downs']['N']);
		shout.lat = parseInt(item['lat']['N']);
		shout.lng = parseInt(item['lng']['N']);
		return shout;
	};

	this.maxTargetsAtLevel = function(level) {
		return Math.min((level * 5), Config.SIMPLEDB_MAX_NUMBER_OF_ITEMS)
	};

	this.reachAtLevel = function(level) {
		return Utils.maxTargetsAtLevel(level);
	};

	this.workRequiredForLevel = function(level) {
		if (level == 1) {
			return Config.SHOUTBREAK_SCORING_WORK_AT_LEVEL_1;
		} else {
			return Config.SHOUTBREAK_SCORING_COEFFECIENT * Utils.workRequiredForLevel(level - 1);
		}
	};

	this.pointsRequiredForLevel = function(level) {
		var result = Math.round(Utils.workRequiredForLevel(level) * Utils.reachAtLevel(level));
		return result;	
	};

	this.pointsForVote = function(level) {
		return 1;
	};

	return this;
})();

// More Includes ///////////////////////////////////////////////////////////////

var SimpleDB = SimpleDB.SimpleDB(Config.SIMPLEDB_CREDENTIALS, Utils.simpleDBLogger);

// User ////////////////////////////////////////////////////////////////////////

var User = function() {
	this.userId = null;
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
};

// Shout ///////////////////////////////////////////////////////////////////////

var Shout = function() {
	this.shoutId = null;
	this.userId = null;	
	this.time = null;
	this.lastActivityTime = null;
	this.text = null;
	this.open = null;
	this.re = null;
	this.hit = null;
	this.power = null;
	this.ups = null;
	this.downs = null;
	this.lat = null;
	this.lng = null;
	this.outbox = null;
};

// Storage /////////////////////////////////////////////////////////////////////

var Storage = (function() {

	// Cache ///////////////////////////////////////////////////////////////////

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
		this.get = function(key, successCallback, failCallback) {
			memcached.get(key, function(err, result) {
				if (err) {
					Log.e(err);
					failCallback();	
				} else {
					successCallback(result);
				}
			});
		};
		this.set = function(key, value, lifetime, successCallback, failCallback) {
			memcached.set(key, value, lifetime, function(err, result) {
				if (err) {
					Log.e(err);
					failCallback();
				} else {
					// This will be true if successful.
					successCallback(result);
				}
			});
		};
		this.delete = function(key, successCallback, failCallback) {
			memcached.del(key, function(err, result) {
				if (err) {
					Log.e(err);
					failCallback();
				} else {
					successCallback(result);
				}
			});
		};
		return this;
	})();

	this.setTempAccount = function(tempUid, successCallback, failCallback) {
		Cache.set(Config.PRE_CREATE_ACCOUNT_USER_TEMP_ID + tempUid, tempUid, 
			Config.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID, successCallback, failCallback);
	};

	this.getTempAccount = function(userId, successCallback, failCallback) {
		Cache.get(Config.PRE_CREATE_ACCOUNT_USER_TEMP_ID + userId, successCallback, failCallback);	
	};

	this.getAuth = function(userId, successCallback, failCallback) {
		Cache.get(Config.PRE_ACTIVE_AUTH + userId, successCallback, failCallback);
	};

	this.deleteAuth = function(userId, successCallback, failCallback) {
		Cache.delete(Config.PRE_ACTIVE_AUTH + userId, successCallback, failCallback);
	};

	// Inbox ///////////////////////////////////////////////////////////////////

	this.Inbox = (function() {
		
		this.checkInbox = function(userId, successCallback, failCallback) {
			Cache.get(Config.PRE_INBOX + userId, successCallback, failCallback);	
		};

		this.addToInbox = function(userId, shoutId, successCallback, failCallback) {
			var callback = function(inboxArray) {
				var targetInbox = [];
				if (inboxArray) {
					targetInbox = inboxArray;
				}
				targetInbox.push(shoutId);
				Cache.set(Config.PRE_INBOX + userId, targetInbox, Config.TIMEOUT_INBOX, successCallback, failCallback);
			};
			Inbox.checkInbox(userId, callback, failCallback);
		};			

		return this;
	})();

	// Shouts //////////////////////////////////////////////////////////////////

	this.Shouts = (function() {

		this.getShout = function(shoutId, successCallback, failCallback) {
			Log.e('getShout');
			var returnShout;
			var callback2 = function(response) {
    			response.on('data', function(chunk) {
    				var item = JSON.parse(chunk);
    				if (item['Item']) {
    					item = item['Item'];
    					var shout = Utils.makeShoutFromDynamoItem(item);
    					Log.obj('makeShoutFromDynamoItem');
    					Log.obj(shout);
						addToCache(shout);
       				} else {
						Log.e(chunk);
						failCallback();
       				}
    			});
    		};
			var callback = function(getResult) {
				if (getResult) {
					Log.e('shout found in cache');
					Log.obj(getResult);
					successCallback(getResult);
				} else {
					var req = {
						'TableName': Config.TABLE_SHOUTS,
						'Key': {
							'HashKeyElement': {'S': shoutId}
						},
						'AttributesToGet': [
							'shout_id',
							'user_id',
							'time',
							'text',
							'open',
							're',
							'hit',
							'power',
							'ups',
							'downs',
							'lat',
							'lng'
						],
						'ConsistentRead': true
					};
					DynamoDB.getItem(req, callback2);
				}
			};
			var callback3 = function(setResult) {
				// We don't really care about the result.
				Log.e('Returning Shout from getShout');
				Log.obj(returnShout);
				successCallback(returnShout);
			};	
			var addToCache = function(shout) {
				Log.e('addToCache');
				Log.obj(shout);
				returnShout = shout;
				Cache.set(Config.PRE_SHOUT + shout.shoutId, shout, Config.TIMEOUT_SHOUT, callback3, 
					function() {
						// Not a huge problem.
						Log.e('Could not save shout to cache.');
						callback3(false);
					}
				);
			};
			Cache.get(Config.PRE_SHOUT + shoutId, callback,
				function() {
					// Ok to fail this get.
					callback(false);
				}
			);
		};

		this.addNewShout = function(shout, successCallback, failCallback) {
			var data = {
				'TableName': Config.TABLE_SHOUTS,
				'Item': {
					'shout_id': 			{'S': shout.shoutId},
					'user_id': 				{'S': shout.userId},
					'time': 				{'S': shout.time},
					'last_activity_time': 	{'S': shout.lastActivityTime},
					'text': 				{'S': shout.text},
					'open': 				{'N': String(shout.open)},
					're': 					{'S': String(shout.re)},
					'hit': 					{'N': String(shout.hit)},
					'power': 				{'N': String(shout.power)},
					'ups': 					{'N': String(shout.ups)},
					'downs': 				{'N': String(shout.downs)},
					'lat': 					{'N': String(shout.lat)},
					'lng': 					{'N': String(shout.lng)}
				}
			};
			var callback2 = function(setResult) {
				successCallback(true);
			};
			var cacheShout = function() {
				Cache.set(Config.PRE_SHOUT + shout.shoutId, shout, Config.TIMEOUT_SHOUT, callback2, 
					function() {
						Log.e('Could not save shout to cache.');
						callback2(false);
					}
				);
			};
			var callback = function(result) {
				result.on('data', function(chunk) {
					Log.obj(chunk + '');
					chunk = JSON.parse(chunk);
					if (chunk['ConsumedCapacityUnits']) {
						cacheShout();
					} else {
						Log.e(String(chunk));
						failCallback();
					}
				});
				result.on('ready', function(data) {
					Log.e(data.error);
					failCallback();
				});
			};
			DynamoDB.putItem(data, callback);
		};

		this.sendShout = function(user, targets, shoutreach, lat, lng, text, successCallback, failCallback) {
			
			var shout = new Shout();
			shout.shoutId = Uuid.v4();
			shout.userId = user.userId;
			shout.time = Utils.getNowISO();
			shout.lastActivityTime = Utils.getNowISO();
			shout.text = text;
			shout.open = 1;
			shout.re = 0;
			shout.hit = targets.length;
			shout.power = shoutreach;
			shout.ups = 0;
			shout.downs = 0;
			shout.lat = Utils.formatLatForSimpleDB(parseFloat(lat));
			shout.lng = Utils.formatLngForSimpleDB(parseFloat(lng));

			// Let's manually add the send to the list of targets.
			targets.push([0, shout.userId]);

			var loop = function(index) {
				if (index < targets.length) {
					var targetId = targets[index][1];
					Storage.Inbox.addToInbox(targetId, shout.shoutId, 
						function() {
							loop(index + 1);		
						},
						function() {
							Log.e('Could not put shout in target inbox.');
							failCallback();
						}
					);
				} else {
					successCallback();
				}
			}
			var callback = function(addNewShoutResult) {
				loop(0);
			}
			Storage.Shouts.addNewShout(shout, callback, 
				function() {
					Log.e('Unable to save shout.');
					failCallback();	
				}
			);
		};

		// pass the full shout object into here
		this.updateVoteCount = function(shout, vote, successCallback, failCallback) {
			if (vote == -1 || vote == 1) {
				var callback = function(setResult) {
					successCallback();
				};
				if (vote == -1) {
					shout.downs++;
				} else {
					shout.ups++;
				}
				var req = {
					'TableName': Config.TABLE_SHOUTS,
					'Key': {
						'HashKeyElement': {'S': shout.shoutId}
					},
					'AttributeUpdates': {
						'ups': {
							'Value': {'N': String(vote)},
							'Action': 'ADD'
						}
					},
					'ReturnValues': 'NONE'
				};
				Storage.Shouts.updateShout(shout, req, callback, failCallback);
			} else {
				failCallback();
			}
		};

		this.isExpired = function(shout, successCallback, failCallback) {
			var d2 = new Date();
			var d1 = new Date(shout.lastActivityTime);
			var idleTime = (d2 - d1) / 60000; // = minutes
			Log.e('idleTime = ' + idleTime);
			if (idleTime > Config.SHOUT_IDLE_TIMEOUT) {
				shout.open = 0;
				var callback = function(updateShoutResult) {
					successCallback(true);
				};
				var req = {
					'TableName': Config.TABLE_SHOUTS,
					'Key': {
						'HashKeyElement': {'S': shout.shoutId}
					},
					'AttributeUpdates': {
						'open': {
							'Value': {'N': String(shout.open)}
						}
					},
					'ReturnValues': 'NONE'
				};
				Storage.Shouts.updateShout(shout, req, callback, failCallback);
			} else {
				successCallback(false);
			}
		};

		this.updateShout = function(shout, dynamoRequest, successCallback, failCallback) {
			var callback2 = function(setResult) {
				successCallback();
			};
			var callback = function(response) {
				response.on('data', function(chunk) {
	    			//	var item = JSON.parse(chunk);
	    			//	if (item['Attributes']) {
	    			//		item = item['Attributes'];
	    			// 		returnShout = Utils.makeShoutFromDynamoItem(item); }
	    			Cache.set(Config.PRE_SHOUT + shout.shoutId, shout, Config.TIMEOUT_SHOUT, callback2, 
						function() {
							Log.e('Could not save shout to cache.');
							failCallback();
						}
					);
	       		});	
			};
			DynamoDB.updateItem(dynamoRequest, callback);
		};

		return this;
	})();

	// Users ///////////////////////////////////////////////////////////////////

	this.Users = (function() {

		this.addNewUser = function(user, successCallback, failCallback) {
			var data = {
				'TableName': Config.TABLE_USERS,
				'Item': {
					'user_id': 				{'S': user.userId},
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
					chunk = JSON.parse(chunk);
					if (chunk['ConsumedCapacityUnits']) {
						successCallback(true);
					} else {
						Log.e(String(chunk));
						failCallback();
					}
				});
				result.on('ready', function(data) {
					Log.e(data.error);
					failCallback();
				});
			};
			DynamoDB.putItem(data, callback);
		};

		this.getUser = function(userId, successCallback, failCallback) {
			var returnUser;
			var callback2 = function(response) {
    			response.on('data', function(chunk) {
    				var item = JSON.parse(chunk);
    				if (item['Item']) {
    					item = item['Item'];
    					var user = new User();
    					user.userId = item['user_id']['S'];
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
						Log.e('got user from db');
       					addToCache(user);
       				} else {
						Log.e(chunk);
						failCallback();
       				}
    			});
    		};
			var callback = function(getResult) {
				if (getResult) {
					successCallback(getResult);
				} else {
					var req = {
						'TableName': Config.TABLE_USERS,
						'Key': {
							'HashKeyElement': {'S': userId}
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
					DynamoDB.getItem(req, callback2);
				}
			};
			var callback3 = function(setResult) {
				// We don't really care about the result.
				successCallback(returnUser);
			};	
			var addToCache = function(user) {
				returnUser = user;
				Log.e('addToCache');
				Cache.set(Config.PRE_USER + user.userId, user, Config.TIMEOUT_USER, callback3, 
					function() {
						// Not a huge problem.
						Log.e('Could not save user to cache.');
						callback3(false);
					}
				);
			};
			Cache.get(Config.PRE_USER + userId, callback,
				function() {
					// Ok to fail this get.
					callback(false);
				}
			);
		};

		this.generateNonce = function(userId, successCallback, failCallback) {
			var callback = function(getUserResult) {
				if (getUserResult) {
					if (getUserResult.userPwHash && getUserResult.userPwSalt) {
						var nonce = Utils.generatePassword(40);
						var authInfo = {'pw_hash': getUserResult.userPwHash, 'pw_salt': getUserResult.userPwSalt, 'nonce': nonce};
						var callback2 = function(setResult) {
							if (setResult) {
								var now = Utils.getNowISO();
								var callback3 = function(updateLastActivityTimeResult) {
									successCallback(nonce);
								};
								Storage.Users.updateLastActivityTime(userId, now, callback3, 
									function() {
										// Not much to do here.
										Log.e('updateLastActivityTime failed.');
									}
								);
							}
						};
						Cache.set(Config.PRE_ACTIVE_AUTH + userId, authInfo,
							Config.TIMEOUT_ACTIVE_AUTH, callback2, 
							function() {
								failCallback()
							}
						);
					}else {
						failCallback();
					}
				} else {
					failCallback();
				}
			};
			Storage.Users.getUser(userId, callback, 
				function() {
					failCallback();
				}
			);
		};

		this.givePoints = function(userId, pointsAmount, successCallback, failCallback) {
			var user = null;
			var req = null;
			var callback3 = function() {
				// Begin here Sunday.
				// Need to create Users.setLevelUp	
			};
			var callback2 = function() {
				Storage.Users.updateUser(user, req, callback3, failCallback);
			};
			var callback = function(getUserResult) {
				user = getUserResult;
				user.points += pointsAmount;
				var levelChange = false;
				var levelDiff = 0;
				if (user.points > Utils.pointsRequiredForLevel(user.level + 1)) {
					user.level++;
					levelDiff = 1;
					levelChange = true;
				} else if (user.points < Utils.pointsRequiredForLevel(user.level - 1)) {
					user.level--;
					levelDiff = -1;
					levelChange = true;
				}
				if (levelChange) {
					var req = {
						'TableName': Config.TABLE_USERS,
						'Key': {
							'HashKeyElement': {'S': userId}
						},
						'AttributeUpdates': {
							'points': {
								'Value': {'N': String(pointsAmount)},
								'Action': 'ADD'
							},
							'level': {
								'Value': {'N': String(levelDiff)},
								'Action': 'ADD'
							},
							'pending_level_up': {
								'Value': {'N': String(user.level)},
							}
						}
						'ReturnValues': 'NONE'
					};
					Storage.Users.setLevelUp(user, user.level, callback2, failCallback);
				} else {
					var req = {
						'TableName': Config.TABLE_USERS,
						'Key': {
							'HashKeyElement': {'S': userId}
						},
						'AttributeUpdates': {
							'points': {
								'Value': {'N': String(pointsAmount)},
								'Action': 'ADD'
							}
						}
						'ReturnValues': 'NONE'
					};
					callback2();
				}
			};
			Storage.Users.getUser(userId, callback, failCallback);
		};

		// We won't use updateUser here 
		this.updateLastActivityTime = function(userId, time, successCallback, failCallback) {
			var req = {
				'TableName': Config.TABLE_USERS,
				'Key': {
					'HashKeyElement': {'S': userId}
				},
				'AttributeUpdates': {
					'last_activity_time': {'S': time}
				},
				'ReturnValues': 'NONE'
			};
			var callback = function(response, result) {
				response.on('data', successCallback);
			};
			DynamoDB.updateItem(data, callback);
		};

		this.updateUser = function(user, dynamoRequest, successCallback, failCallback) {
			var callback2 = function(setResult) {
				successCallback();
			};
			var callback = function(response) {
				response.on('data', function(chunk) {
	    			//	var item = JSON.parse(chunk);
	    			//	if (item['Attributes']) {
	    			//		item = item['Attributes'];
	    			// 		returnShout = Utils.makeShoutFromDynamoItem(item); }
	    			Cache.set(Config.PRE_USER + user.userId, user, Config.TIMEOUT_USER, callback2, 
						function() {
							Log.e('Could not save user to cache.');
							failCallback();
						}
					);
	       		});	
			};
			DynamoDB.updateItem(dynamoRequest, callback);
		};

		return this;
	})();

	// Live Users //////////////////////////////////////////////////////////////

	this.LiveUsers = (function() {
		
		this.putUserOnline = function(userId, lat, lng, successCallback, failCallback) {
			var callback = function(error, result, metadata) {
				if (result) {
					if (error != null) {
						failCallback();
					} else {
						successCallback(true);
					}
				}
			};
			var now = Utils.getNowISO();
			var pLat = Utils.formatLatForSimpleDB(lat);
			var pLng = Utils.formatLngForSimpleDB(lng);
			SimpleDB.putItem(Config.TABLE_LIVE, userId, {
				'user_id': userId,
				'ping_time': now,
				'lat': pLat,
				'lng': pLng
			}, callback);
		};

		this.calculateRadiusOrFindTargets = function(isShouting, user, shoutreach, lat, lng, successCallback, failCallback) {
			
			var SELECT_LIMIT = 20;
			var selectCount = 0;
			var xMin = 0, yMin = 0, xMax = 36000000, yMax = 18000000;
			var x0, x1, x2, x3, y0, y1, y2, y3, xDelta, yDelta, xOffset, yOffset;
			if (!isShouting) {
				var shoutreach = user.level;
			}
			var acceptableExtra = 40;
			var acceptableExtraIncrement = 20;
			var xWrap = false, yWrap = false;
			var xUser = Utils.formatLatForSimpleDB(lat);
			var yUser = Utils.formatLngForSimpleDB(lng);

			var xCenter = xMax / 2;
			var yCenter = yMax / 2;
			xCenter = Utils.formatLatForSimpleDB(lat);
			xCenter = Utils.formatLngForSimpleDB(lng);

			var createCleanBounds = function() {
				xWrap = false;
				yWrap = false;

				// Step 1 - expand bounding box from center of map.
				// Possibly hit full size of map.
				x0 = xCenter - xDelta;
				x1 = xCenter + xDelta;
				y0 = yCenter - yDelta;
				y1 = yCenter + yDelta;
				x0 = (x0 < xMin) ? xMin : x0;
				x1 = (x1 > xMax) ? xMax : x1;
				y0 = (y0 < yMin) ? yMin : y0;
				y1 = (y1 > yMax) ? yMax : y1;
				
				// Step 2 - shift bounding box to user location.
				xOffset = xUser - xCenter;
				yOffset = yUser - yCenter;
				x0 += xOffset;
				x1 += xOffset;
				y0 += yOffset;
				y1 += yOffset;

				// Step 3 - did we just cause wrap around?
				// Wrap on left border.
				if (x0 < xMin) {
					xWrap = true;
					x2 = xMax - (xMin - x0);
					x3 = xMax;
					x0 = xMin;
				}
				// Wrap on right border.				
				if (x1 > xMax) {
					xWrap = true;
					x3 = xMin + (x1 - xMax);
					x2 = xMin;
					x1 = xMax;
				}
				// Wrap on bottom border.
				if (y0 < yMin) {
					yWrap = true;
					y2 = yMax - (yMin - y0);
					y3 = yMax;
					y0 = yMin;
				}
				// Wrap on top border.				
				if (y1 > yMax) {
					yWrap = true;
					y3 = yMin + (y1 - yMax);
					y2 = yMin;
					y1 = yMax;
				}

				// Step 4 - format everything for simple db.
				x0 = String(x0);
				x1 = String(x1);
				x2 = String(x2);
				x3 = String(x3);
				y0 = String(y0);
				y1 = String(y1);
				y2 = String(y2);
				y3 = String(y3);
				x0 = Utils.pad(x0, Config.PAD_COORDS);
				x1 = Utils.pad(x1, Config.PAD_COORDS);
				x2 = Utils.pad(x2, Config.PAD_COORDS);
				x3 = Utils.pad(x3, Config.PAD_COORDS);
				y0 = Utils.pad(y0, Config.PAD_COORDS);
				y1 = Utils.pad(y1, Config.PAD_COORDS);
				y2 = Utils.pad(y2, Config.PAD_COORDS);
				y3 = Utils.pad(y3, Config.PAD_COORDS);

			};

			var fullGetCallback = function(error, result, metadata) {
				var nearby = [];
				var nearbySorter = function(a, b) {
					return b[0] - a[0];	
				};
				if (!isShouting && result.length < shoutreach) {
					// We have a problem.
					Log.e('calculateRadiusOrFindTargets failed to find enough users.');
					failCallback();
				} else {
					for (var row in result) {
						row = result[row];
						var lat2 = (row['lat'] - Config.OFFSET_LAT) / Config.MULTIPLY_COORDS; 
						var lng2 = (row['lng'] - Config.OFFSET_LNG) / Config.MULTIPLY_COORDS;
						var distanceKm = Utils.distanceBetween(lat, lng, lat2, lng2);
						nearby.push([distanceKm, row['user_id']]);
					}
					nearby.sort(nearbySorter);
					
					if (isShouting) {
						var targets = [];
						var targetCount = 0;
						for (var i = 0; i < nearby.length && targetCount < shoutreach; i++) {
							if (nearby[i][1] != user.userId) {
								targets.push(nearby[i]);
								targetCount++;
							}
						}
						successCallback(targets);
					} else {
						var radius = nearby[user.level][0];
						radius *= 1000;
						radius += Config.SHOUTREACH_BUFFER_METERS;
						radius = Math.round(radius);
						successCallback(radius);
					}

				}
			};

			var performSelect = function(justGetCount, callback) {
				createCleanBounds();
				var params = [];
				var sql = "SELECT ";
				if (justGetCount) {
					sql += "count(*) FROM ? WHERE ";
				} else {
					sql += "* FROM ? WHERE ";
				}
				params.push(Config.TABLE_LIVE);
				if (xWrap) {
					sql += "(lng BETWEEN '?' AND '?' OR lng BETWEEN '?' AND '?') ";
					params.push(x0, x1, x2, x3);
				} else {
					sql += "(lng BETWEEN '?' AND '?') ";
					params.push(x0, x1);
				}
				sql += "AND ";
				if (yWrap) {
					sql += "(lat BETWEEN '?' AND '?' OR lat BETWEEN '?' AND '?') ";
					params.push(y0, y1, y2, y3);
				} else {
					sql += "(lat BETWEEN '?' AND '?') ";
					params.push(y0, y1);
				}
				/*
				SimpleDB.select("SELECT count(*) FROM ? WHERE lat BETWEEN '?' AND '?' AND lng BETWEEN '?' AND '?'",
					[Config.TABLE_LIVE, x0, x1, y0, y1], recursiveCallback);
				*/
				SimpleDB.select(sql, params, callback);
			};

			var recursiveCallback = function(error, result, metadata) {
				// Let's not accidentally infinite loop and spend $50,000 on AWS.
				selectCount++;
				Log.e('recursiveCallback on iteration ' + selectCount);
				var makeNextSelect = true;
				var count = result[0]['Count'];
				Log.e('COUNT = ' + count + ', SHOUTREACH = ' + shoutreach);
				if (count >= shoutreach) {
					if (count - shoutreach <= acceptableExtra) {
						makeNextSelect = false;
						performSelect(false, fullGetCallback);
					} else {
						// Go smaller.
						// Important!
						// The multiple for go smaller cannot equal the one for go bigger,
						// or we could infinitely flip between too many and not enough.
						xDelta /= 3;
						yDelta /= 3;
					}
				} else {
					// Go bigger.
					xDelta *= 2;
					yDelta *= 2;
				}
				if (makeNextSelect) {
					if (selectCount <= SELECT_LIMIT) {
						acceptableExtra += acceptableExtraIncrement;
						performSelect(true, recursiveCallback);
					} else {
						// We gotta bail to avoid infinite selects.
						Log.e('INFINITE LOOP WOULD HAVE OCCURED IN recursiveCallback');
						performSelect(false, fullGetCallback);
					}
				}
			};

			// First try.
			xDelta = xMax / 2; // TODO: Change these.
			yDelta = yMax / 2;
			performSelect(true, recursiveCallback);
		};

		return this;
	})();

	// Votes ///////////////////////////////////////////////////////////////////
	this.Votes = (function() {
		
		this.getVote = function(shoutId, userId, successCallback, failCallback) {
			var returnVote;
			var callback2 = function(response) {
    			response.on('data', function(chunk) {
    				var item = JSON.parse(chunk);
    				if (item['Item']) {
    					item = item['Item'];
    					var vote = [item['vote'], item['time']];
    					addToCache(vote);
       				} else {
       					// Vote does not exist.
      					// That's ok though.
						successCallback(false);
       				}
    			});
    		};
			var callback = function(getResult) {
				if (getResult) {
					successCallback(getResult);
				} else {
					var req = {
						'TableName': Config.TABLE_VOTES,
						'Key': {
							'HashKeyElement': {'S': userId},
							'RangeKeyElement': {'S': shoutId}
						},
						'AttributesToGet': [
							'vote'
						],
						'ConsistentRead': true
					};
					DynamoDB.getItem(req, callback2);
				}
			};
			var callback3 = function(setResult) {
				// We don't really care about the result.
				successCallback(returnVote);
			};	
			var addToCache = function(vote) {
				returnVote = vote;
				Cache.set(Config.PRE_VOTE + userId + shoutId, vote, Config.TIMEOUT_VOTE, callback3, 
					function() {
						// Not a huge problem.
						Log.e('Could not save vote to cache.');
						callback3(false);
					}
				);
			};
			Cache.get(Config.PRE_VOTE + userId + shoutId, callback,
				function() {
					// Ok to fail this get.
					callback(false);
				}
			);
		};

		this.addNewVote = function(userId, shoutId, vote, successCallback, failCallback) {
			var now = Utils.getNowISO();
			var data = {
				'TableName': Config.TABLE_VOTES,
				'Item': {
					'shout_id': {'S': shoutId},
					'user_id': 	{'S': userId},
					'vote': 	{'N': String(vote)},
					'time': 	{'S': now}
				}
			};
			var callback2 = function(setResult) {
				successCallback(true);
			};
			var cacheVote = function() {
				Cache.set(Config.PRE_VOTE + userId + shoutId, vote, Config.TIMEOUT_VOTE, callback2, 
					function() {
						// Not a huge problem.
						Log.e('Could not save vote to cache.');
						callback2(false);
					}
				);
			};
			var callback = function(result) {
				result.on('data', function(chunk) {
					Log.obj(chunk + '');
					chunk = JSON.parse(chunk);
					if (chunk['ConsumedCapacityUnits']) {
						cacheVote();
					} else {
						Log.e(String(chunk));
						failCallback();
					}
				});
				result.on('ready', function(data) {
					Log.e(data.error);
					failCallback();
				});
			};
			DynamoDB.putItem(data, callback);
		};

		return this;
	})();

	return this;
})();

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
			if (putUserOnlineResult) {
				json['code'] = 'ping_ok';
				if (reqRadius == 1) {
					Storage.Users.getUser(userId, callback3, 
						function() {
							var json = { 'code': 'error', 'txt': 'Could not get user.' };
							respond(json, response, testCallback);
						}
					);
				} else {
					callback4(true);
				}
			}
		};
		var callback3 = function(getUserResult) {
			if (getUserResult) {
				Storage.LiveUsers.calculateRadiusOrFindTargets(false, getUserResult, null, lat, lng, callback4, 
					function() {
						var json = { 'code': 'error', 'txt': 'Could not calculate radius for shoutreach.' };
						respond(json, response, testCallback);
					}
				);
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

		var callback6 = function(addNewVoteResult) {
			// Begin here Thursday.
			// We must update User's points
			// See if they leveled up...
			// Both shouter and voter.... fuck.
			Log.e("YAY!");
			Log.obj(addNewVoteResult

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