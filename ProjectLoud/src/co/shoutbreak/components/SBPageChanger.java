package co.shoutbreak.components;

import co.shoutbreak.misc.SBLog;
import co.shoutbreak.ui.ShoutbreakUI;

public class SBPageChanger extends SBComponent {
	
	public SBPageChanger(ShoutbreakUI context) {
		super(context, "SBPageChanger");
	}
	
	public void goToInbox() {
		SBLog.i(_TAG, "goToInbox()");
	}
	
}
