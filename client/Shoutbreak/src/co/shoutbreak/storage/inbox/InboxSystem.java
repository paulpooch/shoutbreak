package co.shoutbreak.storage.inbox;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Shout;
import co.shoutbreak.core.utils.ErrorManager;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.Database;

public class InboxSystem {
	
	private static final String TAG = "InboxSystem";
	
	private Mediator _m;
	private InboxSystem _self;
	private List<Shout> _displayedShouts;
	private InboxListViewAdapter _listAdapter;
	private ListView.OnItemClickListener _listViewItemClickListener;
	private Database _db;
	
	private HashMap<String, Date> _scoreLastUpdatedAt; 
	
	public InboxSystem(Mediator mediator, Database db) {
		SBLog.constructor(TAG);
		_m = mediator;
		_db = db;
		_self = InboxSystem.this;
		_displayedShouts = new ArrayList<Shout>();
		_scoreLastUpdatedAt = new HashMap<String, Date>();
		_listAdapter = new InboxListViewAdapter(_m, (LayoutInflater)_m.getSystemService(Context.LAYOUT_INFLATER_SERVICE), InboxSystem.this);
		_listViewItemClickListener = new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				InboxViewHolder holder = (InboxViewHolder) view.getTag();
				String shoutId = holder.shout.id;
				holder.collapsed.setVisibility(View.GONE);
				holder.expanded.setVisibility(View.VISIBLE);
				Shout shout = _displayedShouts.get(position);
				if (shout.state_flag == C.SHOUT_STATE_NEW) {
					// This updates the database.
					_self.markShoutAsRead(shout.id);
					// This updates the temporary _shouts entry.
					shout.state_flag = C.SHOUT_STATE_READ;
					publishChange();
				}
				_listAdapter.getCacheExpandState().put(shoutId, true);
			}
		};
	}
	
	// NON-WRITE METHODS //////////////////////////////////////////////////////
	
	public void refreshSignature(String signature, int maxLength) {
		// TODO Auto-generated method stub
		
	}
	
	public List<Shout> getDisplayedShouts() {
		return _displayedShouts;
	}
	
	public void publishChange() {
		_listAdapter.notifyDataSetChanged();
	}
	
	public void initialize() {
		_m.getUiGateway().setupInboxListView(_listAdapter, false, _listViewItemClickListener);
	}
	
	public void undoVote(String shoutId, int vote) {
		_listAdapter.undoVote(shoutId, vote);
	}
	
	public void enableInputs() {
		_listAdapter.setInputAllowed(true);
	}

	public void disableInputs() {
		_listAdapter.setInputAllowed(false);
	}
	
	public void jumpToShoutInInbox(String shoutId) {
		if (_displayedShouts != null && _displayedShouts.size() > 0) {
			for (int i = 0; i < _displayedShouts.size(); i++) {
				Shout shout = _displayedShouts.get(i);
				if (shout.id.equals(shoutId)) {
					_m.getUiGateway().scrollInboxToPosition(i);
					return;
				}
			}
		}
		_m.getUiGateway().toast("Sorry, shout not found in inbox.", Toast.LENGTH_LONG);
	}
		
	public void refreshAll() {
		// TODO Auto-generated method stub
		_displayedShouts = getShoutsForUI();
		publishChange();
	}
	
	private List<Shout> getShoutsForUI() {
		SBLog.method(TAG, "getShoutsForUI()");
		return dbGetShoutsForUI(0, 50);
	}
	
	private List<Shout> dbGetShoutsForUI(int start, int amount) {		
		// Database 
		SBLog.method(TAG, "getShoutsForUI()");
		ArrayList<Shout> results = new ArrayList<Shout>();
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
				s.state_flag = cursor.getInt(12);
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
	
	class ScoreRequest {
		public String id;
		public Date time;
		public ScoreRequest(String id) {
			this.id = id;
			this.time = new Date(0);
			if (_scoreLastUpdatedAt.containsKey(id)) {
				this.time = _scoreLastUpdatedAt.get(id);
			}
		}
	}
	
	public ArrayList<String> getScoreRequestIds() {
		SBLog.method(TAG, "getScoreRequestIds");
		ArrayList<ScoreRequest> sorter = new ArrayList<ScoreRequest>();
		for (Shout shout : _displayedShouts) {
			if (shout.open) {
				ScoreRequest req = new ScoreRequest(shout.id);		
				if (sorter.size() == 0) {
					sorter.add(req);
				} else {
					int i = 0;
					while (i < sorter.size() && req.time.compareTo(sorter.get(i).time) >= 0) {
						i++;
					}
					sorter.add(i, req);
				}				
			}
		}
		ArrayList<String> results = new ArrayList<String>();
		for (int i = 0; i < C.CONFIG_SCORE_REQUEST_LIMIT && i < sorter.size(); i++) {
			results.add(sorter.get(i).id);
		}
		return results;
	}
	
	private Shout dbGetShoutFromDb(String shoutID) {
		SBLog.method(TAG, "getShout()");
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
				s.state_flag = cursor.getInt(12);
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
	
	public void updateScore(JSONObject jsonScore) {
		Shout shout = new Shout();
		shout.id = jsonScore.optString(C.JSON_SHOUT_ID);
		shout.ups = jsonScore.optInt(C.JSON_SHOUT_UPS, C.NULL_UPS);
		shout.downs = jsonScore.optInt(C.JSON_SHOUT_DOWNS, C.NULL_DOWNS);
		shout.hit = jsonScore.optInt(C.JSON_SHOUT_HIT, C.NULL_HIT);
		shout.pts = jsonScore.optInt(C.JSON_POINTS, C.NULL_PTS);
		shout.open = jsonScore.optInt(C.JSON_SHOUT_OPEN, 0) == 1 ? true : C.NULL_OPEN;
		dbUpdateScore(shout);
		 
		_scoreLastUpdatedAt.put(shout.id, new Date());
		
		// If the shout is closed, we checked if it's in our outbox.
		// If it is, we need to save the points.
		if (!shout.open){
			Shout shoutFromDB = dbGetShoutFromDb(shout.id);
			if (shoutFromDB.is_outbox) {
				_m.handlePointsForShout(C.POINTS_SHOUT, shout.pts, shout.id);
			}
		}
		
		refreshShoutFromDb(shout.id);			
	}
	
	private boolean markShoutAsRead(String shoutId) {
		boolean result = dbMarkShoutAsRead(shoutId);
		if (result) {
			refreshShoutFromDb(shoutId);
		}
		return result;
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// SYNCHRONIZED WRITE METHODS ///////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	
	private synchronized void refreshShoutFromDb(String shoutId) {
		for (int i = 0; i < _displayedShouts.size(); i++) {
			Shout shout = _displayedShouts.get(i);
			if (shout.id.equals(shoutId)) {
				shout = dbGetShoutFromDb(shoutId);
				_displayedShouts.remove(i);
				_displayedShouts.add(i, shout);
			}
		}
		publishChange();
	}
	
	public synchronized Shout addShout(JSONObject jsonShout) {
		Shout shout = new Shout();
		shout.id = jsonShout.optString(C.JSON_SHOUT_ID);
		shout.timestamp = jsonShout.optString(C.JSON_SHOUT_TIMESTAMP);
		shout.text = jsonShout.optString(C.JSON_SHOUT_TEXT);
		shout.re = jsonShout.optString(C.JSON_SHOUT_RE);
		Date d = new Date();
		shout.time_received = d.getTime();
		//shout.open = jsonShout.optInt(C.JSON_SHOUT_OPEN, 0) == 1 ? true : C.NULL_OPEN;
		shout.open = true;
		shout.vote = C.NULL_VOTE;
		shout.is_outbox = (jsonShout.optInt(C.JSON_SHOUT_OUTBOX, C.NULL_OUTBOX) == 1) ? true : false;
		shout.hit = jsonShout.optInt(C.JSON_SHOUT_HIT, C.NULL_HIT);
		//shout.ups = jsonShout.optInt(C.JSON_SHOUT_UPS, C.NULL_UPS);
		//shout.downs = jsonShout.optInt(C.JSON_SHOUT_DOWNS, C.NULL_DOWNS);
		shout.ups = 1;
		shout.downs = 0;
		shout.pts = C.NULL_PTS;
		shout.state_flag = C.SHOUT_STATE_NEW;
		shout.score = C.NULL_SCORE;
		dbAddShoutToInbox(shout);
		
		_displayedShouts.add(0, shout);
		publishChange();
		
		return shout;
	}
	
	private synchronized boolean dbMarkShoutAsRead(String shoutID) {
		// Database
		SBLog.method(TAG, "markShoutAsRead()");
		boolean result = false;
		SQLiteStatement update;
		String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET state_flag = ? WHERE shout_id = ?";
		update = _db.compileStatement(sql);
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
	
	private synchronized boolean dbUpdateScore(Shout shout) {
		SBLog.method(TAG, "updateScore()");
		boolean result = false;
		SQLiteStatement update;
		// do we have hit count?
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
	
	private synchronized Long dbAddShoutToInbox(Shout shout) {
		SBLog.method(TAG, "addShoutToInbox()");
		String sql = "INSERT INTO "
				+ C.DB_TABLE_SHOUTS
				+ " (shout_id, timestamp, time_received, txt, is_outbox, re, vote, hit, open, ups, downs, pts, state_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		SQLiteStatement insert = _db.compileStatement(sql);
		insert.bindString(1, shout.id); // 1-indexed
		insert.bindString(2, shout.timestamp);
		insert.bindLong(3, shout.time_received);
		insert.bindString(4, shout.text);
		insert.bindLong(5, shout.is_outbox ? 1 : 0);
		insert.bindString(6, shout.re);
		insert.bindLong(7, shout.vote);
		insert.bindLong(8, shout.hit);
		insert.bindLong(9, shout.open ? 1 : 0);
		insert.bindLong(10, shout.ups);
		insert.bindLong(11, shout.downs);
		insert.bindLong(12, shout.pts);
		insert.bindLong(13, shout.state_flag);
		try {
			return insert.executeInsert();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		} finally {
			insert.close();
		}
		// Memory
		// None.  We'll always re-pull from database in this case.
		return 0l;
	}
	
	public synchronized boolean reflectVote(String shoutId, int vote) {
		boolean result = dbReflectVote(shoutId, vote);
		if (result) {
			refreshShoutFromDb(shoutId);
		}
		return result;
	}
	
	public synchronized boolean dbReflectVote(String shoutId, int vote) {
		SBLog.method(TAG, "reflectVote()");
		boolean result = false;
		String sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET ups = ups + 1, vote = ? WHERE shout_id = ?";
		if (vote < 0) {
			sql = "UPDATE " + C.DB_TABLE_SHOUTS + " SET downs = downs + 1, vote = ? WHERE shout_id = ?";
		}
		SQLiteStatement update = _db.compileStatement(sql);
		update.bindLong(1, vote);
		update.bindString(2, shoutId);
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
	
	public synchronized boolean deleteShout(String shoutId) {
		boolean result = dbDeleteShout(shoutId);
		if (result) {
			for (Shout shout : _displayedShouts) {
				if (shout.id.equals(shoutId)) {
					_displayedShouts.remove(shout);
					break;
				}
			}
			publishChange();
		}
		return result;
	}
	
	public synchronized boolean dbDeleteShout(String shoutId) {
		SBLog.method(TAG, "deleteShout()");
		boolean result = false;
		SQLiteStatement update;
		String sql = "DELETE FROM " + C.DB_TABLE_SHOUTS + " WHERE shout_id = ?";
		update = _db.compileStatement(sql);
		update.bindString(1, shoutId);
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