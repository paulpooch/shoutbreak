package co.shoutbreak;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.os.Message;

import co.shoutbreak.shared.C;
import co.shoutbreak.shared.ErrorManager;
import co.shoutbreak.shared.SBLog;
import co.shoutbreak.shared.Shout;

public class Inbox implements Colleague {
	
	private static final String TAG = "Inbox";
	
	private Mediator _m;
	private List<Shout> _shouts;
	private Database _db;
	
	public Inbox(Mediator mediator, Database db) {
		SBLog.i(TAG, "");
		_m = mediator;
		_db = db;
		_shouts = new ArrayList<Shout>();
	}

	@Override
	public void unsetMediator() {
		SBLog.i(TAG, "");
		_db = null;
		_m = null;
	}
	
	public void vote(String shoutID, int vote) {
		// TODO: this should go in the inbox, ServiceThread doesn't exist though
		// taken from old service bridge method
		Message message = new Message();
		CrossThreadPacket xPacket = new CrossThreadPacket();
		xPacket.purpose = C.PURPOSE_DEATH;
		xPacket.sArgs = new String[] { shoutID };
		xPacket.iArgs = new int[] { vote };
		message.obj = xPacket;
		message.what = C.STATE_VOTE;
		_m.spawnANewUIServiceThread(message);
	}
	
	public synchronized boolean deleteShout(String shoutID) {
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
	
	// SYNCHRONIZED WRITE METHODS /////////////////////////////////////////////
	
	// needs to be synchronized?
	public synchronized void handleShoutsReceivedEvent(JSONArray shouts) {
		for (int i = 0; i < shouts.length(); i++) {
			try {
				JSONObject jsonShout = shouts.getJSONObject(i);
				addShout(jsonShout);
			} catch (JSONException e) {
				SBLog.e(TAG, e.getMessage());
			}
		}
	}
	
	public synchronized void handleScoresReceivedEvent(JSONArray scores) {
		for (int i = 0; i < scores.length(); i++) {
			try {
				JSONObject jsonScore = scores.getJSONObject(i);
				updateScore(jsonScore);
			} catch (JSONException e) {
				SBLog.e(TAG, e.getMessage());
			}
		}
	}
	
	public synchronized void handleVoteSentEvent(String shoutId, int vote) {
		reflectVote(shoutId, vote);
	}
	
	public synchronized Long addShoutToInbox(Shout shout) {
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
	
	public synchronized boolean reflectVote(String shoutID, int vote) {
		SBLog.i(TAG, "reflectVote()");
		boolean result = false;
		String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET ups = ups + 1, vote = ? WHERE shout_id = ?";
		if (vote < 0) {
			sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET downs = downs + 1, vote = ? WHERE shout_id = ?";
		}
		SQLiteStatement update = _db.compileStatement(sql);
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
	
	public synchronized boolean updateScore(Shout shout) {
		SBLog.i(TAG, "updateScore()");
		boolean result = false;
		SQLiteStatement update;
		// do we have hit count?
		if (shout.hit != C.NULL_HIT) {
			String sql = "UPDATE " + C.DB_TABLE_SHOUTS
					+ " SET ups = ?, downs = ?, hit = ?, pts = ?, open = ? WHERE shout_id = ?";
			update = _db.compileStatement(sql);
			update.bindLong(1, shout.ups);
			update.bindLong(2, shout.downs);
			update.bindLong(3, shout.hit);
			update.bindLong(4, shout.pts);
			int isOpen = (shout.open) ? 1 : 0;
			update.bindLong(5, isOpen);
			update.bindString(6, shout.id);
		} else {
			String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET pts = ?, approval = ?, open = ? WHERE shout_id = ?";
			update = _db.compileStatement(sql);
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
	
	public synchronized Shout getShout(String shoutID) {
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

	public synchronized boolean markShoutAsRead(String shoutID) {
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

	public synchronized void addShout(JSONObject jsonShout) {
		Shout shout = new Shout();
		shout.id = jsonShout.optString(C.JSON_SHOUT_ID);
		shout.timestamp = jsonShout.optString(C.JSON_SHOUT_TIMESTAMP);
		shout.text = jsonShout.optString(C.JSON_SHOUT_TEXT);
		shout.re = jsonShout.optString(C.JSON_SHOUT_RE);
		Date d = new Date();
		shout.time_received = d.getTime();
		shout.open = jsonShout.optInt(C.JSON_SHOUT_OPEN, 0) == 1 ? true : C.NULL_OPEN;
		shout.is_outbox = false;
		shout.vote = C.NULL_VOTE;
		shout.hit = jsonShout.optInt(C.JSON_SHOUT_HIT, C.NULL_HIT);
		shout.ups = jsonShout.optInt(C.JSON_SHOUT_UPS, C.NULL_UPS);
		shout.downs = jsonShout.optInt(C.JSON_SHOUT_DOWNS, C.NULL_DOWNS);
		shout.pts = C.NULL_PTS;
		shout.approval = jsonShout.optInt(C.JSON_SHOUT_APPROVAL, C.NULL_APPROVAL);
		shout.state_flag = C.SHOUT_STATE_NEW;
		shout.score = C.NULL_SCORE;		
		addShoutToInbox(shout);
	}
	
	public synchronized void updateScore(JSONObject jsonScore) {
		Shout shout = new Shout();
		shout.id = jsonScore.optString(C.JSON_SHOUT_ID);
		shout.ups = jsonScore.optInt(C.JSON_SHOUT_UPS, C.NULL_UPS);
		shout.downs = jsonScore.optInt(C.JSON_SHOUT_DOWNS, C.NULL_DOWNS);
		shout.hit = jsonScore.optInt(C.JSON_SHOUT_HIT, C.NULL_HIT);
		shout.pts = C.NULL_PTS;
		shout.approval = jsonScore.optInt(C.JSON_SHOUT_APPROVAL, C.NULL_APPROVAL);
		shout.open = jsonScore.optInt(C.JSON_SHOUT_OPEN, 0) == 1 ? true : C.NULL_OPEN;
		updateScore(shout);
		
		// If the shout is closed, we checked if it's in our outbox.
		// If it is, we need to save the points.
		if (!shout.open){
			Shout shoutFromDB = getShout(shout.id);
			if (shoutFromDB.is_outbox) {
				_m.pointsChangeEvent(shout.pts);								
			}
		}
		
	}
	// READ ONLY METHODS //////////////////////////////////////////////////////
	
	public ArrayList<String> getOpenShoutIDs() {
		SBLog.i(TAG, "getOpenShoutIDs");
		ArrayList<String> result = new ArrayList<String>();
		for (Shout shout : _shouts) {
			if (shout.open) {
				result.add(shout.id);
			}
		}
		return result;
	}
	
	public List<Shout> getShoutsForUI() {
		SBLog.i(TAG, "getShoutsForUI");
		return getShoutsForUI(0, 50);
	}
	
	public List<Shout> getShoutsForUI(int start, int amount) {		
		// TODO: is this read-only?
		SBLog.i(TAG, "getShoutsForUI()");
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
	
	
}
	/*
	private Database _db;
	private List<Shout> _shouts;
	private User _user;	
	
	public Inbox(Database db, User user) {
		_db = db;
		_shouts = new ArrayList<Shout>();
		_user = user;
	}
	
	public List<Shout> getShoutsForUI() {
		return this.getShoutsForUI(0, 50);
	}
	
	public List<Shout> getShoutsForUI(int start, int amount) {
		_shouts = _db.getShouts(start, amount);
		return _shouts;
	}
	
	// update ListView if the ui exists right now
//	protected void updateDisplay(List<Shout> shouts) {
//		ShoutbreakUI uiRef = _app.getUIReference().get();
//		if (uiRef != null) {
//			uiRef.getInboxListViewAdapter().updateDisplay(shouts);
//		}
//	}
	
//	public void refresh() {
//		_shouts = _db.getShouts(0, 50);
//		//updateDisplay(_shouts);
//	}
	
	public void refreshShout(String shoutID) {
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutID)) {
				shout = _db.getShout(shoutID);
			}
		}
		//updateDisplay(_shouts);
	}
	
	public ArrayList<String> getOpenShoutIDs() {
		ArrayList<String> result = new ArrayList<String>();
		for (Shout shout : _shouts) {
			if (shout.open) {
				result.add(shout.id);
			}
		}
		return result;
	}
	
	public ArrayList<String> getNewShoutIDs() {
		ArrayList<String> result = new ArrayList<String>();
		for (Shout shout : _shouts) {
			if (shout.state_flag == C.SHOUT_STATE_NEW) {
				result.add(shout.id);
			}
		}
		return result;
	}
	
	public synchronized void reflectVote(String shoutID, int vote) {
		_db.reflectVote(shoutID, vote);
		refreshShout(shoutID);
	}
	
	public synchronized void markShoutAsRead(String shoutID) {
		_db.markShoutAsRead(shoutID);
		refreshShout(shoutID);
		//updateDisplay(_shouts);
	}
	
	public synchronized void deleteShout(String shoutID) {
		_db.deleteShout(shoutID);
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutID)) {
				_shouts.remove(shout);
				break;
			}
		}
		//updateDisplay(_shouts);
		//ShoutbreakUI uiRef = _service.getUIReference().get();
		//if (uiRef != null) {
		//	uiRef.giveNotice("shout deleted");
		//}
	}
	*/
	

