package co.shoutbreak.user;

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
		putBoolean(C.POWER_STATE_PREF, true);
		_m.onPowerPreferenceEnabled();
	}
	
	public void setPowerPreferenceToOff() {
		putBoolean(C.POWER_STATE_PREF, false);
		_m.onPowerPreferenceDisabled();
	}
	
	public boolean isPowerPreferenceSetToOn() {
		return getBoolean(C.POWER_STATE_PREF, true);
	}
}
