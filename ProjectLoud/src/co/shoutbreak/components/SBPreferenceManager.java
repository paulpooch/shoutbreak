package co.shoutbreak.components;

import android.content.Context;
import android.content.SharedPreferences;

public class SBPreferenceManager extends SBComponent {

	private final String PREFERENCE_FILE = "PREFERENCES";
	
	public static final String POWER_STATE_PREF = "POWER_STATE_PREF";
	
	SharedPreferences _Preferences;
	
	public SBPreferenceManager(Context context) {
		super(context, "SBPreferenceManager");
		_Preferences = _Context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
	}

	public SharedPreferences get() {
		return _Preferences;
	}	
	
	public void putBoolean(String key, boolean value) {
		SharedPreferences.Editor _Editor = _Preferences.edit();
		_Editor.putBoolean(key, value);
		_Editor.commit();
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		return _Preferences.getBoolean(key, defaultValue);
	}
}
