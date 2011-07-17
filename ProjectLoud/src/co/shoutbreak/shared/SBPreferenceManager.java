package co.shoutbreak.shared;

import co.shoutbreak.Component;
import android.content.Context;
import android.content.SharedPreferences;

public class SBPreferenceManager extends Component {

	private static final String PREFERENCE_FILE = "PREFERENCES";
	public static final String POWER_STATE_PREF = "POWER_STATE_PREF";
	
	SharedPreferences _preferences;
	
	public SBPreferenceManager(Context context) {
		super(context, "SBPreferenceManager");
		_preferences = _context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
	}

	public SharedPreferences get() {
		return _preferences;
	}	
	
	public void putBoolean(String key, boolean value) {
		SharedPreferences.Editor _Editor = _preferences.edit();
		_Editor.putBoolean(key, value);
		_Editor.commit();
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		return _preferences.getBoolean(key, defaultValue);
	}
}
