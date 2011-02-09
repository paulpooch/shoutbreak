package com.shoutbreak;

// Constants
public class C {

	public static final String CONFIG_SERVER_ADDRESS = "http://app.shoutbreak.co";
	public static final String CONFIG_CRASH_REPORT_ADDRESS = "http://app.shoutbreak.co/crash_reports/upload.php";
	public static final int CONFIG_PEOPLE_PER_LEVEL = 5;
	public static final int CONFIG_MIN_TARGETS_FOR_HIT_COUNT = 3;
	public static final int CONFIG_MAX_SIMULTANEOUS_HTTP_CONNECTIONS = 5; // for ConnectionQueue
	public static final int CONFIG_GPS_MIN_UPDATE_MILLISECS = 60000; // 0 gives most frequent
	public static final int CONFIG_GPS_MIN_UPDATE_METERS = 20; // 0 gives smallest interval
	public static final int CONFIG_DENSITY_GRID_X_GRANULARITY = 129600; // 10 second cells
	public static final int CONFIG_DENSITY_GRID_Y_GRANULARITY = 64800; // 10 second cells
	public static final long CONFIG_IDLE_LOOP_TIME_WITH_UI_OPEN = 60000; // 30 seconds
	public static final long CONFIG_DENSITY_EXPIRATION = (long) 4.32E8; // 5 days
	public static final double CONFIG_SHOUT_SCORING_DEFAULT_POWER = 0.10; //  0.10 to have a 95% chance that your lower bound is correct
	public static final int CONFIG_RESIZE_ICON_TOUCH_TOLERANCE = 100; // +/- 50 px from center
	public static final int CONFIG_TOUCH_TOLERANCE = 4;

	// map stuff
	public static final int DEFAULT_ZOOM_LEVEL = 15;
	public static final int MIN_RADIUS_PX = 30; // user can't resize below this
	public static final int MIN_RADIUS_METERS = 100; // initializes to this size when no density
	public static final int DEGREE_LAT_IN_METERS = 111133; // 60 nautical miles - avg'd from http://en.wikipedia.org/wiki/Latitude#Degree_length
	
	public static final int SHOUT_STATE_READ = 0;
	public static final int SHOUT_STATE_NEW = 1;
	public static final int SHOUT_VOTE_UP = 1;
	public static final int SHOUT_VOTE_DOWN = -1;	
	
	public static final String EXTRA_REFERRED_FROM_NOTIFICATION = "rfn";
	
	public static final double NORMAL_DIST_B[] = { 1.570796288, 0.03706987906, -0.8364353589e-3, -0.2250947176e-3,
			0.6841218299e-5, 0.5824238515e-5, -0.104527497e-5, 0.8360937017e-7, -0.3231081277e-8, 0.3657763036e-10,
			0.6936233982e-12 };

	// JSON FALLBACKS 
	// what we assume if value not returned by server	
	public static final int NULL_HIT = 0;
	public static final int NULL_APPROVAL = -1;
	public static final int NULL_DOWNS = 0;
	public static final boolean NULL_OPEN = false;
	public static final int NULL_UPS = 0;
	public static final int NULL_PTS = 0;
	public static final int NULL_SCORE = 0;
	public static final int NULL_VOTE = 0;
	
	// PREFERENCES ////////////////////////////////////////////////////////////

	public static final String PREFS_NAMESPACE = "shoutbreak";
	public static final String PREF_APP_ON_OFF_STATUS = "pref_app_on_off_status";

	// DATABASE //////////////////////////////////////////////////////////////

	public static final String DB_NAME = "sbdb";
	public static final String DB_TABLE_USER_SETTINGS = "USER_SETTINGS";
	public static final String DB_TABLE_DENSITY = "DENSITY";
	public static final String DB_TABLE_SHOUTS = "SHOUTS";
	public static final int DB_VERSION = 2;

	public static final String KEY_USER_PW = "user_pw";
	public static final String KEY_USER_ID = "user_id";
	public static final String KEY_USER_LEVEL = "user_level";
	public static final String KEY_USER_POINTS = "user_points";
	public static final String KEY_USER_NEXT_LEVEL_AT = "user_next_level_at";	

	// PURPOSES ///////////////////////////////////////////////////////////////

	public static final int PURPOSE_LOOP_FROM_UI = 0; // no delay
	public static final int PURPOSE_LOOP_FROM_UI_DELAYED = 1; // delay
	public static final int PURPOSE_LOOP_STANDALONE = 2; // no delay
	public static final int PURPOSE_LOOP_STANDALONE_DELAYED = 3; // delay
	public static final int PURPOSE_DEATH = 4; // don't repeat this - just die
	
	// UI CODES ///////////////////////////////////////////////////////////////
	
	public static final int UI_ACCOUNT_CREATED = 10;
	public static final int UI_RECEIVE_SHOUTS = 11;
	public static final int UI_LEVEL_CHANGE = 12;
	public static final int UI_SHOUT_SENT = 13;
	public static final int UI_VOTE_COMPLETED = 14;
	
	// HTTP CODES /////////////////////////////////////////////////////////////
	
	public static final int HTTP_DID_START = 20;
	public static final int HTTP_DID_ERROR = 21;
	public static final int HTTP_DID_SUCCEED = 22;

	// MESSAGES ///////////////////////////////////////////////////////////////
	
	public static final int STATE_IDLE = 30;
	public static final int STATE_CREATE_ACCOUNT_2 = 31;
	public static final int STATE_EXPIRED_AUTH = 32;
	public static final int STATE_RECEIVE_SHOUTS = 33;
	public static final int STATE_LEVEL_CHANGE = 34;
	public static final int STATE_SHOUT = 35;
	public static final int STATE_VOTE = 36;
	
	// JSON KEYS /////////////////////////////////////////////////////////////

	public static final String JSON_CODE = "code";
	public static final String JSON_CODE_PING_OK = "ping_ok";
	public static final String JSON_CODE_SHOUTS = "shouts";
	public static final String JSON_CODE_EXPIRED_AUTH = "expired_auth";
	public static final String JSON_CODE_INVALID_UID = "invalid_uid";
	public static final String JSON_CODE_LEVEL_CHANGE = "level_change";

	public static final String JSON_ACTION = "a";
	public static final String JSON_ACTION_CREATE_ACCOUNT = "create_account";
	public static final String JSON_ACTION_USER_PING = "user_ping";
	public static final String JSON_ACTION_SHOUT = "shout";
	public static final String JSON_ACTION_VOTE = "vote";

	public static final String JSON_ANDROID_ID = "android_id";
	public static final String JSON_AUTH = "auth";
	public static final String JSON_CARRIER_NAME = "carrier";
	public static final String JSON_DENSITY = "rho";
	public static final String JSON_DEVICE_ID = "device_id";
	public static final String JSON_LAT = "lat";
	public static final String JSON_LEVEL = "lvl";
	public static final String JSON_LONG = "long";
	public static final String JSON_NEXT_LEVEL_AT = "next_lvl_at";
	public static final String JSON_NONCE = "nonce";
	public static final String JSON_PHONE_NUM = "phone_num";
	public static final String JSON_POINTS = "pts";
	public static final String JSON_PW = "pw";
	public static final String JSON_SCORES = "scores";
	public static final String JSON_SHOUTS = "shouts";
	public static final String JSON_SHOUT_APPROVAL = "approval";
	public static final String JSON_SHOUT_DOWNS = "downs";
	public static final String JSON_SHOUT_HIT = "hit";
	public static final String JSON_SHOUT_ID = "shout_id";
	public static final String JSON_SHOUT_OPEN = "open";
	public static final String JSON_SHOUT_POWER = "power";
	public static final String JSON_SHOUT_RE = "re";
	public static final String JSON_SHOUT_TEXT = "txt";
	public static final String JSON_SHOUT_TIMESTAMP = "ts";
	public static final String JSON_SHOUT_UPS = "ups";
	public static final String JSON_UID = "uid";
	public static final String JSON_VOTE = "vote";
	
}
