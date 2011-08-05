package co.shoutbreak.core.utils;

import android.util.Log;

// log wrapper class
public class SBLog {
	
	private static final boolean DEBUG_MODE = true; 
	
	public static void i(String tag, String message) {
		if (DEBUG_MODE) {
			Log.i("Shoutbreak", "SB - " + tag + ": " + message);
		}
	}
	
	public static void e(String tag, String message) {
		if (DEBUG_MODE) {
			Log.e("Shoutbreak", "SB - " + tag + ": " + message);
		}
	}
}