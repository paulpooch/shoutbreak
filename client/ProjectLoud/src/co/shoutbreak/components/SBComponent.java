package co.shoutbreak.components;

import co.shoutbreak.ui.SBContext;

public class SBComponent {

	protected final String _TAG;
	protected SBContext _Context;
	
	public SBComponent(SBContext context, String tag) {
		_TAG = tag;
		_Context = context;
	}
}
