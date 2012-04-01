// Includes ////////////////////////////////////////////////////////////////////

// Internal
var Log = 			require('./log'),
	Storage =		require('./storage'),
	C2DM =			require('c2dm').C2DM;
	
// Go //////////////////////////////////////////////////////////////////////////
var C2dmTest = (function() {
				   
	//var testRegId = 'APA91bEGUxgklL3bpPxTxUrEzCNPD4gyaOfUG3iQR30OG85WX4KdeRAmjimeapToN5zhapQaC1g_LrURUOc44_AYWlOHV1f9bf3WPWuZ0T7fpc-vjAzjU7IlOHJcn0xCp3Cys_B7cQsIx0QcHBuYKgd4butXDyGfKg';
	var testRegId = 'APA91bEKEhiVawhdD0odFtJD6pAOhvhFhjIDONA6hfuYYWPzRfLyBxVq8sRfUFnIqgwTPCeoas-YvGPgfMV8tpv-5zKxi_SaBajWq1KoF2X9Jnf-Gef6taD0HQ7E9m4oqEeZghD76SKPp2kiYyLqvrEKi77zV4DgGg';

	var config = {
		//token: 'Auth=DQAAAMIAAADBN5dXXfszWmLCoHH1BbbFK8gJiK-kWMZ0C-LKFgltkWhRM1vFXJpgbv4oFWf_54ao0WTuj8bSRTEu4-SvAdTZ39hPx0sUJ_SJoSswaiN9Z82x4y1h9h7CHTLACDEaxYRvF9Rys3dnWZ8Fr6caorisI7sz3QU_Brav3PlGvfFVpHT6dKlVJPzJUzRSKOOE_Cue4T-0uZelaQnuUKDK1VN3xCGB-tKjmEyN9pu90k8Nw7v0UIOokvkQQbfeJ0uR63zR_2ObF2X_nnL9udS2TLgo',
		user: 'virtuability@gmail.com',
	    password: 'echogolf7r33f0r7=^',
	    source: 'ProjectLoud-Shoutbreak-3.0', 
		keepAlive: true
	};

	var c2dm = new C2DM(config);

	var sendTestMessage = function() {
		var message = {
		    registration_id: testRegId,
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
				} else if (err == 'Error=MessageTooBig') {
				} else if (err == 'Error=MissingCollapseKey') {
				}
				Log.logC2dm('C2DM Send Error: ' + err);
			} else {
				Log.logC2dm('C2DM Send Success: ' + messageId);
			}
		});
	};

	c2dm.login(function(err, token){
		// err - error, received from Google ClientLogin api
	    // token - Auth token
		if (err != null) {
			Log.logC2dm('C2DM Auth Error: ' + err);
		} else {
			var authToken = token;
			Log.logC2dm('C2DM Auth Success: ' + authToken);
			sendTestMessage();
		}
  	});

})();
