package co.shoutbreak.service;

import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.components.SBUser;
import co.shoutbreak.misc.C;
import co.shoutbreak.misc.SBLog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class SBService extends Service implements Observer{

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
		_StateManager.addObserver(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SBLog.i(TAG, "onStartCommand()");
		if (!_isServiceOn) {
			_isServiceOn = true;
			_StateManager.enableData();
			_StateManager.enableUI();
			_StateManager.disableData();
		}
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
		//intent.putExtra(C.ALARM_MESSAGE, "O'Doyle Rules!");
		PendingIntent sender = PendingIntent.getBroadcast(this, C.ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}

	/* OBSERVER METHODS */
	
	public void update(Observable observable, Object data) {
		SBStateManager smgr = (SBStateManager) observable;
		Toast.makeText(getApplicationContext(), smgr.getState() + " " , Toast.LENGTH_SHORT).show();
	}
}
