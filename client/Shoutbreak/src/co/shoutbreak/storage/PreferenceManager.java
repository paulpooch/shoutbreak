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
		SBLog.i(TAG, "new PreferenceManager()");
		_m = mediator;
		_preferences = preferences;
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
	
	public void setPowerPreferenceToOn() {
		putBoolean(C.PREFERENCE_POWER_STATE, true);
		_m.onPowerPreferenceEnabled();
	}
	
	public void setPowerPreferenceToOff() {
		putBoolean(C.PREFERENCE_POWER_STATE, false);
		_m.onPowerPreferenceDisabled();
	}
	
	public boolean isPowerPreferenceSetToOn() {
		return getBoolean(C.PREFERENCE_POWER_STATE, true);
	}
}
