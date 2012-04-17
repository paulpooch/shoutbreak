////////////////////////////////////////////////////////////////////////////////
// 
// C2DM
//
////////////////////////////////////////////////////////////////////////////////
var C2DM = module.exports = {};

(function() {
	var self = this;

	// Includes ////////////////////////////////////////////////////////////////
	var Log = 		require('./log'),
		Storage =	require('./storage'),
		Config =	require('./config'),
		DGram = 	require('dgram');

	var ready = true;

	// TODO: Add exponential backoff on quota errors.
  	this.setupPushTimer = function(targetUser, expectedLastItemId, successCallback, failCallback) {
  		setTimeout(
  			function() {
  				Log.logC2dm('Running C2DM push event.');
  				var callback = function(checkInboxResult) {
  					if (checkInboxResult.length > 0) {
  						if (checkInboxResult[checkInboxResult.length - 1] == expectedLastItemId) {
  							// User has not picked up shout since it was sent...
	  						if (ready) {
			  					Log.logC2dm('Attempting C2DM to ' + targetUser.userId + ' | c2dmId = ' + targetUser.c2dmId);
				  				
				  				var payload = { key1: 'val1' };
			  					var packet = [
			  						targetUser.c2dmId,
			  						'replace-exsting',
			  						JSON.stringify(payload)
			  					];
			  					var message = new Buffer(packet.join(':'));
			  					var client = DGram.createSocket('udp4');
								client.send(message, 0, message.length, Config.C2DM_SERVER_PORT, Config.C2DM_SERVER_ADDRESS, function(err, bytes) {
  									client.close();
								});
				  				

								// TODO: Can C2DM server respond to us via callack port?
								// Especially important for NotRegistered error.

							}
						}
  					}
  				};
  				Storage.Inbox.checkInbox(targetUser.userId, callback, failCallback);

  				
  			},
  			Config.C2DM_UNTOUCHED_INBOX_TIMEOUT
  		);
		successCallback();
  	};
  	
	
}).call(C2DM);