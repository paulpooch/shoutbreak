package co.shoutbreak;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.Flag;
import co.shoutbreak.shared.SBLog;

public class Mediator {
	
	private static final String TAG = "Mediator";
	
	// colleagues
	private ShoutbreakService _service;
	private Shoutbreak _ui;
	private PreferenceManager _preferences;
	//private LocationTracker _location;
	
	// state flags
	private Flag _isUIAlive = new Flag();
	private Flag _isPollingAlive = new Flag();
	private Flag _isServiceAlive = new Flag();
	private Flag _isServiceConnected = new Flag();
	private Flag _isServiceStarted = new Flag();
	private Flag _isLocationAvailable = new Flag();
	private Flag _isWaitingForLocation = new Flag();
	private Flag _isDataAvailable = new Flag();
	
	private LocationProvider _locationProvider;
	
	/* Mediator Lifecycle */
	
	public Mediator(ShoutbreakService service) {
    	SBLog.i(TAG, "new Mediator()");
		// add colleagues
		_service = service;
		_service.setMediator(this);
		_preferences = new PreferenceManager(_service.getSharedPreferences(C.PREFERENCE_FILE, Context.MODE_PRIVATE));
		_preferences.setMediator(this);
		//_location = new LocationTracker();
		//_location.setMediator(this);
	}
	
	public void registerUI(Shoutbreak ui) {
		// ui is created before the mediator exists
		// it must be added once the mediator is created
		SBLog.i(TAG, "registerUI()");
		_isUIAlive.set(true);
		_ui = ui;
		_ui.setMediator(this);
	}
	
	public void unregisterUI(boolean forceKillUI) {
		// called by ui's onDestroy() method
		SBLog.i(TAG, "unregisterUI()");
		if (_isUIAlive.get()) {
			_isUIAlive.set(false);
			_ui.unsetMediator();
			if (forceKillUI) {
				// forces UI to destroy itself if the mediator / service is killed off
				SBLog.e(TAG, "force killed ui, service shutdown while ui running");
				_ui.finish();
			}
			_ui = null;
		} else {
			SBLog.e(TAG, "ui is not alive, unable to unregister");
		}
	}
	
	public void kill() {
		// removes all colleague references to the mediator
		// called by service's onDestroy() method
    	SBLog.i(TAG, "kill()");
		_service = null;
		unregisterUI(true);
		_preferences.unsetMediator();
		_preferences = null;
		//_location.unsetMediator();
		//_location = null;
	}
	
	/* Mediator Commands */
	
	public void onServiceConnected() {
		// called when service handler binds ui and service
		SBLog.i(TAG, "onServiceConnected()");
		_isServiceConnected.set(true);
	}
	
	public void onServiceDisconnected() {
		// called when ui unbinds from the service
		// shouldn't ever be called
		SBLog.i(TAG, "onServiceDisconnected()");
		_isServiceConnected.set(false);
	}
	
	public void appLaunchedFromUI() {
		SBLog.i(TAG, "appLaunchedFromUI()");
		_isServiceStarted.set(true);
		if (_preferences.getBoolean(C.POWER_STATE_PREF, true)) {
			_ui.setPowerState(true);
			startPolling();
		} else {
			_ui.setPowerState(false);
		}
	}
	
	public void appLaunchedFromAlarm() {
		SBLog.i(TAG, "appLaunchedFromAlarm()");
		_isServiceStarted.set(true);
		if (_preferences.getBoolean(C.POWER_STATE_PREF, true)) {
			startPolling();
		}
	}
	
	public void appLaunchedFromNotification() {
		SBLog.i(TAG, "appLaunchedFromNotification()");
		_isServiceStarted.set(true);
		if (_preferences.getBoolean(C.POWER_STATE_PREF, true)) {
			startPolling();
		}
	}
	
	public void startPolling() {
		SBLog.i(TAG, "startPolling()");
		if (!_isPollingAlive.get()) {
			_isPollingAlive.set(true);
			_service.startPolling();
		} else {
			SBLog.e(TAG, "service is already polling, unable to call startPolling()");
		}
	}
	
	public void stopPolling() {
		SBLog.i(TAG, "stopPolling()");
		if (_isPollingAlive.get()) {
			_isPollingAlive.set(false);
			_service.stopPolling();
		} else {
			SBLog.e(TAG, "service is not polling, unable to call stopPolling()");
		}
	}
	
	
	
	/*
		initializeFlags();
		
		if (_isUIAlive) {
			if (_isPowerOn && _isLocationAvailable && _isDataAvailable && _isServiceStartedFromUI) {
				// compose view
				showComposeView();
				startPolling();
			} else if (!_isPowerOn || !_isLocationAvailable || !_isDataAvailable) {
				// map disabled view
				showDisabledView();
			} else if (_isServiceStartedFromNotification) {
				// inbox view
				showInboxView();
			} else {
				// should never get here	
			}
		} else {
			if (_isPowerOn && _isLocationAvailable && _isDataAvailable) {
				startPolling();
			}
		}
		*/
	}
	/*

	
	public void initPowerPreference() {
		SBLog.i(TAG, "initPowerPreference()");
		if (_preferences.contains(C.POWER_STATE_PREF)) {
			_isPowerOn = _preferences.getBoolean(C.POWER_STATE_PREF, true);
		} else {
			_isPowerOn = true;
		}
	}
	
	public void initAlarmReceiver() {
		SBLog.i(TAG, "initAlarmReceiver()");
		ComponentName component = new ComponentName(_service, AlarmReceiver.class);
		int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		if (_isPowerOn) {
			state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		}
		_service.getPackageManager().setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);	
	}

	public void initIsLocationAvailable() {
		LocationManager manager = _location.getLocationManager();

		// check if GPS is enabled
		if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			if (_isUIAlive) {
				new AlertDialog.Builder(_ui)
					.setMessage("Your location seems to be disabled, do you want to enable it?")
				 	.setCancelable(false)
				 	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				 		public void onClick(final DialogInterface dialog, final int id) {
				 	        ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
				 	        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				 	        intent.addCategory(Intent.CATEGORY_LAUNCHER);
				 	        intent.setComponent(toLaunch);
				 	        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				 	        _ui.startActivityForResult(intent, 0);
				 		}
				 })
				 .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				 	public void onClick(final DialogInterface dialog, final int id) {
				 		dialog.cancel();
				 	}
				 })
				 .create().show();
		    }
		}
		
		_isLocationAvailable  = _location.isLocationEnabled();
	}
	
	public void setIsLocationAvailable(boolean isAvailable) {
		_isLocationAvailable = isAvailable;
	}
	
	public void initIsDataAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) _service.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		_isDataAvailable = activeNetworkInfo != null;
	}
	
	public void initializeFlags() {
		if (!_areFlagsInitialized) {
			initPowerPreference();
			initAlarmReceiver();
			initIsLocationAvailable();
			initIsDataAvailable();
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
	
	public void showComposeView() {

	}
	
	public void showInboxView() {
		
	}
	
	public void showProfileView() {
		
	}
	
	public void showDisabledView() {
		
	}
	
	public void hideComposeView() {
		
	}
	
	public void hideInboxView() {
		
	}
	
	public void hideProfileView() {
		
	}
	
	public void showEnableLocationView() {
		
	}
	
	public ShoutbreakService getService() {
		return _service;
	}*/
}
