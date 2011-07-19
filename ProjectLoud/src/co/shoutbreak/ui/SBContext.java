package co.shoutbreak.ui;

import com.google.android.maps.MapActivity;

import co.shoutbreak.R;
import co.shoutbreak.service.AlarmReceiver;
import co.shoutbreak.service.SBNotificationManager;
import co.shoutbreak.service.ShoutbreakService;
import co.shoutbreak.service.SBServiceBridgeInterface;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.SBPreferenceManager;
import co.shoutbreak.shared.StateEvent;
import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.SBLog;
import co.shoutbreak.ui.views.ComposeView;
import co.shoutbreak.ui.views.InboxView;
import co.shoutbreak.ui.views.ProfileView;
import co.shoutbreak.ui.views.SBView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

/* SBContext.java */
// application launcher
// starts service and manages views
// shares StateManager with the service
public class SBContext extends MapActivity {
	
	private static final String TAG = "SBContext";
	
	public static final int COMPOSE_VIEW = 0;
	public static final int INBOX_VIEW = 1;
	public static final int PROFILE_VIEW = 2;

	private StateManager _stateManager;
	private SBNotificationManager _notificationManager;
	private SBPreferenceManager _preferenceManager;
	private SBServiceBridgeInterface _serviceBinder;
	private Intent _serviceIntent;
	private SBView _viewArray[];
	private SBView _currentView;
	private ImageButton _powerButton;
	private TextView _titleBar;
	
	private boolean _isPowerOn;
	
	/* LIFECYCLE METHODS */

	@Override
	public void onCreate(Bundle bundle) {
		
		ImageButton composeTab;
		ImageButton inboxTab;
		ImageButton profileTab;
		
		SBLog.i(TAG, "onCreate()");
		super.onCreate(bundle);
		
		// TODO: Can default view (before 3 views load) be a sweet splash with funky texture?
		// this should be displayed while we're initializing components and determining state
		toggleSplash(true);

		// initialize components
		_notificationManager = new SBNotificationManager(this);
		_preferenceManager = new SBPreferenceManager(this);

		// connect to service
		_serviceIntent = new Intent(SBContext.this, ShoutbreakService.class);
		_serviceIntent.putExtra(C.ALARM_START_FROM_UI, true);
		bindService(_serviceIntent, _ServiceConnection, Context.BIND_AUTO_CREATE);
		
		// register tab listeners
		setContentView(R.layout.main);
		composeTab = (ImageButton) findViewById(R.id.composeTab);
		composeTab.setOnClickListener(_composeTabListener);
		inboxTab = (ImageButton) findViewById(R.id.inboxTab);
		inboxTab.setOnClickListener(_inboxTabListener);
		profileTab = (ImageButton) findViewById(R.id.profileTab);
		profileTab.setOnClickListener(_profileTabListener);
		
		_titleBar = (TextView) findViewById(R.id.tvTitleBar);
	}

	private ServiceConnection _ServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			SBLog.i(TAG, "onServiceConnected()");
			_serviceBinder = (SBServiceBridgeInterface) service;
			_stateManager = _serviceBinder.getStateManager();
			_stateManager.setIsServiceBound(true);
			
			// initialize state
			_stateManager.setIsUIOn(true);
			
			// initialize views
			// state manager must be initialized
			_viewArray = new SBView[3];
			_viewArray[COMPOSE_VIEW] = new ComposeView(SBContext.this, "Send Shout", R.id.compose_view, 0);
			_viewArray[INBOX_VIEW] = new InboxView(SBContext.this, "Inbox", R.id.inbox_view, 1);
			_viewArray[PROFILE_VIEW] = new ProfileView(SBContext.this, "Profile", R.id.profile_view, 2);
			switchView(_viewArray[COMPOSE_VIEW]);
			
			// register power button listener
			_powerButton = (ImageButton) findViewById(R.id.powerButton);
			_powerButton.setOnClickListener(_powerButtonListener);
			
			setPowerState(_preferenceManager.getBoolean(SBPreferenceManager.POWER_STATE_PREF, false));
		}

		public void onServiceDisconnected(ComponentName className) {
			// should never be called
			SBLog.i(TAG, "onServiceDisconnected()");
			_serviceBinder = null;
			_stateManager.setIsServiceBound(false);
		}
	};

	@Override
	public void onNewIntent(Intent intent) {
		SBLog.i(TAG, "onNewIntent()");
		_notificationManager.handleNotificationExtras(intent.getExtras());
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
		/* TODO: not sure if this is needed */
		_notificationManager.handleNotificationExtras(getIntent().getExtras());
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
		
		// unregister Views from StateManager
		for (SBView view: _viewArray) {
			view.destroy();
		}
		
		// kill service if power is off
		if (!_isPowerOn) {
			stopService(_serviceIntent);
		}
		
		_stateManager.setIsUIOn(false);
		_stateManager = null;
		unbindService(_ServiceConnection);
		_serviceBinder = null;
		
		super.onDestroy();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/* VIEW METHODS */

	public void switchView(SBView view) {
		SBLog.i(TAG, "switchView(" + view.getName() + ")");
		if (_currentView != null) {
			_currentView.hide();
		}
		_currentView = view;
		_currentView.show();
	}
	
	public SBView getView(int viewId) {
		return _viewArray[viewId];
	}
	
	private void toggleSplash(boolean show) {
		if (show) {
			// show splash
		} else {
			// hide splash
		}
	}
	
	/* BUTTON LISTENERS */
	
	private OnClickListener _composeTabListener = new OnClickListener() {
		public void onClick(View v) {
			switchView(_viewArray[COMPOSE_VIEW]);
		}
	};
	
	private OnClickListener _inboxTabListener = new OnClickListener() {
		public void onClick(View v) {
			switchView(_viewArray[INBOX_VIEW]);
		}
	};
	
	private OnClickListener _profileTabListener = new OnClickListener() {
		public void onClick(View v) {
			switchView(_viewArray[PROFILE_VIEW]);
		}
	};
	
	private OnClickListener _powerButtonListener = new OnClickListener() {
		public void onClick(View v) {
			setPowerState(!_isPowerOn);
		}
	};
	
	/* MISCELLANEOUS */
	
	private void setPowerState(boolean turnOn) {
		ComponentName component = new ComponentName(SBContext.this, AlarmReceiver.class);
		if (turnOn) {
			_isPowerOn = true;
			_powerButton.setImageResource(R.drawable.power_button_on);
			_preferenceManager.putBoolean(SBPreferenceManager.POWER_STATE_PREF, true);
			getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED , PackageManager.DONT_KILL_APP);
			_stateManager.setIsPowerButtonOn(true);
			_stateManager.setIsPowerPrefOn(true);
			StateEvent e = new StateEvent();
			e.pollingTurnedOn = true;
			_stateManager.fireStateEvent(e);
			startService(_serviceIntent); // must be called, BIND_AUTO_CREATE doesn't start service
		} else {
			_isPowerOn = false;
			_powerButton.setImageResource(R.drawable.power_button_off);
			_preferenceManager.putBoolean(SBPreferenceManager.POWER_STATE_PREF, false);
			getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED , PackageManager.DONT_KILL_APP);
			StateEvent e = new StateEvent();
			e.pollingTurnedOff = true;
			_stateManager.fireStateEvent(e);
		}
	}
	
	public User getUser() {
		return _serviceBinder.getUser();
	}
	
	public void setTitleBarText(String s) {
		_titleBar.setText(s);
	}
	
	/* COMPONENT GETTERS */
	
	public StateManager getStateManager() {
		return _stateManager;
	}
}