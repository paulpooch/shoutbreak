package co.shoutbreak;

import co.shoutbreak.shared.SBLog;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ShoutbreakService extends Service implements Colleague {

	private static String TAG = "ShoutbreakService";
	
	private Mediator _m;
	
	@Override
	public void setMediator(Mediator mediator) {
    	SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
    	SBLog.i(TAG, "unSetMediator()");
		_m.kill(); // this can only be called here
		_m = null;
	}
	
	@Override
	public void onCreate() {
    	SBLog.i(TAG, "onCreate()");
		super.onCreate();
		new Mediator(ShoutbreakService.this);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
    	SBLog.i(TAG, "onBind()");
		return null;
	}
}
