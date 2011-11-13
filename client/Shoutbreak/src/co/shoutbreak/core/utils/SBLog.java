package co.shoutbreak.core.utils;

import java.util.List;

import org.apache.http.NameValuePair;

import android.util.Log;

// log wrapper class
public class SBLog {

	private static final boolean DEBUG_MODE = true;

	public static void d(String tag, String message) {
		if (DEBUG_MODE) {
			Log.d("SB DEBUG", tag + " : " + message);
		}
	}
	
	public static void e(String tag, String message) {
		if (DEBUG_MODE) {
			Log.e("SB ERROR", tag + " : " + message);
		}
	}
	
	public static void e(String tag, Exception e) {
		if (DEBUG_MODE) {
			Log.e("SB ERROR", tag + " : " + e.getMessage());
		}
	}

	public static void i(String tag, String message) {
		if (DEBUG_MODE) {
			Log.i("SB INFO", tag + " : " + message);
		}
	}	
	
	public static void v(String tag, String message) {
		if (DEBUG_MODE) {
			Log.v("SB VERBOSE", tag + " : " + message);
		}
	}

	public static void w(String tag, String message) {
		if (DEBUG_MODE) {
			Log.w("SB WARN", tag + " : " + message);
		}
	}
	
	public static void logic(String info) {
		if (DEBUG_MODE) {
			Log.d("SB POLLING FLOW", info);
		}
	}

	public static void userAction(String info) {
		if (DEBUG_MODE) {
			Log.d("SB USER ACTION", info);
		}
	}

	public static void httpGet(String url) {
		if (DEBUG_MODE) {
			Log.d("SB HTTP GET", "URL = " + url);
		}
	}

	public static void httpPost(String url, List<NameValuePair> postData) {
		if (DEBUG_MODE) {
			StringBuffer params = new StringBuffer("\nPOSTDATA\n");
			for (NameValuePair nvp : postData) {
				params.append(nvp.getName() + " = " + nvp.getValue());
			}
			Log.d("SB HTTP POST", "URL = " + url + params);
		}
	}

	public static void httpResponse(String response) {
		if (DEBUG_MODE) {
			Log.d("SB HTTP RESPONSE", response);
		}
	}
}