package com.shoutbreak.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.shoutbreak.C;
import com.shoutbreak.CrossThreadPacket;
import com.shoutbreak.R;
import com.shoutbreak.ui.IUIBridge;
import com.shoutbreak.ui.Shoutbreak;

public class ShoutbreakService extends Service {
	
	private NotificationManager _notificationManager;
	private IUIBridge _uiBridge; // This is how we access the UI.
	private final Binder _serviceBridge = new ServiceBridge(); // This is how the UI accesses us.
	private User _user;
	private Handler _uiThreadHandler;   
	
	// LIFECYCLE //////////////////////////////////////////////////////////////
    
    @Override
    public void onCreate() {
        _notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        
        _uiThreadHandler = new Handler() { 
			@Override
		    public void handleMessage(Message message) {
				
				CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
				int uiCode = xPacket.uiCode;
				int threadPurpose = xPacket.purpose;
				
				// We need to re-create a new Message object.  The old one falls out of scope.
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
				switch(uiCode) {
					case C.UI_RECEIVE_SHOUTS: {
						_uiBridge.updateInboxView(_user.getInbox().getShoutsForUI());
					}
					case C.UI_SHOUT_SENT: {
						_uiBridge.shoutSent();
					}
					case C.UI_LEVEL_CHANGE: {
						_uiBridge.test("new level = " + _user.getLevel());
					}
					case C.UI_ACCOUNT_CREATED: {
						_uiBridge.test("account created");
						break;
					}
					
				}
				
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
		};
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        // Cancel the persistent notification.
        _notificationManager.cancel(R.string.local_service_started);
        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _serviceBridge;
    }
	
	// UI CONNECTION //////////////////////////////////////////////////////////
	
	public class ServiceBridge extends Binder implements IServiceBridge {
    	
		// When UI connects to service - it provides us a bridge into it.
		public void registerUIBridge(IUIBridge bridge) {
			_uiBridge = bridge;
		}
		
		// When it disconnects, it lets us know the bridge is dead.
		public void unRegisterUIBridge() {
			_uiBridge = null;
		}
		
		public void runServiceFromUI() {
			Message message = new Message();
			CrossThreadPacket xPacket = new CrossThreadPacket();
			xPacket.purpose = C.PURPOSE_LOOP_FROM_UI;
			message.obj = xPacket;
			message.what = C.STATE_IDLE;
			ServiceThread thread = new ServiceThread(_uiThreadHandler, message, _user);
			_uiThreadHandler.post(thread);
		}
		
		public void activateLocationTracker() {
			_user.getLocationTracker().startListeningToLocation();
		}
		
		public void disableLocationTracker() {
			_user.getLocationTracker().stopListeningToLocation();
			
		}
		
		public CellDensity getCurrentCellDensity() {
			return _user.getCellDensity();
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
		
    };
    
	// THE REST ///////////////////////////////////////////////////////////////
    
    
	
	// METHODS THAT WILL RUN INSIDE SERVICE THREAD ////////////////////////////
    
    // Show a notification while this service is running.
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.notification_icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Shoutbreak.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        _notificationManager.notify(R.string.local_service_started, notification);
    }
    
}