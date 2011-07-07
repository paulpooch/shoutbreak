package co.shoutbreak.service;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.components.SBPreferenceManager;
import co.shoutbreak.components.SBStateManager;
import co.shoutbreak.components.SBUser;
import co.shoutbreak.misc.SBLog;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
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
			
			// enable polling if called from alarm receiver
			Bundle bundle = intent.getExtras();
			if (bundle != null & bundle.getBoolean(SBAlarmReceiver.LAUNCHED_FROM_ALARM)) {
				SBPreferenceManager preferences = new SBPreferenceManager(SBService.this);
				preferences.getBoolean(SBPreferenceManager.POWER_STATE_PREF, true);
				_StateManager.call(SBStateManager.ENABLE_POLLING);
			}
			
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		super.onDestroy();
		
		_StateManager.deleteObserver(this);
		_StateManager.call(SBStateManager.DISABLE_POLLING);
	}
	
	public class ServiceBridge extends Binder implements SBServiceBridgeInterface {
		
		public SBUser getUser() {
			return null;
		}
		
		public SBStateManager getStateManager() {
			return _StateManager;
		}
	}

	/* OBSERVER METHODS */
	
	public void update(Observable observable, Object data) {
		int code = (Integer) data;
		switch (code) {
			
			case SBStateManager.ENABLE_POLLING:
				Toast.makeText(getApplicationContext(), "Loop Started" , Toast.LENGTH_SHORT).show();
				_ServiceLoop = new SBServiceLoop(this);
				_ServiceLoop.start();
				break;
				
			case SBStateManager.DISABLE_POLLING:
				if (_ServiceLoop != null) {
					_ServiceLoop.quit();
				}
				break;
		}
	}
}
