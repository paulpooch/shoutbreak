////////////////////////////////////////////////////////////////////////////////
// 
// CONFIG
//
////////////////////////////////////////////////////////////////////////////////
var Config = module.exports = {};

(function() {

	// Settings
	this.VERSION = '006';
	this.C2DM_SERVER_PORT = 8000;
	this.C2DM_SERVER_ADDRESS = '127.0.0.1';
	this.C2DM_ID_LENGTH_LIMIT = 200;
	this.C2DM_UNTOUCHED_INBOX_TIMEOUT = 120000; // 2 minutes
	this.USER_INITIAL_POINTS = 71;
	this.USER_INITIAL_LEVEL = 5;
	this.USER_INITIAL_PENDING_LEVEL_UP = 5;
	this.USER_INITIAL_C2DM_ID = '0';
	this.PASSWORD_LENGTH = 32;
	this.PAD_COORDS = 8;
	this.MULTIPLY_COORDS = 100000;
	this.OFFSET_LAT = 90;
	this.OFFSET_LNG = 180;
	this.SHOUTREACH_BUFFER_METERS = 200;
	this.SHOUTREACH_LIMIT = 500;
	this.SHOUT_LENGTH_LIMIT = 256;
	this.SHOUT_IDLE_TIMEOUT = 30; // minutes
	this.SIMPLEDB_MAX_NUMBER_OF_ITEMS = 2500;
	this.SHOUTBREAK_SCORING_WORK_AT_LEVEL_1 = 10;
	this.SHOUTBREAK_SCORING_COEFFECIENT = 1.09;
	this.SELECT_ALGORITHM_ACCEPTABLE_EXTRA = 100;
	this.SELECT_ALGORITHM_INCREMENT = 50;
	this.SCORE_REQUEST_LIMIT = 20;
	this.RADIUS_REQUEST_LIMIT = 10;
	this.RADIUS_FOR_INSUFFICIENT_USERS_ONLINE = 6000000; // earth is 6378000, let's not push it tho.
	this.LIVE_USERS_TIMEOUT = 2400000; // 40 minutes
	this.AUTH_ATTEMPT_FAIL_LIMIT = 30; // consider lowering this once not debugging (relaunching a million times).
	this.CRON_INTERVAL_CULL_LIVE_USERS = 1800000; // 30 minutes
	this.INTERVAL_LOG_RENAME = 1800000; // 30 minutes
	 
	// AWS
	// For Elasticache:
	//this.CACHE_URL = 'cache-001.ardkb4.0001.use1.cache.amazonaws.com:11211',
	// For local:
	this.CACHE_URL = 'localhost:11211',
	this.DYNAMODB_CREDENTIALS = {
		AccessKeyId:'AKIAINHDEIZ3QVSHQ3PA', 
		SecretKey: 	'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'
	};
	this.SIMPLEDB_CREDENTIALS = {
		keyid: 		'AKIAINHDEIZ3QVSHQ3PA', 
		secret: 	'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'
	};
	
	// Cache Keys
	this.PRE_CREATE_ACCOUNT_USER_TEMP_ID = 		'tempuserid';
	this.TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID = 	300; // 5 minutes
	this.PRE_ACTIVE_AUTH = 						'activeauth';
	this.TIMEOUT_ACTIVE_AUTH = 					1800; // 30 minutes
	this.PRE_USER =								'user';
	this.TIMEOUT_USER =							1800; // 30 minutes
	this.PRE_SHOUT =							'shout';
	this.TIMEOUT_SHOUT =						3600; // 60 minutes
	this.PRE_INBOX =							'inbox';
	this.TIMEOUT_INBOX =						3600; // 60 minutes
	this.PRE_VOTE =								'vote';
	this.TIMEOUT_VOTE = 						300; // 5 minutes
	this.PRE_RADIUS_REQUEST = 					'radiusreq';
	this.TIMEOUT_RADIUS_REQUEST = 				1200; // 20 minutes
	this.PRE_AUTH_ATTEMPT_FAIL =				'authfail';
	this.TIMEOUT_AUTH_ATTEMPT_FAIL =			1200; // 20 minutes
	this.PRE_REPLY =							'reply';
	this.TIMEOUT_REPLY =						1800; // 30 minutes

	// Tables
	this.TABLE_USERS = 'USERS';
	this.TABLE_LIVE = 'LIVE';
	this.TABLE_SHOUTS = 'SHOUTS';
	this.TABLE_VOTES = 'VOTES';
	this.TABLE_REPLIES = 'REPLIES';

}).call(Config);