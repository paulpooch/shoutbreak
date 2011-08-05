package co.shoutbreak.shared;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import co.shoutbreak.service.LocationTracker;
import co.shoutbreak.service.ShoutbreakService;
import co.shoutbreak.shared.utils.Hash;

public class User implements Observer {
	
	// All Database stuff should go through User. Any writes should be
	// synchronized.
	
	private final String TAG = "User";
	
	private ShoutbreakService _service;
	private StateManager _stateManager;
	
	private TelephonyManager _tm;
	private Database _db;
	private CellDensity _cellDensity;
	private LocationTracker _locationTracker;
	protected Inbox _inbox;
	private int _shoutsJustReceived;
	private boolean _levelUpOccured; //This means level up.
	//private boolean _densityJustChanged;
	private boolean _scoresJustReceived;
	private String _uid;
	private String _auth;
	private boolean _passwordExists; // no reason to put actual pw into memory
	private int _level;
	private int _points;
	private int _nextLevelAt;
	
	public User(ShoutbreakService service, StateManager stateManager, LocationTracker locationTracker) {
		_service = service;
		_stateManager = stateManager;
		_stateManager.addObserver(this);
		
		_tm = (TelephonyManager) service.getSystemService(Context.TELEPHONY_SERVICE);
		_db = new Database(_service);
		_locationTracker = locationTracker;
		_inbox = new Inbox(_service, _db, this);
		_passwordExists = false;
		_level = 0;
		_points = 0;
		_auth = "default"; // we don't have auth yet... just give us nonce
		HashMap<String, String> userSettings = _db.getUserSettings();
		if (userSettings.containsKey(C.KEY_USER_PW)) {
			_passwordExists = true;
		}
		if (userSettings.containsKey(C.KEY_USER_ID)) {
			_uid = userSettings.get(C.KEY_USER_ID);
		}
		if (userSettings.containsKey(C.KEY_USER_LEVEL)) {
			_level = Integer.parseInt(userSettings.get(C.KEY_USER_LEVEL));
		}
		if (userSettings.containsKey(C.KEY_USER_NEXT_LEVEL_AT)) {
			_nextLevelAt = Integer.parseInt(userSettings.get(C.KEY_USER_NEXT_LEVEL_AT));
		}
		initializePoints();
		_cellDensity = getCellDensity();
		
		// initialize user state
		StateEvent e = new StateEvent();
		e.locationServicesChanged = true;
		e.levelChanged = true;
		e.pointsChanged = true;
		e.inboxChanged = true;
		e.densityChanged = true;
		_stateManager.fireStateEvent(e);
		
	}
	
	public synchronized void saveDensity(double density) {
		CellDensity tempCellDensity = _locationTracker.getCurrentCell();
		_cellDensity.cellX = tempCellDensity.cellX;
		_cellDensity.cellY = tempCellDensity.cellY;
		_cellDensity.density = density;
		_db.saveCellDensity(_cellDensity);
		_cellDensity.isSet = true;
		//setDensityJustChanged(true);
	}
	
	public LocationTracker getLocationTracker() {
		return _locationTracker;
	}
	
	public void setShoutsJustReceived(int i) {
		_shoutsJustReceived = i;
	}
	
	public int getShoutsJustReceived() {
		return _shoutsJustReceived;
	}
	
	public boolean getLevelUpOccured() {
		return _levelUpOccured;
	}
	
	public void setScoresJustReceived(boolean b) {
		_scoresJustReceived = b;
	}
	
	public boolean getScoresJustReceived() {
		return _scoresJustReceived;
	}
	
	public double getLatitude() {
		return _locationTracker.getLatitude();
	}

	public double getLongitude() {
		return _locationTracker.getLongitude();
	}

	public Inbox getInbox() {
		return _inbox;
	}

	public synchronized CellDensity getCellDensity() {
		if (_cellDensity == null) {
			_cellDensity = new CellDensity();
		} else {
			// If _cellDensity exists, see if it's still valid.
			CellDensity oldCellDensity = _cellDensity;
			CellDensity tempCellDensity = _locationTracker.getCurrentCell();
			_cellDensity.cellX = tempCellDensity.cellX;
			_cellDensity.cellY = tempCellDensity.cellY;
			if (_cellDensity.isSet && _cellDensity.cellX == oldCellDensity.cellX && _cellDensity.cellY == oldCellDensity.cellY) {
				// We're still in the same cell so return this.
				return _cellDensity;
			}
		}
		// Otherwise we'll see if DB has a cached result. If not, isSet will be false.
		CellDensity tempCellDensity = _db.getDensityAtCell(_cellDensity);
		if (tempCellDensity.isSet) {
			_cellDensity.density = tempCellDensity.density;
			_cellDensity.isSet = true;
		}
		return _cellDensity;
	}

	public String getAuth() {
		return _auth;
	}

	public synchronized void updateAuth(String nonce) {
		String pw = "";
		HashMap<String, String> userSettings = _db.getUserSettings();
		if (userSettings.containsKey(C.KEY_USER_PW)) {
			pw = userSettings.get(C.KEY_USER_PW);
		}
		// $auth = sha1($uid . $pw . $nonce);
		//_auth = Hash.sha1(_uid + pw + nonce);	
		_auth = pw + Hash.sha512(pw + nonce + _uid);		
	}

	public boolean hasAccount() {
		return _passwordExists;
	}

	public synchronized void setPassword(String pw) {
		// TODO: should we encrypt or obfuscate this or something?
		// plaintext in db safe?
		_db.saveUserSetting(C.KEY_USER_PW, pw);
		_passwordExists = true;
	}

	public synchronized void setUID(String uid) {
		_db.saveUserSetting(C.KEY_USER_ID, uid);
		_uid = uid;
	}
	
	public synchronized void levelUp(int newLevel, int newPoints, int nextLevelAt) {
		setLevel(newLevel);
		setNextLevelAt(nextLevelAt);
		setPoints(newPoints);
		_levelUpOccured = true;
	}
	
	private synchronized void setLevel(int level) {
		String sLevel = Integer.toString(level);
		_db.saveUserSetting(C.KEY_USER_LEVEL, sLevel);
		_level = level;
	}
	
	private synchronized void setNextLevelAt(int nextLevelAt) {
		String sNextLevelAt = Integer.toString(nextLevelAt);
		_db.saveUserSetting(C.KEY_USER_NEXT_LEVEL_AT, sNextLevelAt);
		_nextLevelAt = nextLevelAt;
	}
	
	// TODO: implement this for when level changes
	private synchronized void setPoints(int points) {
		//String sPoints = Integer.toString(points);
		//_db.saveUserSetting(C.KEY_USER_POINTS, sPoints);
		_db.savePoints(C.POINTS_LEVEL_CHANGE, points);
		initializePoints();
	}
	
	public String getUID() {
		return _uid;
	}
	
	public int getLevel() {
		return _level;
	}
	
	public synchronized void savePoints(int amount) {
		_db.savePoints(C.POINTS_SHOUT, amount);
		_points += amount;
	}
	
	public int getPoints() {
		return _points;
	}
	
	private synchronized void initializePoints() {
		_points = _db.calculateUsersPoints();
	}
	
	public int getNextLevelAt() {
		return _nextLevelAt;
	}

	public String getDeviceId() {
		return _tm.getDeviceId();
	}

	public String getPhoneNumber() {
		return _tm.getLine1Number();
	}

	public String getNetworkOperator() {
		return _tm.getNetworkOperatorName();
	}

	public String getAndroidId() {
		return Settings.Secure.getString(_service.getContentResolver(), Settings.Secure.ANDROID_ID);
	}
	
	public void destroy() {
		_stateManager.deleteObserver(this);
		_stateManager = null;
		_service = null;
	}

	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
		
	}
	
	// STATICS ////////////////////////////////////////////////////////////////

	public static float calculateRadius(int power, double density) {
		int maxPeople = power * C.CONFIG_PEOPLE_PER_LEVEL;
		double area = maxPeople / density;
		float radius = (float) Math.sqrt(area / Math.PI);
		return radius;
	}
	
	public static int calculatePower(int people) {
		return (int)Math.ceil((float)people / (float)C.CONFIG_PEOPLE_PER_LEVEL);
	}

	public static void setBooleanPreference(Context context, String key, boolean val) {
		SharedPreferences settings = context.getSharedPreferences(C.PREFS_NAMESPACE, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, val);
		editor.commit();
	}

	public static boolean getBooleanPreference(Context context, String key, boolean defaultReturnVal) {
		SharedPreferences settings = context.getSharedPreferences(C.PREFS_NAMESPACE, Context.MODE_PRIVATE);
		boolean val = settings.getBoolean(key, defaultReturnVal);
		return val;
	}
}