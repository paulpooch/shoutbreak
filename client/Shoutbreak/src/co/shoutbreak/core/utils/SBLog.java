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
//	
//
//	public static void i(String tag, String message) {
//		if (DEBUG_MODE) {
//			Log.i("SB INFO", tag + " : " + message);
//		}
//	}	
//	
//	public static void v(String tag, String message) {
//		if (DEBUG_MODE) {
//			Log.v("SB VERBOSE", tag + " : " + message);
//		}
//	}
//
//	public static void w(String tag, String message) {
//		if (DEBUG_MODE) {
//			Log.w("SB WARN", tag + " : " + message);
//		}
//	}
	
	// For errors.
	public static void error(String tag, String message) {
		if (DEBUG_MODE) {
			Log.e("SB ERROR", tag + " : " + message);
		}
	}

	// For exceptions.
	public static void e(String tag, Exception e) {
		if (DEBUG_MODE) {
			Log.e("SB EXCEPTION", tag + " : " + e.getMessage());
		}
	}
	
	// For Activity/Service/Mediator state & binding tracking.
	public static void lifecycle(String tag, String message) {
		if (DEBUG_MODE) {
			Log.w("SB LIFECYCLE", tag + " : " + message);
		}
	}
	
	// For Activity/Service/Mediator state & binding tracking.
	public static void constructor(String tag) {
		if (DEBUG_MODE) {
			Log.d("SB CONSTRUCTOR", tag);
		}
	}
	
	// For important methods.
	public static void method(String tag, String message) {
		if (DEBUG_MODE) {
			Log.i("SB METHOD", tag + " : " + message);
		}
	}
	
	// For the state of the protocol.
	public static void logic(String info) {
		if (DEBUG_MODE) {
			Log.d("SB LOGIC FLOW", info);
		}
	}

	// When user touches something.
	public static void userAction(String info) {
		if (DEBUG_MODE) {
			Log.d("SB USER ACTION", info);
		}
	}

	// Any GET traffic.
	public static void httpGet(String url) {
		if (DEBUG_MODE) {
			Log.d("SB HTTP GET", "URL = " + url);
		}
	}

	// Any POST traffic.
	public static void httpPost(String url, List<NameValuePair> postData) {
		if (DEBUG_MODE) {
			StringBuffer params = new StringBuffer("\nPOSTDATA\n");
			for (NameValuePair nvp : postData) {
				params.append(nvp.getName() + " = " + nvp.getValue() + "\n");
			}
			Log.d("SB HTTP POST", "URL = " + url + params);
		}
	}

	// All server responses.
	public static void httpResponse(String response) {
		if (DEBUG_MODE) {
			Log.d("SB HTTP RESPONSE", response);
		}
	}

	// For tracking polling delay
	public static void polling(long delay) {
		if (DEBUG_MODE) {
			Log.w("SB POLLING DELAY", Long.toString(delay));
		}
	}
	
}