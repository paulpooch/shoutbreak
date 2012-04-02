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
		C2DM =		require('c2dm').C2DM;

	var config = {
		//token: 'Auth=DQAAAMIAAADBN5dXXfszWmLCoHH1BbbFK8gJiK-kWMZ0C-LKFgltkWhRM1vFXJpgbv4oFWf_54ao0WTuj8bSRTEu4-SvAdTZ39hPx0sUJ_SJoSswaiN9Z82x4y1h9h7CHTLACDEaxYRvF9Rys3dnWZ8Fr6caorisI7sz3QU_Brav3PlGvfFVpHT6dKlVJPzJUzRSKOOE_Cue4T-0uZelaQnuUKDK1VN3xCGB-tKjmEyN9pu90k8Nw7v0UIOokvkQQbfeJ0uR63zR_2ObF2X_nnL9udS2TLgo',
		user: 'Virtuability@gmail.com',
	    password: 'echogolf7r33f0r7=^',
	    source: 'ProjectLoud-Shoutbreak-3.0', 
		keepAlive: true
	};

	var c2dm = new C2DM(config);
	var ready = false;

	c2dm.login(function(err, token){
		// err - error, received from Google ClientLogin api
	    // token - Auth token
		if (err != null) {
			Log.logC2dm('C2DM Auth Error: ' + err);
		} else {
			// We don't use token.
			//var authToken = token;
			ready = true;
			Log.logC2dm('C2DM Auth Success');
		}
  	});

	// TODO: Add exponential backoff on quota errors.
  	this.setupPushTimer = function(inboxCacheId, expectedLastItemId, user, successCallback, failCallback) {
  		setTimeout(
  			function() {
  				// TODO: Add if we should really send check - based on expectedLastItemId.
  				if (ready) {
  					Log.logC2dm('Attempting C2DM to ' + user.userId + ' | c2dmId = ' + user.c2dmId);
	  				var message = {
			    		registration_id: user.c2dmId,
			    		collapse_key: 'replace-exsting', // this doesn't matter
					    'data.key1': 'value1',
					    'data.key2': 'value2',
					    // If included, waits until device screen is on.
					    // delay_while_idle: '1'
					};
					c2dm.send(message, function(err, messageId) {
						Log.logC2dm('C2DM Send messageId: ' + messageId);
						if (err != null) {
							if (err == 'Error=QuotaExceeded') {
							} else if (err == 'Error=DeviceQuotaExceeded') {
							} else if (err == 'Error=MissingRegistration') {
							} else if (err == 'Error=InvalidRegistration') {
							} else if (err == 'Error=MismatchSenderId') {
							} else if (err == 'Error=NotRegistered ') {
								// Wipe c2dm id from users table.
								// We should resend c2dm id on every sign in?
							} else if (err == 'Error=MessageTooBig') {
							} else if (err == 'Error=MissingCollapseKey') {
							}
							Log.logC2dm('C2DM Send Error: ' + err + '\n' +
								'c2dmId = ' + user.c2dmId +  '\n' +
								'userId = ' + user.userId) ;
						} else {
							Log.logC2dm('C2DM Send Success.' + '\n' +
								'messageId = ' + messageId + '\n' +
								'c2dmId = ' + user.c2dmId + '\n' +
								'userId = ' + user.userId) ;
						}
					});
				}
  			},
  			Config.C2DM_UNTOUCHED_INBOX_TIMEOUT
  		);
		successCallback();
  	};
	
}).call(C2DM);