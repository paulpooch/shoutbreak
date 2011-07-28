package co.shoutbreak;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.Flag;
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
import android.widget.LinearLayout;
import android.widget.Toast;

public class Shoutbreak extends Activity implements Colleague {
	
	private static String TAG = "Shoutbreak";
	
	private Flag _isPowerOn = new Flag("_isPowerOn");
	private Flag _isComposeShowing = new Flag("_isComposeShowing");
	private Flag _isInboxShowing = new Flag("_isInboxShowing");
	private Flag _isProfileShowing = new Flag("_isProfileShowing");
	
	private Mediator _m;
	private Intent _serviceIntent;
	private ServiceBridgeInterface _serviceBridge;
	private ImageButton _powerButton;
	
    @Override
    public void onCreate(Bundle extras) {
    	
		ImageButton composeTab;
		ImageButton inboxTab;
		ImageButton profileTab;
    	
    	SBLog.i(TAG, "onCreate()");
    	super.onCreate(extras);
        setContentView(R.layout.main);
        
		// register button listeners
		composeTab = (ImageButton) findViewById(R.id.composeTab);
		composeTab.setOnClickListener(_composeTabListener);
		inboxTab = (ImageButton) findViewById(R.id.inboxTab);
		inboxTab.setOnClickListener(_inboxTabListener);
		profileTab = (ImageButton) findViewById(R.id.profileTab);
		profileTab.setOnClickListener(_profileTabListener);
		_powerButton = (ImageButton) findViewById(R.id.powerButton);
		_powerButton.setOnClickListener(_powerButtonListener);
		
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
		SBLog.i(TAG, "unsetMediator()");
		_m = null;		
	}

	// all mediator interaction must occur after onServiceConnected()
	private ServiceConnection _serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			SBLog.i(TAG, "onServiceConnected()");
			_serviceBridge = (ServiceBridgeInterface) service;
			_serviceBridge.registerUIWithMediator(Shoutbreak.this);
			_m.onServiceConnected();
			
			// begin the service
			_serviceIntent.putExtra(C.APP_LAUNCHED_FROM_UI, true);
			startService(_serviceIntent);
			
			// call this whenever the ui starts / resumes
			checkLocationProviderStatus();
			
			// hide splash
			((LinearLayout) findViewById(R.id.splash)).setVisibility(View.GONE);
			
			// switch views
			Bundle extras = getIntent().getExtras();
			if (extras != null && extras.getBoolean(C.APP_LAUNCHED_FROM_NOTIFICATION)) {
				showInbox();
			} else {
				showCompose();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			SBLog.i(TAG, "onServiceDisconnected()");
			_m.onServiceDisconnected();
		}
	};
	
	@Override
	public void onResume() {
		SBLog.i(TAG, "onResume()");
		super.onResume();
		if (_m != null) {
			// call this whenever the ui starts / resumes
			checkLocationProviderStatus();
		}
	}
	
	
	@Override
	public void onNewIntent(Intent intent) {
		SBLog.i(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		Bundle extras = intent.getExtras();
		if (extras != null && extras.getBoolean(C.APP_LAUNCHED_FROM_NOTIFICATION)) {
			// show inbox view
		} else {
			SBLog.e(TAG, "ui relaunched from something other than notification");
		}
	}
	
	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		_m.unregisterUI(false);
		_m = null;
		super.onDestroy();
	}
	
	public void setPowerState(boolean isOn) {
		SBLog.i(TAG, "setPowerState()");
		_isPowerOn.set(isOn);
		if (isOn) {
			_powerButton.setImageResource(R.drawable.power_button_on);
			_m.onPowerEnabled();
		} else {
			_powerButton.setImageResource(R.drawable.power_button_off);
			_m.onPowerDisabled();
			if (_isComposeShowing.get()) {
				disableComposeView();
			}
		}
	}
	
	/* Button Listeners */
	
	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {
			showCompose();
		}
	};
	
	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {
			showInbox();
		}
	};
	
	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {
			showProfile();
		}
	};
	
	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {
			if (_isPowerOn.get()) {
				setPowerState(false);
			} else {
				setPowerState(true);
			}
		}
	};
	
	/* View Methods */
	
	public void showCompose() {
		SBLog.i(TAG, "showCompose()");
		_isComposeShowing.set(true);
		_isInboxShowing.set(false);
		_isProfileShowing.set(false);
		findViewById(R.id.compose_view).setVisibility(View.VISIBLE);
		findViewById(R.id.inbox_view).setVisibility(View.GONE);
		findViewById(R.id.profile_view).setVisibility(View.GONE);
	}
	
	public void showInbox() {
		SBLog.i(TAG, "showInbox()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(true);
		_isProfileShowing.set(false);
		findViewById(R.id.compose_view).setVisibility(View.GONE);
		findViewById(R.id.inbox_view).setVisibility(View.VISIBLE);
		findViewById(R.id.profile_view).setVisibility(View.GONE);
	}
	
	public void showProfile() {
		SBLog.i(TAG, "showProfile()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(false);
		_isProfileShowing.set(true);
		findViewById(R.id.compose_view).setVisibility(View.GONE);
		findViewById(R.id.inbox_view).setVisibility(View.GONE);
		findViewById(R.id.profile_view).setVisibility(View.VISIBLE);
	}
	
	public void disableComposeView() {
		SBLog.i(TAG, "disableComposeView()");
	}
	
	/* Location and Data */
	
	public void onLocationDisabled() {
		SBLog.i(TAG, "onLocationDisabled()");
		setPowerState(false);
	}
	
	public void onDataDisabled() {
		SBLog.i(TAG, "onDataDisable()");
		setPowerState(false);
	}
	
	public void checkLocationProviderStatus() {
		SBLog.i(TAG, "checkLocationProviderStatus()");
		_m.checkLocationProviderStatus();
	}
	
	public void unableToTurnOnApp() {
		Toast.makeText(Shoutbreak.this, "unable to turn on app", Toast.LENGTH_SHORT).show();
	}
}