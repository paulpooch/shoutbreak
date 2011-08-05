package co.shoutbreak.service;

import co.shoutbreak.Component;
import co.shoutbreak.R;
import co.shoutbreak.shared.C;
import co.shoutbreak.ui.SBContext;
import co.shoutbreak.views.SBView;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SBNotificationManager extends Component {
	
	public SBNotificationManager(Context context) {
		super(context, "SBNotificationManager");
	}
	
	public void notify(String alert, String title, String message) {
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(_context, SBContext.class);
		intent.putExtra(C.EXTRA_REFERRED_FROM_NOTIFICATION, true);
	    Notification notification = new Notification(R.drawable.notification_icon, alert, System.currentTimeMillis());
	    notification.setLatestEventInfo(_context, title, message,
	    		PendingIntent.getActivity(_context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;
	    notificationManager.notify(C.APP_NOTIFICATION_ID, notification);
	}
	
	public void handleNotificationExtras(Bundle extras) {
		SBView inbox;
		if (extras != null) {
			if (extras.containsKey(C.NOTIFICATION_REFERRAL_ID)
					&& extras.getBoolean(C.NOTIFICATION_REFERRAL_ID)) {
				inbox = ((SBContext) _context).getView(SBContext.INBOX_VIEW);
				((SBContext) _context).switchView(inbox);
			}
		}
	}
}