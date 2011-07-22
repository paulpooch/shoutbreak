package co.shoutbreak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.view.View;
import android.widget.LinearLayout;
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
	private boolean _isServiceAlive;
	private boolean _isServiceConnected;
	private boolean _isLocationAvailable;
	private boolean _isDataAvailable;
	private boolean _isBeingReferredFromNotification;
	private boolean _isPowerOn;
	
	// shit show of variables
	private Intent _serviceIntent;
	
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
		SBLog.i(TAG, "registerUI()");
		_ui = ui;
		_ui.setMediator(this);	
	}
	
	public void kill() {
    	SBLog.i(TAG, "kill()");
		_service = null;
		
		if (_isUIAlive) {
			_ui.unsetMediator();
			_ui = null;
		}
		
		_preferences.unsetMediator();
		_preferences = null;
		
		_location.unsetMediator();
		_location = null;
	}
	
	/* Mediator Commands */
	
	public void onServiceConnected() {
		SBLog.i(TAG, "onServiceConnected()");
		_isUIAlive = true;
		_isServiceConnected = true;
			
		// hide splash
		((LinearLayout) _ui.findViewById(R.id.splash)).setVisibility(View.GONE);
		
		setPowerPreference();
		setAlarmReceiver();
		setIsLocationAvailable();
		setIsDataAvailable();
		setIsBeingReferredFromNotification();
		
		if (_isPowerOn && _isLocationAvailable && _isDataAvailable && !_isBeingReferredFromNotification) {
			// compose view
			switchView();
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
		} else {
			// should never get here	
		}
	}
	
	public void onServiceDisconnected() {
		SBLog.i(TAG, "onServiceDisconnected()");
		_isServiceConnected = false;
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
		
	}
	
	public void setIsDataAvailable() {
		
	}
	
	public void setIsBeingReferredFromNotification() {
		
	}
	
	public void switchView() {
		
	}
}
