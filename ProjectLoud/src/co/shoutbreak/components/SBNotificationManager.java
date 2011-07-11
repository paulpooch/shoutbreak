package co.shoutbreak.components;

import co.shoutbreak.misc.C;
import co.shoutbreak.ui.SBContext;
import co.shoutbreak.ui.views.SBView;
import android.os.Bundle;

public class SBNotificationManager extends SBComponent {
	
	public SBNotificationManager(SBContext context) {
		super(context, "SBNotificationManager");
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