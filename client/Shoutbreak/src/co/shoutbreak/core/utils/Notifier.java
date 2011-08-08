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
	
	public Notifier(Mediator mediator, ShoutbreakService service) {
		SBLog.i(TAG, "new Notifier()");
		_service = service;
	}
	
	public void notify(String tickerText, String title, String message) {
		SBLog.i(TAG, "notify()");
		NotificationManager notificationManager = (NotificationManager) _service.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(_service, Shoutbreak.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra(C.APP_LAUNCHED_FROM_NOTIFICATION, true);
	    Notification notification = new Notification(R.drawable.notification_icon, tickerText, System.currentTimeMillis());
	    notification.setLatestEventInfo(_service, title, message, PendingIntent.getActivity(_service, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;
	    notificationManager.notify(C.APP_NOTIFICATION_ID, notification);
	}
	
	public void handleShoutsReceived(int newShouts) {
		if (newShouts > 0) {
			String pluralShout = "shout" + (newShouts > 1 ? "s" : "");
			notify(newShouts + " " + pluralShout + " received", "Shoutbreak", "you have " + newShouts + " new " + pluralShout);
		}
	}
}