package co.shoutbreak.components;

import android.content.Context;

public class SBComponent {

	protected final String _TAG;
	protected Context _context;
	
	public SBComponent(Context context, String tag) {
		_TAG = tag;
		_context = context;
	}
}
