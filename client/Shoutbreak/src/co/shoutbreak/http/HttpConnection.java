package co.shoutbreak.http;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;

import co.shoutbreak.CrossThreadPacket;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.CustomExceptionHandler;
import co.shoutbreak.shared.ErrorManager;

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
	private CrossThreadPacket _xPacket;

	public HttpConnection() {
		this(new Handler());
		// TODO: remove this once we know it's not crashing - probably hurting performance
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(C.CONFIG_CRASH_REPORT_ADDRESS));
	}

	public HttpConnection(Handler handler) {
		_handler = handler;
	}

	public void create(int method, String url, List<NameValuePair> data, CrossThreadPacket xPacket) {
		_method = method;
		_url = url;
		_data = data;
		if (xPacket == null) {
			_xPacket = new CrossThreadPacket();
		} else {
			_xPacket = xPacket;
		}
		ConnectionQueue.getInstance().push(this);
	}

	public void get(String url) {
		create(GET, url, null, null);
	}

	public void post(PostData postData) {
		post(C.CONFIG_SERVER_ADDRESS, postData, null);
	}
	
	public void post(PostData postData, CrossThreadPacket xPacket) {
		post(C.CONFIG_SERVER_ADDRESS, postData, xPacket);
	}
	
	public void post(String url, PostData postData, CrossThreadPacket xPacket) {
		List<NameValuePair> data = postData.getNameValuePairs();
		create(POST, url, data, xPacket);
	}

	public void run() {
		_handler.sendMessage(Message.obtain(_handler, C.HTTP_DID_START));
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
			_xPacket.exception = e;
			_handler.sendMessage(Message.obtain(_handler, C.HTTP_DID_ERROR, _xPacket));
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
				json = Filter.filterJSON(json);
			} else {
				// blank response - ping ok
				json = new JSONObject();
				json.put("code",  C.JSON_CODE_PING_OK);
			}
			
			_xPacket.json = json;			
			Message message = Message.obtain(_handler, C.HTTP_DID_SUCCEED, _xPacket);
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
