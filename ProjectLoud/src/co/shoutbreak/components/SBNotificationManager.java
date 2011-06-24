package co.shoutbreak.components;

import co.shoutbreak.misc.C;
import co.shoutbreak.ui.ShoutbreakUI;
import android.os.Bundle;

public class SBNotificationManager extends SBComponent {
	
	public SBNotificationManager(ShoutbreakUI context) {
		super(context, "SBNotificationManager");
	}
	
	public void handleNotificationExtras(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(C.EXTRA_REFERRED_FROM_NOTIFICATION)
					&& extras.getBoolean(C.EXTRA_REFERRED_FROM_NOTIFICATION)) {
				_ShoutbreakUI.getSBPageChanger().goToInbox();
			}
		}
	}
}