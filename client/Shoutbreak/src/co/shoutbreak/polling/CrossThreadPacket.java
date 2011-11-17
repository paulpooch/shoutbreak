package co.shoutbreak.polling;

import org.json.JSONObject;

import co.shoutbreak.core.utils.SBLog;

public class CrossThreadPacket {
	
	private static final String TAG = "CrossThreadPacket";
	
	public CrossThreadPacket() {
		SBLog.constructor(TAG);
	}

	public JSONObject json;
	public Exception exception;
	public int purpose;
	public String[] sArgs;
	public int[] iArgs;
	
}
