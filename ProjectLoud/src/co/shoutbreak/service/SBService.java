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

/* SBService.java */
// communicates with the UI via the StateManager
// launches the service loop
public class SBService extends Service implements Observer {

	private final String TAG = "SBService";
	
	private SBStateManager _StateManager;	
	private SBServiceLoop _ServiceLoop;
	
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
			Toast.makeText(getApplicationContext(), "Service On" , Toast.LENGTH_SHORT).show();
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		_StateManager.deleteObserver(this);
		_ServiceLoop.quit();
		Toast.makeText(getApplicationContext(), "Service Destroyed" , Toast.LENGTH_SHORT).show();
		SBLog.i(TAG, "onDestroy()");
		super.onDestroy();
	}
	
	public class ServiceBridge extends Binder implements SBServiceBridgeInterface {
		
		public SBUser getUser() {
			return null;
		}
		
		public SBStateManager getStateManager() {
			return _StateManager;
		}
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
		int code = (Integer) data;
		switch (code) {
			
			case SBStateManager.ENABLE_POLLING:
				_ServiceLoop = new SBServiceLoop(this);
				_ServiceLoop.start();
				break;
				
			case SBStateManager.DISABLE_POLLING:
				_ServiceLoop.quit();
				break;
		}
	}
}
