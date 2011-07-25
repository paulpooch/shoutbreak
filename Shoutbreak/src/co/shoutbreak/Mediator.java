package co.shoutbreak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.view.View;
import android.widget.LinearLayout;
import co.shoutbreak.LocationTracker.CustomLocationListener;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.SBLog;

public class Mediator {
	
	private static final String TAG = "Mediator";
	
	// colleagues
	private ShoutbreakService _service;
	private Shoutbreak _ui;
	private PreferenceManager _preferences;
	private LocationTracker _location;
	
	// state flags
	private boolean _isUIAlive;
	private boolean _isPollingAlive;
	private boolean _isServiceConnected;
	private boolean _isServiceStarted;
	private boolean _isServiceStartedFromUI;
	private boolean _isServiceStartedFromAlarm;
	private boolean _isServiceStartedFromNotification;
	private boolean _areFlagsInitialized;
	private boolean _isLocationAvailable;
	private boolean _isDataAvailable;
	private boolean _isBeingReferredFromNotification;
	private boolean _isPowerOn;
	
	/* Mediator Lifecycle */
	
	public Mediator(ShoutbreakService service) {
    	SBLog.i(TAG, "new Mediator()");
		// add colleagues
		_service = service;
		_service.setMediator(this);
		_preferences = new PreferenceManager();
		_preferences.setMediator(this);
		_location = new LocationTracker();
		_location.setMediator(this);
	}
	
	public void registerUI(Shoutbreak ui) {
		// ui is created before the mediator exists
		// it must be added once the mediator is created
		SBLog.i(TAG, "registerUI()");
		_isUIAlive = true;
		_ui = ui;
		_ui.setMediator(this);
	}
	
	public void unregisterUI() {
		// called by ui's onDestroy() method
		SBLog.i(TAG, "registerUI()");
		_isUIAlive = false;
		_ui.unsetMediator();
		_ui = null;
	}
	
	public void kill() {
		// removes all colleague references to the mediator
		// called by service's onDestroy() method
    	SBLog.i(TAG, "kill()");
		_service = null;
		
		if (_isUIAlive) {
			// kill the ui if it has been register
			_ui.unsetMediator();
			_ui = null;
		}
		
		_preferences.unsetMediator();
		_preferences = null;
		
		_location.unsetMediator();
		_location = null;
	}
	
	/* Mediator Commands */
	
	// service connected to ui
	public void onServiceConnected(Intent serviceIntent) {
		// called when service handler binds ui and service
		SBLog.i(TAG, "onServiceConnected()");
		_isServiceConnected = true;
			
		// hide splash
		((LinearLayout) _ui.findViewById(R.id.splash)).setVisibility(View.GONE);
		
		// begin the service
		serviceIntent.putExtra(C.APP_LAUNCHED_FROM_UI, true);
		_ui.startService(serviceIntent);
	}
	
	public void onServiceDisconnected() {
		// called when ui unbinds from the service
		SBLog.i(TAG, "onServiceDisconnected()");
		_isServiceConnected = false;
	}

	public void onServiceStartCommand(Intent intent) {
		// never call this directly
		SBLog.i(TAG, "onServiceStart()");
		_isServiceStarted = true;
		
		// determine if app was started from an alarm or notification
		if (intent.getBundleExtra(C.APP_LAUNCHED_FROM_UI) != null) {
			_isServiceStartedFromUI = true;
		} else if (intent.getBundleExtra(C.APP_LAUNCHED_FROM_ALARM) != null) {
			_isServiceStartedFromAlarm = true;
		} else if (intent.getBundleExtra(C.APP_LAUNCHED_FROM_NOTIFICATION) != null) {
			_isServiceStartedFromNotification = true;
		}
		
		initializeFlags();

		if (_isPowerOn && _isLocationAvailable && _isDataAvailable && !_isBeingReferredFromNotification) {
			// compose view
			switchView();
			startPolling();
		} else if (!_isPowerOn) {
			// map disabled view
		} else if (!_isLocationAvailable) {
			// location disabled view
			switchView();
		} else if (!_isDataAvailable) {
			// data disabled view
			switchView();
		} else if (_isBeingReferredFromNotification) {
			// inbox view
			switchView();
			if (_isPowerOn) {
				
			}
		} else {
			// should never get here	
		}
	}
	
	public SharedPreferences getSharedPreferences() {
		SBLog.i(TAG, "getSharedPreferences()");
		return _service.getSharedPreferences(C.PREFERENCE_FILE, Context.MODE_PRIVATE);
	}
	
	public void setPowerPreference() {
		SBLog.i(TAG, "setPowerPreference()");
		if (_preferences.contains(C.POWER_STATE_PREF)) {
			_isPowerOn = _preferences.getBoolean(C.POWER_STATE_PREF, true);
		} else {
			_isPowerOn = true;
		}
	}
	
	public void setAlarmReceiver() {
		ComponentName component = new ComponentName(_service, AlarmReceiver.class);
		int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		if (_isPowerOn) {
			state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		}
		_service.getPackageManager().setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);	
	}

	public void setIsLocationAvailable() {
		LocationManager locationManager = (LocationManager) _service.getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new co.shoutbreak.CustomLocationListener();

		//_location = _locationManager.getLastKnownLocation(_provider);
		_criteria = new Criteria();
		_criteria.setAccuracy(Criteria.ACCURACY_FINE);
		_criteria.setAltitudeRequired(false);
		_criteria.setBearingRequired(false);
		_criteria.setSpeedRequired(false);
		_criteria.setCostAllowed(true);
		_criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		_provider = _locationManager.getBestProvider(_criteria, true);
		//String allowedProviders = Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		_location = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
		isLocationEnabled();
	}
	
	public void setIsDataAvailable() {
		
	}
	
	public void setIsBeingReferredFromNotification() {
		
	}
	
	public void switchView() {
		if (_isUIAlive) {
			
		}
	}
	
	public void initializeFlags() {
		if (!_areFlagsInitialized) {
			setPowerPreference();
			setAlarmReceiver();
			setIsLocationAvailable();
			setIsDataAvailable();
			setIsBeingReferredFromNotification();
			_areFlagsInitialized = true;
		}
	}
	
	public void startPolling() {
		if (!_isPollingAlive) {
			
			initializeFlags();
			
			if (_isPowerOn && _isLocationAvailable && _isDataAvailable) {
				
			}
		}
	}
}
