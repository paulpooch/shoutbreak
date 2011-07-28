package co.shoutbreak;


import co.shoutbreak.shared.C;
import co.shoutbreak.shared.SBLog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class ShoutbreakService extends Service implements Colleague {

	private static String TAG = "ShoutbreakService";
	
	private Mediator _m;
	private boolean _isStarted;
	
	@Override
	public void setMediator(Mediator mediator) {
    	SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.e(TAG, "unsetMediator()");
		// should never be called
	}
	
	@Override
	public void onCreate() {
    	SBLog.i(TAG, "onCreate()");
		super.onCreate();
		new Mediator(ShoutbreakService.this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SBLog.i(TAG, "onStartCommand()");
		super.onStartCommand(intent, flags, startId);
		if (!_isStarted) {
			_isStarted = true;
			_m.onServiceStart();
		} else {
			SBLog.i(TAG, "service already started");
		}
		Bundle extras = intent.getExtras();
		if (!extras.isEmpty()) {
			// determine what launched the app
			if (extras.getBoolean(C.APP_LAUNCHED_FROM_UI)) {
				_m.appLaunchedFromUI();
			} else if (extras.getBoolean(C.APP_LAUNCHED_FROM_ALARM)) {
				_m.appLaunchedFromAlarm();
			}
		} else {
			SBLog.e(TAG, "Service bundle must contain referral information");
		}
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
    	SBLog.i(TAG, "onBind()");
		return new ServiceBridge();
	}
	
	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		_m.kill(); // this can only be called here
		_m = null;
		super.onDestroy();
	}

	public class ServiceBridge extends Binder implements ServiceBridgeInterface {

		@Override
		public void registerUIWithMediator(Shoutbreak ui) {
			SBLog.i(TAG, "registerUIWithMediator()");
			_m.registerUI(ui);
		}
	
	}
	
	public void enableAlarmReceiver() {
		SBLog.i(TAG, "enableAlarmReceiver()");
		ComponentName component = new ComponentName(ShoutbreakService.this, AlarmReceiver.class);
		int state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		getPackageManager().setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);	
	}
	
	public void disableAlarmReceiver() {
		SBLog.i(TAG, "disableAlarmReceiver()");
		ComponentName component = new ComponentName(ShoutbreakService.this, AlarmReceiver.class);
		int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		getPackageManager().setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);		
	}
}
