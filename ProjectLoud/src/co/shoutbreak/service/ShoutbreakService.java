package co.shoutbreak.service;

import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.R;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.SBPreferenceManager;
import co.shoutbreak.shared.StateEvent;
import co.shoutbreak.shared.StateManager;
import co.shoutbreak.shared.User;
import co.shoutbreak.shared.utils.SBLog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

// communicates with the UI via the StateManager
// launches the service loop
public class ShoutbreakService extends Service implements Observer {

	private final String TAG = "ShoutbreakService.java";
	
	private StateManager _stateManager;	
	private User _user;
	private Handler _uiThreadHandler;	
	private boolean _isServiceOn = false;
	private boolean _isPollingAllowed = false;
	private SBNotificationManager _notificationManager;
	
	/* LIFECYCLE METHODS */
	
	@Override
	public IBinder onBind(Intent intent) {
		SBLog.i(TAG, "onBind()");
		return new ServiceBridge();
	}
	
	@Override
	public void onCreate() {
		SBLog.i(TAG, "onCreate()");
		super.onCreate();
		
		_stateManager = new StateManager();
		_stateManager.addObserver(this);
		_stateManager.setIsServiceAlive(true);
		
		_notificationManager = new SBNotificationManager(ShoutbreakService.this);
		
		_user = new User(this, _stateManager);
				
		_uiThreadHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {

				CrossThreadPacket xPacket = (CrossThreadPacket) message.obj;
				int uiCode = xPacket.uiCode;
				int threadPurpose = xPacket.purpose;

				// We need to re-create a new Message object. The old one falls out of scope.
				// This is probably good for dumping stale data anyway.
				Message freshMessage = new Message();
				freshMessage.what = message.what;
				CrossThreadPacket freshXPacket = new CrossThreadPacket();
				freshXPacket.json = xPacket.json; // Sometimes JSON needs to carry between states.
				if (threadPurpose == C.PURPOSE_LOOP_FROM_UI || threadPurpose == C.PURPOSE_LOOP_FROM_UI_DELAYED) {
					freshXPacket.purpose = C.PURPOSE_LOOP_FROM_UI;
				}
				freshMessage.obj = freshXPacket;

				// Anything to do on UI?
				switch (uiCode) {
					case C.UI_RECEIVE_SHOUTS: {
						int newShouts = _user.getShoutsJustReceived();
						if (newShouts > 0) {
							String pluralShout = "shout" + (newShouts > 1 ? "s" : "");
							_notificationManager.notify(newShouts + " " + pluralShout + " received", "Shoutbreak", "you have " + newShouts + " new " + pluralShout);
						}
						break;
					}
				}

				if (_isPollingAllowed) {
					// Do we return Logic?
					switch (threadPurpose) {
						case C.PURPOSE_LOOP_FROM_UI: {
							ServiceThread thread = new ServiceThread(_uiThreadHandler, freshMessage, _user);
							_uiThreadHandler.post(thread);
							break;
						}
						case C.PURPOSE_LOOP_FROM_UI_DELAYED: {
							ServiceThread thread = new ServiceThread(_uiThreadHandler, freshMessage, _user);
							_uiThreadHandler.postDelayed(thread, C.CONFIG_IDLE_LOOP_TIME_WITH_UI_OPEN);
							break;
						}
					}
				}
			}
		};
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SBLog.i(TAG, "onStartCommand()");
		if (!_isServiceOn) {
			_isServiceOn = true;
			
			// enable polling if called from alarm receiver
			Bundle bundle = intent.getExtras();
			if (bundle != null & bundle.getBoolean(AlarmReceiver.LAUNCHED_FROM_ALARM)) {
				SBPreferenceManager preferences = new SBPreferenceManager(ShoutbreakService.this);
				preferences.putBoolean(SBPreferenceManager.POWER_STATE_PREF, true);
				_stateManager.setIsPowerPrefOn(true);
			}
			
			// this is caught in update below.
			_stateManager.setIsPollingOn(true);
			StateEvent e = new StateEvent();
			e.pollingTurnedOn = true;
			_stateManager.fireStateEvent(e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		super.onDestroy();
		
		_user.destroy();
		_user = null;
		
		_stateManager.deleteObserver(this);
		_stateManager.setIsServiceAlive(false);
		_stateManager.setIsServiceBound(false);
		_stateManager.setIsPollingOn(false);
		StateEvent e = new StateEvent();
		e.pollingTurnedOff = true;
		_stateManager.fireStateEvent(e);
		_stateManager = null;
	}
	
	public class ServiceBridge extends Binder implements SBServiceBridgeInterface {
		
		public User getUser() {
			return _user;
		}
		
		public StateManager getStateManager() {
			return _stateManager;
		}
	}

	/* OBSERVER METHODS */
	
	public void update(Observable observable, Object data) {
	
		StateEvent e = (StateEvent)data;
	
		if (e.pollingTurnedOn) {
			_stateManager.setIsDataAvailable(true);
			Toast.makeText(getApplicationContext(), "Polling Started" , Toast.LENGTH_SHORT).show();
			_isPollingAllowed = true;
			Message message = new Message();
			CrossThreadPacket xPacket = new CrossThreadPacket();
			xPacket.purpose = C.PURPOSE_LOOP_FROM_UI;
			message.obj = xPacket;
			message.what = C.STATE_IDLE;
			ServiceThread thread = new ServiceThread(_uiThreadHandler, message, _user);
			_uiThreadHandler.post(thread);
		}
					
		if (e.pollingTurnedOff) {
			Toast.makeText(getApplicationContext(), "Polling Stopped" , Toast.LENGTH_SHORT).show();
			_isPollingAllowed = false;
		}
		
		if (e.uiJustSentShout) {
			Message message = new Message();
			CrossThreadPacket xPacket = new CrossThreadPacket();
			xPacket.purpose = C.PURPOSE_DEATH;
			xPacket.sArgs = new String[] { e.shoutText };
			xPacket.iArgs = new int[] { e.shoutPower };
			message.obj = xPacket;
			message.what = C.STATE_SHOUT;
			ServiceThread thread = new ServiceThread(_uiThreadHandler, message, _user);
			_uiThreadHandler.post(thread);
		}
	}
	
	public StateManager getStateManager() {
		return _stateManager;
	}
	
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
	
}
