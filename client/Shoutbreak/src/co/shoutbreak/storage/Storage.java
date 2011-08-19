package co.shoutbreak.storage;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Shout;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.inbox.Inbox;
import co.shoutbreak.storage.noticetab.NoticeTabSystem;

public class Storage implements Colleague {

	private static final String TAG = "Storage";
	
	private Mediator _m;
	private Database _db;
	private User _user;
	private Inbox _inbox;
	private NoticeTabSystem _noticeTabSystem;
	
	public Storage(Mediator mediator, Database db) {
		_m = mediator;
		_db = db;
		_user = new User(_db);
		_inbox = new Inbox(_db);	
		_noticeTabSystem = new NoticeTabSystem(_m, _db);
	}

	@Override
	public void unsetMediator() {
		// TODO Auto-generated method stub
		_m = null;
		_db = null;
		_user = null;
		_inbox = null;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// HANDLE STUFF ///////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public void handlePointsChange(int additonalPoints) {
		 _user.savePoints(additonalPoints);		
	}
	
	public void handleDensityChange(double density, CellDensity currentCell) {
		_user.saveDensity(density, currentCell);
	}
	
	public void handleLevelUp(int newLevel, int newPoints, int nextLevelAt) {
		_user.levelUp(newLevel, newPoints, nextLevelAt);
		_noticeTabSystem.createNotice(C.NOTICE_LEVEL_UP, newLevel, C.STRING_LEVEL_UP_1 + getUserLevel() + "\n" + C.STRING_LEVEL_UP_2 + (C.CONFIG_PEOPLE_PER_LEVEL * getUserLevel()) + " people.", null);			
	}
	
	public void handleAccountCreated(String uid, String password) {
		_user.setUserId(uid);
		_user.setPassword(password);
		_noticeTabSystem.createNotice(C.NOTICE_ACCOUNT_CREATED, 0, C.STRING_ACCOUNT_CREATED, null);
	}
	
	public void handleShoutSent() {
		_noticeTabSystem.createNotice(C.NOTICE_SHOUT_SENT, 0, C.STRING_SHOUT_SENT, null);	
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
		int count = shouts.length();
		String pluralShout = "shout" + (count > 1 ? "s" : "");
		String notice = "Just heard " + count + " new " + pluralShout + ".";
		_noticeTabSystem.createNotice(C.NOTICE_SHOUTS_RECEIVED, count, notice, null);		
	}
	
	public void handleVoteFinish(String shoutId, int vote) {
		_inbox.reflectVote(shoutId, vote);
	}
	
	public void handleShoutFailed() {
		_noticeTabSystem.createNotice(C.NOTICE_SHOUT_FAILED, 0, C.STRING_SHOUT_FAILED, null);
	}
		
	public void handleVoteFailed(int vote, String shoutId) {
		_noticeTabSystem.createNotice(C.NOTICE_VOTE_FAILED, vote, C.STRING_VOTE_FAILED, shoutId);
	}
	
	public void handleCreateAccountStarted() {
		_noticeTabSystem.createNotice(C.NOTICE_NO_ACCOUNT, 0, C.STRING_NO_ACCOUNT, null);
	}
	
	public void handleCreateAccountFailed() {
		_noticeTabSystem.createNotice(C.NOTICE_CREATE_ACCOUNT_FAILED, 0, C.STRING_CREATE_ACCOUNT_FAILED, null);
	}
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public boolean deleteShout(String shoutID) {
		return _inbox.deleteShout(shoutID);
	}

	public List<Shout> getShoutsForUI() {
		return _inbox.getShoutsForUI();
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
	
	public int getUserPoints() {
		return _user.getPoints();
	}
	
	public int getUserLevel() {
		return _user.getLevel();
	}
	
	public int getUserNextLevelAt() {
		return _user.getNextLevelAt();
	}
	
	public void initializeDensity(CellDensity currentCell) {
		_user.initializeDensity(currentCell);
	}
	
	public CellDensity getCellDensity(CellDensity currentCell) {
		return _user.getCellDensity(currentCell);
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

	public void refreshNoticeTab() {
		_noticeTabSystem.refresh();
	}

	public void initializeNoticeTabSystem() {
		_noticeTabSystem.initialize();		
	}
	
}
