package co.shoutbreak;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.SBLog;
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
		newIntent.putExtra(C.APP_LAUNCHED_FROM_ALARM, true);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(newIntent);
	}
}
