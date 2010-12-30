package com.shoutbreak.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.shoutbreak.Shout;
import com.shoutbreak.ShoutbreakUI;
import com.shoutbreak.Vars;
import com.shoutbreak.service.Database;
import com.shoutbreak.ui.InboxListViewAdapter.InboxViewHolder;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class Inbox {
	
	private Context _context;
	private ShoutbreakUI _ui;
	private Database _db;
	private List<Shout> _shouts;
	private InboxListViewAdapter _adapter;
	
	public Inbox(Context context, ShoutbreakUI ui, Database db, ListView lv) {
		_context = context;
		_ui = ui;
		_db = db;
		_shouts = new ArrayList<Shout>();        
       	_adapter = new InboxListViewAdapter(_context, _ui, _shouts);
        lv.setAdapter(_adapter);
        lv.setItemsCanFocus(false);
        
        // item expand listener
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {        
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				// TODO Auto-generated method stub
				InboxViewHolder holder = (InboxViewHolder) view.getTag();
				String shoutID = holder.shoutID;
				holder.collapsed.setVisibility(View.GONE);
		        holder.expanded.setVisibility(View.VISIBLE);
		        _adapter.getCacheExpandState().put(shoutID, true);
			}
        });
        
	}
	
	public void refresh() {
		_shouts = _db.getShouts(0, 50);
		_adapter.updateDisplay(_shouts);
	}
	
	public void refreshShout(String shoutID) {
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutID)) {
				shout = _db.getShout(shoutID);
			}
		}
		_adapter.updateDisplay(_shouts);
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
	
	public synchronized void addShout(JSONObject jsonShout) {
		Shout shout = new Shout();
		shout.id = jsonShout.optString(Vars.JSON_SHOUT_ID);
		shout.timestamp = jsonShout.optString(Vars.JSON_SHOUT_TIMESTAMP);
		shout.text = jsonShout.optString(Vars.JSON_SHOUT_TEXT);
		shout.re = jsonShout.optString(Vars.JSON_SHOUT_RE);
		Date d = new Date();
		shout.time_received = d.getTime();
		shout.open = jsonShout.optInt(Vars.JSON_SHOUT_OPEN, 0) == 1 ? true : Vars.NULL_OPEN;
		shout.is_outbox = false;
		shout.vote = Vars.NULL_VOTE;
		shout.hit = jsonShout.optInt(Vars.JSON_SHOUT_HIT, Vars.NULL_HIT);
		shout.ups = jsonShout.optInt(Vars.JSON_SHOUT_UPS, Vars.NULL_UPS);
		shout.downs = jsonShout.optInt(Vars.JSON_SHOUT_DOWNS, Vars.NULL_DOWNS);
		shout.pts = Vars.NULL_PTS;
		shout.approval = jsonShout.optInt(Vars.JSON_SHOUT_APPROVAL, Vars.NULL_APPROVAL);
		shout.state_flag = Vars.SHOUT_STATE_NEW;
		shout.score = Vars.NULL_SCORE;		
		_db.addShoutToInbox(shout);
	}
	
	public synchronized void updateScore(JSONObject jsonScore) {
		Shout shout = new Shout();
		shout.id = jsonScore.optString(Vars.JSON_SHOUT_ID);
		shout.ups = jsonScore.optInt(Vars.JSON_SHOUT_UPS, Vars.NULL_UPS);
		shout.downs = jsonScore.optInt(Vars.JSON_SHOUT_DOWNS, Vars.NULL_DOWNS);
		shout.hit = jsonScore.optInt(Vars.JSON_SHOUT_HIT, Vars.NULL_HIT);
		shout.pts = Vars.NULL_PTS;
		shout.approval = jsonScore.optInt(Vars.JSON_SHOUT_APPROVAL, Vars.NULL_APPROVAL);
		shout.open = jsonScore.optInt(Vars.JSON_SHOUT_OPEN, 0) == 1 ? true : Vars.NULL_OPEN;
		_db.updateScore(shout);
	}

	
	public synchronized void reflectVote(String shoutID, int vote) {
		_db.reflectVote(shoutID, vote);
		refreshShout(shoutID);
	}
	
}
