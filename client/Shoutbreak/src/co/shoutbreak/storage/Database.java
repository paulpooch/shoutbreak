package co.shoutbreak.storage;

import java.text.SimpleDateFormat;
import java.util.Date;

import co.shoutbreak.core.C;
import co.shoutbreak.core.ShoutbreakService;
import co.shoutbreak.core.utils.SBLog;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class Database {

	private static final String TAG = "Database";
	
	private ShoutbreakService _service;
	private SQLiteDatabase _db;
	private OpenHelper _openHelper;
	private static SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public Database(ShoutbreakService service) {
		SBLog.constructor(TAG);
		_service = service;
		_openHelper = new OpenHelper(_service);
		open();
	}
	
	public void open() {
		SBLog.method(TAG, "open()");
		_db = _openHelper.getWritableDatabase();
		_db.setLockingEnabled(true);
	}

	public void close() {
		SBLog.method(TAG, "close()");
		_db.close();
	}

	public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return _db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}
	
	public SQLiteStatement compileStatement(String sql) {
		return _db.compileStatement(sql);
	}
	
	public Cursor rawQuery(String sql, String[] selectionArgs) {
		return _db.rawQuery(sql, selectionArgs);
	}
	
	private static class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(ShoutbreakService service) {
			super(service, C.DB_NAME, null, C.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + C.DB_TABLE_USER_SETTINGS + " (setting_key TEXT, setting_value TEXT)");
			db.execSQL("CREATE TABLE " + C.DB_TABLE_DENSITY
					+ " (cell_x INTEGER, cell_y INTEGER, density REAL, last_updated TEXT)");
			db.execSQL("CREATE TABLE "
					+ C.DB_TABLE_SHOUTS
					+ " (shout_id TEXT, timestamp TEXT, time_received INTEGER, txt TEXT, is_outbox INTEGER, re TEXT, vote INTEGER, hit INTEGER, open INTEGER, ups INTEGER, downs INTEGER, pts INTEGER, state_flag INTEGER)");
			db.execSQL("CREATE TABLE " + C.DB_TABLE_POINTS + " (value INTEGER, type INTEGER, timestamp TEXT)");
			db.execSQL("CREATE TABLE " + C.DB_TABLE_NOTICES + " (type INTEGER, value INTEGER, text TEXT, ref TEXT, timestamp INTEGER, state_flag INTEGER)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_USER_SETTINGS);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_DENSITY);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_SHOUTS);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_POINTS);
			db.execSQL("DROP TABLE IF EXISTS " + C.DB_TABLE_NOTICES);
			onCreate(db);
		}
	}

	public static String getDateAsISO8601String(Date date) {
		String result = ISO8601FORMAT.format(date);
		// convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00
		// - note the added colon for the Timezone
		result = result.substring(0, result.length() - 2) + ":" + result.substring(result.length() - 2);
		return result;
	}
	
}
