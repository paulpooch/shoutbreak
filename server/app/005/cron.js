// Includes ////////////////////////////////////////////////////////////////////

// Internal
var Log = 			require('./log'),
	Storage =		require('./storage');
	
// Go //////////////////////////////////////////////////////////////////////////

var Cron = (function() {
	Log.init('cron.log');
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
})();
