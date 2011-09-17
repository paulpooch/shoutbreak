package co.shoutbreak.ui;

import android.widget.AdapterView.OnItemClickListener;
import co.shoutbreak.storage.inbox.InboxListViewAdapter;
import co.shoutbreak.storage.noticetab.NoticeTabListViewAdapter;

public interface IUiGateway {
	
	///////////////////////////////////////////////////////////////////////////
	// HANDLE STUFF ///////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public void handleCreateAccountFailed();
	public void handleShoutSent();
	public void handleShoutFailed();
	public void handleDensityChange(boolean isDensitySet, double newDensity, int level);
	public void handleLevelUp(double cellDensity, int newLevel);
	public void handlePointsChange(int newPoints);
	public void handleServerFailure();
			
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////		
	
	public void refreshUiComponents();
	public void refreshProfile(int level, int points, int nextLevelAt);
	public void enableInputs();
	public void disableInputs();
	public void clearNoticeTab();
	public void showPointsNotice(String noticeText);
	public void showShoutNotice(String noticeText);
	public void unsetUiMediator();
	public void finishUi();
	public void onDataEnabled();
	public void onDataDisabled();
	public void onLocationEnabled();
	public void onLocationDisabled();
	public void onPowerPreferenceEnabled();
	public void onPowerPreferenceDisabled();
	public void setupNoticeTabListView(NoticeTabListViewAdapter listAdapter, boolean itemsCanFocus, OnItemClickListener listViewItemClickListener);	
	public void setupInboxListView(InboxListViewAdapter listAdapter, boolean itemsCanFocus, OnItemClickListener inboxItemClickListener);
	public void showTopNotice();
	public void hideNoticeTab();
	public void jumpToShoutInInbox(String shoutId);
	public void scrollInboxToPosition(int position);
	public void toast(String text, int duration);
	public void jumpToProfile();
	
}
