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
// RESOURCES:
// http://blog.mixu.net/2011/02/02/essential-node-js-patterns-and-snippets/
// http://nodejsmodules.org/tags/password
// http://docs.amazonwebservices.com/AWSRubySDK/latest/AWS/DynamoDB/AttributeCollection.html
// http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/API_GetItem.html
// http://www.paperplanes.de/2012/1/30/a-tour-of-amazons-dynamodb.html
// https://github.com/nodejitsu/forever
// http://stackoverflow.com/questions/8276132/how-come-node-js-doesnt-catch-my-errors
////////////////////////////////////////////////////////////////////////////////

// Includes ////////////////////////////////////////////////////////////////////

// Internal
var Log = 			require('./log'),
	Utils =			require('./utils'),
	Config =		require('./config'),
	User =			require('./user'),
	Shout =			require('./shout'),
	Storage =		require('./storage'),
	Clean =			require('./clean'),
	Cron = 			require('./cron');

// External (Modules)
var Http = 			require('http'),
	QueryString = 	require('querystring'),
	// https://github.com/chriso/node-validator
	Uuid = 			require('node-uuid'),
	Assert =		require('assert');

// Entry Methods ///////////////////////////////////////////////////////////////

// Uncaught Exceptions
process.on('uncaughtException', function (error) {
	Log.logError(error);
	Log.logError(error.stack);
});

// Front door.
var init = function(request, response) {	
	processRequest(request, response);	
};

// Parse POST variables.
var processRequest = function(request, response) {
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
			var callback = function(routingObject) {
				var objPost = routingObject['post'];
				var objResponse = routingObject['response'];
				var objTestCallback = routingObject['testCallback'];
				Clean.validate(objPost, objResponse, objTestCallback, callback2);
			};
			var callback2 = function(routingObject) {
				var objPost = routingObject['post'];
				var objResponse = routingObject['response'];
				var objTestCallback = routingObject['testCallback'];
				route(objPost, objResponse, objTestCallback);
			};
			Log.l('\n\n///////////////////////////////////////////////////////////////////////////\nREQUEST = ');
			POST['ip'] = request.connection.remoteAddress;
			Log.l(POST);
			Clean.sanitize(POST, response, null, callback);
        });
    } else {
    	var json = { 'status': 'online'};
    	response.writeHead(200, {'Content-Type': 'application/json'});
		response.write(JSON.stringify(json));
		response.end();	
    }
};

// Used by tests to skip front door.
var fakePost = function(dirty, response, testCallback) {
	Utils.sleep(1, function() {
		var callback = function(routingObject) {
			var objPost = routingObject['post'];
			var objResponse = routingObject['response'];
			var objTestCallback = routingObject['testCallback'];
			Clean.validate(objPost, objResponse, objTestCallback, callback2);
		};
		var callback2 = function(routingObject) {
			var objPost = routingObject['post'];
			var objResponse = routingObject['response'];
			var objTestCallback = routingObject['testCallback'];
			route(objPost, objResponse, objTestCallback);
		};
		Clean.sanitize(dirty, response, testCallback, callback);
	});
};

var route = function(clean, response, testCallback) {
	var action = clean['a'];
	switch(action) {
		case 'create_account':
			createAccount(clean, response, testCallback);
			break;
		case 'ping':
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
	if (typeof userId != 'undefined' &&
	typeof auth != 'undefined' &&
	typeof lat != 'undefined' &&
	typeof lng != 'undefined') {

		var json = {};
		var user = false;

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
				user = getUserResult;
				json['pts'] = user.points;
				var doCallback = true;
				if (user.pendingLevelUp > 0) {
					var skipLevelChange = false;
					if (typeof(level) != 'undefined') {
						if (level == user.level) {
							// User has achknowledged level up.
							skipLevelChange = true;
							doCallback = false;
							Storage.Users.acknowledgeLevelUp(user, callback4, 
								function() {
									var json = { 'code': 'error', 'txt': 'Could not achknowledge level up.' };
									respond(json, response, testCallback);
								}
							);
						}
					} 
					if (!skipLevelChange) {
						json['lvl_change'] = {
							'lvl': user.level,
							'lvl_at': Utils.pointsRequiredForLevel(user.level),
							'next_lvl_at': Utils.pointsRequiredForLevel(user.level + 1)
						};
					}
				}
				if (doCallback) {
					callback4();
				}				
			}
		};
		var callback4 = function() {
			if (reqRadius == 1) {
				Log.l('User requested radius.');
				Storage.LiveUsers.userCanRequestRadius(userId, callback45, 
					function() {
						var json = { 'code': 'error', 'txt': 'Radius request limit exceeded.' };
						respond(json, response, testCallback);	
					}
				);
			} else {
				callback5(false);
			}
		};
		var callback45 = function(userCanRequestRadiusResult) {
			Log.l('userCanRequestRadiusResult = ' + userCanRequestRadiusResult);
			if (userCanRequestRadiusResult) {
				Storage.LiveUsers.calculateRadiusOrFindTargets(false, user, null, lat, lng, callback5, 
					function() {
						var json = { 'code': 'error', 'txt': 'Could not calculate radius for shoutreach.' };
						respond(json, response, testCallback);
					}
				);	
			} else {
				// User keeps requesting radius, no longer allowed to get it.  Ignore request.
				callback5(false);
			}
				
		};
		var callback5 = function(calculateRadiusOrFindTargetsResult) {
			if (calculateRadiusOrFindTargetsResult) {
				json['radius'] = calculateRadiusOrFindTargetsResult;	
			}
			Storage.Inbox.checkInbox(userId, callback6,
				function() {
					Log.e('Could not check inbox.');
				}
			);
		};
		var callback6 = function(inboxContent) {
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
							sArray.push(Utils.buildShoutJson(shout, userId));
						}
						json['shouts'] = sArray;
						callback7();
					}
				}
				loop(0);
			} else {
				callback7();
			}
		};
		var callback7 = function() {
			Storage.Inbox.clearInbox(userId, callback8, 
				function() {
					var json = { 'code': 'error', 'txt': 'Could not clear user\'s inbox.' };
					respond(json, response, testCallback);
				}
			);	
		};
		var callback8 = function() {
			if (reqScores) {
				var pulledScores = [];
				var loop = function(index) {
					if (index < reqScores.length) {
						Storage.Shouts.getShout(reqScores[index], 
							function(shout) {
								Storage.Shouts.isExpired(shout, 
									function(isExpiredResult) {	
										pulledScores.push(shout);
										loop(index + 1);		
									},
									function() {
										var json = { 'code': 'error', 'txt': 'Could not check shout expiration.' };
										respond(json, response, testCallback);			
									}
								);
							},
							function() {
								var json = { 'code': 'error', 'txt': 'Could not get a shout from storage for requested scores.' };
								respond(json, response, testCallback);
							}
						);
					} else {
						var sArray = [];
						for (var key in pulledScores) {
							var shout = pulledScores[key];
							sArray.push(Utils.buildScoreJson(shout));
						}
						json['scores'] = sArray;
						callback9();
					}
				}
				loop(0);
			} else {
				callback9();
			}
		};
		var callback9 = function() {
			respond(json, response, testCallback);
		};
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
	var radiusHint = clean['hint'];
	var re = clean['re'];
	Log.l('shout re = ' + re);
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
		var callback2 = function(getUserResult) {
			if (getUserResult) {
				user = getUserResult;
				if (user.level >= shoutreach) {
					if (typeof(re) != 'undefined') {
						Log.l('this is a reply');
						// This is a reply.
						Storage.Replies.getRecipients(re, callback3, 
							function() {
								var json = { 'code': 'error', 'txt': 'Could not get recipients of parent shout for reply.' };
								respond(json, response, testCallback);
							}
						);
					} else {
						Log.l('this is not a reply');
						// This is a normal 'parent' shout.
						Storage.LiveUsers.calculateRadiusOrFindTargetsWithRadiusHint(true, user, shoutreach, lat, lng, radiusHint, callback3, 
							function() {
								var json = { 'code': 'error', 'txt': 'Error finding targets.' };
								respond(json, response, testCallback);
							}
						); 		
					}
				} else {
					var json = { 'code': 'error', 'txt': 'User does not have requested shoutreach.' };
					respond(json, response, testCallback);
				}
			}	
		};
		var callback3 = function(getTargetsResult) {
			Storage.Shouts.sendShout(user, getTargetsResult, shoutreach, lat, lng, text, re, clean['ip'], callback4,
				function() {
					var json = { 'code': 'error', 'txt': 'Send shout failed.' };
					respond(json, response, testCallback);
				}
			);
		};
		var callback4 = function(sendShoutResult) {
			var json = { 'code': 'shout_sent' };
			respond(json, response, testCallback);		
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
		};
		var callback3 = function(getShoutResult) {
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
		var callback4 = function(isExpiredResult) {	
			if (isExpiredResult) {
				var json = { 'code': 'error', 'txt': 'Cannot vote.  Shout is expired.' };
				respond(json, response, testCallback);	
			} else {
				Storage.Shouts.updateVoteCount(shout, vote, callback5,
					function() {
						var json = { 'code': 'error', 'txt': 'Shout does not exist or could not update vote count.' };
						respond(json, response, testCallback);	
					}
				);
			}
		};
		var callback5 = function(updateVoteCountResult) {
			Storage.Votes.addNewVote(userId, shoutId, vote, callback6,
				function() {
					var json = { 'code': 'error', 'txt': 'Could not create new vote.' };
					respond(json, response, testCallback);	
				}
			);
		};
		var callback6 = function(addNewVoteResult) {
			// WARNING - we hardcoded in level 1 here.
			Storage.Users.givePoints(userId, Utils.pointsForVote(1), callback7, 
				function() {
					var json = { 'code': 'error', 'txt': 'Could not give vote points to voter.' };
					respond(json, response, testCallback);			
				}
			);
		};
		var callback7 = function(givePointsResult) {
			// WARNING - we hardcoded in level 1 here.
			Storage.Users.givePoints(shout.userId, Utils.pointsForVote(1), callback8,
			function() {
					var json = { 'code': 'error', 'txt': 'Could not give vote points to sender.' };
					respond(json, response, testCallback);			
				}
			);
		};
		var callback8 = function() {
			var json = { 'code': 'vote_ok' };
			respond(json, response, testCallback);
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
				// Log.l('authIsValid------------------------------------');
				// Log.l('auth = ' + auth);
				// Log.l('submittedPw = ' + submittedPw);
				// Log.l('submittedHashChunk = ' + submittedHashChunk);
				// Log.l('pwHash = ' + pwHash);
				// Log.l('pwSalt = ' + pwSalt);
				// Log.l('nonce = ' + nonce);
				// Does password match?
				// Log.l('Utils.hashSha512(submittedPw + pwSalt) = ' + Utils.hashSha512(submittedPw + pwSalt));
				// Log.l('pwHash = ' + pwHash);	
				if (Utils.hashSha512(submittedPw + pwSalt) == pwHash) {
					// Does nonce match?
					// Log.l('Utils.hashSha512(submittedPw + nonce + userId) = ' + Utils.hashSha512(submittedPw + nonce + userId));
					// Log.l('submittedHashChunk = ' + submittedHashChunk);
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
				function(errorJson) {
					var resultObject = {
						'valid': false,
						json: errorJson
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
	Log.l('RESPONSE = ');
	Log.l(json);
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
		var newShouts = null;
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
				'phone_num': '', // 1234567890
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
				'a': 'ping',
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
			auth = pw + Utils.hashSha512(pw + nonce + userId);
			var post = {
				'a': 'ping',
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

		// Send a shout.
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

		// Go receive the shout.
		var test5 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'shout_sent');
			var post = {
				'a': 'ping',
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

		// Check its score.
		var test6 = function(json) {
			Log.l(json);
			newShouts = json['shouts'];
			sentShoutId = newShouts[0]['shout_id'];
			Assert.equal(json['code'], 'ping_ok');
			var post = {
				'a': 'ping',
				'uid': userId,
				'auth': auth,
				'lat': 40.00000,
				'lng': -70.00000,
				'scores': [sentShoutId]
			};
			Log.l('***********POST**************');
			Log.obj(post);
			Log.e('fakePost test7 go');
			fakePost(post, null, test7);
		};

		// Vote on it.
		var test7 = function(json) {
			Log.l(json);
			Assert.equal(json['scores'][0]['ups'], 0);
			var post = {
				'a': 'vote',
				'uid': userId,
				'auth': auth,
				'shout_id': sentShoutId,
				'vote': 1,
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test8);
		};

		// Check its new score.
		var test8 = function(json) {
			Log.l(json);
			Assert.equal(json['code'], 'vote_ok');
			var post = {
				'a': 'ping',
				'uid': userId,
				'auth': auth,
				'lat': 40.00000,
				'lng': -70.00000,
				'scores': [sentShoutId]
			};
			Log.l('***********POST**************');
			Log.obj(post);
			fakePost(post, null, test9);
		};	

		var test9 = function(json) {
			Log.l(json);
			Assert.equal(json['scores'][0]['ups'], 1);	
		};

		// Trigger first test...
		var post = { 'a': 'create_account' };
		fakePost(post, null, test1);
	};

	return this;
})();

// Bootstrap //////////////////////////////////////////////////////////////////

Log.l('Server launched.');
Http.createServer(init).listen(80);
Log.l('Listening...');

//Tests.run();

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