package co.shoutbreak.polling.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import co.shoutbreak.core.utils.SBLog;

public class PostData {
	
	private static final String TAG = "PostData";
	private final int DEFAULT_SIZE = 10;
	private List<NameValuePair> data = new ArrayList<NameValuePair>(DEFAULT_SIZE);
	
	public PostData() {
		SBLog.constructor(TAG);
	}
	
	public void add(String key, String value) {
		data.add(new BasicNameValuePair(key, value));
	}
	
	public List<NameValuePair> getNameValuePairs() {
		return data;
	}
	
}