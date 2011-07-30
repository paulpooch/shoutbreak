<?php
///////////////////////////////////////////////////////////////////////////////
// Config
///////////////////////////////////////////////////////////////////////////////
class Config {
	
	public static $SHOUTBREAK_SCORING_COEFFECIENT = 1.8;
	public static $USER_INITIAL_LEVEL = 1;
	public static $USER_INITIAL_POINTS = 0;
	public static $USER_INITIAL_PENDING_LEVEL_UP = 1;
	public static $DEFAULT_POPULATION_DENSITY = .0001; // NYC = .0106 / m^2, but that's PEOPLE not USERS
	public static $DATE_FORMAT = DATE_ISO8601;
	public static $PAD_USER_LEVEL = 4;
	public static $PAD_USER_POINTS = 10;
	public static $PAD_COORDS = 8;
	public static $OFFSET_LAT = 90;
	public static $OFFSET_LONG = 180; 
	public static $MIN_TARGETS_FOR_HIT_COUNT = 3;
	public static $VOTING_WINDOW = 300; // 5 minutes
	public static $SCORE_REQUEST_LIMIT = 10;
	public static $SIMPLEDB_MAX_NUMBER_OF_ITEMS = 2500;
	public static $HASHING_ALGORITHM = 'sha512';
	public static $PASSWORD_LENGTH = 32;
	
	// actions
	public static $ACTION_CREATE_ACCOUNT = 'create_account';
	public static $ACTION_USER_PING = 'user_ping';
	public static $ACTION_SHOUT = 'shout';
	public static $ACTION_VOTE = 'vote';
	public static $ACTION_CRON_LIVE_USER_CULL = 'cron_live_user_cull';
	public static $ACTION_CRON_CLOSE_SHOUTS = 'cron_close_shouts';
	
	// memcached
	public static     $PRE_SHOUT = 'shout';
	public static $TIMEOUT_SHOUT = 1800;
	public static     $PRE_INBOX = 'inbox';
	public static $TIMEOUT_INBOX = 1800;
	public static     $PRE_CREATE_ACCOUNT_USER_TEMP_ID = 'tempuserid';
	public static $TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID = 300;
	public static     $PRE_ACTIVE_AUTH = 'activeauth';
	public static $TIMEOUT_ACTIVE_AUTH = 1800;
	public static     $PRE_SHOUT_TABLE_INDEX = 'shouttableindex';
	public static $TIMEOUT_SHOUT_TABLE_INDEX = 2592000; // 30 days
	public static     $PRE_USER_PENDING_LEVEL_UP = 'pending';
	public static $TIMEOUT_USER_PENDING_LEVEL_UP = 1800;
	public static	  $PRE_DDOS_SHIELD = 'ddos';
	public static $TIMEOUT_DDOS_SHIELD = 5;
	public static $DDOS_SHIELD_LIMIT = 20;
	public static $TIMEOUT_DDOS_BAN_LENGTH = 600; // 10 minutes
	
	// SHOUT SCORING //////////////////////////////////////////////////////////
	
	public static function maxTargetsAtLevel($level) {
		// + 1 since we'll probably hit the sender & that shouldn't count
		return min(($level * 5) + 1, Config::$SIMPLEDB_MAX_NUMBER_OF_ITEMS);
	}
	
	public static function reachAtLevel($level) {
		return Config::maxTargetsAtLevel($level);	
	}
	
	public static function actionsRequiredForLevel($level) {
		return pow($level, Config::$SHOUTBREAK_SCORING_COEFFECIENT); 
	}
	
	public static function pointsRequiredForLevel($level) {
		e("pointsRequiredForLevel $level");
		$result = round(Config::actionsRequiredForLevel($level) * Config::reachAtLevel($level));
		e("pointsRequired = $result");
		return $result;
	}
	
	///////////////////////////////////////////////////////////////////////////
		
}

?>