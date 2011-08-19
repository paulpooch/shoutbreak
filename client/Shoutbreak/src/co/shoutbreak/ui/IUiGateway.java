package co.shoutbreak.ui;

import java.util.List;

import android.widget.AdapterView.OnItemClickListener;
import co.shoutbreak.core.Shout;
import co.shoutbreak.storage.noticetab.NoticeTabListViewAdapter;

public interface IUiGateway {
	
	///////////////////////////////////////////////////////////////////////////
	// HANDLE STUFF ///////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	public void handleCreateAccountFailed();
	public void handleShoutSent();
	public void handleShoutFailed();
	public void handleVoteFailed(String shoutId, int vote);
	public void handleShoutsReceived(List<Shout> inboxContent, int newShouts);
	public void handleDensityChange(double newDensity, int level);
	public void handleLevelUp(double cellDensity, int newLevel);
	public void handlePointsChange(int newPoints);
	public void handleServerFailure();
			
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////		
	
	public void refreshUiComponents();
	public void refreshInbox(List<Shout> inboxContent);
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
	public void showTopNotice();
	
}
