package co.shoutbreak.components;

import co.shoutbreak.misc.C;
import co.shoutbreak.ui.SBContext;
import android.os.Bundle;

public class SBNotificationManager extends SBComponent {
	
	public SBNotificationManager(SBContext context) {
		super(context, "SBNotificationManager");
	}
	
	public void handleNotificationExtras(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(C.EXTRA_REFERRED_FROM_NOTIFICATION)
					&& extras.getBoolean(C.EXTRA_REFERRED_FROM_NOTIFICATION)) {
				//_SBContext.getSBPageChanger().goToInbox();
			}
		}
	}
}