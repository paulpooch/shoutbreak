package co.shoutbreak.storage.noticetab;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.utils.ErrorManager;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.Database;

public class NoticeTabSystem implements Colleague {
	
	private static final String TAG = "NoticeTabSystem";
	
	private Mediator _m;
	private NoticeTabListViewAdapter _listAdapter;
	private ListView.OnItemClickListener _listViewItemClickListener;
	private Database _db;
	
	public NoticeTabSystem(Mediator mediator, Database db) {
		SBLog.constructor(TAG);
		_m = mediator;
		_db = db;
		_listAdapter = new NoticeTabListViewAdapter(_m, (LayoutInflater)_m.getSystemService(Context.LAYOUT_INFLATER_SERVICE));		
		_listViewItemClickListener = new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {

			}
		};
	}	

	public void initialize() {
		_m.getUiGateway().setupNoticeTabListView(_listAdapter, false, _listViewItemClickListener);
	}
	
	@Override
	public void unsetMediator() {
		_m = null;
	}
	
	public void refresh() {
		List<Notice> notices = getNoticesForUI();

		int unreadCount = 0;
		int points = 0;
		boolean levelUp = false;
				
		for (Notice notice : notices) {
			if (notice.state_flag == C.NOTICE_STATE_NEW) {			
				// level up?
				if (notice.type == C.NOTICE_LEVEL_UP) {
					levelUp = true;
				}
				// new shouts?
				if (notice.type == C.NOTICE_SHOUTS_RECEIVED) {
					unreadCount += notice.value;
				}
				if (notice.type == C.NOTICE_POINTS_SHOUT || notice.type == C.NOTICE_POINTS_VOTING) {
					points += notice.value;
				}
			}
		}
		if (unreadCount > 0) {
			_m.getUiGateway().showShoutNotice(unreadCount);
		}
		if (levelUp) {
			_m.getUiGateway().showPointsNotice(C.LEVEL_UP_NOTICE);
		} else if (points != 0) {
			_m.getUiGateway().showPointsNotice(points);
		}		

		_listAdapter.refresh(notices);
	}
	
	public void createNotice(int noticeType, int noticeValue, String noticeText, String noticeRef) {
		saveNotice(noticeType, noticeValue, noticeText, noticeRef);
		refresh();
		showOneLine();
	}
	
	public void showOneLine() {
		_m.getUiGateway().showTopNotice();
	}
	
	private List<Notice> getNoticesForUI() {
		SBLog.method(TAG, "getNoticesForUI()");
		return getNoticesForUI(0, C.CONFIG_NOTICES_DISPLAYED_IN_TAB);
	}
	
	private List<Notice> getNoticesForUI(int start, int amount) {		
		SBLog.method(TAG, "getNoticesForUI()");
		ArrayList<Notice> results = new ArrayList<Notice>();
		String sql = "SELECT rowid, type, value, text, ref, timestamp, state_flag FROM " + C.DB_TABLE_NOTICES + " ORDER BY timestamp DESC LIMIT ? OFFSET ? ";
		Cursor cursor = null;
		try {
			cursor = _db.rawQuery(sql, new String[] { Integer.toString(amount), Integer.toString(start) });
			while (cursor.moveToNext()) {
				Notice n = new Notice();
				n.id = cursor.getLong(0);
				n.type = cursor.getInt(1);
				n.value = cursor.getInt(2);
				n.text = cursor.getString(3);
				n.ref = cursor.getString(4);
				n.timestamp = cursor.getLong(5);
				n.state_flag = cursor.getInt(6);
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
	
	// SYNCHRONIZED WRITE METHODS /////////////////////////////////////////////
	
	public synchronized boolean markAllNoticesAsRead() {
		// Database
		SBLog.method(TAG, "markAllNoticesAsRead()");
		boolean result = false;
		SQLiteStatement update;
		String sql = "UPDATE " + C.DB_TABLE_NOTICES + " SET state_flag = ? WHERE state_flag = ?";
		update = _db.compileStatement(sql);
		update.bindString(1, C.NOTICE_STATE_READ + "");
		update.bindString(2, C.NOTICE_STATE_NEW + "");
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
	
	private synchronized Long saveNotice(int noticeType, int noticeValue, String noticeText, String noticeRef) {
		SBLog.method(TAG, "saveNotice()");
		noticeRef = (noticeRef == null) ? "" : noticeRef;
		Date date = new Date();
		String sql = "INSERT INTO " + C.DB_TABLE_NOTICES + " (type, value, text, ref, timestamp, state_flag) VALUES (?, ?, ?, ?, ?, ?)";
		SQLiteStatement insert = _db.compileStatement(sql);
		insert.bindLong(1, noticeType);
		insert.bindLong(2, noticeValue);
		insert.bindString(3, noticeText);
		insert.bindString(4, noticeRef);
		insert.bindLong(5, date.getTime());
		insert.bindLong(6, C.NOTICE_STATE_NEW);
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
