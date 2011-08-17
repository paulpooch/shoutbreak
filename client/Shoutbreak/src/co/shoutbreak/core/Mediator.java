// TODO: force repush

package co.shoutbreak.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

import co.shoutbreak.R;
import co.shoutbreak.core.utils.DataListener;
import co.shoutbreak.core.utils.DialogBuilder;
import co.shoutbreak.core.utils.Flag;
import co.shoutbreak.core.utils.Notifier;
import co.shoutbreak.core.utils.SBLog;
import co.shoutbreak.polling.CrossThreadPacket;
import co.shoutbreak.polling.ThreadLauncher;
import co.shoutbreak.ui.Shoutbreak;
import co.shoutbreak.user.CellDensity;
import co.shoutbreak.user.Database;
import co.shoutbreak.user.DeviceInformation;
import co.shoutbreak.user.Inbox;
import co.shoutbreak.user.LocationTracker;
import co.shoutbreak.user.PreferenceManager;
import co.shoutbreak.user.User;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Message;

public class Mediator {
	
	private static final String TAG = "Mediator";
	
	// colleagues
	private Mediator _self;
	private ShoutbreakService _service;
	private Shoutbreak _ui;
	private User _user;
	private Inbox _inbox;
	private PreferenceManager _preferences;
	private DeviceInformation _device;
	private Notifier _notifier;
	private LocationTracker _location;
	private DataListener _data;
	private Database _db;
	private ThreadLauncher _threadLauncher;
	
	// state flags
	private Flag _isUIAlive = new Flag("m:_isUIAlive");
	private Flag _isPollingAlive = new Flag("m:_isPollingAlive");
	private Flag _isServiceConnected = new Flag("m:_isServiceConnected");
	private Flag _isServiceStarted = new Flag("m:_isServiceStarted");
	private Flag _isLocationEnabled = new Flag("m:_isLocationEnabled");
	private Flag _isDataEnabled = new Flag("m:_isDataEnabled");
	private Flag _isPowerPreferenceEnabled = new Flag("m:_isPowerPreferenceEnabled");		// is power preference set to on
	
	private UiGateway _uiGateway;
	
	/* Mediator Lifecycle */
	
	public Mediator(ShoutbreakService service) {
    	SBLog.i(TAG, "new Mediator()");
    	_self = this;
    	
		// add colleagues
		_service = service;
		_preferences = new PreferenceManager(this, _service.getSharedPreferences(C.PREFERENCE_FILE, Context.MODE_PRIVATE));
		_device = new DeviceInformation(_service);
		_notifier = new Notifier(this, _service);
		_location = new LocationTracker(this);
		_data = new DataListener(this);
		_db = new Database(_service);
		_user = new User(this, _db);
		_inbox = new Inbox(this, _db);
		_threadLauncher = new ThreadLauncher(this);
		_uiGateway = new UiGateway();
		
		// Initialize State.
		
		// Polling has already been set and launched from DataListener calling onDataEnabled.
		// Make sure we don't launch a second one.
		if (!_isPollingAlive.isInitialized()) {
			_isPollingAlive.set(false);
		}
		_isUIAlive.set(false);
		
		_isDataEnabled.set(isDataEnabled());
		_isLocationEnabled.set(isLocationEnabled());
		_isPowerPreferenceEnabled.set(isPowerPreferenceEnabled());
		
		if (_isDataEnabled.get()) {
			onDataEnabled();
		} else {
			onDataDisabled();
		}
		
		if (_isLocationEnabled.get()) {
			onLocationEnabled();
		} else {
			onLocationDisabled();
		}
		
		if (_isPowerPreferenceEnabled.get()) {
			onPowerPreferenceEnabled();
		} else {
			onPowerPreferenceDisabled();
		}
	}
	
	public void registerUI(Shoutbreak ui) {
		// ui is created before the mediator exists
		// it must be added once the mediator is created
		SBLog.i(TAG, "registerUI()");
		_isUIAlive.set(true);
		_ui = ui;
		_ui.setMediator(this);	
	}
	
	public void unregisterUI(boolean forceKillUI) {
		// called by ui's onDestroy() method
		SBLog.i(TAG, "unregisterUI()");
		if (_isUIAlive.get()) {
			_isUIAlive.set(false);
			_ui.unsetMediator();
			if (forceKillUI) {
				// forces UI to destroy itself if the mediator / service is killed off
				SBLog.e(TAG, "force killed ui, service shutdown while ui running");
				_ui.finish();
			}
			_ui = null;
		} else {
			SBLog.e(TAG, "ui is not alive, unable to unregister");
		}
	}
	
	public void kill() {
		// removes all colleague references to the mediator
		// called by service's onDestroy() method
    	SBLog.i(TAG, "kill()");
		_service = null;
		unregisterUI(true);
		_preferences = null;
		_device = null;
		_notifier = null;
		_location.unsetMediator();
		_location = null;
		_data.unsetMediator();
		_data = null;
		_db = null;
		_inbox.unsetMediator();
		_inbox = null;
		_threadLauncher.unsetMediator();
		_threadLauncher = null;
	}
	
	/* Mediator Commands */
	
	public void onServiceConnected() {
		// called when service handler binds ui and service
		SBLog.i(TAG, "onServiceConnected()");
		_isServiceConnected.set(true);
	}
	
	public void onServiceDisconnected() {
		// called when ui unbinds from the service
		// shouldn't ever be called
		SBLog.i(TAG, "onServiceDisconnected()");
		_isServiceConnected.set(false);
	}
	
	public void onServiceStart() {
		SBLog.i(TAG, "onServiceStart()");
		_isServiceStarted.set(true);
	}
	
	public void appLaunchedFromUI() {
		SBLog.i(TAG, "appLaunchedFromUI()");
		_isServiceStarted.set(true);
		startPolling();
	}
	
	public void appLaunchedFromAlarm() {
		SBLog.i(TAG, "appLaunchedFromAlarm()");
		_isServiceStarted.set(true);
		startPolling();
	}
	
	public void startPolling() {
		SBLog.i(TAG, "startPolling()");
		if (!_isPollingAlive.get()) {
			if (_isPowerPreferenceEnabled.get() && _isLocationEnabled.get() && _isDataEnabled.get() && _isServiceStarted.get()) {
				SBLog.i(TAG, "app fully functional");
				_isPollingAlive.set(true);
				_threadLauncher.startPolling();
				_uiGateway.enableInputs();
			} else {
				if (!_isPowerPreferenceEnabled.get()) {
					SBLog.e(TAG, "unable to start service, power preference set to off");
				}
				if (!_isLocationEnabled.get()) {
					SBLog.e(TAG, "unable to start service, location unavailable");
				}
				if (!_isDataEnabled.get()) {
					SBLog.e(TAG, "unable to start service, data unavailable");
				}
			}
		} else {
			SBLog.i(TAG, "service is already polling, unable to call startPolling()");
		}
	}
	
	public void stopPolling() {
		SBLog.i(TAG, "stopPolling()");
		if (_isPollingAlive.get()) {
			_uiGateway.disableInputs();
			_isPollingAlive.set(false);
			_threadLauncher.stopPolling();
		} else {
			SBLog.i(TAG, "service is not polling, unable to call stopPolling()");
		}
	}
	
	private void possiblyStopPolling(Message message) {
		if (message != null && message.obj != null) {
			CrossThreadPacket xPacket = (CrossThreadPacket)message.obj;
			if (xPacket.purpose == C.PURPOSE_LOOP_FROM_UI || xPacket.purpose == C.PURPOSE_LOOP_FROM_UI_DELAYED) {
				// The Polling loop just crashed.
				stopPolling();
			}
		} else {
			// Something really bad happened
			stopPolling();
		}
	}
	
	public void setPowerPreferenceToOn() {
		_preferences.setPowerPreferenceToOn();
	}
	
	public void setPowerPreferenceToOff() {
		_preferences.setPowerPreferenceToOff();
	}
	
	public boolean isPowerPreferenceEnabled() {
		return _preferences.isPowerPreferenceSetToOn();
	}
	
	public void onPowerPreferenceEnabled() {
		SBLog.i(TAG, "onPowerEnabled()");
		_isPowerPreferenceEnabled.set(true);
		_service.enableAlarmReceiver();
		if (_isUIAlive.get()) {
			_ui.onPowerPreferenceEnabled();
		}
		startPolling();
	}
	
	public void onPowerPreferenceDisabled() {
		SBLog.i(TAG, "onPowerDisabled()");
		_isPowerPreferenceEnabled.set(false);
		_service.disableAlarmReceiver();
		if (_isUIAlive.get()) {
			_ui.onPowerPreferenceDisabled();
		}
		stopPolling();
	}
	
	public boolean isLocationEnabled() {
		return _location.isLocationEnabled();
	}
	
	public void onLocationEnabled() {
		SBLog.i(TAG, "onLocationEnabled()");
		_isLocationEnabled.set(true);
		if (_isUIAlive.get()) {
			_ui.onLocationEnabled();
		}
		startPolling();
	}
	
	public void onLocationDisabled() {
		SBLog.i(TAG, "onLocationDisabled()");
		_isLocationEnabled.set(false);
		if (_isUIAlive.get()) {
			_ui.onLocationDisabled();
		}
		stopPolling();
	}
	
	public boolean isDataEnabled() {
		return _data.isDataEnabled();
	}
	
	public void onDataEnabled() {
		SBLog.i(TAG, "onDataEnabled()");
		_isDataEnabled.set(true);
		if (_isUIAlive.get()) {
			_ui.onDataEnabled();
		}
		startPolling();
	}
	
	public void onDataDisabled() {
		SBLog.i(TAG, "onDataDisabled()");
		_isDataEnabled.set(false);
		if (_isUIAlive.get()) {
			_ui.onDataDisabled();
		}
		stopPolling();
	}
	
	public boolean isFirstRun() {
		SBLog.i(TAG, "isFirstRun()");
		boolean isFirstRun = _preferences.getBoolean(C.PREFERENCE_IS_FIRST_RUN, true);
		_preferences.putBoolean(C.PREFERENCE_IS_FIRST_RUN, false);
		return isFirstRun;
	}
	
	private void createNotice(int noticeType, String noticeText, String noticeRef) {
		_user.saveNotice(noticeType, noticeText, noticeRef);
		_uiGateway.giveNotice(_user.getNoticesForUI());
	}
	
	public void shoutStart(String text, int power) {
		SBLog.i(TAG, "shout()");
		_threadLauncher.handleShoutStart(text, power);
	}
	
	public Object getSystemService(String name) {
		SBLog.i(TAG, "getSystemService()");
		return _service.getSystemService(name);
	}
	
	public void handlePollingResponse(Message message) {
		SBLog.i(TAG, "handlePollingResponse()");
		if (_isPollingAlive.get()) {
			_threadLauncher.spawnNextPollingThread(message);
		}
		// Else polling thread dies.
	}
	
	public CellDensity getCurrentCell() {
		// TODO: should this be moved to ThreadSafeMediator?
		SBLog.i(TAG, "getCurrentCell()");
		if (!_isLocationEnabled.get()) {
			SBLog.e(TAG, "location is unavailable, unable to get current cell");
		}
		return _location.getCurrentCell();
	}
	
	public void deleteShout(String shoutId) {
		SBLog.i(TAG, "deleteShout()");
		_inbox.deleteShout(shoutId);
		_uiGateway.refreshInbox(_inbox.getShoutsForUI());
	}
	
	public void launchPollingThread(Message message) {
		SBLog.i(TAG, "launchPollingThread()");
		_threadLauncher.launchPollingThread(message, false);
	}
		
	public ThreadSafeMediator getAThreadSafeMediator() {
		SBLog.i(TAG, "getAThreadSafeMediator()");
		return new ThreadSafeMediator();
	}
	
	// EVENTS /////////////////////////////////////////////////////////////////
	
	public void inboxNewShoutSelected(Shout shout) {
		_inbox.handleInboxNewShoutSelected(shout);
	}
		
	// Triggered from a Shout close.  Have user save earned points.
	public void pointsChange(int additionalPoints) {
		SBLog.i(TAG, "pointsChange()");
		_user.handlePointsChange(additionalPoints);
		_uiGateway.handlePointsChange(_user.getPoints());
	}
	
	public void voteStart(String shoutId, int vote) {
		SBLog.i(TAG, "voteStart()");
		_threadLauncher.handleVoteStart(shoutId, vote);
	}
	
	// THREAD SAFE MEDIATOR ///////////////////////////////////////////////////
	
	public class ThreadSafeMediator {
		// Methods of any other classes called from here should be synchronized or read only.
		
		public ThreadSafeMediator() {
			SBLog.i(TAG, "new ThreadSafeMediator()");
		}
		
		public void createNotice(int noticeType, String noticeText, String noticeRef) {
			_self.createNotice(noticeType, noticeText, noticeRef);
		}
		
		public void densityChange(double density) {
			SBLog.i(TAG, "densityChange()");
			// Note: The order in these matters.
			_user.handleDensityChange(density);
			_uiGateway.handleDensityChange(density, _user.getLevel());
		}
	
		public void shoutsReceived(JSONArray shouts) {
			SBLog.i(TAG, "shoutsReceived()");
			_inbox.handleShoutsReceived(shouts);
			if (_isUIAlive.get()) {
				_uiGateway.handleShoutsReceived(_inbox.getShoutsForUI(), shouts.length());
			} else {
				_notifier.handleShoutsReceived(shouts.length());				
			}
			String pluralShout = "shout" + (shouts.length() > 1 ? "s" : "");
			String notice = "Just heard " + shouts.length() + " new " + pluralShout + ".";
			createNotice(C.NOTICE_SHOUTS_RECEIVED, notice, null);
		}
		
		public void scoresReceived(JSONArray scores) {
			SBLog.i(TAG, "scoresReceived()");
			_inbox.handleScoresReceived(scores);
			_uiGateway.refreshInbox(_inbox.getShoutsForUI());
		}
			
		public void levelUp(JSONObject levelInfo) {
			SBLog.i(TAG, "levelUp()");
			_user.handleLevelUp(levelInfo);
			_uiGateway.handleLevelUp(_user.getCellDensity().density, _user.getLevel());
			_uiGateway.handlePointsChange(_user.getPoints());
			createNotice(C.NOTICE_LEVEL_UP, C.STRING_LEVEL_UP_1 + _user.getLevel() + "\n" + C.STRING_LEVEL_UP_2 + (C.CONFIG_PEOPLE_PER_LEVEL * _user.getLevel()) + " people.", null);
		}
		
		public void shoutSent() {
			SBLog.i(TAG, "shoutSent()");
			if (_isUIAlive.get()) {
				_uiGateway.handleShoutSent();
			}
			createNotice(C.NOTICE_SHOUT_SENT, C.STRING_SHOUT_SENT, null);
		}
		
		public void shoutFailed(Message message) {
			if (_isUIAlive.get()) {
				_uiGateway.handleShoutFailed();
			}
			createNotice(C.NOTICE_SHOUT_FAILED, C.STRING_SHOUT_FAILED, null);
			_uiGateway.handleServerFailure();
			possiblyStopPolling(message);
		}
		
		public void voteFinish(String shoutId, int vote) {
			SBLog.i(TAG, "voteFinish()");
			_inbox.handleVoteFinish(shoutId, vote);
		}
		
		public void voteFailed(Message message, String shoutId, int vote) {
			_uiGateway.handleVoteFailed(shoutId, vote);
			createNotice(C.NOTICE_SHOUT_FAILED, C.STRING_VOTE_FAILED, null);
			_uiGateway.handleServerFailure();
			possiblyStopPolling(message);
		}
		
		public void accountCreated(String uid, String password) {
			SBLog.i(TAG, "accountCreated()");
			createNotice(C.NOTICE_ACCOUNT_CREATED, C.STRING_ACCOUNT_CREATED, null);
			_user.handleAccountCreated(uid, password);
			// Maybe we should do something in the UI?
		}
		

		public void createAccountFailed(Message message) {
			createNotice(C.NOTICE_CREATE_ACCOUNT_FAILED, C.STRING_CREATE_ACCOUNT_FAILED, null);
			_uiGateway.handleServerFailure();
			possiblyStopPolling(message);
		}
		
		public void pingFailed(Message message) {
			createNotice(C.NOTICE_PING_FAILED, C.STRING_PING_FAILED, null);
			_uiGateway.handleServerFailure();
			possiblyStopPolling(message);
		}
		
		public boolean userHasAccount() {
			SBLog.i(TAG, "userHasAccount()");
			return _user.hasAccount();
		}
		
		public String getUserId() {
			SBLog.i(TAG, "getUserId()");
			return _user.getUserId();
		}
		
		public ArrayList<String> getOpenShoutIds() {
			SBLog.i(TAG, "getOpenShoutIds()");
			return _inbox.getOpenShoutIDs();
		}
		
		public String getAuth() {
			SBLog.i(TAG, "getAuth()");
			return _user.getAuth();
		}
		
		public CellDensity getCellDensity() {
			SBLog.i(TAG, "getCellDensity()");
			return _user.getCellDensity();
		}
		
		public double getLongitude() {
			SBLog.i(TAG, "getLongitude()");
			return _location.getLongitude();
		}
		
		public double getLatitude() {
			SBLog.i(TAG, "getLatitude()");
			return _location.getLatitude();
		}
		
		public boolean getLevelUpOccurred() {
			SBLog.i(TAG, "getLevelUpOccurred()");
			return _user.getLevelUpOccured();
		}
		
		public int getLevel() {
			SBLog.i(TAG, "getLevel()");
			return _user.getLevel();
		}
		
		public void updateAuth(String nonce) {
			SBLog.i(TAG, "updateAuth()");
			_user.updateAuth(nonce);
		}
		
		public void updateScore(JSONObject jsonScore) {
			SBLog.i(TAG, "updateScore()");
			_inbox.updateScore(jsonScore);
		}
		
		public final String getAndroidId() {
			SBLog.i(TAG, "getAndroidId()");
			return _device.getAndroidId();
		}
		
		public final String getDeviceId() {
			SBLog.i(TAG, "getDeviceId()");
			return _device.getDeviceId();
		}
		
		public final String getPhoneNumber() {
			SBLog.i(TAG, "getPhoneNumber()");
			return _device.getPhoneNumber();
		}
		
		public final String getNetworkOperator() {
			SBLog.i(TAG, "getNetworkOperator()");
			return _device.getNetworkOperator();
		}

	}
	
	// UI GATEWAY /////////////////////////////////////////////////////////////
	
	public void refreshUiComponents() {
		_uiGateway.refreshUiComponents();
	}
	
	private class UiGateway {
		
		public UiGateway() {
			
		}

		public void handleShoutSent() {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "handleShoutSent()");
				AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _ui.shoutBtn.getDrawable();
				shoutButtonAnimation.stop();
				_ui.shoutBtn.setImageResource(R.drawable.shout_button_up);
				_ui.shoutInputEt.setText("");
			}
		}
		
		public void handleShoutFailed() {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "handleShoutFailed()");
				AnimationDrawable shoutButtonAnimation = (AnimationDrawable) _ui.shoutBtn.getDrawable();
				shoutButtonAnimation.stop();
				_ui.shoutBtn.setImageResource(R.drawable.shout_button_up);
			}
		}

		public void handleVoteFailed(String shoutId, int vote) {
			if (_isUIAlive.get()) {
				_ui.inboxListViewAdapter.undoVote(shoutId, vote);
			}
		}
		
		public void refreshUiComponents() {	
			refreshInbox(_inbox.getShoutsForUI());
			refreshNoticeTab(_user.getNoticesForUI());
			refreshProfile(_user.getLevel(), _user.getPoints(), _user.getNextLevelAt());
		}
		
		public void giveNotice(List<Notice> noticeContent) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "giveNotice()");
				_ui.noticeListViewAdapter.refresh(noticeContent);
				_ui.noticeTab.showOneLine();
			}
		}
		
		public void handleShoutsReceived(List<Shout> inboxContent, int newShouts) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "handleShoutsReceived()");
				_ui.inboxListViewAdapter.refresh(inboxContent);
			}
		}
		

		public void handleDensityChange(double newDensity, int level) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "handleDensityChange()");
				_ui.overlay.handleDensityChange(newDensity, level);
			}
		}

		public void handleLevelUp(double cellDensity, int newLevel) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "handleLevelUp()");
				_ui.overlay.handleLevelUp(cellDensity, newLevel);
				_ui.profileViewAdapter.refresh(_user.getLevel(), _user.getPoints(), _user.getNextLevelAt());
			}
		}

		public void handlePointsChange(int newPoints) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "handlePointsChange()");
				_ui.profileViewAdapter.refresh(_user.getLevel(), _user.getPoints(), _user.getNextLevelAt());
			}
		}
		
		public void refreshInbox(List<Shout> inboxContent) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "refreshInbox()");
				_ui.inboxListViewAdapter.refresh(inboxContent);
			}
		}
		
		public void refreshNoticeTab(List<Notice> noticeContent) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "refreshNoticeTab()");
				_ui.noticeListViewAdapter.refresh(noticeContent);
			}
		}
		
		public void refreshProfile(int level, int points, int nextLevelAt) {
			if (_isUIAlive.get()) {
				SBLog.i(TAG, "refreshProfile()");
				_ui.profileViewAdapter.refresh(level, points, nextLevelAt);
			}
		}
		
		public void handleServerFailure() {
			if (_isUIAlive.get()) {
				_ui.dialogBuilder.showDialog(DialogBuilder.DIALOG_SERVER_DOWN);
			}
		}
		
		public void enableInputs() {
			if (_isUIAlive.get()) {
				_ui.shoutBtn.setEnabled(true);
				_ui.shoutInputEt.setEnabled(true);
				//_cShoutText.setText("");
				_ui.inboxListViewAdapter.setInputAllowed(true);
			}
		}
		
		public void disableInputs() {
			if (_isUIAlive.get()) {
				_ui.shoutBtn.setEnabled(false);
				_ui.shoutInputEt.setEnabled(false);
				//_cShoutText.setText("   Turn on power to shout...");
				_ui.inboxListViewAdapter.setInputAllowed(false);
			}
		}
		
	}
	
}
