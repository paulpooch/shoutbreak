package co.shoutbreak;

import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.CellDensity;
import co.shoutbreak.shared.ErrorManager;
import co.shoutbreak.shared.ISO8601DateParser;
import co.shoutbreak.shared.SBLog;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

public class User implements Colleague {
	
	// All Database stuff should go through User. Any writes should be
	// synchronized.
	private static final String TAG = "User";
	
	private Mediator _m;
	private Database _db;
	private HashMap<String, String> _userSettings;
	private CellDensity _cellDensity;
	private boolean _passwordExists; // no reason to put actual pw into memory
	private boolean _userSettingsAreStale;
	private boolean _levelUpOccured;
	private String _uid;
	private String _auth;
	private int _level;
	
	@Override
	public void setMediator(Mediator mediator) {
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		_db = null;
		_m = null;
	}
	
	public User(Database db) {
		_db = db;
		_passwordExists = false;
		_userSettingsAreStale = true;
	}
	
	// SYNCHRONIZED WRITE METHODS /////////////////////////////////////////////
	public synchronized Long saveUserSetting(String key, String value) {
		SBLog.i(TAG, "saveUserSetting()");
		_userSettingsAreStale = true;
		String sql = "INSERT INTO " + C.DB_TABLE_USER_SETTINGS + " (setting_key, setting_value) VALUES (?, ?)";
		SQLiteStatement insert = _db.compileStatement(sql);
		insert.bindString(1, key); // 1-indexed
		insert.bindString(2, value);
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			SBLog.e(TAG, "saveUserSettings()");
		} finally {
			insert.close();
		}
		return 0l;
	}
	
	public synchronized Long saveCellDensity(CellDensity cellDensity) {
		SBLog.i(TAG, "saveCellDensity()");
		String sql = "INSERT INTO " + C.DB_TABLE_DENSITY
				+ " (cell_x, cell_y, density, last_updated) VALUES (?, ?, ?, ?)";
		SQLiteStatement insert = this._db.compileStatement(sql);
		insert.bindLong(1, cellDensity.cellX);
		insert.bindLong(2, cellDensity.cellY);
		insert.bindDouble(3, cellDensity.density);
		insert.bindString(4, Database.getDateAsISO8601String(new Date()));
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			Log.e(getClass().getSimpleName(), "saveUserSetting");
		} finally {
			insert.close();
		}
		return 0l;
	}
	
	public synchronized int calculateUsersPoints() {
		SBLog.i(TAG, "calculateUserPoints()");
		String sql = "SELECT points_value, points_timestamp FROM " + C.DB_TABLE_POINTS + " WHERE points_type = ? ORDER BY points_timestamp DESC";
		Cursor cursor = null;
		String cutoffDate = null;
		int points = 0;
		try {
			
			// Get most recent level_change info
			cursor = _db.rawQuery(sql, new String[] { Integer.toString(C.POINTS_LEVEL_CHANGE) });
			if (cursor.moveToFirst()) {
				points = cursor.getInt(0);
				cutoffDate = cursor.getString(1);
			}
			cursor.close();
			
			// Get all valid points
			if (cutoffDate != null) {
				String sql2 = "SELECT points_value FROM " + C.DB_TABLE_POINTS + " WHERE points_timestamp > ?";
				cursor = _db.rawQuery(sql2, new String[] { cutoffDate });
			} else {
				String sql2 = "SELECT points_value FROM " + C.DB_TABLE_POINTS;
				cursor = _db.rawQuery(sql2, null);
			}	
			while (cursor.moveToNext()) {
				points += cursor.getInt(0);
			}
		
		} catch (Exception ex) {
			ErrorManager.manage(ex);
			points = 0;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return points;	
	}
	
	public synchronized CellDensity getDensityAtCell(CellDensity cell) {
		SBLog.i(TAG, "getDensityAtCell()");
		CellDensity result = new CellDensity();
		result.isSet = false;
		String sql = "SELECT density, last_updated FROM " + C.DB_TABLE_DENSITY + " WHERE cell_x = ? AND cell_y = ?";
		Cursor cursor = null;
		try {
			cursor = _db.rawQuery(sql, new String[] { Integer.toString(cell.cellX), Integer.toString(cell.cellY) });
			if (cursor.moveToFirst()) {
				String lastUpdated = cursor.getString(1);
				long lastUpdatedMillisecs = ISO8601DateParser.parse(lastUpdated).getTime();
				long diff = (new Date().getTime()) - lastUpdatedMillisecs;
				if (diff < C.CONFIG_DENSITY_EXPIRATION) {
					result.density = cursor.getDouble(0);
					result.isSet = true;
					return result;
				}
			}
		} catch (Exception ex) {
			Log.e(getClass().getSimpleName(), "getUserSettings");
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return result;
	}
	
	public synchronized CellDensity getCellDensity() {
		SBLog.i(TAG, "getCellDensity()");
		if (_cellDensity == null) {
			_cellDensity = new CellDensity();
		} else {
			// If _cellDensity exists, see if it's still valid.
			CellDensity oldCellDensity = _cellDensity;
			CellDensity tempCellDensity = _m.getCurrentCell();
			_cellDensity.cellX = tempCellDensity.cellX;
			_cellDensity.cellY = tempCellDensity.cellY;
			if (_cellDensity.isSet && _cellDensity.cellX == oldCellDensity.cellX && _cellDensity.cellY == oldCellDensity.cellY) {
				// We're still in the same cell so return this.
				return _cellDensity;
			}
		}
		// Otherwise we'll see if DB has a cached result. If not, isSet will be false.
		CellDensity tempCellDensity = getDensityAtCell(_cellDensity);
		if (tempCellDensity.isSet) {
			_cellDensity.density = tempCellDensity.density;
			_cellDensity.isSet = true;
		}
		return _cellDensity;
	}
	// READ ONLY METHODS //////////////////////////////////////////////////////

	public boolean hasAccount() {
		return _passwordExists;
	}
	
	public String getUID() {
		return _uid;
	}
	
	public String getAuth() {
		return _auth;
	}
	
	public HashMap<String, String> getUserSettings() {
		// TODO: is this read-only?
		SBLog.i(TAG, "getUserSettings()");
		if (_userSettingsAreStale) {
			_userSettings = new HashMap<String, String>();
			Cursor cursor = null;
			try {
				cursor = _db.query(C.DB_TABLE_USER_SETTINGS, null, null, null, null, null, null, null);
				while (cursor.moveToNext()) {
					_userSettings.put(cursor.getString(0), cursor.getString(1));
				}
				_userSettingsAreStale = false;
			} catch (Exception ex) {
				SBLog.e(TAG, "getUserSettings()");
			} finally {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
		}
		return _userSettings;
	}
	
	public boolean getLevelUpOccured() {
		return _levelUpOccured;
	}
	
	public int getLevel() {
		return _level;
	}
	
	/*
	private ShoutbreakService _service;
	private TelephonyManager _tm;
	private Database _db;
	private CellDensity _cellDensity;
	private LocationTracker _locationTracker;
	protected Inbox _inbox;
	private int _shoutsJustReceived;
	private boolean _levelUpOccured; //This means level up.
	//private boolean _densityJustChanged;
	private boolean _scoresJustReceived;

	private String _auth;
	private boolean _passwordExists; // no reason to put actual pw into memory
	private int _level;
	private int _points;
	private int _nextLevelAt;
	
	public User(ShoutbreakService service, StateManager stateManager, LocationTracker locationTracker) {
		_service = service;
		
		_tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
		_db = new Database(_service);
		_locationTracker = locationTracker;
		_inbox = new Inbox(_service, _db, this);
		_passwordExists = false;
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
		if (userSettings.containsKey(C.KEY_USER_NEXT_LEVEL_AT)) {
			_nextLevelAt = Integer.parseInt(userSettings.get(C.KEY_USER_NEXT_LEVEL_AT));
		}
		initializePoints();
		_cellDensity = getCellDensity();
		
		// initialize user state
		StateEvent e = new StateEvent();
		e.locationServicesChanged = true;
		e.levelChanged = true;
		e.pointsChanged = true;
		e.inboxChanged = true;
		e.densityChanged = true;
		_stateManager.fireStateEvent(e);
		
	}
	
	public synchronized void saveDensity(double density) {
		CellDensity tempCellDensity = _locationTracker.getCurrentCell();
		_cellDensity.cellX = tempCellDensity.cellX;
		_cellDensity.cellY = tempCellDensity.cellY;
		_cellDensity.density = density;
		_db.saveCellDensity(_cellDensity);
		_cellDensity.isSet = true;
		//setDensityJustChanged(true);
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
		//_auth = Hash.sha1(_uid + pw + nonce);	
		_auth = pw + Hash.sha512(pw + nonce + _uid);		
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
	
	public synchronized void levelUp(int newLevel, int newPoints, int nextLevelAt) {
		setLevel(newLevel);
		setNextLevelAt(nextLevelAt);
		setPoints(newPoints);
		_levelUpOccured = true;
	}
	
	private synchronized void setLevel(int level) {
		String sLevel = Integer.toString(level);
		_db.saveUserSetting(C.KEY_USER_LEVEL, sLevel);
		_level = level;
	}
	
	private synchronized void setNextLevelAt(int nextLevelAt) {
		String sNextLevelAt = Integer.toString(nextLevelAt);
		_db.saveUserSetting(C.KEY_USER_NEXT_LEVEL_AT, sNextLevelAt);
		_nextLevelAt = nextLevelAt;
	}
	
	// TODO: implement this for when level changes
	private synchronized void setPoints(int points) {
		//String sPoints = Integer.toString(points);
		//_db.saveUserSetting(C.KEY_USER_POINTS, sPoints);
		_db.savePoints(C.POINTS_LEVEL_CHANGE, points);
		initializePoints();
	}
	
	public synchronized void savePoints(int amount) {
		_db.savePoints(C.POINTS_SHOUT, amount);
		_points += amount;
	}
	
	public int getPoints() {
		return _points;
	}
	
	private synchronized void initializePoints() {
		_points = _db.calculateUsersPoints();
	}
	
	public int getNextLevelAt() {
		return _nextLevelAt;
	}
	
	public void destroy() {
		_stateManager.deleteObserver(this);
		_stateManager = null;
		_service = null;
	}

	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
		
	}
	
	// STATICS ////////////////////////////////////////////////////////////////

	public static float calculateRadius(int power, double density) {
		int maxPeople = power * C.CONFIG_PEOPLE_PER_LEVEL;
		double area = maxPeople / density;
		float radius = (float) Math.sqrt(area / Math.PI);
		return radius;
	}
	
	public static int calculatePower(int people) {
		return (int)Math.ceil((float)people / (float)C.CONFIG_PEOPLE_PER_LEVEL);
	}

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
	
	*/
}