////////////////////////////////////////////////////////////////////////////////
// 
// STORAGE
//
////////////////////////////////////////////////////////////////////////////////
module.exports = (function() {
	var self = this;

	// Includes ////////////////////////////////////////////////////////////////
	// https://github.com/xiepeng/dynamoDB
	// https://github.com/rjrodger/simpledb
	var	Config =	require('./config'),
		Utils = 	require('./utils'),
		User =		require('./user'),
		Log =		require('./log'),
		Shout =		require('./shout'),
		Uuid = 		require('node-uuid'),
		DynamoDB = 	require('dynamoDB').DynamoDB(Config.DYNAMODB_CREDENTIALS),
		Memcached = require('memcached'),
		SimpleDB =	require('simpledb').SimpleDB(Config.SIMPLEDB_CREDENTIALS, Utils.simpleDBLogger);
		Memcached = new Memcached(Config.CACHE_URL);

	// Cache ///////////////////////////////////////////////////////////////////

	var Cache = (function() {
		Memcached.on('issue', function(issue) {
			Log.e('Issue occured on server ' + issue.server + ', ' + issue.retries  + 
			' attempts left untill failure');
		});
		Memcached.on('failure', function(issue) {
			Log.e(issue.server + ' failed!');
		});
		Memcached.on('reconnecting', function(issue) {
			Log.e('reconnecting to server: ' + issue.server + ' failed!');
		})
		this.get = function(key, successCallback, failCallback) {
			Memcached.get(key, function(err, result) {
				if (err) {
					Log.e(err);
					failCallback();	
				} else {
					successCallback(result);
				}
			});
		};
		this.set = function(key, value, lifetime, successCallback, failCallback) {
			Memcached.set(key, value, lifetime, function(err, result) {
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
			Memcached.del(key, function(err, result) {
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

	// Replies /////////////////////////////////////////////////////////////////

	this.Replies = (function() {
		
		this.addNewParentShout = function(shoutId, targets, successCallback, failCallback) {
			var data = {
				'TableName': Config.TABLE_REPLIES,
				'Item': {
					'shout_id': 			{'S': shoutId},
					'recipients': 			{'SS': targets}
				}
			};
			var callback = function(result) {
				result.on('data', function(chunk) {
					Log.obj(chunk + '');
					chunk = JSON.parse(chunk);
					if (chunk['ConsumedCapacityUnits']) {
						cacheReplyTo();
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
			var cacheReplyTo = function() {
				Cache.set(Config.PRE_REPLY + shoutId, targets, Config.TIMEOUT_REPLY, callback2, 
					function() {
						Log.e('Could not save reply targets to cache.');
						callback2(false);
					}
				);
			};
			var callback2 = function(setResult) {
				successCallback(true);
			};
		};

		this.getRecipients = function(shoutId, successCallback, failCallback) {
			var returnRecipients;
			var callback = function(getResult) {
				if (getResult) {
					successCallback(getResult);
				} else {
					var req = {
						'TableName': Config.TABLE_REPLY,
						'Key': {
							'HashKeyElement': {'S': shoutId}
						},
						'AttributesToGet': [
							'recipients'
						]
						// False used by not including the param.
						//'ConsistentRead': false 
					};
					DynamoDB.getItem(req, callback2);
				}
			};
			var callback2 = function(response) {
    			response.on('data', function(chunk) {
    				var item = JSON.parse(chunk);
    				if (item['Item']) {
    					var recipients = item['Item'];
    					Log.obj('recipients');
    					Log.obj(recipients);
						addToCache();
       				} else {
						Log.e(chunk);
						failCallback();
       				}
    			});
    		};
			var addToCache = function(recipients) {
				Log.e('addToCache');
				Log.obj(shout);
				returnRecipients = recipients;
				Cache.set(Config.PRE_REPLY + shoutId, returnRecipients, Config.TIMEOUT_REPLY, callback3, 
					function() {
						// Not a huge problem.
						Log.e('Could not save recipients to cache.');
						callback3(false);
					}
				);
			};
			var callback3 = function(setResult) {
				// We don't really care about the result.
				Log.e('Returning Recipients from getRecipients');
				Log.obj(returnRecipients);
				successCallback(returnRecipients);
			};	
			Cache.get(Config.PRE_REPLY + shoutId, callback,
				function() {
					// Ok to fail this get.
					callback(false);
				}
			);
		};
		
		return this;
	})();

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

		this.clearInbox = function(userId, successCallback, failCallback) {
			Cache.delete(Config.PRE_INBOX + userId, successCallback, failCallback);
		};

		return this;
	})();

	// Shouts //////////////////////////////////////////////////////////////////

	this.Shouts = (function() {

		this.getShout = function(shoutId, successCallback, failCallback) {
			var returnShout;
			var callback = function(getResult) {
				if (getResult) {
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
							'last_activity_time',
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
			var callback3 = function(setResult) {
				// We don't really care about the result.
				Log.e('Returning Shout from getShout');
				Log.obj(returnShout);
				successCallback(returnShout);
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
			var cacheShout = function() {
				Cache.set(Config.PRE_SHOUT + shout.shoutId, shout, Config.TIMEOUT_SHOUT, callback2, 
					function() {
						Log.e('Could not save shout to cache.');
						callback2(false);
					}
				);
			};
			var callback2 = function(setResult) {
				successCallback(true);
			};
			DynamoDB.putItem(data, callback);
		};

		this.sendShout = function(user, targets, shoutreach, lat, lng, text, re, successCallback, failCallback) {
			
			var shout = new Shout();
			shout.shoutId = Uuid.v4();
			shout.userId = user.userId;
			shout.time = Utils.getNowISO();
			shout.lastActivityTime = Utils.getNowISO();
			shout.text = text;
			shout.open = 1;
			shout.re = 0;
			if (typeof(re) != 'undefined') {
				shout.re = re;
			}
			shout.hit = targets.length;
			shout.power = shoutreach;
			shout.ups = 0;
			shout.downs = 0;
			shout.lat = Utils.formatLatForSimpleDB(parseFloat(lat));
			shout.lng = Utils.formatLngForSimpleDB(parseFloat(lng));

			// Let's manually add the sender to the list of targets.
			if (shout.re == 0) {
				targets.push(shout.userId);
			}
			
			var loop = function(index) {
				if (index < targets.length) {
					var targetId = targets[index];
					self.Inbox.addToInbox(targetId, shout.shoutId, 
						function() {
							loop(index + 1);		
						},
						function() {
							Log.e('Could not put shout in target inbox.');
							failCallback();
						}
					);
				} else {
					callback2();
				}
			}
			var callback = function(addNewShoutResult) {
				loop(0);
			}
			var callback2 = function() {
				if (shout.re == 0) {
					self.Replies.addNewParentShout(shout.shoutId, targets, callback3,
						function() {
							failCallback();		
						}
					);	
				} else {
					callback3();
				}
			};
			var callback3 = function() {
				successCallback();	
			};
			self.Shouts.addNewShout(shout, callback, 
				function() {
					Log.e('Unable to save shout.');
					failCallback();	
				}
			);
		};

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
						},
						'last_activity_time': {'S': Utils.getNowISO() }
					},
					'ReturnValues': 'NONE'
				};
				self.Shouts.updateShout(shout, req, callback, failCallback);
			} else {
				failCallback();
			}
		};

		this.isExpired = function(shout, successCallback, failCallback) {
			if (shout.open == 0) {
				successCallback(true);
			}
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
				self.Shouts.updateShout(shout, req, callback, failCallback);
			} else {
				successCallback(false);
			}
		};

		this.updateShout = function(shout, dynamoRequest, successCallback, failCallback) {
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
			var callback2 = function(setResult) {
				successCallback();
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
       					addToCache(user);
       				} else {
						Log.e(chunk);
						failCallback();
       				}
    			});
    		};
			var addToCache = function(user) {
				returnUser = user;
				Log.e('addToCache');
				Log.e(Config.PRE_USER);
				Cache.set(Config.PRE_USER + user.userId, user, Config.TIMEOUT_USER, callback3, 
					function() {
						// Not a huge problem.
						Log.e('Could not save user to cache.');
						callback3(false);
					}
				);
			};
			var callback3 = function(setResult) {
				// We don't really care about the result.
				successCallback(returnUser);
			};	
			Cache.get(Config.PRE_USER + userId, callback,
				function() {
					// Ok to fail this get.
					callback(false);
				}
			);
		};

		this.generateNonce = function(userId, successCallback, failCallback) {
			var callback0 = function(getResult) {
				var failCount = 1;
				if (getResult) {
					failCount = Number(getResult) + 1;
				}
				Log.l('Failed auth count = ' + failCount);
				if (failCount > Config.AUTH_ATTEMPT_FAIL_LIMIT) {
					var json = { 
						'code': 'error',
						'txt': 'Too many failed auth attempts.'
					};
					failCallback(json);
				} else {
					Cache.set(Config.PRE_AUTH_ATTEMPT_FAIL + userId, failCount, Config.TIMEOUT_AUTH_ATTEMPT_FAIL, callback01, 
						function() {
							var json = { 
								'code': 'error',
								'txt': 'Could not cache failed auth attempt.'
							};
							failCallback(json);
						}
					);
				}	
			};
			var callback01 = function(setResult) {
				self.Users.getUser(userId, callback, 
					function() {
						var json = { 
							'code': 'error',
							'txt': 'Could not get user to generate nonce.'
						};
						failCallback(json);
					}
				);
			};
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
								self.Users.updateLastActivityTime(userId, now, callback3, 
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
								var json = { 
									'code': 'error',
									'txt': 'Could not add auth to cache.'
								};
								failCallback(json);
							}
						);
					}else {
						var json = { 
							'code': 'error',
							'txt': 'User does not have a password.'
						};
						failCallback(json);
					}
				} else {
					var json = { 'code': 'invalid_uid' };
					failCallback(json);
				}
			};
			Cache.get(Config.PRE_AUTH_ATTEMPT_FAIL + userId, callback0, 
				function() {
					callback0(false);
				}
			);
		};

		this.givePoints = function(userId, pointsAmount, successCallback, failCallback) {
			var user = null;
			var req = null;
			var callback = function(getUserResult) {
				user = getUserResult;
				user.points += pointsAmount;
				var levelDiff = 0;
				if (user.points > Utils.pointsRequiredForLevel(user.level + 1)) {
					user.level++;
					levelDiff = 1;
				} else if (user.points < Utils.pointsRequiredForLevel(user.level - 1)) {
					user.level--;
					levelDiff = -1;
				}
				if (levelDiff != 0) {
					req = {
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
						},
						'ReturnValues': 'NONE'
					};
					callback2();
				} else {
					req = {
						'TableName': Config.TABLE_USERS,
						'Key': {
							'HashKeyElement': {'S': userId}
						},
						'AttributeUpdates': {
							'points': {
								'Value': {'N': String(pointsAmount)},
								'Action': 'ADD'
							}
						},
						'ReturnValues': 'NONE'
					};
					callback2();
				}
			};
			var callback2 = function() {
				self.Users.updateUser(user, req, successCallback, failCallback);
			};
			self.Users.getUser(userId, callback, failCallback);
		};

		this.acknowledgeLevelUp = function(user, successCallback, failCallback) {
			user.pendingLevelUp = 0;
			var req = {
				'TableName': Config.TABLE_USERS,
				'Key': {
					'HashKeyElement': {'S': user.userId}
				},
				'AttributeUpdates': {
					'pending_level_up': {
						'Value': {'N': String(user.pendingLevelUp)},
					}
				},
				'ReturnValues': 'NONE'
			};
			self.Users.updateUser(user, req, successCallback, failCallback);
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
			DynamoDB.updateItem(req, callback);
		};

		this.updateUser = function(user, dynamoRequest, successCallback, failCallback) {
			var callback = function(response) {
				response.on('data', function(chunk) {
	    			//	var item = JSON.parse(chunk);
	    			//	if (item['Attributes']) {
	    			//		item = item['Attributes'];
	    			// 		returnShout = Utils.makeShoutFromDynamoItem(item); }
	    			Log.e('updateUser database result:');
	    			Log.obj(chunk + '');
	    			Cache.set(Config.PRE_USER + user.userId, user, Config.TIMEOUT_USER, callback2, 
						function() {
							Log.e('Could not save user to cache.');
							failCallback();
						}
					);
	       		});	
			};
			var callback2 = function(setResult) {
				successCallback();
			};
			DynamoDB.updateItem(dynamoRequest, callback);
		};

		return this;
	})();

	// Live Users //////////////////////////////////////////////////////////////

	this.LiveUsers = (function() {
		var liveUsersSelf = this;

		this.cull = function(successCallback, failCallback) {
			Log.l('CRON CULL LIVE USERS');
			var usersToCull = false;
			var loop = function(index) {
				if (usersToCull.length > index) {
					var userId = usersToCull[index]['user_id'];	
					SimpleDB.deleteItem(Config.TABLE_LIVE, userId, 
						function(error, result, metadata) {
							if (result) {
								if (error != null) {
									Log.l('Failed to delete a user from LIVE');
									failCallback();
								} else {
									loop(index + 1);
								}
							}
						}
					);
				} else {
					successCallback(usersToCull.length);
				}
			};
			var callback = function(error, result, metadata) {
				if (result) {
					if (error != null) {
						Log.l('Failed to select expired users from LIVE');
						failCallback();
					} else {
						usersToCull = result;
						loop(0);
					}
				}
			};
			var now = new Date();
			var lastAcceptableCheckInTime = String(new Date(now.getTime() - Config.LIVE_USERS_TIMEOUT));
			var params = [];
			var query = "SELECT user_id, ping_time FROM " + Config.TABLE_LIVE + " WHERE ping_time < '?'";
			params.push(lastAcceptableCheckInTime);
			SimpleDB.select(query, params, callback);
		};

		this.userCanRequestRadius = function(userId, successCallback, failCallback) {
			var result = false;
			var callback = function(getResult) {
				var reqCount = 1;
				if (getResult) {
					reqCount = Number(getResult) + 1;
					Log.l('userCanRequestRadius Count = ' + reqCount);
					if (reqCount > Config.RADIUS_REQUEST_LIMIT) {
						result = false;
					} else {
						result = true;
					}
				} else {
					// If we can't find it...
					result = true;
				}
				Cache.set(Config.PRE_RADIUS_REQUEST + userId, reqCount, Config.TIMEOUT_RADIUS_REQUEST, callback2,
					function() {
						Log.e('Could not save radius request limit to cache.');
						failCallback();
					}
				);
			};
			var callback2 = function(setResult) {
				successCallback(result);
			};
			Cache.get(Config.PRE_RADIUS_REQUEST + userId, callback,
				function() {
					callback(false);
				}
			);
		};

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
			liveUsersSelf.calculateRadiusOrFindTargetsWithRadiusHint(isShouting, user, shoutreach, lat, lng, null, successCallback, failCallback)
		};

		this.calculateRadiusOrFindTargetsWithRadiusHint = function(isShouting, user, shoutreach, lat, lng, radiusHint, successCallback, failCallback) {
			
			var SELECT_LIMIT = 20;
			var selectCount = 0;
			var radiusHint = false;
			var xMin = 0, yMin = 0, xMax = 36000000, yMax = 18000000;
			var x0, x1, x2, x3, y0, y1, y2, y3, xDelta, yDelta, xOffset, yOffset;
			if (!isShouting) {
				var shoutreach = user.level;
			}
			var acceptableExtra = Config.SELECT_ALGORITHM_ACCEPTABLE_EXTRA;
			var acceptableExtraIncrement = Config.SELECT_ALGORITHM_INCREMENT;
			var xWrap = false, yWrap = false;
			var xUser = Utils.formatLatForSimpleDB(lat);
			var yUser = Utils.formatLngForSimpleDB(lng);
			var selectEntirePlanet = false;

			var xCenter = xMax / 2;
			var yCenter = yMax / 2;
			xCenter = Utils.formatLatForSimpleDB(lat);
			yCenter = Utils.formatLngForSimpleDB(lng);

			var createCleanBounds = function() {
				selectEntirePlanet = false;
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
				
				if (x0 == xMin && x1 == xMax && y0 == yMin && y1 == yMax) {
					selectEntirePlanet = true;
				}

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
				Log.l('fullGetCallback');
				var nearby = [];
				var nearbySorter = function(a, b) {
					return b[0] - a[0];	
				};
				if (!isShouting && (!selectEntirePlanet && result.length < shoutreach)) {
					// We have a problem.
					Log.e('calculateRadiusOrFindTargets failed to find enough users.');
					failCallback();
				} else {
					for (var row in result) {
						row = result[row];
						if (row['user_id'] != user.userId) {
							Log.l('row = ' + row['lat'] + ', ' + row['lng']);
							var lat2 = (row['lat'] / Config.MULTIPLY_COORDS) - Config.OFFSET_LAT; 
							var lng2 = (row['lng'] / Config.MULTIPLY_COORDS) - Config.OFFSET_LNG;
							Log.l('lat2, lng2 = ' + lat2 + ', ' + lng2);
							var distanceKm = Utils.distanceBetween(lat, lng, lat2, lng2);
							nearby.push([distanceKm, row['user_id']]);
							Log.l('nearby.push(' + distanceKm + ', ' + row['user_id'] + ')');
						}
					}
					nearby.sort(nearbySorter);
					Log.l('nearby = ');
					Log.l(nearby);
					
					if (isShouting) {
						var targets = [];
						var targetCount = 0;
						for (var i = 0; i < nearby.length && targetCount < shoutreach; i++) {
							if (nearby[i][1] != user.userId) {
								targets.push(nearby[i][1]);
								targetCount++;
							}
						}
						successCallback(targets);
					} else {
						Log.l('nearby = ');
						Log.l(nearby);
						if (nearby.length >= user.level) {
							var radius = nearby[user.level - 1][0];
							radius *= 1000;
							radius += Config.SHOUTREACH_BUFFER_METERS;
							radius = Math.round(radius);
							successCallback(radius);	
						} else {
							// Not enough live users online.
							successCallback(Config.RADIUS_FOR_INSUFFICIENT_USERS_ONLINE);
						}

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
				Log.e('COUNT = ' + count + ', SHOUTREACH = ' + shoutreach + ', selectEntirePlanet = ' + selectEntirePlanet);
				if (count > shoutreach || selectEntirePlanet) {
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
			if (radiusHint != null && typeof(radiusHint) != 'undefined' && radiusHint > 100) {
				var degreeLatAt45InMeters = 111132;
				var degrees = radiusHint / degreeLatAt45InMeters;
				degrees *= 100000; // degrees with 5 zeros;
				xDelta = degrees;
				yDelta = degrees;
				Log.l('radius hint = ' + degrees);
			} else {
				xDelta = xMax / 2; // TODO: Change these.
				yDelta = yMax / 2;
			}
			performSelect(true, recursiveCallback);
		};

		return this;
	})();

	// Votes ///////////////////////////////////////////////////////////////////
	this.Votes = (function() {
		
		this.getVote = function(shoutId, userId, successCallback, failCallback) {
			var returnVote;
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
						// False used by not including the param.
						//'ConsistentRead': false 
					};
					DynamoDB.getItem(req, callback2);
				}
			};
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
			var callback3 = function(setResult) {
				// We don't really care about the result.
				successCallback(returnVote);
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
			var cacheVote = function() {
				Cache.set(Config.PRE_VOTE + userId + shoutId, vote, Config.TIMEOUT_VOTE, callback2, 
					function() {
						// Not a huge problem.
						Log.e('Could not save vote to cache.');
						callback2(false);
					}
				);
			};
			var callback2 = function(setResult) {
				successCallback(true);
			};
			DynamoDB.putItem(data, callback);
		};

		return this;
	})();

	return this;
})();