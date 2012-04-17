package co.shoutbreak.storage;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import co.shoutbreak.core.C;
import co.shoutbreak.core.Colleague;
import co.shoutbreak.core.Mediator;
import co.shoutbreak.core.Shout;
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
		SBLog.constructor(TAG);
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
	
	public void handlePointsForShout(int pointsType, int pointsValue, String shoutId) {
		 _user.savePoints(pointsType, pointsValue);
		 String strPoints = (pointsValue == 1 || pointsValue == -1) ? "point" : "points";		
		_noticeTabSystem.createNotice(C.NOTICE_POINTS_SHOUT, pointsValue, "Your shout earned " + pointsValue + " " + strPoints + ".", shoutId);
	}
	
	public void handlePointsSync(int currentPoints) {
		_user.setPoints(currentPoints);
	}
	
	public void handleRadiusChange(long radius, RadiusCacheCell currentCell) {
		_user.saveRadiusForCell(radius, currentCell);
	}
	
	public void handleLevelUp(int newLevel, int levelAt, int nextLevelAt) {
		_user.levelUp(newLevel, levelAt, nextLevelAt);
		_noticeTabSystem.createNotice(C.NOTICE_LEVEL_UP, newLevel, "Your shoutreach was increased to " + getUserLevel() + " people!", null);			
	}
	
	public void handleAccountCreated(String uid, String password) {
		_user.setUserId(uid);
		_user.setPassword(password);
		_noticeTabSystem.createNotice(C.NOTICE_ACCOUNT_CREATED, 0, C.STRING_ACCOUNT_CREATED, null);
	}
	
	public void handleScoresReceived(JSONArray scores) {
		for (int i = 0; i < scores.length(); i++) {
			try {
				JSONObject jsonScore = scores.getJSONObject(i);
				_inboxSystem.updateScore(jsonScore);
			} catch (JSONException e) {
				SBLog.error(TAG, e.getMessage());
			}
		}
	}
	
	public void handleShoutsReceived(JSONArray shouts) {
		String noticeRef = "";
		int newCount = 0;
		int reachOfFirstSent = -1;
		try {
			for (int i = 0; i < shouts.length(); i++) {
				JSONObject jsonShout = shouts.getJSONObject(i);
				Shout shout = _inboxSystem.addShout(jsonShout);
				if (i == 0) {
					noticeRef = jsonShout.optString(C.JSON_SHOUT_ID);
				}
				if (!shout.is_outbox) {
					newCount++;
				} else {
					if (reachOfFirstSent < 0) {
						reachOfFirstSent = shout.hit;
					}
				}
			}
		} catch (JSONException e) {
			SBLog.error(TAG, e.getMessage());
		}
		String noticeText = "";
		if (newCount == 0 && shouts.length() == 1) {
			noticeText = "shout hit " + reachOfFirstSent + " people";
			_m.getUiGateway().showShoutNotice(noticeText);
		} else if (newCount == 0 && shouts.length() > 0) {
			noticeText = "heard your shouts";
			_m.getUiGateway().showShoutNotice(noticeText);
		} else {
			String pluralShout = "shout" + (newCount > 1 ? "s" : "");
			noticeText = "Just heard " + newCount + " new " + pluralShout + ".";
			_noticeTabSystem.createNotice(C.NOTICE_SHOUTS_RECEIVED, newCount, noticeText, noticeRef);
		}
	}
	
	public void handleVoteFinish(String shoutId, int vote) {
		_inboxSystem.reflectVote(shoutId, vote);
		_user.savePoints(C.POINTS_VOTE, User.calculatePointsForVote(getUserLevel()));
		int points = User.calculatePointsForVote(getUserLevel());
		String strPoints = (points == 1 || points == -1) ? "point" : "points";		
		_noticeTabSystem.createNotice(C.NOTICE_POINTS_VOTING, points, "You gained " + points + " " + strPoints + " for voting.", shoutId);
	}
	
	public void handleShoutSent() {
		_noticeTabSystem.createNotice(C.NOTICE_SHOUT_SENT, 0, C.STRING_SHOUT_SENT, null);	
		//_m.getUiGateway().toast("Shout successful.", Toast.LENGTH_SHORT);
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
	
	public void handleForcedPollingStop() {
		_noticeTabSystem.createNotice(C.NOTICE_FORCED_POLLING_STOP, 0, C.STRING_FORCED_POLLING_STOP, null);
	}
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public void deleteShout(String shoutID) {
		_inboxSystem.deleteShout(shoutID);
	}
	
	public ArrayList<String> getScoreRequestIds() {
		return _inboxSystem.getScoreRequestIds();
	}
	
	public int getUserPoints() {
		return _user.getPoints();
	}
	
	public int getUserLevel() {
		return _user.getLevel();
	}
	
	public int getUserLevelAt() {
		return _user.getLevelAt();
	}
		
	public int getUserNextLevelAt() {
		return _user.getNextLevelAt();
	}
	
	/*
	public void initializeRadiusAtCell(RadiusCacheCell currentCell) {
		RadiusCacheCell radiusAtCell = _user.getInitialRadiusAtCell(currentCell);
		_m.getUiGateway().handleRadiusChange(radiusAtCell.isSet, radiusAtCell.radius, getUserLevel());
	}
	*/
	
	public RadiusCacheCell getRadiusAtCell(RadiusCacheCell currentCell) {
		SBLog.method(TAG, "getRadiusAtCell(currentCell)");
		RadiusCacheCell radiusAtCell = _user.getRadiusAtCell(currentCell);
		return radiusAtCell;
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
	
	public void setLevelUpOccured(boolean b) {
		_user.setLevelUpOccured(b);
	}
	
	public void updateAuth(String nonce) {
		_user.updateAuth(nonce);
	}

	public void refreshUiComponents() {
		_inboxSystem.refreshAll();
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

	public void markAllNoticesRead() {
		_noticeTabSystem.markAllNoticesAsRead();		
	}

	public void refreshNoticeTab() {
		_noticeTabSystem.refresh();		
	}

	// Assumes shout is in the inbox using Storage.isShoutInInbox(shoutId)
	public void jumpToShoutInInbox(String shoutId) {
		_inboxSystem.jumpToShoutInInbox(shoutId);
	}

}
