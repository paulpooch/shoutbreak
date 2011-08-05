package co.shoutbreak.core.utils;

import org.json.JSONException;


import android.content.Context;
import android.widget.Toast;

public class ErrorManager {
	
	private static final String TAG = "ErrorManager";

	public static void manage(Exception ex) {
		SBLog.i(TAG, "manage()");
		if (ex instanceof JSONException) {
						
		}	
	}
	
	public static void warnUser(Context context, String s) {
		SBLog.i(TAG, "warnUser()");
		Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
	}
}