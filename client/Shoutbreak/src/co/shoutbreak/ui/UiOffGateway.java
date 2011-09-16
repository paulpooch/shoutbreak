package co.shoutbreak.ui;

import android.widget.AdapterView.OnItemClickListener;
import co.shoutbreak.storage.inbox.InboxListViewAdapter;
import co.shoutbreak.storage.noticetab.NoticeTabListViewAdapter;

public class UiOffGateway implements IUiGateway {

	public UiOffGateway() {}
	
	@Override
	public void disableInputs() {}
	
	@Override
	public void enableInputs() {}

	@Override
	public void handleCreateAccountFailed() {}

	@Override
	public void handleDensityChange(boolean isDensitySet, double newDensity, int level) {}

	@Override
	public void handleLevelUp(double cellDensity, int newLevel) {}

	@Override
	public void handlePointsChange(int newPoints) {}

	@Override
	public void handleServerFailure() {}

	@Override
	public void handleShoutFailed() {}

	@Override
	public void handleShoutSent() {}

	@Override
	public void refreshProfile(int level, int points, int nextLevelAt) {}

	@Override
	public void refreshUiComponents() {}

	@Override
	public void finishUi() {}

	@Override
	public void onDataDisabled() {}

	@Override
	public void onDataEnabled() {}

	@Override
	public void onLocationDisabled() {}

	@Override
	public void onLocationEnabled() {}

	@Override
	public void onPowerPreferenceDisabled() {}

	@Override
	public void onPowerPreferenceEnabled() {}

	@Override
	public void unsetUiMediator() {}

	@Override
	public void clearNoticeTab() {}

	@Override
	public void showPointsNotice(String noticeText) {}

	@Override
	public void showShoutNotice(String noticeText) {}

	@Override
	public void setupNoticeTabListView(NoticeTabListViewAdapter listAdapter, boolean itemsCanFocus,
			OnItemClickListener listViewItemClickListener) {}

	@Override
	public void showTopNotice() {}

	@Override
	public void setupInboxListView(InboxListViewAdapter listAdapter, boolean itemsCanFocus, OnItemClickListener inboxItemClickListener) {}

	@Override
	public void jumpToShoutInInbox(String shoutId) {}

	@Override
	public void scrollInboxToPosition(int position) {}

	@Override
	public void toast(String text, int duration) {}

	@Override
	public void hideNoticeTab() {}

	@Override
	public void jumpToProfile() {}
	
}
