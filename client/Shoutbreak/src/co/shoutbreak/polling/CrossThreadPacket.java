package co.shoutbreak.polling;

import org.json.JSONObject;

public class CrossThreadPacket {
	public JSONObject json;
	public Exception exception;
	public int purpose;
	public String[] sArgs;
	public int[] iArgs;
}