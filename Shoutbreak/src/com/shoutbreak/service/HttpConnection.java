package com.shoutbreak.service;

import java.io.*;
import java.util.List;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;

import com.shoutbreak.CustomExceptionHandler;
import com.shoutbreak.Vars;

import android.os.*;

// original from
// http://masl.cis.gvsu.edu/2010/04/05/android-code-sample-asynchronous-http-connections/
public class HttpConnection implements Runnable {

	private static final int GET = 0;
	private static final int POST = 1;

	private String _url;
	private int _method;
	private Handler _handler;
	private List<NameValuePair> _data;
	private HttpClient httpClient;
	private MessageObject _messageObject;

	public HttpConnection() {
		this(new Handler());
		// TODO: remove this once we know it's not crashing - probably hurting performance
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(Vars.CRASH_REPORT_ADDRESS));
	}

	public HttpConnection(Handler handler) {
		_handler = handler;
	}

	public void create(int method, String url, List<NameValuePair> data, MessageObject messageObject) {
		_method = method;
		_url = url;
		_data = data;
		if (messageObject == null) {
			_messageObject = new MessageObject();
		} else {
			_messageObject = messageObject;
		}
		ConnectionQueue.getInstance().push(this);
	}

	public void get(String url) {
		create(GET, url, null, null);
	}

	public void post(PostData postData) {
		post(Vars.SERVER_ADDRESS, postData, null);
	}
	
	public void post(PostData postData, MessageObject messageObject) {
		post(Vars.SERVER_ADDRESS, postData, messageObject);
	}
	
	public void post(String url, PostData postData, MessageObject messageObject) {
		List<NameValuePair> data = postData.getNameValuePairs();
		create(POST, url, data, messageObject);
	}

	public void run() {
		_handler.sendMessage(Message.obtain(_handler, Vars.MESSAGE_HTTP_DID_START));
		httpClient = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), 25000);
		try {
			HttpResponse response = null;
			switch (_method) {
				case GET:
					response = httpClient.execute(new HttpGet(_url));
					break;
				case POST:
					HttpPost httpPost = new HttpPost(_url);
					httpPost.setEntity(new UrlEncodedFormEntity(_data));
					response = httpClient.execute(httpPost);				
					break;
			}
			if (_method == GET || _method == POST) {
				processEntity(response.getEntity());
			}
		} catch (Exception e) {
			_messageObject.exception = e;
			_handler.sendMessage(Message.obtain(_handler, Vars.MESSAGE_HTTP_DID_ERROR, _messageObject));
		} finally {
			ConnectionQueue.getInstance().didComplete(this);
		}
	}

	// if continued errors, consider catching throwables
	// https://github.com/appcelerator/titanium_mobile/commit/28b82751c1ceacc7166bd0135518f97b08c2691b
	
	private void processEntity(HttpEntity entity) throws IllegalStateException,	IOException {
		InputStream inStream = null;
		InputStreamReader inStreamReader = null;
		BufferedReader bufferedReader = null;
		JSONObject json;
		try {
			//BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
			
			inStream = entity.getContent();
			inStreamReader = new InputStreamReader(inStream);
			bufferedReader = new BufferedReader(inStreamReader, 8192); // 8kB
			
			String line, result = "";
			while ((line = bufferedReader.readLine()) != null) {
				result += line;
			}				
			if (result.length() > 0) {
				json = new JSONObject(result);
				json = Security.filterJSON(json);
			} else {
				// blank response - ping ok
				json = new JSONObject();
				json.put("code",  Vars.JSON_CODE_PING_OK);
			}
			
			_messageObject.json = json;			
			Message message = Message.obtain(_handler, Vars.MESSAGE_HTTP_DID_SUCCEED, _messageObject);
			_handler.sendMessage(message);
			
		} catch (JSONException ex) {
			ErrorManager.manage(ex);
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			if (inStreamReader != null) {
				inStreamReader.close();
			}
			if (inStream != null) {
				inStream.close();
			}
		}
	}

}
