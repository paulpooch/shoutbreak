package co.shoutbreak;

import android.content.Context;

public class Component {

	protected final String _TAG;
	protected Context _context;
	
	public Component(Context context, String tag) {
		_TAG = tag;
		_context = context;
	}
}
