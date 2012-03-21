package co.shoutbreak.core;

import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.polling.OnBootAlarmReceiver;
import co.shoutbreak.ui.Shoutbreak;
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
	
	public ShoutbreakService() {
    	SBLog.lifecycle(TAG, "ShoutbreakService()");
    	SBLog.constructor(TAG);
	}

	@Override
	public void unsetMediator() {
		SBLog.error(TAG, "unsetMediator()");
		// should never be called /
	}
	
	@Override
	public void onCreate() {
    super.onCreate();
		_m = new Mediator(ShoutbreakService.this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SBLog.lifecycle(TAG, "onStartCommand()");
		super.onStartCommand(intent, flags, startId);
		if (!_isStarted) {
			_isStarted = true;
		} else {
			SBLog.error(TAG, "service already started");
		}
		Bundle extras = intent.getExtras();
		if (extras != null && !extras.isEmpty()) {
			// determine what launched the app
			if (extras.getBoolean(C.NOTIFICATION_LAUNCHED_FROM_UI)) {
				_m.attemptTurnOn(true);
			} else if (extras.getBoolean(C.NOTIFICATION_LAUNCHED_FROM_ALARM)) {
				_m.attemptTurnOn(true);
			}
		} else {
			SBLog.error(TAG, "Service bundle must contain referral information");
			_m.attemptTurnOn(true);
		}
		return START_REDELIVER_INTENT;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
    	SBLog.lifecycle(TAG, "onBind()");
		return new ServiceBridge();
	}
	
	@Override
	public void onDestroy() {
		SBLog.lifecycle(TAG, "onDestroy()");
		_m.kill(); // this can only be called here
		_m = null;
		super.onDestroy();
	}

	public class ServiceBridge extends Binder implements ServiceBridgeInterface {

		@Override
		public void registerUIWithMediator(Shoutbreak ui) {
			SBLog.lifecycle(TAG, "registerUIWithMediator()");
			_m.registerUI(ui);
		}
	
	}
	
	public void enableOnBootAlarmReceiver() {
		SBLog.lifecycle(TAG, "enableOnBootAlarmReceiver()");
		ComponentName component = new ComponentName(ShoutbreakService.this, OnBootAlarmReceiver.class);
		int state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		getPackageManager().setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);	
	}
	
	public void disableOnBootAlarmReceiver() {
		SBLog.lifecycle(TAG, "disableOnBootAlarmReceiver()");
		ComponentName component = new ComponentName(ShoutbreakService.this, OnBootAlarmReceiver.class);
		int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		getPackageManager().setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);		
	}
}
