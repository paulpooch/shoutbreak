package co.shoutbreak.ui;

import co.shoutbreak.core.Shout;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.storage.inbox.InboxListViewAdapter;
import co.shoutbreak.storage.noticetab.NoticeTabListViewAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class UiOffGateway implements IUiGateway {

	private static final String TAG = "UiOffGateway";
	
	public UiOffGateway() {
		SBLog.constructor(TAG);
	}
	
	@Override
	public void disableInputs() {}
	
	@Override
	public void enableInputs() {}

	@Override
	public void handleCreateAccountFailed() {}

	@Override
	public void handlePointsChange(int newPoints) {}

	@Override
	public void handleInvalidServerResponse() {}

	@Override
	public void handleShoutFailed() {}

	@Override
	public void handleShoutSent() {}

	@Override
	public void refreshProfile(int level, int levelBeginPoints, int currentPoints, int levelEndPoints) {}

	@Override
	public void refreshUiComponents() {}

	@Override
	public void finishUi() {}

	@Override
	public void unsetUiMediator() {}

	@Override
	public void clearNoticeTab() {}

	@Override
	public void showPointsNotice(int newPoints) {}

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

	@Override
	public void handleServerAnnouncementCode(String text) {}

	@Override
	public void handleServerDowntimeCode(String text) {}

	@Override
	public void handleServerHttpError() {}

	@Override
	public void handleScoreDetailsRequest(int ups, int downs, int score) {}

	@Override
	public void refreshSignature(String signature) {}
	
	@Override
	public void loadUserPreferencesToUi() {}

	@Override
	public void handleRadiusChange(boolean isRadiusSet, long newRadius, int level) {}

	@Override
	public void handleLevelUp(long cellRadius, int newLevel) {}

	@Override
	public void createReplyDialog(Shout shout) {}

	@Override
	public void refreshOnOffState(boolean onUiThread, boolean causedByPowerButton) {}

}
