// Includes ////////////////////////////////////////////////////////////////////

// Internal
var Log = 			require('./log'),
	Storage =		require('./storage');
	
// Go //////////////////////////////////////////////////////////////////////////

var Cron = (function() {
	Log.logCron(new Date());
	Log.logCron('MANUAL CRON\n/////////////////////////////////////////////////////////////////////////\nCULL LIVE USERS');
	if (process.argv.length > 2) {
		var job = process.argv[2];
		if (job == 'cull_live_users') {
			var callback = function(cullResult) {
				Log.logCron('CULLED ' + cullResult + ' USERS FROM LIVE');
			};
			Storage.LiveUsers.cull(callback, 
				function() {
					Log.logCron('Could not cull live users.');
				}
			);
		}
	}
})();
