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
import co.shoutbreak.ui.SBContext;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
	private NotificationManager _notificationManager;
	
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
		_user = new User(this);
		_user.addObserver(this);
		
		_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
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
							giveStatusBarNotification(newShouts + " " + pluralShout + " received", "Shoutbreak", "you have " + newShouts + " new " + pluralShout);
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
			_stateManager.setIsServiceOn(true);
			
			// enable polling if called from alarm receiver
			Bundle bundle = intent.getExtras();
			if (bundle != null & bundle.getBoolean(AlarmReceiver.LAUNCHED_FROM_ALARM)) {
				SBPreferenceManager preferences = new SBPreferenceManager(ShoutbreakService.this);
				preferences.putBoolean(SBPreferenceManager.POWER_STATE_PREF, true);
				_stateManager.setIsPowerPrefOn(true);
				_stateManager.setIsPollingOn(true);
				StateEvent e = new StateEvent();
				e.pollingTurnedOn = true;
				_stateManager.fireStateEvent(e);
			}
			
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		SBLog.i(TAG, "onDestroy()");
		super.onDestroy();
		
		_stateManager.deleteObserver(this);
		_stateManager.setIsServiceOn(false);
		_stateManager.setIsPollingOn(false);
		StateEvent e = new StateEvent();
		e.pollingTurnedOff = true;
		_stateManager.fireStateEvent(e);
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
		if (observable instanceof StateManager) {
			// STATE MANAGER //////////////////////////////////////////////////
			StateEvent e = (StateEvent)data;
			if (e.pollingTurnedOn) {
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
		} else if (observable instanceof User) {
			// USER ///////////////////////////////////////////////////////////
				
		}
	}

	// This gives notifications to the status bar.
	// Unable to find a logical way to put this into SBNotificationManager.
	public void giveStatusBarNotification(String alert, String title, String message) {
		Intent intent = new Intent(this, SBContext.class);
		intent.putExtra(C.EXTRA_REFERRED_FROM_NOTIFICATION, true);
	    Notification notification = new Notification(R.drawable.notification_icon, alert, System.currentTimeMillis());
	    notification.setLatestEventInfo(this, title, message,
	    		PendingIntent.getActivity(this.getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;
	    _notificationManager.notify(C.APP_NOTIFICATION_ID, notification);
	}
	
	// TODO: is this ok to do?
	public StateManager getStateManager() {
		return _stateManager;
	}
	
}
