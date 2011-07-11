package co.shoutbreak.service.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class SBPostData {
	
	private final int DEFAULT_SIZE = 10;
	private List<NameValuePair> _data = new ArrayList<NameValuePair>(DEFAULT_SIZE);
	
	public void add(String key, String value) {
		_data.add(new BasicNameValuePair(key, value));
	}
	
	public List<NameValuePair> getNameValuePairs() {
		return _data;
	}
}