package co.shoutbreak.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import co.shoutbreak.service.ShoutbreakService;

public class Inbox {
	
	private ShoutbreakService _service;
	private Database _db;
	private List<Shout> _shouts;
	private User _user;	
	
	public Inbox(ShoutbreakService service, Database db, User user) {
		_service = service;
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
		_db.addShoutToInbox(shout);
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
		_db.updateScore(shout);
		
		// If the shout is closed, we checked if it's in our outbox.
		// If it is, we need to save the points.
		if (!shout.open){
			Shout shoutFromDB = _db.getShout(shout.id);
			if (shoutFromDB.is_outbox) {
				_user.savePoints(shout.pts);
				StateEvent e = new StateEvent();
				e.pointsChanged = true;
				_service.getStateManager().fireStateEvent(e);
			}
		}
		
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
}
