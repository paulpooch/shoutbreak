package com.shoutbreak;

import org.json.JSONException;

import android.content.Context;
import android.widget.Toast;

public class ErrorManager {

	public static void manage(Exception ex) {
		if (ex instanceof JSONException) {
						
		}	
	}
	
	public static void warnUser(Context context, String s) {
		Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
	}
		
}