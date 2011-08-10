package co.shoutbreak.core;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import co.shoutbreak.core.utils.DataListener;
import co.shoutbreak.core.utils.Flag;
import co.shoutbreak.core.utils.Notifier;
import co.shoutbreak.core.utils.SBLog;
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
import android.os.Message;

public class Mediator {
	
	private static final String TAG = "Mediator";
	
	// colleagues
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
	private Flag _isUIAlive = new Flag("_isUIAlive");
	private Flag _isPollingAlive = new Flag("_isPollingAlive");
	private Flag _isServiceConnected = new Flag("_isServiceConnected");
	private Flag _isServiceStarted = new Flag("_isServiceStarted");
	private Flag _isLocationAvailable = new Flag("_isLocationAvailable");
	private Flag _isDataAvailable = new Flag("_isDataAvailable");
	
	/* Mediator Lifecycle */
	
	public Mediator(ShoutbreakService service) {
    	SBLog.i(TAG, "new Mediator()");
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
		
		// initialize state
		_isLocationAvailable.set(_location.isLocationEnabled());
		_isDataAvailable.set(_data.isDataEnabled());
		_isPollingAlive.set(false);
		_isUIAlive.set(false);
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
		_isPollingAlive.set(false);
	}
	
	public void appLaunchedFromUI() {
		SBLog.i(TAG, "appLaunchedFromUI()");
		_isServiceStarted.set(true);
		if (_preferences.getBoolean(C.POWER_STATE_PREF, true)) {
			_ui.setPowerState(true);
		} else {
			_ui.setPowerState(false);
		}
	}
	
	public void appLaunchedFromAlarm() {
		SBLog.i(TAG, "appLaunchedFromAlarm()");
		_isServiceStarted.set(true);
		if (_preferences.getBoolean(C.POWER_STATE_PREF, true)) {
			startPolling();
		} else {
			stopPolling();
		}
	}
	
	public void startPolling() {
		SBLog.i(TAG, "startPolling()");
		if (!_isPollingAlive.get() && _isLocationAvailable.get() && _isDataAvailable.get()) {
			SBLog.i(TAG, "app fully functional");
			_isPollingAlive.set(true);
			_threadLauncher.startPolling();
		} else if (!_isLocationAvailable.get() || !_isDataAvailable.get()) {
			if (!_isLocationAvailable.get()) {
				SBLog.e(TAG, "unable to start service, location unavailable");
			}
			if (!_isDataAvailable.get()) {
				SBLog.e(TAG, "unable to start service, data unavailable");
			}
			if (_isUIAlive.get()) {
				_ui.setPowerState(false);
				_ui.unableToTurnOnApp(_isLocationAvailable.get(), _isDataAvailable.get());
			} else {
				onPowerDisabled();
			}
		} else if (_isPollingAlive.get()) {
			SBLog.i(TAG, "service is already polling, unable to call startPolling()");
		}
	}
	
	public void stopPolling() {
		SBLog.i(TAG, "stopPolling()");
		if (_isPollingAlive.get()) {
			_isPollingAlive.set(false);
			_threadLauncher.stopPolling();
		} else {
			SBLog.i(TAG, "service is not polling, unable to call stopPolling()");
		}
	}
	
	public void onPowerEnabled() {
		SBLog.i(TAG, "onPowerEnabled()");
		_preferences.putBoolean(C.POWER_STATE_PREF, true);
		_service.enableAlarmReceiver();
		startPolling();
	}
	
	public void onPowerDisabled() {
		SBLog.i(TAG, "onPowerDisabled()");
		_preferences.putBoolean(C.POWER_STATE_PREF, false);
		_service.disableAlarmReceiver();
		stopPolling();
	}
	
	public void onLocationEnabled() {
		SBLog.i(TAG, "onLocationEnabled()");
		_isLocationAvailable.set(true);
		if (_isUIAlive.get() && _isDataAvailable.get()) {
			_ui.enableMapAndOverlay();
		}
	}
	
	public void onLocationDisabled() {
		SBLog.i(TAG, "onLocationDisabled()");
		_isLocationAvailable.set(false);
		stopPolling();
		if (_isUIAlive.get()) {
			_ui.onLocationDisabled();
			_ui.disableMapAndOverlay();
		}
	}
	
	public void onDataEnabled() {
		SBLog.i(TAG, "onDataEnabled()");
		_isDataAvailable.set(true);
		if (_isUIAlive.get() && _isLocationAvailable.get()) {
			_ui.enableMapAndOverlay();
		}
	}
	
	public void onDataDisabled() {
		SBLog.i(TAG, "onDataDisabled()");
		_isDataAvailable.set(false);
		stopPolling();
		if (_isUIAlive.get()) {
			_ui.onDataDisabled();
			_ui.disableMapAndOverlay();
		}
	}
	
	private void createNotice(int noticeType, String noticeText, String noticeRef) {
		_user.saveNotice(noticeType, noticeText, noticeRef);
		_ui.giveNotice(_user.getNoticesForUI());
	}
	
	public void createDebugNotice(String noticeText) {
		_user.saveNotice(C.NOTICE_DEBUG, noticeText, null);
		_ui.giveNotice(_user.getNoticesForUI());
	}
	
	public void checkLocationProviderStatus() {
		SBLog.i(TAG, "checkLocationProviderStatus()");
		createDebugNotice("Mediator.checkLocationProviderStatus = " + _location.isLocationEnabled());		
		if (_location.isLocationEnabled()) {
			onLocationEnabled();
		} else {
			onLocationDisabled();
		}
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
		if (!_isLocationAvailable.get()) {
			SBLog.e(TAG, "location is unavailable, unable to get current cell");
		}
		return _location.getCurrentCell();
	}
	
	public void deleteShout(String shoutId) {
		SBLog.i(TAG, "deleteShout()");
		_inbox.deleteShout(shoutId);
		if (_isUIAlive.get()) {
			_ui.refreshInbox(_inbox.getShoutsForUI());
		}
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
		if (_isUIAlive.get()) {
			_ui.handlePointsChange(_user.getPoints());
		}
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
		
		public void densityChange(double density) {
			SBLog.i(TAG, "densityChange()");
			// Note: The order in these matters.
			_user.handleDensityChange(density);
			if (_isUIAlive.get()) {
				_ui.handleDensityChange(density, _user.getLevel());
			}
		}
	
		public void shoutsReceived(JSONArray shouts) {
			SBLog.i(TAG, "shoutsReceived()");
			_inbox.handleShoutsReceived(shouts);
			if (_isUIAlive.get()) {
				_ui.handleShoutsReceived(_inbox.getShoutsForUI(), shouts.length());
			} else {
				_notifier.handleShoutsReceived(shouts.length());				
			}
			String pluralShout = "shout" + (shouts.length() > 1 ? "s" : "");
			String notice = "just heard " + shouts.length() + " new " + pluralShout;
			createNotice(C.NOTICE_SHOUTS_RECEIVED, notice, null);
		}
		
		public void scoresReceived(JSONArray scores) {
			SBLog.i(TAG, "scoresReceived()");
			_inbox.handleScoresReceived(scores);
			if (_isUIAlive.get()) {
				_ui.refreshInbox(_inbox.getShoutsForUI());
			}
		}
			
		public void levelUp(JSONObject levelInfo) {
			SBLog.i(TAG, "levelUp()");
			_user.handleLevelUp(levelInfo);
			if (_isUIAlive.get()) {
				_ui.handleLevelUp(_user.getCellDensity().density, _user.getLevel());
				_ui.handlePointsChange(_user.getPoints());
			}
			createNotice(C.NOTICE_LEVEL_UP, "You leveled up! You're now level " + _user.getLevel(), null);
		}
		
		public void shoutSent() {
			SBLog.i(TAG, "shoutSent()");
			if (_isUIAlive.get()) {
				_ui.handleShoutSent();
			}
			createNotice(C.NOTICE_SHOUT_SENT, "shout sent", null);
		}
		
		public void voteFinish(String shoutId, int vote) {
			SBLog.i(TAG, "voteFinish()");
			_inbox.handleVoteFinish(shoutId, vote);
			if (_isUIAlive.get()) {
				_ui.refreshInbox(_inbox.getShoutsForUI());
			}
		}
		
		public void accountCreated(String uid, String password) {
			SBLog.i(TAG, "accountCreated()");
			_user.handleAccountCreated(uid, password);
			// Maybe we should do something in the UI?
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
}
