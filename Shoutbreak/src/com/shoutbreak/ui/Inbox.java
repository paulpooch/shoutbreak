package com.shoutbreak.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.shoutbreak.Shout;
import com.shoutbreak.ShoutbreakApplication;
import com.shoutbreak.ShoutbreakUI;
import com.shoutbreak.Vars;
import com.shoutbreak.service.Database;

public class Inbox {
	
	private ShoutbreakApplication _app;
	private Database _db;
	private List<Shout> _shouts;
		
	public Inbox(ShoutbreakApplication app, Database db) {
		_app = app;
		_db = db;
		_shouts = new ArrayList<Shout>();
	}
	
	// update ListView if the ui exists right now
	protected void updateDisplay(List<Shout> shouts) {
		ShoutbreakUI uiRef = _app.getUIReference().get();
		if (uiRef != null) {
			uiRef.getInboxListViewAdapter().updateDisplay(shouts);
		}
	}
	
	public void refresh() {
		_shouts = _db.getShouts(0, 50);
		updateDisplay(_shouts);
	}
	
	public void refreshShout(String shoutID) {
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutID)) {
				shout = _db.getShout(shoutID);
			}
		}
		updateDisplay(_shouts);
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
			if (shout.state_flag == Vars.SHOUT_STATE_NEW) {
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
	
	public synchronized void markShoutAsRead(String shoutID) {
		_db.markShoutAsRead(shoutID);
		refreshShout(shoutID);
		updateDisplay(_shouts);
	}
	
	public synchronized void deleteShout(String shoutID) {
		_db.deleteShout(shoutID);
		for (Shout shout : _shouts) {
			if (shout.id.equals(shoutID)) {
				_shouts.remove(shout);
				break;
			}
		}
		updateDisplay(_shouts);
		ShoutbreakUI uiRef = _app.getUIReference().get();
		if (uiRef != null) {
			uiRef.giveNotice("shout deleted");
		}
	}
	
}
