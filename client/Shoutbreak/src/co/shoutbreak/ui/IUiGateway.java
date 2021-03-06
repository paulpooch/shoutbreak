package co.shoutbreak.ui;

import android.widget.AdapterView.OnItemClickListener;
import co.shoutbreak.core.Shout;
import co.shoutbreak.storage.inbox.InboxListViewAdapter;
import co.shoutbreak.storage.noticetab.NoticeTabListViewAdapter;

public interface IUiGateway {
	
	///////////////////////////////////////////////////////////////////////////
	// HANDLE STUFF ///////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public void handleCreateAccountFailed();
	public void handleServerDowntimeCode(String text);
	public void handleServerHttpError();
	public void handleServerAnnouncementCode(String text);
	public void handleShoutSent();
	public void handleShoutFailed();
	public void handleRadiusChange(boolean isRadiusSet, long newRadius, int level);
	public void handleLevelUp(long cellRadius, int newLevel);
	public void handlePointsChange(int newPoints);
	public void handleInvalidServerResponse();
	public void handleScoreDetailsRequest(int ups, int downs, int score);		
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////		
	
	public void createReplyDialog(Shout shout);
	public void refreshUiComponents();
	public void refreshProfile(int level, int levelBeginPoints, int currentPoints, int levelEndPoints);
	public void refreshSignature(String signature);
	public void loadUserPreferencesToUi();
	public void enableInputs();
	public void disableInputs();
	public void clearNoticeTab();
	public void showPointsNotice(int newPoints);
	public void showShoutNotice(String noticeText);
	public void unsetUiMediator();
	public void finishUi();
	public void setupNoticeTabListView(NoticeTabListViewAdapter listAdapter, boolean itemsCanFocus, OnItemClickListener listViewItemClickListener);	
	public void setupInboxListView(InboxListViewAdapter listAdapter, boolean itemsCanFocus, OnItemClickListener inboxItemClickListener);
	public void showTopNotice();
	public void hideNoticeTab();
	public void jumpToShoutInInbox(String shoutId);
	public void scrollInboxToPosition(int position);
	public void toast(String text, int duration);
	public void jumpToProfile();
	public void refreshOnOffState(boolean onUiThread, boolean causedByPowerButton);
	
}
