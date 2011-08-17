package co.shoutbreak.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Notice;
import co.shoutbreak.core.Shout;
import co.shoutbreak.core.utils.ErrorManager;
import co.shoutbreak.core.utils.SBLog;

public class Storage implements Colleague {

	private static final String TAG = "Storage";
	
	private Mediator _m;
	private Database _db;
	private User _user;
	private Inbox _inbox;
	
	public Storage(Mediator mediator, Database db, CellDensity currentCell) {
		_m = mediator;
		_db = db;
		_user = new User(_db, currentCell);
		_inbox = new Inbox(_db);	
	}

	@Override
	public void unsetMediator() {
		// TODO Auto-generated method stub
		_m = null;
		_db = null;
		_user = null;
		_inbox = null;
	}
	
	public boolean deleteShout(String shoutID) {
		return _inbox.deleteShout(shoutID);
	}

	public List<Shout> getShoutsForUI() {
		return _inbox.getShoutsForUI();
	}
	
	public void handleInboxNewShoutSelected(Shout shout) {
		_inbox.markShoutAsRead(shout.id);
	}
	
	public void handleScoresReceived(JSONArray scores) {
		for (int i = 0; i < scores.length(); i++) {
			try {
				JSONObject jsonScore = scores.getJSONObject(i);
				updateScore(jsonScore);
			} catch (JSONException e) {
				SBLog.e(TAG, e.getMessage());
			}
		}
	}
	
	public void handleShoutsReceived(JSONArray shouts) {
		for (int i = 0; i < shouts.length(); i++) {
			try {
				JSONObject jsonShout = shouts.getJSONObject(i);
				_inbox.addShout(jsonShout);
			} catch (JSONException e) {
				SBLog.e(TAG, e.getMessage());
			}
		}
	}
	
	public void handleVoteFinish(String shoutId, int vote) {
		_inbox.reflectVote(shoutId, vote);
	}
	
	public void updateScore(JSONObject jsonScore) {
		Shout shout = new Shout();
		shout.id = jsonScore.optString(C.JSON_SHOUT_ID);
		shout.ups = jsonScore.optInt(C.JSON_SHOUT_UPS, C.NULL_UPS);
		shout.downs = jsonScore.optInt(C.JSON_SHOUT_DOWNS, C.NULL_DOWNS);
		shout.hit = jsonScore.optInt(C.JSON_SHOUT_HIT, C.NULL_HIT);
		shout.pts = C.NULL_PTS;
		shout.approval = jsonScore.optInt(C.JSON_SHOUT_APPROVAL, C.NULL_APPROVAL);
		shout.open = jsonScore.optInt(C.JSON_SHOUT_OPEN, 0) == 1 ? true : C.NULL_OPEN;
		_inbox.updateScore(shout);
		
		// If the shout is closed, we checked if it's in our outbox.
		// If it is, we need to save the points.
		if (!shout.open){
			Shout shoutFromDB = _inbox.getShout(shout.id);
			if (shoutFromDB.is_outbox) {
				_m.pointsChange(shout.pts);								
			}
		}
	}
	
	public ArrayList<String> getOpenShoutIds() {
		return _inbox.getOpenShoutIDs();
	}
	
	public List<Notice> getNoticesForUI() {
		SBLog.i(TAG, "getNoticesForUI()");
		return getNoticesForUI(0, 50);
	}
	
	public List<Notice> getNoticesForUI(int start, int amount) {		
		SBLog.i(TAG, "getNoticesForUI()");
		ArrayList<Notice> results = new ArrayList<Notice>();
		String sql = "SELECT rowid, type, text, ref, timestamp, state_flag FROM " + C.DB_TABLE_NOTICES + " ORDER BY timestamp DESC LIMIT ? OFFSET ? ";
		Cursor cursor = null;
		try {
			cursor = _db.rawQuery(sql, new String[] { Integer.toString(amount), Integer.toString(start) });
			while (cursor.moveToNext()) {
				Notice n = new Notice();
				n.id = cursor.getLong(0);
				n.type = cursor.getInt(1);
				n.text = cursor.getString(2);
				n.ref = cursor.getString(3);
				n.timestamp = cursor.getLong(4);
				n.state_flag = cursor.getInt(5);
				results.add(n);
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
	
	public void handlePointsChange(int additonalPoints) {
		 _user.savePoints(additonalPoints);		
	}
	
	public void handleDensityChange(double density, CellDensity currentCell) {
		_user.saveDensity(density, currentCell);
	}
	
	public void handleLevelUp(JSONObject levelInfo) {
		try {
			int newLevel = (int) levelInfo.getLong(C.JSON_LEVEL);
			int newPoints = (int) levelInfo.getLong(C.JSON_POINTS);
			int nextLevelAt = (int) levelInfo.getLong(C.JSON_NEXT_LEVEL_AT);
			_user.levelUp(newLevel, newPoints, nextLevelAt);
		} catch (JSONException e) {
			SBLog.e(TAG, e.getMessage());
		}
	}
	
	public int getUserPoints() {
		return _user.getPoints();
	}
	
	public int getUserLevel() {
		return _user.getLevel();
	}
	
	public CellDensity getCellDensity(CellDensity currentCell) {
		return _user.getCellDensity(currentCell);
	}
	
	public void handleAccountCreated(String uid, String password) {
		_user.setUserId(uid);
		_user.setPassword(password);
	}
	
	public boolean getUserHasAccount() {
		return _user.hasAccount();
	}
	
	public String getUserId() {
		return _user.getUserId();
	}
	
	public String getUserAuth() {
		return _user.getAuth();
	}
	
	public boolean getLevelUpOccured() {
		return _user.getLevelUpOccured();
	}	
	
	public ArrayList<String> getOpenShoutIDs() {
		return _inbox.getOpenShoutIDs();
	}
	
	public void updateAuth(String nonce) {
		_user.updateAuth(nonce);
	}
	
	// SYNCHRONIZED WRITE METHODS /////////////////////////////////////////////
	
	public synchronized Long saveNotice(int noticeType, String noticeText, String noticeRef) {
		noticeRef = (noticeRef == null) ? "" : noticeRef;
		Date date = new Date();
		SBLog.i(TAG, "addNotice()");
		String sql = "INSERT INTO " + C.DB_TABLE_NOTICES + " (type, text, ref, timestamp, state_flag) VALUES (?, ?, ?, ?, ?)";
		SQLiteStatement insert = this._db.compileStatement(sql);
		insert.bindLong(1, noticeType);
		insert.bindString(2, noticeText);
		insert.bindString(3, noticeRef);
		insert.bindLong(4, date.getTime());
		insert.bindLong(5, C.SHOUT_STATE_NEW);
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			insert.close();
		}
		return 0l;
	}
	
}
