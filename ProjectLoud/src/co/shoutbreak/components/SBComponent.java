package co.shoutbreak.components;

import co.shoutbreak.ui.ShoutbreakUI;

public class SBComponent {

	protected final String _TAG;
	protected ShoutbreakUI _ShoutbreakUI;
	
	public SBComponent(ShoutbreakUI context, String tag) {
		_TAG = tag;
		_ShoutbreakUI = context;
	}
}
