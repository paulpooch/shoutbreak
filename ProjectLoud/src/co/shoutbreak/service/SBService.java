package co.shoutbreak.service;

import java.util.Calendar;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.components.SBUser;
import co.shoutbreak.misc.C;
import co.shoutbreak.misc.SBLog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class SBService extends Service {

	private final String TAG = "SBService";
	
	private SBStateManager _StateManager;
	private boolean _isServiceOn = false;
	
	/* LIFECYCLE METHODS */
	
	@Override
	public IBinder onBind(Intent intent) {
		SBLog.i(TAG, "onBind()");
		return new ServiceBridge();
	}
	
	@Override
	public void onCreate() {
		SBLog.i(TAG, "onCreate()");
		super.onCreate();
		
		_StateManager = new SBStateManager();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SBLog.i(TAG, "onStartCommand()");
		_isServiceOn = true;
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		super.onDestroy();
	}
	
	public class ServiceBridge extends Binder implements ServiceBridgeInterface {
		
		public SBUser getUser() {
			return null;
		}
	}
	
	public void continueService(String task) {
		SBLog.i(TAG, "startService()");
		scheduleAlarm(10);
	}
	
	/* ADDITIONAL METHODS */
	
	private void scheduleAlarm(int numSeconds) {
		// to immediately trigger alarm, set numSeconds to -1
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, numSeconds);
		Intent intent = new Intent(getApplicationContext(), SBAlarmReceiver.class);
		intent.putExtra(C.ALARM_MESSAGE, "O'Doyle Rules!");
		PendingIntent sender = PendingIntent.getBroadcast(this, C.ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}
}
