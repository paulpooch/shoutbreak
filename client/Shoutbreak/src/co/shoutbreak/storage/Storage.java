package co.shoutbreak.storage;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.inbox.InboxSystem;
import co.shoutbreak.storage.noticetab.NoticeTabSystem;

public class Storage implements Colleague {

	private static final String TAG = "Storage";
	
	private Mediator _m;
	private Database _db;
	private User _user;
	private InboxSystem _inboxSystem;
	private NoticeTabSystem _noticeTabSystem;
	
	public Storage(Mediator mediator, Database db) {
		_m = mediator;
		_db = db;
		_user = new User(_db);
		_inboxSystem = new InboxSystem(_m, _db);	
		_noticeTabSystem = new NoticeTabSystem(_m, _db);
	}

	@Override
	public void unsetMediator() {
		// TODO Auto-generated method stub
		_m = null;
		_db = null;
		_user = null;
		_inboxSystem = null;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// HANDLE STUFF ///////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public void handlePointsChange(int pointsType, int pointsValue) {
		 _user.savePoints(pointsType, pointsValue);		
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
	
	public void handleScoresReceived(JSONArray scores) {
		for (int i = 0; i < scores.length(); i++) {
			try {
				JSONObject jsonScore = scores.getJSONObject(i);
				_inboxSystem.updateScore(jsonScore);
			} catch (JSONException e) {
				SBLog.e(TAG, e.getMessage());
			}
		}
		_inboxSystem.refresh();
	}
	
	public void handleShoutsReceived(JSONArray shouts) {
		for (int i = 0; i < shouts.length(); i++) {
			try {
				JSONObject jsonShout = shouts.getJSONObject(i);
				_inboxSystem.addShout(jsonShout);
			} catch (JSONException e) {
				SBLog.e(TAG, e.getMessage());
			}
		}
		int count = shouts.length();
		String pluralShout = "shout" + (count > 1 ? "s" : "");
		String notice = "Just heard " + count + " new " + pluralShout + ".";
		_noticeTabSystem.createNotice(C.NOTICE_SHOUTS_RECEIVED, count, notice, null);
		_inboxSystem.refresh();
	}
	
	public void handleVoteFinish(String shoutId, int vote) {
		_inboxSystem.reflectVote(shoutId, vote);
		_user.savePoints(C.POINTS_VOTE, User.calculatePointsForVote(this.getUserLevel()));
		_noticeTabSystem.createNotice(C.NOTICE_POINTS, User.calculatePointsForVote(this.getUserLevel()), "You gained " + User.calculatePointsForVote(this.getUserLevel()) + " points for voting.", shoutId);
	}
	
	public void handleShoutFailed() {
		_noticeTabSystem.createNotice(C.NOTICE_SHOUT_FAILED, 0, C.STRING_SHOUT_FAILED, null);
	}
		
	public void handleVoteFailed(String shoutId, int vote) {
		_inboxSystem.undoVote(shoutId, vote);
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
	
	public void deleteShout(String shoutID) {
		_inboxSystem.deleteShout(shoutID);
		_inboxSystem.refresh();
	}
	
	public ArrayList<String> getOpenShoutIds() {
		return _inboxSystem.getOpenShoutIDs();
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
		CellDensity cellDensity = _user.getInitialDensity(currentCell);
		if (cellDensity.isSet) {
			_m.getUiGateway().handleDensityChange(cellDensity.density, this.getUserLevel());
		}
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
		return _inboxSystem.getOpenShoutIDs();
	}
	
	public void updateAuth(String nonce) {
		_user.updateAuth(nonce);
	}

	public void refreshUiComponents() {
		_inboxSystem.refresh();
		_noticeTabSystem.refresh();
	}

	public void initializeUiComponents() {
		_inboxSystem.initialize();
		_noticeTabSystem.initialize();		
	}

	public void enableInputs() {
		_inboxSystem.enableInputs();
	}

	public void disableInputs() {
		_inboxSystem.disableInputs();
	}
	
}
