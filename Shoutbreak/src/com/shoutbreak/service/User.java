package com.shoutbreak.service;

import java.util.HashMap;

import com.shoutbreak.C;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class User {
	// All Database stuff should go through User. Any writes should be
	// synchronized.

	// STATICS ////////////////////////////////////////////////////////////////

	public static float calculateRadius(int power, double density) {
		int maxPeople = power * C.CONFIG_PEOPLE_PER_LEVEL;
		double area = maxPeople / density;
		float radius = (float) Math.sqrt(area / Math.PI);
		return radius;
	}
	
	public static int calculatePower(int people) {
		return (int)Math.ceil(people / C.CONFIG_PEOPLE_PER_LEVEL);
	}

	// END STATICS ////////////////////////////////////////////////////////////
	
	private ShoutbreakService _service;
	private TelephonyManager _tm;
	private Database _db;
	private CellDensity _cellDensity;
	private LocationTracker _locationTracker;
	protected Inbox _inbox;
	private int _shoutsJustReceived;
	private boolean _levelJustChanged;
	private boolean _densityJustChanged;
	private boolean _scoresJustReceived;
	private String _uid;
	private String _auth;
	private boolean _passwordExists; // no reason to put actual pw into memory
	private int _level;
	private int _points;
	private int _nextLevelAt;
	
	public static void setBooleanPreference(Context context, String key, boolean val) {
		SharedPreferences settings = context.getSharedPreferences(C.PREFS_NAMESPACE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, val);
		editor.commit();
	}

	public static boolean getBooleanPreference(Context context, String key, boolean defaultReturnVal) {
		SharedPreferences settings = context.getSharedPreferences(C.PREFS_NAMESPACE, Context.MODE_PRIVATE);
		boolean val = settings.getBoolean(key, defaultReturnVal);
		return val;
	}
	
	public User(ShoutbreakService service) {
		_service = service;
		_tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
		_db = new Database(_service);
		_locationTracker = new LocationTracker(_service);
		_inbox = new Inbox(_db);
		_passwordExists = false;
		_shoutsJustReceived = 0;
		_scoresJustReceived = false;
		_levelJustChanged = false;
		_densityJustChanged = false;
		_level = 0;
		_points = 0;
		_auth = "default"; // we don't have auth yet... just give us nonce
		HashMap<String, String> userSettings = _db.getUserSettings();
		if (userSettings.containsKey(C.KEY_USER_PW)) {
			_passwordExists = true;
		}
		if (userSettings.containsKey(C.KEY_USER_ID)) {
			_uid = userSettings.get(C.KEY_USER_ID);
		}
		if (userSettings.containsKey(C.KEY_USER_LEVEL)) {
			_level = Integer.parseInt(userSettings.get(C.KEY_USER_LEVEL));
		}
		_cellDensity = new CellDensity();
		_cellDensity.isSet = false;
	}
	
	public LocationTracker getLocationTracker() {
		return _locationTracker;
	}
	
	public void setShoutsJustReceived(int i) {
		_shoutsJustReceived = i;
	}
	
	public int getShoutsJustReceived() {
		return _shoutsJustReceived;
	}
	
	public void setLevelJustChanged(boolean b) {
		_levelJustChanged = b;
	}
	
	public boolean getLevelJustChanged() {
		return _levelJustChanged;
	}
	
	public void setDensityJustChanged(boolean b) {
		_densityJustChanged = b;
	}
	
	public boolean getDensityJustChanged() {
		return _densityJustChanged;
	}
	
	public void setScoresJustReceived(boolean b) {
		_scoresJustReceived = b;
	}
	
	public boolean getScoresJustReceived() {
		return _scoresJustReceived;
	}
	
	public double getLatitude() {
		return _locationTracker.getLatitude();
	}

	public double getLongitude() {
		return _locationTracker.getLongitude();
	}

	public Inbox getInbox() {
		return _inbox;
	}

	public synchronized CellDensity getCellDensity() {

		CellDensity oldCellDensity = _cellDensity;
		CellDensity tempCellDensity = _locationTracker.getCurrentCell();
		_cellDensity.cellX = tempCellDensity.cellX;
		_cellDensity.cellY = tempCellDensity.cellY;

		if (_cellDensity.isSet && _cellDensity.cellX == oldCellDensity.cellX
				&& _cellDensity.cellY == oldCellDensity.cellY) {
			// in same cell
			return _cellDensity;
		} else {
			// check db for cached result
			tempCellDensity = _db.getDensityAtCell(_cellDensity);
			if (tempCellDensity.isSet) {
				_cellDensity.density = tempCellDensity.density;
				_cellDensity.isSet = true;
			}
		}
		return _cellDensity;
	}

	public synchronized void saveDensity(double density) {
		CellDensity tempCellDensity = _locationTracker.getCurrentCell();
		_cellDensity.cellX = tempCellDensity.cellX;
		_cellDensity.cellY = tempCellDensity.cellY;
		_cellDensity.density = density;
		_db.saveCellDensity(_cellDensity);
		_cellDensity.isSet = true;
		setDensityJustChanged(true);
	}

	public String getAuth() {
		return _auth;
	}

	public synchronized void updateAuth(String nonce) {
		String pw = "";
		HashMap<String, String> userSettings = _db.getUserSettings();
		if (userSettings.containsKey(C.KEY_USER_PW)) {
			pw = userSettings.get(C.KEY_USER_PW);
		}
		// $auth = sha1($uid . $pw . $nonce);
		_auth = Hash.sha1(_uid + pw + nonce);
	}

	public boolean hasAccount() {
		return _passwordExists;
	}

	public synchronized void setPassword(String pw) {
		// TODO: should we encrypt or obfuscate this or something?
		// plaintext in db safe?
		_db.saveUserSetting(C.KEY_USER_PW, pw);
		_passwordExists = true;
	}

	public synchronized void setUID(String uid) {
		_db.saveUserSetting(C.KEY_USER_ID, uid);
		_uid = uid;
	}
	
	public synchronized void setLevel(int level) {
		String sLevel = Integer.toString(level);
		_db.saveUserSetting(C.KEY_USER_LEVEL, sLevel);
		_level = level;
		setLevelJustChanged(true);
	}
	
	public synchronized void setPoints(int points) {
		String sPoints = Integer.toString(points);
		_db.saveUserSetting(C.KEY_USER_POINTS, sPoints);
		_points = points;
	}
	
	public synchronized void setNextLevelAt(int nextLevelAt) {
		String sNextLevelAt = Integer.toString(nextLevelAt);
		_db.saveUserSetting(C.KEY_USER_NEXT_LEVEL_AT, sNextLevelAt);
		_nextLevelAt = nextLevelAt;
	}
	
	public String getUID() {
		return _uid;
	}
	
	public int getLevel() {
		return _level;
	}
	
	public int getPoints() {
		return _points;
	}
	
	public int getNextLevelAt() {
		return _nextLevelAt;
	}

	public String getDeviceId() {
		return _tm.getDeviceId();
	}

	public String getPhoneNumber() {
		return _tm.getLine1Number();
	}

	public String getNetworkOperator() {
		return _tm.getNetworkOperatorName();
	}

	public String getAndroidId() {
		return Settings.Secure.getString(_service.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

}