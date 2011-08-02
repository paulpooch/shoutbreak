package co.shoutbreak;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.Flag;
import co.shoutbreak.shared.SBLog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Shoutbreak extends MapActivity implements Colleague {
	
	private static String TAG = "Shoutbreak";
	
	private Flag _isPowerOn = new Flag("_isPowerOn");
	private Flag _isComposeShowing = new Flag("_isComposeShowing");
	private Flag _isInboxShowing = new Flag("_isInboxShowing");
	private Flag _isProfileShowing = new Flag("_isProfileShowing");
	private Flag _isMapInitialized = new Flag("_isMapInitialized");
	
	private Mediator _m;
	private Intent _serviceIntent;
	private ServiceBridgeInterface _serviceBridge;
	private ImageButton _powerButton;
	private ImageButton _composeTab;
	private ImageButton _inboxTab;
	private ImageButton _profileTab;
	private ImageButton _shoutButton;
	private TextView _textbox;
	private CustomMapView _map;
	
    @Override
    public void onCreate(Bundle extras) {
    	SBLog.i(TAG, "onCreate()");
    	super.onCreate(extras);
        setContentView(R.layout.main);
        
        // intialized flags
        _isMapInitialized.set(false);
        
		// register button listeners
		_composeTab = (ImageButton) findViewById(R.id.composeTab);
		_composeTab.setOnClickListener(_composeTabListener);
		_inboxTab = (ImageButton) findViewById(R.id.inboxTab);
		_inboxTab.setOnClickListener(_inboxTabListener);
		_profileTab = (ImageButton) findViewById(R.id.profileTab);
		_profileTab.setOnClickListener(_profileTabListener);
		_powerButton = (ImageButton) findViewById(R.id.powerButton);
		_powerButton.setOnClickListener(_powerButtonListener);
		_shoutButton = (ImageButton) findViewById(R.id.shoutButton);
		_shoutButton.setOnClickListener(_shoutButtonListener);
		_textbox = (TextView) findViewById(R.id.etShoutText);
		
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
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
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
	
	private OnClickListener _shoutButtonListener = new OnClickListener() {
		public void onClick(View v) {
			_m.shout(_textbox.getText());
			hideKeyboard();
		}
	};
	
	/* View Methods */
	
	public void showCompose() {
		SBLog.i(TAG, "showCompose()");
		_isComposeShowing.set(true);
		_isInboxShowing.set(false);
		_isProfileShowing.set(false);
		if (!_isMapInitialized.get()) {
			_map = (CustomMapView) findViewById(R.id.cmvMap);
			UserLocationOverlay userLocationOverlay = new UserLocationOverlay(Shoutbreak.this, _map);
			if (_map.getOverlays().size() == 0) {
				_map.getOverlays().add(userLocationOverlay);
			}
			MapController mapController = _map.getController();
			mapController.setZoom(C.DEFAULT_ZOOM_LEVEL);
			_map.setClickable(true);
			_map.setEnabled(true);
			_map.setUserLocationOverlay(userLocationOverlay);
			_map.postInvalidate();
			
			// TODO: remove this. called when 'on/off' switch is clicked
			//_userLocationOverlay.runOnFirstFix(new Runnable() {
			//	public void run() {
			//		GeoPoint loc = _userLocationOverlay.getMyLocation();
			//		_mapController.animateTo(loc);
			//	}
			//});
			userLocationOverlay.enableMyLocation();
			_isMapInitialized.set(true);
		}
		_composeTab.setImageResource(R.drawable.tab_shouting_on);
		_inboxTab.setImageResource(R.drawable.tab_inbox);
		_profileTab.setImageResource(R.drawable.tab_user);
		findViewById(R.id.compose_view).setVisibility(View.VISIBLE);
		findViewById(R.id.inbox_view).setVisibility(View.GONE);
		findViewById(R.id.profile_view).setVisibility(View.GONE);
	}
	
	public void showInbox() {
		SBLog.i(TAG, "showInbox()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(true);
		_isProfileShowing.set(false);
		_composeTab.setImageResource(R.drawable.tab_shouting);
		_inboxTab.setImageResource(R.drawable.tab_inbox_on);
		_profileTab.setImageResource(R.drawable.tab_user);
		findViewById(R.id.compose_view).setVisibility(View.GONE);
		findViewById(R.id.inbox_view).setVisibility(View.VISIBLE);
		findViewById(R.id.profile_view).setVisibility(View.GONE);
	}
	
	public void showProfile() {
		SBLog.i(TAG, "showProfile()");
		_isComposeShowing.set(false);
		_isInboxShowing.set(false);
		_isProfileShowing.set(true);
		_composeTab.setImageResource(R.drawable.tab_shouting);
		_inboxTab.setImageResource(R.drawable.tab_inbox);
		_profileTab.setImageResource(R.drawable.tab_user_on);
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
	
	public void unableToTurnOnApp(boolean isLocationAvailable, boolean isDataAvailable) {
		SBLog.i(TAG, "unableToTurnOnApp()");
		String text = "unable to turn on app, ";
		if (!isLocationAvailable && !isDataAvailable) {
			text += "location and data connection unavailable";
		} else if (!isLocationAvailable) {
			text += "location unavailable";
		} else if (!isDataAvailable) {
			text += "data unavailable";
		}
		Toast.makeText(Shoutbreak.this, text, Toast.LENGTH_SHORT).show();
	}

	public void setTitleBarText(String text) {
		SBLog.i(TAG, "setTitleBarText()");
		((TextView) findViewById(R.id.tvTitleBar)).setText(text);
	}
	
	public void hideKeyboard() {
		SBLog.i(TAG, "hideKeyboard()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(_textbox.getWindowToken(), 0);
	}
}