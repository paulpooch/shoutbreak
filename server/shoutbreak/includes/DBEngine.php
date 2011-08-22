<?php
///////////////////////////////////////////////////////////////////////////////
// DBEngine
///////////////////////////////////////////////////////////////////////////////
// REFERENCES
// http://practicalcloudcomputing.com/post/313922691/5-steps-simpledb-performance
// we can trade more requests for faster performance by sharding across domains
// we can trade consistency for performance
// for performance, we should stop using sdb... it's an extra layer.  just paste code.
class DBEngine {
	/*
	 * USERS
	 * user_id
	 * android_id
	 * device_id
	 * phone_num
	 * creation_time
	 * last_activity_time // this is rough estimate, fuck logging every activity
	 * level
	 * points
	 * pending_level_up
	 * 
	 * 
	 * LIVE_USERS
	 * user_id
	 * ping_time
	 * lat
	 * long
	 * 
	 * RHO
	 * x
	 * y
	 * count
	 * density
	 * 
	 * SHOUTS 
	 * shout_id
	 * user_id
	 * time
	 * txt
	 * lat
	 * long
	 * power
	 * hit
	 * open
	 * ups
	 * downs
	 */
	
	private $key = 'AKIAINHDEIZ3QVSHQ3PA';
	private $secret = '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';
	private $sdb = null;
	private $TABLE_USERS = 'USERS';
	private $TABLE_LIVE = 'LIVE_USERS';
	private $TABLE_RHO_PREFIX = 'RHO_';
	private $TABLE_SHOUT_INDEX = 0;
	private $TABLE_SHOUT_PREFIX = 'SHOUTS_';
	private $TABLE_VOTES = 'VOTES';
	private $LIVE_USERS_TIMEOUT = 20; // 20 minutes
	private $DENSITY_GRID_X_GRANULARITY = 129600; // 10 second cells
	private $DENSITY_GRID_Y_GRANULARITY = 64800; // 10 second cells
	private $RHO_HASH_WIDTH = 10; // counting units
	private $RHO_HASH_HEIGHT = 5; // counting units
	private $RHO_HASH_WIDTH_IN_CELLS = 12960; // 10 second cells
	private $RHO_HASH_HEIGHT_IN_CELLS = 6480; // 10 second cells
	private $SAFE_SELECT_ATTEMPTS = 3;
	private $SAFE_GET_ATTEMPTS = 3;
	private $SAFE_PUT_ATTEMPTS = 3;	
	private $BACKOFF_INITIAL = 1;
	private $BACKOFF_FACTOR = 2;
	private $EARTHS_RADIUS = 6378137; // in meters
	private $SIMPLEDB_BATCH_PUT_LIMIT = 25;

	public function __construct() {
		$this->sdb = new SimpleDB($this->key, $this->secret, false);
		$this->RHO_HASH_WIDTH_IN_CELLS = $this->DENSITY_GRID_X_GRANULARITY / $this->RHO_HASH_WIDTH;
		$this->RHO_HASH_HEIGHT_IN_CELLS = $this->DENSITY_GRID_Y_GRANULARITY / $this->RHO_HASH_HEIGHT;
	}
	
	public function __destruct() {
	}
	
	public function addUser($uid, $androidID, $deviceID, $phoneNum, $carrier) {
		global $mem, $log;
		$time = date(Config::$DATE_FORMAT);
		$pw = Utils::generatePassword();
		$pw_salt = Utils::generatePassword(16, 3); // 16 chars long, tier 3 charset = all special chars
		$pw_hash = hash(Config::$HASHING_ALGORITHM, $pw . $pw_salt);
		$points = Config::$USER_INITIAL_POINTS;
		$pointsPadded = Utils::pad($points, Config::$PAD_USER_POINTS);
		$level = Config::$USER_INITIAL_LEVEL;
		$levelPadded = Utils::pad($level, Config::$PAD_USER_LEVEL);
		$pendingLevelUpPadded = Utils::pad(Config::$USER_INITIAL_PENDING_LEVEL_UP, Config::$PAD_USER_LEVEL);
		$attrs = array(
				'user_id' => array('value' => $uid), 
				'user_pw_hash' => array('value' => $pw_hash),
				'user_pw_salt' => array('value' => $pw_salt),
				'android_id' => array('value' => $androidID), 
				'device_id' => array('value' => $deviceID), 
				'phone_num' => array('value' => $phoneNum),
				'carrier' => array('value' => $carrier),
				'creation_time' => array('value' => $time),
				'last_activity_time' => array('value' => $time),
				'points' => array('value' => $pointsPadded),
				'level' => array('value' => $levelPadded),
				'pending_level_up' => array('value' => $pendingLevelUpPadded));
		$attempts = $this->SAFE_PUT_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		while ($attempts > 0) {
			$put = $this->sdb->putAttributes($this->TABLE_USERS, $uid, $attrs);
			if ($put) {
				$mem->delete(Config::$PRE_ACTIVE_AUTH . $uid); 

				// User will get a level_change packet on first ping
//				$levelUpInfo = array('level' => $level, 'pts' => $points);
//				$log->LogInfo("LEVEL UP INFO | level = $level, pts = $points");
//				$replaced = $mem->replace(Config::$PRE_USER_PENDING_LEVEL_UP . $uid, $levelUpInfo);
//				if(!$replaced) {
//					$replaced = $mem->set(Config::$PRE_USER_PENDING_LEVEL_UP . $uid, $levelUpInfo, false, Config::$TIMEOUT_USER_PENDING_LEVEL_UP);
//				}
				
				return $pw;
				break;
			} else {
				if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;				
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}
		return false;
	}
	
	public function generateNonce($uid) {
		global $mem, $log;
		$log->LogError("generate Nonce $uid");
		$attempts = $this->SAFE_GET_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		$pw_hash = null;
		$pw_salt = null;
		while ($attempts > 0) {
			$row = $this->sdb->getAttributes($this->TABLE_USERS, $uid);
			if ($row) {
				$pwHash = $row['user_pw_hash'];
				$pwSalt = $row['user_pw_salt'];
				
				// See if user recently leveled up - put in memcache
				$pendingLevelUp = (int)$row['pending_level_up'];
				if ($pendingLevelUp) {
					$levelUpInfo = array('level' => $pendingLevelUp, 'pts' => (int)$row['points']);
					$replaced = $mem->replace(Config::$PRE_USER_PENDING_LEVEL_UP . $uid, $levelUpInfo, false, Config::$TIMEOUT_USER_PENDING_LEVEL_UP);
					if (!$replaced) {
						$replaced = $mem->set(Config::$PRE_USER_PENDING_LEVEL_UP . $uid, $levelUpInfo, false, Config::$TIMEOUT_USER_PENDING_LEVEL_UP);
					}				
				}				
				
				break;
			} else {
				if (count($row) == 0) {
					global $app;
					$resp = array('code' => 'invalid_uid');
				 	$app->respond($resp);			
					break;
				} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}
		if ($pwHash && $pwSalt) {
			// we'd have to sha1 every user_ping to use this
			//$softNonce = sha1(ceil(time() / Config::$NONCE_LIFETIME) . Config::$SOFT_NONCE_SALT);
			$nonce = Utils::generatePassword(40);
			$authInfo = array('pw_hash' => $pwHash, 'pw_salt' => $pwSalt, 'nonce' => $nonce);
			$replaced = $mem->replace(Config::$PRE_ACTIVE_AUTH . $uid, $authInfo); 
			if(!$replaced) {
				$replaced = $mem->set(Config::$PRE_ACTIVE_AUTH . $uid, $authInfo, false, Config::$TIMEOUT_ACTIVE_AUTH); 
			}
			$time = date(Config::$DATE_FORMAT);
			$attrs = array(
				'last_activity_time' => array ('value' => $time, 'replace' => 'true'));
			$attempts = $this->SAFE_PUT_ATTEMPTS;
			$backOffTimer = $this->BACKOFF_INITIAL;
			while ($attempts > 0) {
				$put = $this->sdb->putAttributes($this->TABLE_USERS, $uid, $attrs);
				if ($put) {
					return $nonce;					
					break;
				} else {
					if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
						sleep($backOffTimer);
						$backOffTimer *= $this->BACKOFF_FACTOR;				
					} else {
						$this->error();
						break;
					}
				}
				$attempts--;
			}			
		}
		return null;
	}
	
	public function putUserOnline($uid, $lat, $long) {
		$time = date(Config::$DATE_FORMAT);
		// offset negatives
		$lat += Config::$OFFSET_LAT;
		$long += Config::$OFFSET_LONG;
		$lat = Utils::pad(round($lat, 5) * 100000, Config::$PAD_COORDS);
		$long = Utils::pad(round($long, 5) * 100000, Config::$PAD_COORDS);
		$attrs = array(
				'user_id' => array('value' => $uid), 
				'ping_time' => array('value' => $time, 'replace' => 'true'), 
				'lat' => array('value' => $lat, 'replace' => 'true'), 
				'long' => array('value' => $long, 'replace' => 'true'));
		$attempts = $this->SAFE_PUT_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		while ($attempts > 0) {
			$put = $this->sdb->putAttributes($this->TABLE_LIVE, $uid, $attrs);
			if ($put) {
				return true;
				break;
			} else {
				if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;				
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}
		return false;
	}
	
	public function checkInbox($uid) {
		global $mem, $log;
		//$log->LogInfo("checkInbox($uid)");		
		$inbox = $mem->get(Config::$PRE_INBOX . $uid);
		//$log->LogInfo("inbox count = " . count($inbox));		
		if ($inbox) {
			$shouts = array();
			foreach ($inbox as $shoutID) {
				$shout = $this->getShout($shoutID);
				if ($shout) {
					array_push($shouts, $shout->toShoutsArray());
				}
			}
			$mem->delete(Config::$PRE_INBOX . $uid);
			return $shouts;
		}
		return false;		
	}
	
	public function getShout($shoutID) {
		global $mem, $log;
		$shout = $mem->get(Config::$PRE_SHOUT . $shoutID);
		if ($shout) {
			//$log->LogInfo("shout returned from memcached = " . $shout->open);
			return $shout;
		} else {
			$shoutTableIndex = $mem->get(Config::$PRE_SHOUT_TABLE_INDEX);
			$shoutTable = $this->TABLE_SHOUT_PREFIX . $shoutTableIndex;
			$attempts = $this->SAFE_GET_ATTEMPTS;
			$backOffTimer = $this->BACKOFF_INITIAL;
			while ($attempts > 0) {
				$row = $this->sdb->getAttributes($shoutTable, $shoutID);
				if ($row) {
					//$log->LogInfo("shout pulled from db = " . $row['open']);
					$shout = new Shout($row['shout_id'], $row['user_id'], $row['time'], $row['txt'], $row['lat'], $row['long'], $row['power'], $row['hit'], $row['open'], $row['ups'], $row['downs']);
					$mem->set(Config::$PRE_SHOUT . $shoutID, $shout, false, Config::$TIMEOUT_SHOUT);
					return $shout;
					break;
				} else {
					if (count($row) == 0) {
						break;
					} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
						sleep($backOffTimer);
						$backOffTimer *= $this->BACKOFF_FACTOR;
					} else {
						$this->error();
						break;
					}
				}
				$attempts--;
			}
		}
		return null;
	}
	
	public function getScores($reqScores) {
		// TODO: we don't need to pull the entire shout from db
		// TODO: this should be capped to a limit of scores requested
		// JSON decoding done in filter now.
		//$scoreIDs = json_decode($reqScores);
		$scoreIDs = $reqScores;
		$scores = array();
		$count = 0;
		foreach ($scoreIDs as $shoutID) {
			if ($count++ > Config::$SCORE_REQUEST_LIMIT) {
				break;
			}
			$shout = $this->getShout($shoutID);
			if ($shout) {
				array_push($scores, $shout->toScoresArray());
			}
		}
		return $scores;
	}
	
	public function shout($uid, $lat, $long, $txt, $power) {
		global $mem, $log, $app;
		//$log->LogInfo("shout($uid, $lat, $long, $txt, $power)");		
		// TODO: validate power
		$shoutTime = date(Config::$DATE_FORMAT);
		$shoutID = Utils::uuid();
		$rho = $this->getPopulationDensity($lat, $long, true); // true = increment count for density
		
		e("RHO = $rho <br/>");
		$maxTargets = Config::maxTargetsAtLevel($power);
		$log->LogInfo("maxtargets = $maxTargets");
		//$radius = $maxTargets / $rho; // this is really a square
		$radius = sqrt($maxTargets / ($rho * pi()));
		e("radius = $radius");
		// http://wiki.answers.com/Q/How_many_miles_are_in_a_degree_of_longitude_or_latitude
		$degreeLatInMeters = 111132;
		$degreeLongInMeters = $degreeLatInMeters * cos(deg2rad($lat));
		$yDiff = $radius / $degreeLatInMeters;
		$xDiff = $radius / $degreeLongInMeters;
		//if ($lat < 0) { $yDiff = -$yDiff; }
		//if ($long < 0) { $xDiff = -$xDiff; }

		$lat1 = $lat - $yDiff;
		$lat2 = $lat + $yDiff;
		$long1 = $long - $xDiff;
		$long2 = $long + $xDiff;
		
		// offset negatives
		$lat1 += Config::$OFFSET_LAT;
		$lat2 += Config::$OFFSET_LAT;
		$long1 += Config::$OFFSET_LONG;
		$long2 += Config::$OFFSET_LONG;
		
		$lat1 = Utils::pad(round($lat1, 5) * 100000, Config::$PAD_COORDS);
		$lat2 = Utils::pad(round($lat2, 5) * 100000, Config::$PAD_COORDS);
		$long1 = Utils::pad(round($long1, 5) * 100000, Config::$PAD_COORDS);
		$long2 = Utils::pad(round($long2, 5) * 100000, Config::$PAD_COORDS);

		e("lat = $lat1, $lat2 || long = $long1, $long2");
		
		$putShoutIntoDB = false;
		$attempts = $this->SAFE_SELECT_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		while ($attempts > 0) {
			e("SELECT user_id FROM $this->TABLE_LIVE WHERE lat BETWEEN '$lat1' AND '$lat2' AND long BETWEEN '$long1' AND '$long2' LIMIT $maxTargets");
			$targets = $this->sdb->select($this->TABLE_LIVE, "SELECT user_id FROM $this->TABLE_LIVE WHERE lat BETWEEN '$lat1' AND '$lat2' AND long BETWEEN '$long1' AND '$long2' LIMIT $maxTargets" );
			$log->LogInfo(count($targets) . " targets");		
			if ($targets) {
				$wasSenderHit = false;				
				
				e("COUNT USERS " . count($targets));
				// offset negatives
				$lat += Config::$OFFSET_LAT;
				$long += Config::$OFFSET_LONG;
				$lat = Utils::pad(round($lat, 5) * 100000, Config::$PAD_COORDS);
				$long = Utils::pad(round($long, 5) * 100000, Config::$PAD_COORDS);
						        //($shout_id, $uid, $time, $txt, $lat, $long, $power, $hit, $open = null, $ups = null, $downs = null)
				$shout = new Shout($shoutID, $uid, $shoutTime, $txt, $lat, $long, $power, count($targets), 1, 1, 0);
				$mem->set(Config::$PRE_SHOUT . $shoutID, $shout, false, Config::$TIMEOUT_SHOUT);				
				e("SET $shoutID for $uid");
				foreach ($targets as $target) {
					$targetUID = $target['Attributes']['user_id'];
					if ($targetUID == $uid) {
						$wasSenderHit = true;
					}
					//$log->LogInfo("targets = $targetUID");		
					$inbox = $mem->get(Config::$PRE_INBOX . $targetUID);
					if (!$inbox) {
						$inbox = array();	
					}
					array_push($inbox, $shoutID);
					$replaced = $mem->replace(Config::$PRE_INBOX . $targetUID, $inbox, false, Config::$TIMEOUT_INBOX);
					if (!$replaced) {
						$replaced = $mem->set(Config::$PRE_INBOX . $targetUID, $inbox, false, Config::$TIMEOUT_INBOX);
					}				
				}
				
				if (!$wasSenderHit) {
					$targetUID = $uid;
					$inbox = $mem->get(Config::$PRE_INBOX . $targetUID);
					if (!$inbox) {
						$inbox = array();	
					}
					array_push($inbox, $shoutID);
					$replaced = $mem->replace(Config::$PRE_INBOX . $targetUID, $inbox, false, Config::$TIMEOUT_INBOX);
					if (!$replaced) {
						$replaced = $mem->set(Config::$PRE_INBOX . $targetUID, $inbox, false, Config::$TIMEOUT_INBOX);
					}							
				}
				
				$putShoutIntoDB = true;
				break;
			} else {
				if (count($targets) == 0) {
					$resp = array('code' => 'no_targets');
				 	$app->respond($resp);		
					break;
				} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}	
		if ($putShoutIntoDB) {
			$attrs = $shout->toSimpleDBAttrs();
			$shoutTableIndex = $mem->get(Config::$PRE_SHOUT_TABLE_INDEX);
			if ($shoutTableIndex == null) {
				$shoutTableIndex = $this->TABLE_SHOUT_INDEX;
				$mem->set(Config::$PRE_SHOUT_TABLE_INDEX, $shoutTableIndex, false, Config::$TIMEOUT_SHOUT_TABLE_INDEX);
			}
			$shoutTable = $this->TABLE_SHOUT_PREFIX . $shoutTableIndex;
			$domainWasFull = false;
			$attempts = $this->SAFE_PUT_ATTEMPTS;
			$backOffTimer = $this->BACKOFF_INITIAL;
			do {
				while ($attempts > 0) {
					$put = $this->sdb->putAttributes($shoutTable, $shoutID, $attrs);
					if ($put) {
						$resp = array('code' => 'shout_sent');
				 		$app->respond($resp);
						break;
					} else {
						if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
							sleep($backOffTimer);
							$backOffTimer *= $this->BACKOFF_FACTOR;				
						} else if ($this->sdb->ErrorCode == 'NumberDomainAttributesExceeded' || $this->sdb->ErrorCode == 'NumberDomainBytesExceeded') {
							$shoutTableIndex++;
							$replaced =	$mem->replace(Config::$PRE_SHOUT_TABLE_INDEX, $shoutTableIndex, false, Config::$TIMEOUT_SHOUT_TABLE_INDEX);
							if (!$replaced) {
								$replaced =	$mem->set(Config::$PRE_SHOUT_TABLE_INDEX, $shoutTableIndex, false, Config::$TIMEOUT_SHOUT_TABLE_INDEX);
							}
							$shoutTable = $this->TABLE_SHOUT_PREFIX . $shoutTableIndex;
							$this->sdb->createDomain($shoutTable);
							$attempts++;
							$domainWasFull = true;	
							sleep(3);				
						} else {
							$this->error();
							break;
						}
					}
					$attempts--;
				}
			} while ($domainWasFull);
		}
	}
	

	public function getPopulationDensity($lat, $long, $incrementCount) {
		//http://mathforum.org/library/drmath/view/63767.html
		// there's gotta be a more effecient way to do below
		$lat = $lat + 90;
		$long = $long + 180;
		$y10Seconds = $lat * 60 * 6;
		$x10Seconds = $long * 60 * 6;
		$cellY = floor($y10Seconds);
		$cellX = floor($x10Seconds);
		//$lat1 = ($cellY / 360) - 90;
		//$lat2 = (($cellY + 1) / 360) - 90;
		//$long1 = ($cellX / 360) - 180;
		//$long2 = (($cellX + 1) / 360) - 180;
		//$area = (pi() / 180) * pow($this->EARTHS_RADIUS, 2);
    	//$area *= abs(sin(deg2rad($lat1)) - sin(deg2rad($lat2))) * abs($long1 - $long2);
    	//echo "lats = $lat1 , $lat2 <br/>";
    	//echo "longs = $long1 , $long2 <br/>";
    	//echo "area = $area <br/>"; 	
		//echo "cell x = $cellX, cellY = $cellY <br/>";
						
		// GRID WRAPPING
		// X must be between 0 & 129,599
		if ($cellX == $this->DENSITY_GRID_X_GRANULARITY) {
			$cellX = 0;
		}
		// Y must be between 0 & 64,799
		if ($cellY == $this->DENSITY_GRID_Y_GRANULARITY) {
			$cellY = 0;
		}		
		$rhoKeyX = floor($cellX / $this->RHO_HASH_WIDTH_IN_CELLS);
		$rhoKeyY = floor($cellY / $this->RHO_HASH_HEIGHT_IN_CELLS);
		e("rhokeyx = $rhoKeyX, rhokeyy = $rhoKeyY");
		$tableName = $this->TABLE_RHO_PREFIX . $rhoKeyX . '_' . $rhoKeyY;
		$itemName =  $cellX . '_' . $cellY;
		e("CELL KEY = $itemName");
		$count = 0;
		$expected = null;
		$rho = Config::$DEFAULT_POPULATION_DENSITY;
		$attempts = $this->SAFE_GET_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		while ($attempts > 0) {
			// consistent read
			$row = $this->sdb->getAttributes($tableName, $itemName, null, true);
			e("ROW COUNT " . count($row));		
			if ($row) {
				$count = $row['count'];
				if (key_exists('density', $row)) {
					$rho = $row['density'];
				}
				$expected = array('count' => array('value' => $count));
				break;
			} else {
				if (count($row) == 0) {
					break;	
				} else if ($this->sdb->ErrorCode == 'NoSuchDomain') {
					$this->sdb->createDomain($tableName);
					$attempts++;	
					sleep(3);
					break;
				} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}
		
		if ($incrementCount) {		
			// TODO: can count get reset by random errors?  this is risky
			$count++;
			$attrs = array(
				'count' => array('value' => $count, 'replace' => 'true'));
			$attempts = $this->SAFE_PUT_ATTEMPTS;
			$backOffTimer = $this->BACKOFF_INITIAL;
			while ($attempts > 0) {
				$put = $this->sdb->putAttributes($tableName, $itemName, $attrs, $expected);
				if ($put) {
					e("COUNT = $count");
					return $rho;	
					break;
				} else {
					if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
						sleep($backOffTimer);
						$backOffTimer *= $this->BACKOFF_FACTOR;				
					} else {
						$this->error();
						break;
					}
				}
				$attempts--;
			}
		} else {
			return $rho;
		}
		
		return null;
	}
	
	public function vote($uid, $shoutID, $vote) {
		// TODO: we should check isOpen FIRST... and get ups/downs in the process
		// TODO: give voter points?
		global $mem, $log;
		$shout = $this->getShout($shoutID);
		// does shout exist?
		if ($shout) {
			// is shout open?
			if ($shout->open == 1) {
				$diff = time() - strtotime($shout->time);
				e("shout is $diff seconds old");
				$shoutTableIndex = $mem->get(Config::$PRE_SHOUT_TABLE_INDEX);
				$shoutTable = $this->TABLE_SHOUT_PREFIX . $shoutTableIndex;
				// should shout be closed? (too old)
				if ($diff < Config::$VOTING_WINDOW) {
					$illegalVote = false;
					$itemKey = $uid . $shoutID;
					$attempts = $this->SAFE_GET_ATTEMPTS;
					$backOffTimer = $this->BACKOFF_INITIAL;
					// did user vote on this shout?
					while ($attempts > 0) {
						$row = $this->sdb->getAttributes($this->TABLE_VOTES, $itemKey, 'v');
						if ($row) {
							$illegalVote = true;
							break;
						} else {
							if (count($row) == 0) {
								break;
							} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
								sleep($backOffTimer);
								$backOffTimer *= $this->BACKOFF_FACTOR;
							} else {
								$this->error();
								break;
							}
						}
						$attempts--;
					}
					if (!$illegalVote) {
						$attr = 'ups';
						if ($vote == -1) {
							$attr = 'downs';
						}
						$count = $shout->$attr;
						$expected = array($attr => array('value' => $count));
						$count++;
						$attrs = array(
							$attr => array('value' => $count, 'replace' => 'true'));
						$attempts = $this->SAFE_PUT_ATTEMPTS;
						$backOffTimer = $this->BACKOFF_INITIAL;
						// update shout table
						while ($attempts > 0) {
							$put = $this->sdb->putAttributes($shoutTable, $shoutID, $attrs, $expected);
							if ($put) {
								if ($vote == -1) {
									$shout->downs = $count;
								} else {
									$shout->ups = $count;
								}
								// update memcache
								$replaced = $mem->replace(Config::$PRE_SHOUT . $shoutID, $shout, false, Config::$TIMEOUT_SHOUT);
								if (!$replaced) {
									$replaced = $mem->set(Config::$PRE_SHOUT . $shoutID, $shout, false, Config::$TIMEOUT_SHOUT);
								}
								break;
							} else {
								if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
									sleep($backOffTimer);
									$backOffTimer *= $this->BACKOFF_FACTOR;				
								} else {
									$this->error();
									break;
								}
							}
							$attempts--;
						}
						$attrs = array('v' => array('value' => 1));
						$attempts = $this->SAFE_PUT_ATTEMPTS;
						$backOffTimer = $this->BACKOFF_INITIAL;
						// update votes table
						while ($attempts > 0) {
							$put = $this->sdb->putAttributes($this->TABLE_VOTES, $itemKey, $attrs);
							if ($put) {
								//$log->LogInfo("Just voted $vote on $shoutID");
								return true;
								break;
							} else {
								if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
									sleep($backOffTimer);
									$backOffTimer *= $this->BACKOFF_FACTOR;				
								} else {
									$this->error();
									break;
								}
							}
							$attempts--;
						}							
					}
				} else {
					$shoutAsArray = array();
					array_push($shoutAsArray, $shoutID);
					$this->closeShouts($shoutAsArray, $shoutTable);
				}
			}
		}
		return false;
	}

	public function closeShouts($shoutIDs, $shoutTable) {
		global $mem, $log;
		$batch = array();
		
		// step 1) close all in memcache
		foreach($shoutIDs as $shoutID) {
			$log->LogInfo("CLOSE SHOUT $shoutID");
			$attributes['open'] = array('value' => 0, 'replace' => 'true');
			$batch[$shoutID] = array('name' => $shoutID, 'attributes' => $attributes);
			$shout = $mem->get(Config::$PRE_SHOUT . $shoutID);
			if ($shout) {
				$shout->open = 0;	
				$replaced = $mem->replace(Config::$PRE_SHOUT . $shoutID, $shout);
				if (!$replaced) {
					$replaced = $mem->set(Config::$PRE_SHOUT . $shoutID, $shout, false, Config::$TIMEOUT_SHOUT);
				}
			}
		}
		$continue = true;
		$batchChunks = array_chunk($batch, $this->SIMPLEDB_BATCH_PUT_LIMIT);
		
		// step 2) close all in db
		foreach ($batchChunks as $batchChunk) {
			$attempts = $this->SAFE_PUT_ATTEMPTS;
			$backOffTimer = $this->BACKOFF_INITIAL;
			$worked = false;
			while ($attempts > 0) {
				$put = $this->sdb->batchPutAttributes($shoutTable, $batchChunk);
				if ($put) {
					$worked = true;
					break;
				} else {
					if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
						sleep($backOffTimer);
						$backOffTimer *= $this->BACKOFF_FACTOR;				
					} else {
						$this->error();
						break;
					}
				}
				$attempts--;
			}
			if (!$worked) {
				$continue = false;
			}
		}
					
		// step 3) distribute points
		if ($continue) {
			foreach($shoutIDs as $shoutID) {
				$worked = false;	
				$shout = $this->getShout($shoutID);
				$expected = null;
				$attempts = $this->SAFE_GET_ATTEMPTS;
				$backOffTimer = $this->BACKOFF_INITIAL;
				$points = null;
				$level = null;
				while ($attempts > 0) {
					// consistent read
					$row = $this->sdb->getAttributes($this->TABLE_USERS, $shout->uid, null, true);
					if ($row) {
						$points = $row['points'];
						$level = $row['level'];
						break;
					} else {
						if (count($row) == 0) {
							break;	
						} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
							sleep($backOffTimer);
							$backOffTimer *= $this->BACKOFF_FACTOR;
						} else {
							$this->error();
							break;
						}
					}
					$attempts--;
				}

				if ($points) {
					$expected = array('points' => array('value' => $points));
					$newPoints = $points + $shout->calculatePoints();
					$newPointsPadded = Utils::pad($newPoints, Config::$PAD_USER_POINTS);
					$attrs = array(
						'points' => array('value' => $newPointsPadded, 'replace' => 'true'));
					
					// did user level up?
					$newLevel = $level + 1;
					if ($newPoints >= Config::pointsRequiredForLevel($newLevel)) {					
						$newLevelPadded = Utils::pad($newLevel, Config::$PAD_USER_LEVEL);
						$attrs = array( // overrides the above attrs
							'points' => array('value' => $newPointsPadded, 'replace' => 'true'),
							'pending_level_up' => array('value' => $newLevelPadded, 'replace' => 'true'),
							'level' => array('value' => $newLevelPadded, 'replace' => 'true'));
						$levelUpInfo = array('level' => $newLevel, 'pts' => $newPoints);
						$replaced = $mem->replace(Config::$PRE_USER_PENDING_LEVEL_UP . $shout->uid, $levelUpInfo, false, Config::$TIMEOUT_USER_PENDING_LEVEL_UP);
						if (!$replaced) {
							$replaced = $mem->set(Config::$PRE_USER_PENDING_LEVEL_UP . $shout->uid, $levelUpInfo, false, Config::$TIMEOUT_USER_PENDING_LEVEL_UP);
						}
					}
						
					$attempts = $this->SAFE_PUT_ATTEMPTS;
					$backOffTimer = $this->BACKOFF_INITIAL;
					while ($attempts > 0) {
						$put = $this->sdb->putAttributes($this->TABLE_USERS, $shout->uid, $attrs, $expected);
						if ($put) {
							$worked = true;
							break;
						} else {
							if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
								sleep($backOffTimer);
								$backOffTimer *= $this->BACKOFF_FACTOR;				
							} else {
								$this->error();
								break;
							}
						}
						$attempts--;
					}
				}				
					
				if (!$worked) { // did this iteration work?
					$continue = false;
				}
			} // end foreach shout
					
			// see if users leveled up
		}
		if ($continue) {
			return true;
		}
		return false;
	}
	
	public function clearLevelUpInfo($uid) {
		global $mem, $log;
		$zeroLevel = Utils::pad(0, Config::$PAD_USER_LEVEL);
		$attrs = array(
			'pending_level_up' => array('value' => $zeroLevel, 'replace' => 'true')
		);
		$attempts = $this->SAFE_PUT_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		while ($attempts > 0) {
			$put = $this->sdb->putAttributes($this->TABLE_USERS, $uid, $attrs);
			if ($put) {
				$worked = true;
				$mem->delete(Config::$PRE_USER_PENDING_LEVEL_UP . $uid);
				return true;
				break;
			} else {
				if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;				
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}
		return false;
	}
	
	/*
	public function closeShout($shoutID, $shoutTable) {
		global $mem;
		$attrs = array(
			'open' => array('value' => 0, 'replace' => 'true'));
		$attempts = $this->SAFE_PUT_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		$continue = false;
		while ($attempts > 0) {
			$put = $this->sdb->putAttributes($shoutTable, $shoutID, $attrs);
			if ($put) {
				// TODO: should this just be memcache?
				$shout = $mem->get(Config::$PRE_SHOUT . $shoutID);
				if ($shout) {
					$shout->open = 0;
					$mem->set(Config::$PRE_SHOUT . $shoutID, $shout, false, Config::$TIMEOUT_SHOUT);
				}
				$continue = true;
			} else {
				if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;				
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}
		
		if ($continue) {
			
		}
		
		$shout = $this->getShout($shoutID);
				
				// give user pts
				
				
				// did user level up?
		
		return false;
	}
	*/
	
	// CRON JOBS //////////////////////////////////////////////////////////////
	
	public function cron_cullLiveUsers() {
		global $log;
		$log->LogInfo("CRON CULL LIVE USERS");
		$lastAcceptableCheckInTime = date(Config::$DATE_FORMAT, time() - $this->LIVE_USERS_TIMEOUT);
		$timedOutUsers = $this->sdb->select($this->TABLE_LIVE, "SELECT user_id, ping_time FROM $this->TABLE_LIVE WHERE ping_time < '$lastAcceptableCheckInTime'");
		// perhaps it's cheaper to find people still online?  that is select online rather than delete offline?
		foreach ($timedOutUsers as $user) {
			$this->sdb->deleteAttributes($this->TABLE_LIVE, $user['Attributes']['user_id']);
		}
	}
	
	public function cron_closeShouts() {
		global $mem, $log;
		$log->LogInfo("CRON CLOSE SHOUTS");
		$result = true;
		$shoutTableIndex = $mem->get(Config::$PRE_SHOUT_TABLE_INDEX);
		if ($shoutTableIndex == null) {
			$shoutTableIndex = $this->TABLE_SHOUT_INDEX;
			$mem->set(Config::$PRE_SHOUT_TABLE_INDEX, $shoutTableIndex, false, Config::$TIMEOUT_SHOUT_TABLE_INDEX);
		}
		$shoutTable = $this->TABLE_SHOUT_PREFIX . $shoutTableIndex;
		$attempts = $this->SAFE_SELECT_ATTEMPTS;
		$backOffTimer = $this->BACKOFF_INITIAL;
		while ($attempts > 0) {
			$openShouts = $this->sdb->select($shoutTable, "SELECT shout_id, time FROM $shoutTable WHERE open = '1'");
			if ($openShouts) {
				$shoutIDsToClose = array();
				foreach ($openShouts as $shout) {
					$shoutID = $shout['Attributes']['shout_id'];
					$shoutTime = $shout['Attributes']['time'];
					$diff = time() - strtotime($shoutTime);
					// should shout be closed? (too old)
					if ($diff > Config::$VOTING_WINDOW) {
						array_push($shoutIDsToClose, $shoutID);
					}
				}
			 	if (!$this->closeShouts($shoutIDsToClose, $shoutTable)) { // if a close fails
					$result = false;
		  		}
		 		break;
			} else {
				if (count($openShouts) == 0) {
					// no open shouts right now		
					break;
				} else if ($this->sdb->ErrorCode == 'ServiceUnavailable' || $this->sdb->ErrorCode == 'InternalError') {
					sleep($backOffTimer);
					$backOffTimer *= $this->BACKOFF_FACTOR;
				} else {
					$this->error();
					break;
				}
			}
			$attempts--;
		}	
		return $result;
	}
	
	// DEV ////////////////////////////////////////////////////////////////////
	
	public function admin_getRecentShouts() {
		global $app;
		$shoutTable = $this->TABLE_SHOUT_PREFIX . $this->TABLE_SHOUT_INDEX;
		$shouts = $this->sdb->select($shoutTable, "SELECT * FROM $shoutTable WHERE time IS NOT NULL ORDER BY time LIMIT 50");
		if (count($shouts) > 0) {
			$app->respond($shouts);
		}
	}
	
	public function countLiveUsers() {
		$results = $this->sdb->select($this->TABLE_LIVE, "SELECT COUNT(*) FROM $this->TABLE_LIVE");
		$numUsers = $results[0]['Attributes']['Count'];
		echo $numUsers . " LIVE USERS!";
	}
	
	public function listDomains() {
		$a = $this->sdb->listDomains();
		foreach ($a as $domain) {
			echo $domain . '<br/>';
		}
	}
		
	public function error() {
		echo 'unknown error';
		exit;
	}
	
	public function admin_viewUsers() {
		echo '<table border="1" cellpadding="3">';
		echo '<tr><th>USERS</th></tr>';
		$users = $this->sdb->select($this->TABLE_USERS, "SELECT * FROM $this->TABLE_USERS");
		if (count($users) > 0) {
			$i = 0;
			foreach ($users as $user) {
				$arr = $user['Attributes'];
				if ($i++ == 0) {
					echo '<tr>';
					foreach ($arr as $key => $val) {
						echo '<td>' . $key . '</td>';
					}
					echo '</tr>';
				}
				echo '<tr>';
				foreach ($arr as $key => $val) {
					echo '<td>' . $val . '</td>';
				}
				$uid = $user['Attributes']['user_id'];
				echo '<td><form><input type="hidden" name="a" value="admin_delete_user"/><input type="hidden" name="uid" value="' . $uid . '"/><input type="submit" value="delete"/></form></td>';
				echo '</tr>';
			}
		}
		echo '</table><br/><br/>';
	}
	
	public function admin_viewLiveUsers() {
		echo '<table border="1" cellpadding="3">';
		echo '<tr><th>LIVE USERS</th></tr>';
		$users = $this->sdb->select($this->TABLE_USERS, "SELECT * FROM $this->TABLE_LIVE");
		if (count($users) > 0) {
			$i = 0;
			foreach ($users as $user) {
				$arr = $user['Attributes'];
				if ($i++ == 0) {
					echo '<tr>';
					foreach ($arr as $key => $val) {
						echo '<td>' . $key . '</td>';
					}
					echo '</tr>';
				}
				echo '<tr>';
				foreach ($arr as $key => $val) {
					echo '<td>' . $val . '</td>';
				}
				echo '</tr>';
			}
		}
		echo '</table><br/><br/>';	
	}
	
	public function admin_viewShouts() {	
		echo '<table border="1" cellpadding="3">';
		echo '<tr><th>SHOUTS</th></tr>';
		$shoutTable = $this->TABLE_SHOUT_PREFIX . $this->TABLE_SHOUT_INDEX;
		$shouts = $this->sdb->select($shoutTable, "SELECT * FROM $shoutTable");
		if (count($shouts) > 0) {
			$i = 0;
			foreach ($shouts as $shout) {
				$arr = $shout['Attributes'];
				if ($i++ == 0) {
					echo '<tr>';
					foreach ($arr as $key => $val) {
						echo '<td>' . $key . '</td>';
					}
					echo '</tr>';
				}
				echo '<tr>';
				foreach ($arr as $key => $val) {
					echo '<td>' . $val . '</td>';
				}
				echo '</tr>';
			}
		}
		echo '</table><br/><br/>';
	}
	
	public function admin_tableMetadata() {
		echo 'LIST DOMAINS:';
		echo '<table border="1" cellpadding="3">';
		$a = $this->sdb->listDomains();
		foreach ($a as $d) {
			echo '<tr><td>' . $d . '</td></tr>';
		}
		echo '</table><br/><br/>';
		
		echo 'SHOUTS_0';
		echo '<table border="1" cellpadding="3">';
		$arr = $this->sdb->domainMetadata('SHOUTS_0');
		foreach ($arr as $key => $val) {
			echo '<tr><td>' . $key . '</td><td>' . $val . '</td></tr>';
		}
		echo '</table><br/><br/>';
		
		echo 'USERS';
		echo '<table border="1" cellpadding="3">';
		$arr = $this->sdb->domainMetadata($this->TABLE_USERS);
		foreach ($arr as $key => $val) {
			echo '<tr><td>' . $key . '</td><td>' . $val . '</td></tr>';
		}
		echo '</table><br/><br/>';
		
		echo 'LIVE_USERS';
		echo '<table border="1" cellpadding="3">';
		$arr = $this->sdb->domainMetadata($this->TABLE_LIVE);
		foreach ($arr as $key => $val) {
			echo '<tr><td>' . $key . '</td><td>' . $val . '</td></tr>';
		}
		echo '</table>';
	}
	
	public function admin_deleteUser($uid) {
		$this->sdb->deleteAttributes($this->TABLE_USERS, $uid);
	}
	
	public function admin_resetTables() {
		$this->sdb->createDomain($this->TABLE_SHOUT_PREFIX . $this->TABLE_SHOUT_INDEX);
		//$this->sdb->deleteDomain($this->TABLE_USERS);
		//$this->sdb->deleteDomain($this->TABLE_LIVE);
		sleep(3);
		//$this->sdb->createDomain($this->TABLE_USERS);
		//$this->sdb->createDomain($this->TABLE_LIVE);
	}
	
	public function admin_cullLiveUsers() {
		$lastAcceptableCheckInTime = date(Config::$DATE_FORMAT, time() - $this->LIVE_USERS_TIMEOUT);
		$timedOutUsers = $this->sdb->select($this->TABLE_LIVE, "SELECT user_id, ping_time FROM $this->TABLE_LIVE WHERE ping_time < '$lastAcceptableCheckInTime'");
		// perhaps it's cheaper to find people still online?  that is select online rather than delete offline?
		foreach ($timedOutUsers as $user) {
			$this->sdb->deleteAttributes($this->TABLE_LIVE, $user['Attributes']['user_id']);
		}
	}
	
//	public function startCreateAccount() {
//		$userID = Utils::uuid ();
//		$response = array ('user_id' => $userID );
//		echo json_encode ( $response );
//	}
//	
//	
//	public function search() {
//		// "SELECT * FROM $this->TABLE_LIVE WHERE lat BETWEEN '0151000000' AND '0152000000' AND long BETWEEN '0055000000' AND '0056000000'");
//		$users = $this->sdb->select ( $this->TABLE_LIVE, "SELECT * FROM $this->TABLE_LIVE WHERE lat BETWEEN '0150000000' AND '0160000000' AND long BETWEEN '0050000000' AND '0060000000'" );
//		if (count ( $users ) > 0) {
//			$total = 0;
//			echo '<table border="1" cellpadding="3"><tr><th colspan="4">LIVE_USERS</th></tr><tr><th>user_id</th><th>ping_time</th><th>lat</th><th>long</th></tr>';
//			foreach ( $users as $user ) {
//				$total ++;
//				echo '<tr><td>';
//				echo $user ['Attributes'] ['user_id'];
//				echo '</td><td>';
//				echo $user ['Attributes'] ['ping_time'];
//				echo '</td><td>';
//				echo $user ['Attributes'] ['lat'];
//				echo '</td><td>';
//				echo $user ['Attributes'] ['long'];
//				echo '</td></tr>';
//			}
//			echo '</table>';
//			printf ( 'box usage = %s <br/>', $this->sdb->BoxUsage );
//			printf ( '%d users found </br>', $total );
//		}
//	}
//	
//	public function reset() {
//		//$this->sdb->deleteDomain($this->TABLE_LIVE);
//		//$this->sdb->createDomain($this->TABLE_LIVE);
//		for($i = 3; $i < 7; $i ++) {
//			for($j = 0; $j < 5; $j ++) {
//				$b = "RHO_ALPHA_" . $i . "_" . $j;
//				$this->sdb->deleteDomain ( $b );
//			}
//		}
//	}
//	
//	
//	
//	public function addDemoUsers($TTL) {
//		$batch = array ();
//		$time = date (DATE_ISO8601);
//		for($i = 0; $i < 25; $i ++) {
//			$userID = Utils::uuid ();
//			$attributes = array ('user_id' => array ('value' => $userID ), 'ping_time' => array ('value' => $time ), 'lat' => array ('value' => Utils::pad ( rand ( 0, 180000000 ), 10 ) ), 'long' => array ('value' => Utils::pad ( rand ( 0, 360000000 ), 10 ) ) );
//			echo $attributes ['long'] ['value'];
//			$batch [$userID] = array ('name' => $userID, 'attributes' => $attributes );
//		}
//		if ($this->sdb->batchPutAttributes ( $this->TABLE_LIVE, $batch )) {
//			echo ('Items created<br/>');
//			echo ('RequestId: ' . $this->sdb->RequestId . '<br/>');
//			echo ('BoxUsage: ' . $this->sdb->BoxUsage . '<br/>');
//			if ($TTL > 0) {
//				$TTL --;
//				$this->addDemoUsers ( $TTL );
//			}
//		} else {
//			echo ('Items FAILED<br/>');
//			echo ('ErrorCode: ' . $this->sdb->ErrorCode);
//		}
//	}
//	
//	
//	private function listLiveUsers($startingIndex = null, $runningTotal = 0) {
//		if ($startingIndex) {
//			$users = $this->sdb->select ( $this->TABLE_LIVE, "SELECT * FROM $this->TABLE_LIVE", $startingIndex );
//		} else {
//			$users = $this->sdb->select ( $this->TABLE_LIVE, "SELECT * FROM $this->TABLE_LIVE" );
//		}
//		if (count ( $users ) > 0) {
//			echo '<table border="1" cellpadding="3"><tr><th colspan="4">LIVE_USERS</th></tr><tr><th>user_id</th><th>ping_time</th><th>lat</th><th>long</th></tr>';
//			foreach ( $users as $user ) {
//				$runningTotal ++;
//				echo '<tr><td>';
//				echo $user ['Attributes'] ['user_id'];
//				echo '</td><td>';
//				echo $user ['Attributes'] ['ping_time'];
//				echo '</td><td>';
//				echo $user ['Attributes'] ['lat'];
//				echo '</td><td>';
//				echo $user ['Attributes'] ['long'];
//				echo '</td></tr>';
//			}
//			echo '</table>';
//			if ($this->sdb->NextToken != null) {
//				printf ( 'box usage = %s', $this->sdb->BoxUsage );
//				$this->listLiveUsers ( $this->sdb->NextToken, $runningTotal );
//			} else {
//				echo "<br/>$runningTotal TOTAL LIVE USERS</br>";
//			}
//		}
//	}
//	
//	
//		

}

?>