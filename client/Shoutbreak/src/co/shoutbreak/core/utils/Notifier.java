package co.shoutbreak.core.utils;

import co.shoutbreak.R;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.ui.Shoutbreak;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Notifier {
	
	private static final String TAG = "Notifier";
	
	private ShoutbreakService _service;
	// Note this number is new shouts since last application open.
	// It's not unread count of inbox.
	// It is also not unacknowledged noticeTab shout notices.
	private int _newShoutsSinceLastOpen;
	private NotificationManager _notificationManager;

	public Notifier(Mediator mediator, ShoutbreakService service) {
    	SBLog.constructor(TAG);
		_service = service;
		_newShoutsSinceLastOpen = 0;
		_notificationManager = (NotificationManager) _service.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	private void resetNotifierShoutCount() {
		_newShoutsSinceLastOpen = 0;
	}
	
	public void clearNotifications() {
		resetNotifierShoutCount();
		_notificationManager.cancel(C.APP_NOTIFICATION_ID);
	}
	
	public void notify(String tickerText, String title, String message) {
		SBLog.method(TAG, "notify()");
		Intent intent = new Intent(_service, Shoutbreak.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra(C.NOTIFICATION_LAUNCHED_FROM_NOTIFICATION, true);
	    Notification notification = new Notification(R.drawable.notification_icon, tickerText, System.currentTimeMillis());
	    notification.setLatestEventInfo(_service, title, message, PendingIntent.getActivity(_service, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;
	    _notificationManager.notify(C.APP_NOTIFICATION_ID, notification);
	}
	
	public void handleShoutsReceived(int newShouts) {
		if (newShouts > 0) {
			_newShoutsSinceLastOpen += newShouts;
			String pluralShout = "shout" + (_newShoutsSinceLastOpen > 1 ? "s" : "");
			notify(_newShoutsSinceLastOpen + " " + pluralShout + " received", "Shoutbreak", "You have " + _newShoutsSinceLastOpen + " new " + pluralShout + ".");
		}
	}
}