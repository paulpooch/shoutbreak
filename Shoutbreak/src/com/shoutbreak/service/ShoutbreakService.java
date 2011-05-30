package com.shoutbreak.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.shoutbreak.C;
import com.shoutbreak.CrossThreadPacket;
import com.shoutbreak.R;
import com.shoutbreak.ui.Shoutbreak;

public class ShoutbreakService extends Service {

	// TODO: are we doing anything heavy in UI thread? DON'T!

	//private IUIBridge _uiBridge; // This is how we access the UI.
	// This is how the UI accesses us.
	private final Binder _serviceBridge = new ServiceBridge(); 
	private NotificationManager _notificationManager;
	private User _user;
	private Handler _uiThreadHandler;
	private boolean _isOn;

	// LIFECYCLE //////////////////////////////////////////////////////////////

	@Override
	public void onCreate() {
		super.onCreate();
		
		_notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		_uiThreadHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {

				CrossThreadPacket xPacket = (CrossThreadPacket) message.obj;
				int uiCode = xPacket.uiCode;
				int threadPurpose = xPacket.purpose;

				// We need to re-create a new Message object. The old one falls
				// out of scope.
				// This is probably good for dumping stale data anyway.
				Message freshMessage = new Message();
				freshMessage.what = message.what;
				CrossThreadPacket freshXPacket = new CrossThreadPacket();
				freshXPacket.json = xPacket.json; // Sometimes JSON needs to
													// carry between states.
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

				if (_isOn) {
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
		super.onStartCommand(intent, flags, startId);
		
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.

		if (_user == null) {
			_user = new User(this);
		}
		
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Cancel the persistent notification.
		// _notificationManager.cancel(R.string.local_service_started);
		// Tell the user we stopped.
		//Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return _serviceBridge;
	}

	// UI CONNECTION //////////////////////////////////////////////////////////

	public class ServiceBridge extends Binder implements IServiceBridge {

		public void registerUserListener(UserListener listener) {
			_user.addUserListener(listener);
		}
		
		
		// When UI connects to service - it provides us a bridge into it.
		//public void registerUIBridge(IUIBridge bridge) {
			// Cancel any status bar notification.
			//_notificationManager.cancel(C.APP_NOTIFICATION_ID);
			//_uiBridge = bridge;
		//}

		// When it disconnects, it lets us know the bridge is dead.
		//public void unRegisterUIBridge() {
		//	_uiBridge = null;
		//}

		public void runServiceFromUI() {
			_isOn = true;
			Message message = new Message();
			CrossThreadPacket xPacket = new CrossThreadPacket();
			xPacket.purpose = C.PURPOSE_LOOP_FROM_UI;
			message.obj = xPacket;
			message.what = C.STATE_IDLE;
			ServiceThread thread = new ServiceThread(_uiThreadHandler, message, _user);
			_uiThreadHandler.post(thread);
		}

		public void stopServiceFromUI() {
			_isOn = false;
			_user.removeAllListeners();
		}

		public void toggleLocationTracker(boolean turnOn) {
			_user.getLocationTracker().toggleListeningToLocation(turnOn);
		}
		
		public void pullUserInfo() {
			_user.pullUserInfo();
		}
		
		public void markShoutAsRead(String shoutID) {
			_user.getInbox().markShoutAsRead(shoutID);
		}

		public void shout(String text, int power) {
			Message message = new Message();
			CrossThreadPacket xPacket = new CrossThreadPacket();
			xPacket.purpose = C.PURPOSE_DEATH;
			xPacket.sArgs = new String[] { text };
			xPacket.iArgs = new int[] { power };
			message.obj = xPacket;
			message.what = C.STATE_SHOUT;
			ServiceThread thread = new ServiceThread(_uiThreadHandler, message, _user);
			_uiThreadHandler.post(thread);
		}

		public void vote(String shoutID, int vote) {
			Message message = new Message();
			CrossThreadPacket xPacket = new CrossThreadPacket();
			xPacket.purpose = C.PURPOSE_DEATH;
			xPacket.sArgs = new String[] { shoutID };
			xPacket.iArgs = new int[] { vote };
			message.obj = xPacket;
			message.what = C.STATE_VOTE;
			ServiceThread thread = new ServiceThread(_uiThreadHandler, message, _user);
			_uiThreadHandler.post(thread);
		}

		public void deleteShout(String shoutID) {
			_user.getInbox().deleteShout(shoutID);
		}

	};

	// THE REST ///////////////////////////////////////////////////////////////	
	
	public void giveStatusBarNotification(String alert, String title, String message) {
		Intent intent = new Intent(this, Shoutbreak.class);
		intent.putExtra(C.EXTRA_REFERRED_FROM_NOTIFICATION, true);
	    Notification notification = new Notification(R.drawable.notification_icon, alert, System.currentTimeMillis());
	    notification.setLatestEventInfo(this, title, message,
	    		PendingIntent.getActivity(this.getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;
	    _notificationManager.notify(C.APP_NOTIFICATION_ID, notification);
	}
	
	public boolean doesUIExist() {
		if (_user.getListenerCount() > 0) {
			return true;
		}
		return false;
	}
	
}