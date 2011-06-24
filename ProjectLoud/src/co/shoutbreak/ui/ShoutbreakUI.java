package co.shoutbreak.ui;

import co.shoutbreak.R;
import co.shoutbreak.components.SBNotificationManager;
import co.shoutbreak.components.SBPageChanger;
import co.shoutbreak.components.SBUser;
import co.shoutbreak.misc.C;
import co.shoutbreak.misc.SBLog;
import co.shoutbreak.service.SBService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class ShoutbreakUI extends Activity {
	
	private final String TAG = "Shoutbreak.java";

	private SBService _Service;
	private SBUser _User;
	private SBNotificationManager _NotificationManager;
	private SBPageChanger _PageChanger;
	private Intent serviceIntent;
	
	/* LIFECYCLE METHODS */
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	//Intent serviceIntent;
    	
    	SBLog.i(TAG, "onCreate()");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // initialize components
        _NotificationManager = new SBNotificationManager(this);
        _PageChanger = new SBPageChanger(this);
        
        // connect to service
        serviceIntent = new Intent(ShoutbreakUI.this, SBService.class);
        serviceIntent.putExtra(C.START_FROM_UI, true);
        startService(serviceIntent); // must be called, BIND_AUTO_CREATE doesn't start service
        bindService(serviceIntent, _ServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private ServiceConnection _ServiceConnection = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName className, IBinder service) {
    		SBLog.i(TAG, "onServiceConnected()");
    		//ServiceBridgeInterface serviceBinder = (ServiceBridgeInterface) service;
    		//_User = serviceBinder.getUser();
    	}

    	public void onServiceDisconnected(ComponentName className) {
    		// should never be called
    		SBLog.i(TAG, "onServiceDisconnected()");
    	}
    	
    };

    @Override
	public void onNewIntent(Intent intent) {
    	SBLog.i(TAG, "onNewIntent()");
		_NotificationManager.handleNotificationExtras(intent.getExtras());
	}
    
	@Override
	protected void onStart() {
		SBLog.i(TAG, "onStart()");
		super.onStart();
	}

	@Override
	protected void onResume() {
		SBLog.i(TAG, "onResume()");
		super.onResume();
		_NotificationManager.handleNotificationExtras(getIntent().getExtras());
	}

	@Override
	protected void onPause() {
		SBLog.i(TAG, "onResume()");
		super.onPause();
	}

	@Override
	protected void onStop() {
		SBLog.i(TAG, "onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		unbindService(_ServiceConnection);
		_User = null;
		super.onDestroy();
	}
	
	/* COMPONENT GETTERS */
	
	public SBPageChanger getSBPageChanger() {
		return _PageChanger;
	}
	
}