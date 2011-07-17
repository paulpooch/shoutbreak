package co.shoutbreak.service;

import org.json.JSONObject;

public class CrossThreadPacket {
	public JSONObject json;
	public Exception exception;
	public int purpose;
	public int uiCode;
	public String[] sArgs;
	public int[] iArgs;
}
