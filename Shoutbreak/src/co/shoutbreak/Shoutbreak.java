package co.shoutbreak;

import co.shoutbreak.shared.SBLog;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class Shoutbreak extends Activity implements Colleague {
	
	private static String TAG = "Shoutbreak";
	
	private Mediator _m;
	private Intent _serviceIntent;
	private ServiceBridgeInterface _serviceBridge;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
		ImageButton composeTab;
		ImageButton inboxTab;
		ImageButton profileTab;
		ImageButton powerButton;
    	
    	SBLog.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		// register button listeners
		composeTab = (ImageButton) findViewById(R.id.composeTab);
		composeTab.setOnClickListener(_composeTabListener);
		inboxTab = (ImageButton) findViewById(R.id.inboxTab);
		inboxTab.setOnClickListener(_inboxTabListener);
		profileTab = (ImageButton) findViewById(R.id.profileTab);
		profileTab.setOnClickListener(_profileTabListener);
		powerButton = (ImageButton) findViewById(R.id.powerButton);
		powerButton.setOnClickListener(_powerButtonListener);
		
		// bind to service, initializes mediator
		_serviceIntent = new Intent(Shoutbreak.this, ShoutbreakService.class);
		bindService(_serviceIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
    }

	@Override
	public void setMediator(Mediator mediator) {
		SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "unSetMediator()");
		_m = null;		
	}

	// all mediator interaction must occur after onServiceConnected
	private ServiceConnection _serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			_serviceBridge = (ServiceBridgeInterface) service;
			_serviceBridge.registerUIWithMediator(Shoutbreak.this);
			_m.onServiceConnected();			
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			_m.onServiceDisconnected();
		}
	};
	
	/* Button Listeners */
	
	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {

		}
	};
	
}

