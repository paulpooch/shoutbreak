package co.shoutbreak.storage.inbox;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Shout;
import co.shoutbreak.core.utils.ErrorManager;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.Database;

public class InboxSystem {
	
	private static final String TAG = "Inbox";
	
	private Mediator _m;
	private InboxSystem _self;
	private List<Shout> _shouts;
	private InboxListViewAdapter _listAdapter;
	private ListView.OnItemClickListener _listViewItemClickListener;
	private Database _db;
	
	public InboxSystem(Mediator mediator, Database db) {
		_m = mediator;
		_db = db;
		_self = this;
		_shouts = new ArrayList<Shout>();
		_listAdapter = new InboxListViewAdapter(_m, (LayoutInflater)_m.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
		_listViewItemClickListener = new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				InboxViewHolder holder = (InboxViewHolder) view.getTag();
				String shoutId = holder.shoutId;
				holder.collapsed.setVisibility(View.GONE);
				holder.expanded.setVisibility(View.VISIBLE);
				Shout shout = (Shout)_listAdapter.getItem(position);
				if (shout.state_flag == C.SHOUT_STATE_NEW) {
					_self.markShoutAsRead(shout.id);
					_listAdapter.notifyDataSetChanged();
				}
				_listAdapter.getCacheExpandState().put(shoutId, true);
			}
		};
	}
	
	// NON-WRITE METHODS //////////////////////////////////////////////////////
	
	public void initialize() {
		_m.getUiGateway().setupInboxListView(_listAdapter, false, _listViewItemClickListener);
	}
	
	public void refresh() {
		// TODO Auto-generated method stub
		_shouts = this.getShoutsForUI();
		_listAdapter.refresh(_shouts);
	}
	
	public void undoVote(String shoutId, int vote) {
		_listAdapter.undoVote(shoutId, vote);
	}
	
	private List<Shout> getShoutsForUI() {
		SBLog.i(TAG, "getShoutsForUI");
		return getShoutsForUI(0, 50);
	}
	
	private List<Shout> getShoutsForUI(int start, int amount) {		
		// Database 
		SBLog.i(TAG, "getShoutsForUI()");
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
		// Memory
		updateShoutsList(results);
		return _shouts;
	}
	
	public void addShout(JSONObject jsonShout) {
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
		shout.state_flag = C.SHOUT_STATE_NEW;
		shout.score = C.NULL_SCORE;		
		addShoutToInbox(shout);
	}
	
	private Shout getShout(String shoutID) {
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
	

	
	// SYNCHRONIZED WRITE METHODS /////////////////////////////////////////////
	
	private synchronized boolean markShoutAsRead(String shoutID) {
		// Database
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
		// Memory
		refreshShout(shoutID);
		return result;
	}
	
	public synchronized void updateScore(JSONObject jsonScore) {
		Shout shout = new Shout();
		shout.id = jsonScore.optString(C.JSON_SHOUT_ID);
		shout.ups = jsonScore.optInt(C.JSON_SHOUT_UPS, C.NULL_UPS);
		shout.downs = jsonScore.optInt(C.JSON_SHOUT_DOWNS, C.NULL_DOWNS);
		shout.hit = jsonScore.optInt(C.JSON_SHOUT_HIT, C.NULL_HIT);
		shout.pts = jsonScore.optInt(C.JSON_POINTS, C.NULL_PTS);
		shout.open = jsonScore.optInt(C.JSON_SHOUT_OPEN, 0) == 1 ? true : C.NULL_OPEN;
		this.updateScore(shout);
		
		// If the shout is closed, we checked if it's in our outbox.
		// If it is, we need to save the points.
		if (!shout.open){
			Shout shoutFromDB = this.getShout(shout.id);
			if (shoutFromDB.is_outbox) {
				_m.handlePointsForShout(C.POINTS_SHOUT, shout.pts, shout.id);
			}
		}
	
	}
	
	public synchronized boolean deleteShout(String shoutID) {
		// Database
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
		// Memory
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutID)) {
				_shouts.remove(shout);
				break;
			}
		}
		return result;
	}
	
	private synchronized Long addShoutToInbox(Shout shout) {
		// Database
		SBLog.i(TAG, "addShoutToInbox()");
		String sql = "INSERT INTO "
				+ C.DB_TABLE_SHOUTS
				+ " (shout_id, timestamp, time_received, txt, is_outbox, re, vote, hit, open, ups, downs, pts, state_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
	
	private synchronized void refreshShout(String shoutId) {
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutId)) {
				shout = getShout(shoutId);
			}
		}
	}
	
	private synchronized void updateShoutsList(List<Shout> newList) {
		_shouts = newList;
	}
	
	private synchronized boolean updateScore(Shout shout) {
		SBLog.i(TAG, "updateScore()");
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

	public synchronized boolean reflectVote(String shoutID, int vote) {
		// Database
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
		// Memory
		refreshShout(shoutID);		
		return result;
	}

	public void enableInputs() {
		_listAdapter.setInputAllowed(true);
		
	}

	public void disableInputs() {
		_listAdapter.setInputAllowed(false);
	}

	public void jumpToShoutInInbox(String shoutId) {
		_listAdapter.jumpToShoutInInbox(shoutId);
	}
	
}