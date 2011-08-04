package co.shoutbreak;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Message;
import android.widget.Toast;
import co.shoutbreak.shared.C;
import co.shoutbreak.shared.CellDensity;
import co.shoutbreak.shared.Flag;
import co.shoutbreak.shared.SBLog;

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
	private UserLocationOverlay _userLocationOverlay;
	private InboxListViewAdapter _inboxListViewAdapter;
	
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
		_device = new DeviceInformation(this, _service);
		_notifier = new Notifier(this, _service);
		_location = new LocationTracker(this);
		_data = new DataListener(this);
		_db = new Database(this, _service);
		_user = new User(this, _db);
		_inbox = new Inbox(this, _db);
		_threadLauncher = new ThreadLauncher(this);
		_userLocationOverlay = new UserLocationOverlay();
		
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
		_preferences.unsetMediator();
		_preferences = null;
		_device.unsetMediator();
		_device = null;
		_notifier.unsetMediator();
		_notifier = null;
		_location.unsetMediator();
		_location = null;
		_data.unsetMediator();
		_data = null;
		_db.unsetMediator();
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
		if (_isUIAlive.get()) {

		}
	}
	
	public void onLocationDisabled() {
		SBLog.i(TAG, "onLocationDisabled()");
		_isLocationAvailable.set(false);
		stopPolling();
		if (_isUIAlive.get()) {
			_ui.onLocationDisabled();
		}
	}
	
	public void onDataEnabled() {
		SBLog.i(TAG, "onDataEnabled()");
		_isDataAvailable.set(true);
		if (_isUIAlive.get()) {
			_ui.onDataDisabled();
		}
	}
	
	public void onDataDisabled() {
		SBLog.i(TAG, "onDataDisabled()");
		_isDataAvailable.set(false);
		stopPolling();
		if (_isUIAlive.get()) {
			
		}
	}
	
	public void checkLocationProviderStatus() {
		SBLog.i(TAG, "checkLocationProviderStatus()");
		if (_location.isLocationEnabled()) {
			onLocationEnabled();
		} else {
			onLocationDisabled();
		}
	}
	
	public void shout(CharSequence text) {
		SBLog.i(TAG, "shout()");
		if (text.length() == 0) {
			Toast.makeText(_ui, "cannot shout blanks", Toast.LENGTH_SHORT).show();
		} else {
			// TODO: filter all text going to server
		}
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
		
	// Triggered from a Shout close.  Have user save earned points.
	public void pointsChangeEvent(int additionalPoints) {
		_user.handlePointsChangeEvent(additionalPoints);
		_ui.handlePointsChangeEvent(_user.getPoints());
	}
	
	public void deleteShout(String shoutId) {
		_inbox.deleteShout(shoutId);
	}
	
	public void vote(String shoutID, int vote) {
		_inbox.vote(shoutID, vote);
	}
	
	// TODO: rename this method
	public void spawnANewUIServiceThread(Message message) {
		_threadLauncher.spawnANewUIServiceThread(message);
	}
	
	public ThreadSafeMediator getAThreadSafeMediator() {
		SBLog.i(TAG, "getAThreadSafeMediator()");
		return new ThreadSafeMediator();
	}
	
	// THREAD SAFE MEDIATOR ///////////////////////////////////////////////////
	
	public class ThreadSafeMediator {
		// Methods of any other classes called from here should be synchronized or read only.
		
		public ThreadSafeMediator() {
		
		}
		
		public void densityChangeEvent(double density) {
			// Note: The order in these matters.
			_user.handleDensityChangeEvent(density);
			_userLocationOverlay.handleDensityChangeEvent(density, _user.getLevel());
		}
	
		public void shoutsReceivedEvent(JSONArray shouts) {		
			_inbox.handleShoutsReceivedEvent(shouts);
			_ui.handleShoutsReceivedEvent(shouts.length());
			_notifier.handleShoutsReceivedEvent(shouts.length());			
			_inboxListViewAdapter.handleShoutsReceivedEvent(_inbox.getShoutsForUI());
		}
		
		public void scoresReceivedEvent(JSONArray scores) {
			_inbox.handleScoresReceivedEvent(scores);
			_inboxListViewAdapter.handleScoresReceivedEvent(_inbox.getShoutsForUI());
		}
			
		public void levelUpEvent(JSONObject levelInfo) {
			_user.handleLevelUpEvent(levelInfo);
			_ui.handleLevelUpEvent(_user.getLevel());
			_ui.handlePointsChangeEvent(_user.getPoints());
			_userLocationOverlay.handleLevelUpEvent(_user.getCellDensity().density, _user.getLevel());
		}
		
		public void shoutSentEvent() {
			_ui.handleShoutSentEvent();
		}
		
		public void voteSentEvent(String shoutId, int vote) {
			_inbox.handleVoteSentEvent(shoutId, vote);
			_inboxListViewAdapter.handleVoteSentEvent(_inbox.getShoutsForUI());
		}
		
		public void accountCreatedEvent(String uid, String password) {
			_user.handleAccountCreatedEvent(uid, password);
			// Maybe we should do something in the UI?
		}
		
		public boolean userHasAccount() {
			return _user.hasAccount();
		}
		
		public String getUserId() {
			return _user.getUserId();
		}
		
		public ArrayList<String> getOpenShoutIds() {
			return _inbox.getOpenShoutIDs();
		}
		
		public String getAuth() {
			return _user.getAuth();
		}
		
		public CellDensity getCellDensity() {
			return _user.getCellDensity();
		}
		
		public double getLongitude() {
			return _location.getLongitude();
		}
		
		public double getLatitude() {
			return _location.getLatitude();
		}
		
		public boolean getLevelUpOccurred() {
			return _user.getLevelUpOccured();
		}
		
		public int getLevel() {
			return _user.getLevel();
		}
		
		public void updateAuth(String nonce) {
			_user.updateAuth(nonce);
		}
		
		public void updateScore(JSONObject jsonScore) {
			_inbox.updateScore(jsonScore);
		}
		
		public void reflectVote(String shoutID, int vote) {
			_inbox.reflectVote(shoutID, vote);
		}
		
		public final String getAndroidId() {
			return _device.getAndroidId();
		}
		
		public final String getDeviceId() {
			return _device.getDeviceId();
		}
		
		public final String getPhoneNumber() {
			return _device.getPhoneNumber();
		}
		
		public final String getNetworkOperator() {
			return _device.getNetworkOperator();
		}
	}
}
	/*

	public void initIsLocationAvailable() {
		LocationManager manager = _location.getLocationManager();

		// check if GPS is enabled
		if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			if (_isUIAlive) {
				new AlertDialog.Builder(_ui)
					.setMessage("Your location seems to be disabled, do you want to enable it?")
				 	.setCancelable(false)
				 	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				 		public void onClick(final DialogInterface dialog, final int id) {
				 	        ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
				 	        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				 	        intent.addCategory(Intent.CATEGORY_LAUNCHER);
				 	        intent.setComponent(toLaunch);
				 	        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				 	        _ui.startActivityForResult(intent, 0);
				 		}
				 })
				 .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				 	public void onClick(final DialogInterface dialog, final int id) {
				 		dialog.cancel();
				 	}
				 })
				 .create().show();
		    }
		}
		
		_isLocationAvailable  = _location.isLocationEnabled();
	}
*/
