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

	setInterval(cullLiveUsers, Config.CRON_INTERVAL_CULL_LIVE_USERS);

	/* OLD CODE
	Log.useFile('cron.log');
	Log.l(new Date());
	Log.l('CRON - CULL LIVE USERS');
	if (process.argv.length > 2) {
		var job = process.argv[2];
		if (job == 'cull_live_users') {
			var callback = function(cullResult) {
				Log.l('CULLED ' + cullResult + ' USERS FROM LIVE');
			};
			Storage.LiveUsers.cull(callback, 
				function() {
					Log.l('Could not cull live users.');
				}
			);
		}
	}
	*/

})();
