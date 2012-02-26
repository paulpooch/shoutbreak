package co.shoutbreak.storage;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.crittercism.app.Crittercism;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import co.shoutbreak.core.C;
import co.shoutbreak.core.utils.ErrorManager;
import co.shoutbreak.core.utils.Hash;
import co.shoutbreak.core.utils.ISO8601DateParser;
import co.shoutbreak.core.utils.SBLog;

public class User {
	
	private static final String TAG = "User";
	
	private Database _db;
	private HashMap<String, String> _userSettings;
	private RadiusCacheCell _radiusAtCell;
	private boolean _passwordExists; // no reason to put actual pw into memory
	private boolean _userSettingsAreStale;
	private boolean _levelUpOccured;
	private String _uid;
	private String _auth;
	private int _level;
	private int _points;
	private int _levelAt;
	private int _nextLevelAt;
	private String _signature;
	private boolean _isSignatureEnabled;
	
	public User(Database db) {
		SBLog.constructor(TAG);
		
		_db = db;
		_passwordExists = false;
		_userSettingsAreStale = true;
		_level = 0;
		_points = 0;
		_auth = "default"; // we don't have auth yet... just give us nonce
		_radiusAtCell = null;
		JSONObject crittercismMetadata = new JSONObject();
		
		HashMap<String, String> userSettings = getUserSettings();
		if (userSettings.containsKey(C.KEY_USER_PW)) {
			_passwordExists = true;
			SBLog.logic("User has an account.");
		} else {
			SBLog.logic("User does not have an account.");
		}
		if (userSettings.containsKey(C.KEY_USER_ID)) {
			_uid = userSettings.get(C.KEY_USER_ID);
			Crittercism.setUsername(_uid);
		}
		if (userSettings.containsKey(C.KEY_USER_LEVEL)) {
			_level = Integer.parseInt(userSettings.get(C.KEY_USER_LEVEL));
			try {
				crittercismMetadata.put("user level", _level);
			} catch (JSONException e) {
				SBLog.e(TAG, e);
			}
		}
		if (userSettings.containsKey(C.KEY_USER_LEVEL_AT)) {
			_levelAt = Integer.parseInt(userSettings.get(C.KEY_USER_LEVEL_AT));
		}
		if (userSettings.containsKey(C.KEY_USER_NEXT_LEVEL_AT)) {
			_nextLevelAt = Integer.parseInt(userSettings.get(C.KEY_USER_NEXT_LEVEL_AT));
		}
		if (userSettings.containsKey(C.KEY_USER_POINTS)) {
			_points = Integer.parseInt(userSettings.get(C.KEY_USER_POINTS));
		}
		
		// send metadata to crittercism (asynchronously)
		Crittercism.setMetadata(crittercismMetadata);
	}

	// STATICS ////////////////////////////////////////////////////////////////

	public static int calculatePower(int people) {
		//return (int)Math.ceil((float)people / (float)C.CONFIG_PEOPLE_PER_LEVEL);
		return people;
	}
	
	public static int calculatePointsForVote(int userLevel) {
		// TODO: This may change to scale for level.
		return 1;
	}
	
	public static int calculateMaxShoutreach(int level) {
		return level;
	}
	
	// NON-WRITE METHODS //////////////////////////////////////////////////////
	
	public int getPoints() {
		return _points;
	}
	
	public int getLevel() {
		return _level;
	}
	
	public boolean hasAccount() {
		return _passwordExists;
	}
	
	public String getUserId() {
		return _uid;
	}
	
	public String getAuth() {
		return _auth;
	}
	
	public boolean getLevelUpOccured() {
		return _levelUpOccured;
	}
	
	public boolean setLevelUpOccured(boolean b) {
		return _levelUpOccured = b;
	}
	
	public int getLevelAt() {
		return _levelAt;
	}
	
	public int getNextLevelAt() {
		return _nextLevelAt;
	}
	
	public RadiusCacheCell getInitialRadiusAtCell(RadiusCacheCell currentCell) {
		_radiusAtCell = getRadiusAtCellDb(currentCell);
		return _radiusAtCell;
	}
	
	public RadiusCacheCell getRadiusAtCellDb(RadiusCacheCell cell) {
		SBLog.method(TAG, "getDensityAtCell()");
		RadiusCacheCell result = new RadiusCacheCell();
		result.isSet = false;
		String sql = "SELECT radius, last_updated FROM " + C.DB_TABLE_RADIUS + " WHERE cell_x = ? AND cell_y = ? AND level = ? ORDER BY last_updated DESC";
		Cursor cursor = null;
		try {
			cursor = _db.rawQuery(sql, new String[] { Integer.toString(cell.cellX), Integer.toString(cell.cellY), Integer.toString(cell.level) });
			if (cursor.moveToFirst()) {
				String lastUpdated = cursor.getString(1);
				long lastUpdatedMillisecs = ISO8601DateParser.parse(lastUpdated).getTime();
				long diff = (new Date().getTime()) - lastUpdatedMillisecs;
				if (diff < C.CONFIG_SHOUTREACH_RADIUS_EXPIRATION) {
					result.radius = cursor.getLong(0);
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
			
	// SYNCHRONIZED WRITE METHODS /////////////////////////////////////////////
	
	public synchronized void savePoints(int pointsType, int pointsValue) {
		storePoints(pointsType, pointsValue);
		_points += pointsValue;
	}
	
	private synchronized Long storePoints(int pointsType, int pointsValue) {
		SBLog.method(TAG, "storePoints()");
		String sql = "INSERT INTO " + C.DB_TABLE_POINTS + " (type, value, timestamp) VALUES (?, ?, ?)";
		SQLiteStatement insert = _db.compileStatement(sql);
		insert.bindLong(1, pointsType);
		insert.bindLong(2, pointsValue);
		insert.bindString(3, Database.getDateAsISO8601String(new Date()));
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			insert.close();
		}
		return 0l;
	}	
	
	public synchronized void saveRadiusForCell(long radius, RadiusCacheCell tempCell) {
		_radiusAtCell.cellX = tempCell.cellX;
		_radiusAtCell.cellY = tempCell.cellY;
		_radiusAtCell.level = getLevel();
		_radiusAtCell.radius = radius;
		saveRadiusForCellToDb(_radiusAtCell);
		_radiusAtCell.isSet = true;
		//setDensityJustChanged(true);
	}
	
	public synchronized Long saveRadiusForCellToDb(RadiusCacheCell radiusAtCell) {
		SBLog.method(TAG, "saveCellDensity()");
		String sql = "INSERT INTO " + C.DB_TABLE_RADIUS
				+ " (cell_x, cell_y, level, radius, last_updated) VALUES (?, ?, ?, ?, ?)";
		SQLiteStatement insert = _db.compileStatement(sql);
		insert.bindLong(1, radiusAtCell.cellX);
		insert.bindLong(2, radiusAtCell.cellY);
		insert.bindLong(3, radiusAtCell.level);
		insert.bindDouble(4, radiusAtCell.radius);
		insert.bindString(5, Database.getDateAsISO8601String(new Date()));
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			Log.e(getClass().getSimpleName(), "saveUserSetting");
		} finally {
			insert.close();
		}
		return 0l;
	}
	
	public synchronized void levelUp(int newLevel, int levelAt, int nextLevelAt) {
		setLevel(newLevel);
		setLevelAt(levelAt);
		setNextLevelAt(nextLevelAt);
		setLevelUpOccured(true);
	}
	
	public synchronized void setPoints(int currentPoints) {
		if (currentPoints != _points) {
			String sPoints = Integer.toString(currentPoints);
			saveUserSetting(C.KEY_USER_POINTS, sPoints);
			_points = currentPoints;
		}
	}
	
	private synchronized void setLevel(int level) {
		String sLevel = Integer.toString(level);
		saveUserSetting(C.KEY_USER_LEVEL, sLevel);
		_level = level;
	}
	
	private synchronized void setLevelAt(int levelAt) {
		String sLevelAt = Integer.toString(levelAt);
		saveUserSetting(C.KEY_USER_LEVEL_AT, sLevelAt);
		_levelAt = levelAt;
	}
	
	private synchronized void setNextLevelAt(int nextLevelAt) {
		String sNextLevelAt = Integer.toString(nextLevelAt);
		saveUserSetting(C.KEY_USER_NEXT_LEVEL_AT, sNextLevelAt);
		_nextLevelAt = nextLevelAt;
	}
	
//	// TODO: implement this for when level changes
//	private synchronized void resetPointsTotalAt(int points) {
//		//String sPoints = Integer.toString(points);
//		//_db.saveUserSetting(C.KEY_USER_POINTS, sPoints);
//		savePoints(C.POINTS_LEVEL_CHANGE, points);
//		initializePoints();
//	}
	
	public synchronized RadiusCacheCell getRadiusAtCell(RadiusCacheCell currentCell) {
		SBLog.method(TAG, "getRadiusAtCell()");
		
		if (_radiusAtCell != null && _radiusAtCell.level == _level && _radiusAtCell.cellX == currentCell.cellX && _radiusAtCell.cellY == currentCell.cellY) {
			// Are we still in the same cell?
			return _radiusAtCell;
		} else {
			// Different cell, let's make a new RadiusCacheCell object
			_radiusAtCell = new RadiusCacheCell();
			_radiusAtCell.cellX = _radiusAtCell.cellX;
			_radiusAtCell.cellY = _radiusAtCell.cellY;
			_radiusAtCell.level = _level;
		
			// Let's see if the database has a value for this cell
			RadiusCacheCell tempCell = getRadiusAtCellDb(_radiusAtCell);
			if (tempCell.isSet) {
				_radiusAtCell.radius = tempCell.radius;
				_radiusAtCell.isSet = true;
			}
			
			// This will only be isSet if database had a usable value
			return _radiusAtCell;
		}
	}
	
	public synchronized void updateAuth(String nonce) {
		String pw = "";
		HashMap<String, String> userSettings = getUserSettings();
		if (userSettings.containsKey(C.KEY_USER_PW)) {
			pw = userSettings.get(C.KEY_USER_PW);
		}
		// $auth = sha1($uid . $pw . $nonce);
		//_auth = Hash.sha1(_uid + pw + nonce);	
		_auth = pw + Hash.sha512(pw + nonce + _uid);		
	}
	
	public synchronized HashMap<String, String> getUserSettings() {
		SBLog.method(TAG, "getUserSettings()");
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
				SBLog.error(TAG, "getUserSettings()");
			} finally {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
		}
		return _userSettings;
	}

//	public synchronized int calculateUsersPoints() {
//		SBLog.method(TAG, "calculateUserPoints()");
//		String sql = "SELECT value, timestamp FROM " + C.DB_TABLE_POINTS + " WHERE type = ? ORDER BY timestamp DESC";
//		Cursor cursor = null;
//		String cutoffDate = null;
//		int points = 0;
//		try {
//			
//			// Get most recent level_change info
//			cursor = _db.rawQuery(sql, new String[] { Integer.toString(C.POINTS_LEVEL_CHANGE) });
//			if (cursor.moveToFirst()) {
//				points = cursor.getInt(0);
//				cutoffDate = cursor.getString(1);
//			}
//			cursor.close();
//			
//			// Get all valid points
//			if (cutoffDate != null) {
//				String sql2 = "SELECT value FROM " + C.DB_TABLE_POINTS + " WHERE timestamp > ?";
//				cursor = _db.rawQuery(sql2, new String[] { cutoffDate });
//			} else {
//				String sql2 = "SELECT value FROM " + C.DB_TABLE_POINTS;
//				cursor = _db.rawQuery(sql2, null);
//			}	
//			while (cursor.moveToNext()) {
//				points += cursor.getInt(0);
//			}
//		
//		} catch (Exception ex) {
//			ErrorManager.manage(ex);
//			points = 0;
//		} finally {
//			if (cursor != null && !cursor.isClosed()) {
//				cursor.close();
//			}
//		}
//		return points;	
//	}
	
	public synchronized Long saveUserSetting(String key, String value) {
		SBLog.method(TAG, "saveUserSetting()");
		_userSettingsAreStale = true;
		String sql = "INSERT INTO " + C.DB_TABLE_USER_SETTINGS + " (setting_key, setting_value) VALUES (?, ?)";
		SQLiteStatement insert = _db.compileStatement(sql);
		insert.bindString(1, key); // 1-indexed
		insert.bindString(2, value);
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			SBLog.error(TAG, "saveUserSettings()");
		} finally {
			insert.close();
		}
		return 0l;
	}
	
	public synchronized void setPassword(String pw) {
		// TODO: should we encrypt or obfuscate this or something?
		// plaintext in db safe?
		saveUserSetting(C.KEY_USER_PW, pw);
		_passwordExists = true;
	}
	

	public synchronized void setUserId(String uid) {
		saveUserSetting(C.KEY_USER_ID, uid);
		_uid = uid;
	}

	public void setSignature(String signature, boolean isSignatureEnabled) {
		_signature = signature;
		_isSignatureEnabled = isSignatureEnabled;
	}
	
	public String getSignature() {
		return _signature;
	}
	
	public boolean getIsSignatureEnabled() {
		return _isSignatureEnabled;
	}
	
}
