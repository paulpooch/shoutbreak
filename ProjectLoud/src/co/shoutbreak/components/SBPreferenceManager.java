package co.shoutbreak.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import co.shoutbreak.ui.SBContext;

public class SBPreferenceManager extends SBComponent {

	private final String PREFERENCE_FILE = "PREFERENCES";
	
	SharedPreferences _Preferences;
	
	public SBPreferenceManager(SBContext context) {
		super(context, "SBPreferenceManager");
		_Preferences = _Context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
	}

	public SharedPreferences get() {
		return _Preferences;
	}	
	
	public void putBoolean(String key, boolean value) {
		SharedPreferences.Editor _Editor = _Preferences.edit();
		_Editor.putBoolean(key, value);
		Toast.makeText(_Context, "state: " + value, Toast.LENGTH_SHORT).show();

		_Editor.commit();
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		return _Preferences.getBoolean(key, defaultValue);
	}
}
