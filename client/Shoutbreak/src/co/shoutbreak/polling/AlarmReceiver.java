package co.shoutbreak.polling;

import co.shoutbreak.core.C;
import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.core.utils.SBLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* starts service when actions fired off as defined in the manifest are triggered */

public class AlarmReceiver extends BroadcastReceiver {
	
	private static final String TAG = "AlarmReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SBLog.i(TAG, "onReceive()");
		Intent newIntent = new Intent(context, ShoutbreakService.class);
		newIntent.putExtra(C.NOTIFICATION_LAUNCHED_FROM_ALARM, true);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(newIntent);
	}
}
