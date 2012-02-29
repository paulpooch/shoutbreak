// Includes ////////////////////////////////////////////////////////////////////

module.exports = (function() {

	// Internal
	var Log = 			require('./log'),
		Storage =		require('./storage'),
		Config = 		require('./config');
		
	// Go //////////////////////////////////////////////////////////////////////////
	
	var cullLiveUsers = function() {
		Log.logCron('CULL LIVE USERS');
		var callback = function(cullResult) {
			Log.logCron('Culled ' + cullResult + ' users from LIVE');
		};
		Storage.LiveUsers.cull(callback, 
			function() {
				Log.logCron('Could not cull live users.');
			}
		);
	};

	cullLiveUsers();
	setInterval(cullLiveUsers, Config.CRON_INTERVAL_CULL_LIVE_USERS);
	
	return this;
})();
