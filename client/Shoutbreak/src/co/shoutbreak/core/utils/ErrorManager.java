package co.shoutbreak.core.utils;

import org.json.JSONException;


import android.content.Context;
import android.widget.Toast;

public class ErrorManager {
	
	private static final String TAG = "ErrorManager";

	public static void manage(Exception ex) {
		SBLog.error(TAG, "manage()");
		if (ex instanceof JSONException) {
			SBLog.error(TAG, ex.getMessage());
			ex.printStackTrace();
		}	
	}
	
	public static void warnUser(Context context, String s) {
		Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
	}
}
