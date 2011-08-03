package co.shoutbreak;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.CellDensity;
import co.shoutbreak.shared.ErrorManager;
import co.shoutbreak.shared.ISO8601DateParser;
import co.shoutbreak.shared.SBLog;
import co.shoutbreak.shared.Shout;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class Database implements Colleague {

	private static final String TAG = "Database";
	
	private Mediator _m;
	private HashMap<String, String> _userSettings;
	private boolean _userSettingsAreStale;
	private ShoutbreakService _service;
	private SQLiteDatabase _db;
	private OpenHelper _openHelper;
	private static SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public Database(ShoutbreakService service) {
		SBLog.i(TAG, "new Database()");
		_service = service;
		_userSettingsAreStale = true;
		_openHelper = new OpenHelper(_service);
		open();
	}

	@Override
	public void setMediator(Mediator mediator) {
		SBLog.i(TAG, "setMediator()");
		_m = mediator;
	}

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "unsetMediator()");
		if (_db.isOpen()) {
			close();
		}
		_service = null;
		_m = null;	
	}	
	
	public void open() {
		SBLog.i(TAG, "open()");
		_db = _openHelper.getWritableDatabase();
		_db.setLockingEnabled(true);
	}

	public void close() {
		SBLog.i(TAG, "close()");
		_db.close();
	}

	public HashMap<String, String> getUserSettings() {
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

	public Long saveUserSetting(String key, String value) {
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

	private static class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(ShoutbreakService service) {
			super(service, C.DB_NAME, null, C.DB_VERSION);
			SBLog.i(TAG, "new OpenHelper()");
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			SBLog.i(TAG, "onCreate()");
			db.execSQL("CREATE TABLE " + C.DB_TABLE_USER_SETTINGS + " (setting_key TEXT, setting_value TEXT)");
			db.execSQL("CREATE TABLE " + C.DB_TABLE_DENSITY
					+ " (cell_x INTEGER, cell_y INTEGER, density REAL, last_updated TEXT)");
			db.execSQL("CREATE TABLE "
					+ C.DB_TABLE_SHOUTS
					+ " (shout_id TEXT, timestamp TEXT, time_received INTEGER, txt TEXT, is_outbox INTEGER, re TEXT, vote INTEGER, hit INTEGER, open INTEGER, ups INTEGER, downs INTEGER, pts INTEGER, approval INTEGER, state_flag INTEGER)");
			db.execSQL("CREATE TABLE " + C.DB_TABLE_POINTS + " (points_value INTEGER, points_type INTEGER, points_timestamp TEXT)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			SBLog.i(TAG, "onUpgrade()");
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_USER_SETTINGS);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_DENSITY);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_SHOUTS);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_POINTS);
			onCreate(db);
		}
	}

	public static String getDateAsISO8601String(Date date) {
		SBLog.i(TAG, "getDateAsISO8601String()");
		String result = ISO8601FORMAT.format(date);
		// convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00
		// - note the added colon for the Timezone
		result = result.substring(0, result.length() - 2) + ":" + result.substring(result.length() - 2);
		return result;
	}

	public CellDensity getDensityAtCell(CellDensity cell) {
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

	public Long saveCellDensity(CellDensity cellDensity) {
		SBLog.i(TAG, "new saveCellDensity()");
		String sql = "INSERT INTO " + C.DB_TABLE_DENSITY
				+ " (cell_x, cell_y, density, last_updated) VALUES (?, ?, ?, ?)";
		SQLiteStatement insert = this._db.compileStatement(sql);
		insert.bindLong(1, cellDensity.cellX);
		insert.bindLong(2, cellDensity.cellY);
		insert.bindDouble(3, cellDensity.density);
		insert.bindString(4, getDateAsISO8601String(new Date()));
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			Log.e(getClass().getSimpleName(), "saveUserSetting");
		} finally {
			insert.close();
		}
		return 0l;
	}

	public Long addShoutToInbox(Shout shout) {
		SBLog.i(TAG, "addShoutToInbox()");
		// (shout_id TEXT, timestamp TEXT, time_received INTEGER, txt TEXT,
		// is_outbox INTEGER, re TEXT, vote INTEGER, hit INTEGER, open INTEGER,
		// ups INTEGER, downs INTEGER, pts INTEGER, approval INTEGER)
		String sql = "INSERT INTO "
				+ C.DB_TABLE_SHOUTS
				+ " (shout_id, timestamp, time_received, txt, is_outbox, re, vote, hit, open, ups, downs, pts, approval, state_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		SQLiteStatement insert = this._db.compileStatement(sql);
		insert.bindString(1, shout.id); // 1-indexed
		insert.bindString(2, shout.timestamp);
		insert.bindLong(3, shout.time_received);
		insert.bindString(4, shout.text);
		insert.bindLong(5, shout.is_outbox ? 0 : 1);
		insert.bindString(6, shout.re);
		insert.bindLong(7, shout.vote);
		insert.bindLong(8, shout.hit);
		insert.bindLong(9, shout.open ? 0 : 1);
		insert.bindLong(10, shout.ups);
		insert.bindLong(11, shout.downs);
		insert.bindLong(12, shout.pts);
		insert.bindLong(13, shout.approval);
		insert.bindLong(14, shout.state_flag);
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			insert.close();
		}
		return 0l;
	}

	public boolean reflectVote(String shoutID, int vote) {
		SBLog.i(TAG, "reflectVote()");
		boolean result = false;
		String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET ups = ups + 1, vote = ? WHERE shout_id = ?";
		if (vote < 0) {
			sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET downs = downs + 1, vote = ? WHERE shout_id = ?";
		}
		SQLiteStatement update = this._db.compileStatement(sql);
		update.bindLong(1, vote);
		update.bindString(2, shoutID);
		try {
			update.execute();
			result = true;
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			update.close();
		}
		return result;
	}

	public boolean updateScore(Shout shout) {
		SBLog.i(TAG, "updateScore()");
		boolean result = false;
		SQLiteStatement update;
		// do we have hit count?
		if (shout.hit != C.NULL_HIT) {
			String sql = "UPDATE " + C.DB_TABLE_SHOUTS
					+ " SET ups = ?, downs = ?, hit = ?, pts = ?, open = ? WHERE shout_id = ?";
			update = this._db.compileStatement(sql);
			update.bindLong(1, shout.ups);
			update.bindLong(2, shout.downs);
			update.bindLong(3, shout.hit);
			update.bindLong(4, shout.pts);
			int isOpen = (shout.open) ? 1 : 0;
			update.bindLong(5, isOpen);
			update.bindString(6, shout.id);
		} else {
			String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET pts = ?, approval = ?, open = ? WHERE shout_id = ?";
			update = this._db.compileStatement(sql);
			update.bindLong(1, shout.pts);
			update.bindLong(2, shout.approval);
			int isOpen = (shout.open) ? 1 : 0;
			update.bindLong(3, isOpen);
			update.bindString(4, shout.id);
		}
		try {
			update.execute();			
			result = true;
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			update.close();
		}
		return result;
	}

	public Long savePoints(int pointsType, int pointsValue) {
		SBLog.i(TAG, "savePoints()");
		String sql = "INSERT INTO " + C.DB_TABLE_POINTS + " (points_type, points_value, points_timestamp) VALUES (?, ?, ?)";
		SQLiteStatement insert = this._db.compileStatement(sql);
		insert.bindLong(1, pointsType);
		insert.bindLong(2, pointsValue);
		insert.bindString(3, getDateAsISO8601String(new Date()));
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			insert.close();
		}
		return 0l;
	}
	
	public ArrayList<Shout> getShouts(int start, int amount) {
		SBLog.i(TAG, "getShouts()");
		// shout_id TEXT, timestamp TEXT, time_received INTEGER, txt TEXT,
		// is_outbox INTEGER, re TEXT, vote INTEGER, hit INTEGER, open INTEGER,
		// ups INTEGER, downs INTEGER, pts INTEGER, approval INTEGER, state_flag
		// INTEGER
		ArrayList<Shout> results = new ArrayList<Shout>();
		// String sql = "SELECT * FROM " + Vars.DB_TABLE_SHOUTS ; // OFFSET ?
		String sql = "SELECT * FROM " + C.DB_TABLE_SHOUTS + " ORDER BY time_received DESC LIMIT ? OFFSET ? ";
		Cursor cursor = null;
		try {
			cursor = _db.rawQuery(sql, new String[] { Integer.toString(amount), Integer.toString(start) });
			// cursor = _db.rawQuery(sql, new String[]);
			while (cursor.moveToNext()) {
				Shout s = new Shout();
				s.id = cursor.getString(0);
				s.timestamp = cursor.getString(1);
				s.time_received = cursor.getLong(2);
				s.text = cursor.getString(3);
				s.is_outbox = cursor.getInt(4) == 1 ? true : false;
				s.re = cursor.getString(5);
				s.vote = cursor.getInt(6);
				s.hit = cursor.getInt(7);
				s.open = cursor.getInt(8) == 1 ? true : false;
				s.ups = cursor.getInt(9);
				s.downs = cursor.getInt(10);
				s.pts = cursor.getInt(11);
				s.approval = cursor.getInt(12);
				s.state_flag = cursor.getInt(13);
				s.calculateScore();
				results.add(s);
			}
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return results;
	}

	public int calculateUsersPoints() {
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
	
	public Shout getShout(String shoutID) {
		SBLog.i(TAG, "getShout()");
		String sql = "SELECT * FROM " + C.DB_TABLE_SHOUTS + " WHERE shout_id = ?";
		Cursor cursor = null;
		try {
			cursor = _db.rawQuery(sql, new String[] { shoutID });
			if (cursor.moveToNext()) {
				Shout s = new Shout();
				s.id = cursor.getString(0);
				s.timestamp = cursor.getString(1);
				s.time_received = cursor.getLong(2);
				s.text = cursor.getString(3);
				s.is_outbox = cursor.getInt(4) == 1 ? true : false;
				s.re = cursor.getString(5);
				s.vote = cursor.getInt(6);
				s.hit = cursor.getInt(7);
				s.open = cursor.getInt(8) == 1 ? true : false;
				s.ups = cursor.getInt(9);
				s.downs = cursor.getInt(10);
				s.pts = cursor.getInt(11);
				s.approval = cursor.getInt(12);
				s.state_flag = cursor.getInt(13);
				s.calculateScore();
				return s;
			}
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return null;
	}

	public boolean markShoutAsRead(String shoutID) {
		SBLog.i(TAG, "markShoutAsRead()");
		boolean result = false;
		SQLiteStatement update;
		String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET state_flag = ? WHERE shout_id = ?";
		update = this._db.compileStatement(sql);
		update.bindString(1, C.SHOUT_STATE_READ + "");
		update.bindString(2, shoutID);
		try {
			update.execute();
			result = true;
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			update.close();
		}
		return result;
	}

	public boolean deleteShout(String shoutID) {
		SBLog.i(TAG, "new deleteShout()");
		boolean result = false;
		SQLiteStatement update;
		String sql = "DELETE FROM " + C.DB_TABLE_SHOUTS + " WHERE shout_id = ?";
		update = this._db.compileStatement(sql);
		update.bindString(1, shoutID);
		try {
			update.execute();
			result = true;
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			update.close();
		}
		return result;
	}
}