package co.shoutbreak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.widget.LinearLayout;
import co.shoutbreak.shared.SBLog;

public class Mediator {
	
	private static final String TAG = "Mediator";
	
	// colleagues
	private ShoutbreakService _service;
	private Shoutbreak _ui;
	
	// state flags
	private boolean _isUIOn;
	private boolean _isServiceConnected;
	private boolean _isLocationAvailable;
	private boolean _isDataAvailable;
	private boolean _isBeingReferredFromNotification;
	private boolean _isPowerOn;
	
	// shit show of variables
	private Intent _serviceIntent;
	private ServiceBridge _serviceBridge;
	
	/* Mediator Lifecycle */
	
	public Mediator(ShoutbreakService service) {
    	SBLog.i(TAG, "new Mediator()");
		// add colleagues
		_service = service;
		_service.setMediator(this);
		_ui = new Shoutbreak();
		_ui.setMediator(this);
	}
	
	public void kill() {
    	SBLog.i(TAG, "kill()");
		_service = null;
		_ui.unsetMediator();
		_ui = null;
	}
	
	/* Mediator Commands */
	
	public void setUIOn() {
		_isUIOn = true;
	}
	
	public void bindUIToService() {
		if (_isUIOn) {
			_serviceIntent = new Intent(_ui, ShoutbreakService.class);
			_ui.bindService(_serviceIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	private ServiceConnection _serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			_isServiceConnected = true;
			_serviceBridge = (ServiceBridge) service;
			// hide splash
			((LinearLayout) _ui.findViewById(R.id.splash)).setVisibility(View.GONE);
			
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
				
			}
			
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			_isServiceConnected = false;
		}
	
	};

	protected void switchView() {
		// TODO Auto-generated method stub
		
	}
}
