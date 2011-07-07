package co.shoutbreak.service;

import co.shoutbreak.misc.SBLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/* starts service when actions fired off in the manifest are triggered */

public class SBAlarmReceiver extends BroadcastReceiver {
	
	private static final String TAG = "AlarmReceiver";
	
	public static final String LAUNCHED_FROM_ALARM = "LAUNCHED_FROM_ALARM";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SBLog.i(TAG, "onReceive()");
		// launch service intent
		//Bundle bundle = intent.getExtras();
		//String message = bundle.getString(C.ALARM_MESSAGE);
		Intent newIntent = new Intent(context, SBService.class);
		newIntent.putExtra(LAUNCHED_FROM_ALARM, true);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Toast.makeText(context, "service started", Toast.LENGTH_SHORT).show();
		context.startService(newIntent);
	}

}
