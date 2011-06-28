package co.shoutbreak.ui;

import co.shoutbreak.R;
import co.shoutbreak.components.SBNotificationManager;
import co.shoutbreak.misc.C;
import co.shoutbreak.misc.SBLog;
import co.shoutbreak.service.SBService;
import co.shoutbreak.service.ServiceBridgeInterface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class SBContext extends Activity {

	private final static String TAG = "Launcher.java";

	private SBNotificationManager _NotificationManager;
	private ServiceBridgeInterface _ServiceBinder;
	private SBView _ViewArray[];
	private SBView _currentView;

	/* LIFECYCLE METHODS */

	@Override
	public void onCreate(Bundle bundle) {
		SBLog.i(TAG, "onCreate()");
		super.onCreate(bundle);

		setContentView(R.layout.main);

		// initialize components
		_NotificationManager = new SBNotificationManager(this);

		// set current view
		_ViewArray = new SBView[3];
		_ViewArray[0] = new ComposeView(SBContext.this, "Send Shout", R.id.compose_view, 0);
		_ViewArray[1] = new InboxView(SBContext.this, "Inbox", R.id.inbox_view, 1);
		_ViewArray[2] = new ProfileView(SBContext.this, "Profile", R.id.profile_view, 2);
		switchView(_ViewArray[0]);
		switchView(_ViewArray[2]);

		// connect to service
		Intent serviceIntent = new Intent(SBContext.this, SBService.class);
		serviceIntent.putExtra(C.START_FROM_UI, true);
		startService(serviceIntent); // must be called, BIND_AUTO_CREATE doesn't start service
		bindService(serviceIntent, _ServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection _ServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			SBLog.i(TAG, "onServiceConnected()");
			_ServiceBinder = (ServiceBridgeInterface) service;
			_ServiceBinder.getStateManager().enableData();
			_ServiceBinder.getStateManager().disableData();
			_ServiceBinder.getStateManager().enableData();
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
		/* TODO: not sure if this is needed */
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
		_ServiceBinder = null;
		super.onDestroy();
	}

	/* VIEW METHODS */

	public void switchView(SBView view) {
		if (_currentView != null) {
			_currentView.hide();
		}
		_currentView = view;
		_currentView.show();
	}
}