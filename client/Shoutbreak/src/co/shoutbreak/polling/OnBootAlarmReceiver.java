package co.shoutbreak.polling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import co.shoutbreak.core.C;
import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.core.utils.SBLog;

/* starts service when actions fired off as defined in the manifest are triggered */

public class OnBootAlarmReceiver extends BroadcastReceiver {
	
	private static final String TAG = "AlarmReceiver";
	
	// TODO: Do we need to worry about WakeLock's?
	// Code below if needed.
	// PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	// PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Shoutbreak WakeLock");
	// wakeLock.acquire();
	// wakeLock.release();
	
	@Override
	public void onReceive(Context context, Intent intent) {		
		SBLog.i(TAG, "onReceive()");
		Intent currentIntent = new Intent(context, ShoutbreakService.class);
		currentIntent.putExtra(C.NOTIFICATION_LAUNCHED_FROM_ALARM, true);
		currentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(currentIntent);
	}
	
}
