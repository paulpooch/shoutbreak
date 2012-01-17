package co.shoutbreak.storage;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.utils.SBLog;
import android.content.SharedPreferences;

public class PreferenceManager {

	private static String TAG = "PreferenceManager";
	
	private Mediator _m;
	private SharedPreferences _preferences;
	
	public PreferenceManager(Mediator mediator, SharedPreferences preferences) {
		SBLog.constructor(TAG);
		_m = mediator;
		_preferences = preferences;
	}
	
	public boolean contains(String key) {
		return _preferences.contains(key);
	}
	
	public void putBoolean(String key, boolean value) {
		SBLog.method(TAG, "putBoolean()");
		SharedPreferences.Editor _Editor = _preferences.edit();
		_Editor.putBoolean(key, value);
		_Editor.commit();
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		return _preferences.getBoolean(key, defaultValue);
	}
	
	public void setPowerPreferenceToOn(boolean onUiThread) {
		putBoolean(C.PREFERENCE_POWER_STATE, true);
		_m.onPowerPreferenceEnabled(onUiThread);
	}
	
	public void setPowerPreferenceToOff(boolean onUiThread) {
		putBoolean(C.PREFERENCE_POWER_STATE, false);
		_m.onPowerPreferenceDisabled(onUiThread);
	}
	
	public boolean isPowerPreferenceSetToOn() {
		return getBoolean(C.PREFERENCE_POWER_STATE, true);
	}

	public String getString(String key) {
		return _preferences.getString(key, "");
	}
	
	public void setString(String key, String value) {
		SharedPreferences.Editor _Editor = _preferences.edit();
		_Editor.putString(key, value);
		_Editor.commit();
	}

}
