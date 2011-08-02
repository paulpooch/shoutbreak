package co.shoutbreak;

import co.shoutbreak.shared.SBLog;
import android.content.SharedPreferences;

public class PreferenceManager implements Colleague {

	private static String TAG = "PreferenceManager";
	
	private Mediator _m;
	private SharedPreferences _preferences;
	
	public PreferenceManager(SharedPreferences preferences) {
		SBLog.i(TAG, "new PreferenceManager()");
		_preferences = preferences;
	}

	@Override
	public void setMediator(Mediator mediator) {
		SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "unSetMediator()");
		_preferences = null;
		_m = null;
	}
	
	public boolean contains(String key) {
		SBLog.i(TAG, "contains()");
		return _preferences.contains(key);
	}
	
	public void putBoolean(String key, boolean value) {
		SBLog.i(TAG, "putBoolean()");
		SharedPreferences.Editor _Editor = _preferences.edit();
		_Editor.putBoolean(key, value);
		_Editor.commit();
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		SBLog.i(TAG, "getBoolean()");
		return _preferences.getBoolean(key, defaultValue);
	}
}
