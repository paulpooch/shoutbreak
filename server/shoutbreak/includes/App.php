<?php
///////////////////////////////////////////////////////////////////////////////
// App
///////////////////////////////////////////////////////////////////////////////
class App {
	
	function __construct() {
		global $mem;
		$mem = new Memcache();
		$mem->connect('localhost', 11211) or die('memcache failure');
	}
	
	function __destruct() {
	}
	
	public function handleRequest() {		
		global $mem, $log;
		
		$packetCount = $mem->get(Config::$PRE_DDOS_SHIELD . $_SERVER['REMOTE_ADDR']);
		if ($packetCount) {
			if ($packetCount > Config::$DDOS_SHIELD_LIMIT) {
				// temporarily ban IP
				$replaced = $mem->replace(Config::$PRE_DDOS_SHIELD . $_SERVER['REMOTE_ADDR'], ++$packetCount, false, Config::$TIMEOUT_DDOS_BAN_LENGTH);
				$log->LogWarn("BANNING IP " . $_SERVER['REMOTE_ADDR']);
				exit();
			} else {
				$replaced = $mem->replace(Config::$PRE_DDOS_SHIELD . $_SERVER['REMOTE_ADDR'], ++$packetCount, false, Config::$TIMEOUT_DDOS_SHIELD);
			}
		} else {
			$packetCount = 1;
			$mem->set(Config::$PRE_DDOS_SHIELD . $_SERVER['REMOTE_ADDR'], ++$packetCount, false, Config::$TIMEOUT_DDOS_SHIELD);
		}
		
		if (empty($_POST['a'])) {
			$_POST = $_GET; // DEBUGGING
		}
		
		$a = empty($_POST['a']) ? null : $_POST['a'];
				
		$log->LogInfo("");
		$tempLog = "";
		foreach($_POST as $var => $value) {
			$tempLog .= $var . ' : ' . $value . ', ';
		}
		$log->LogWarn("ACTION = $a\nUNFILTERED = $tempLog");
		
		$_POST = Filter::sanitize($_POST);
		$_POST = Filter::validate($_POST);
		
//		$tempLog2 = "";
//		foreach($_POST as $var => $value) {
//			$tempLog2 .= $var . ' : ' . $value . ', ';
//		}
//		$log->LogWarn("FILTERED = $tempLog2");
				
		switch ($a) {
			case null:
				break;
			
			case Config::$ACTION_CREATE_ACCOUNT:
				$uid = empty($_POST['uid']) ? null : $_POST['uid'];
				$androidID = empty($_POST['android_id']) ? null : $_POST['android_id'];
				$deviceID = empty($_POST['device_id']) ? null : $_POST['device_id'];
				$phoneNum = empty($_POST['phone_num']) ? null : $_POST['phone_num'];
				$carrier = empty($_POST['carrier']) ? null : $_POST['carrier'];
				if ($uid && $androidID && $deviceID && $phoneNum && $carrier) {			
					$hit = $mem->get(Config::$PRE_CREATE_ACCOUNT_USER_TEMP_ID . $uid);
					if ($hit) {
						$engine = new DBEngine();
						$pw = $engine->addUser($uid, $androidID, $deviceID, $phoneNum, $carrier);
						$resp = array('code' => 'create_account_1', 'pw' => $pw);
						$this->respond($resp);
					}
				} else {
					$uid = Utils::uuid();
					$resp = array('code' => 'create_account_0', 'uid' => $uid);
					$mem->set(Config::$PRE_CREATE_ACCOUNT_USER_TEMP_ID . $uid, $uid, false, Config::$TIMEOUT_CREATE_ACCOUNT_USER_TEMP_ID);
					$this->respond($resp);
				}
				break;
			
			// TODO: if user submits lvl, remove pending level up stuff, it's been acknowledged 
			case Config::$ACTION_USER_PING:
				$uid = empty($_POST['uid']) ? null : $_POST['uid'];
				$auth = empty($_POST['auth']) ? null : $_POST['auth'];
				$lat = empty($_POST['lat']) ? null : $_POST['lat'];
				$long = empty($_POST['long']) ? null : $_POST['long'];
				$reqScores = empty($_POST['scores']) ? null : $_POST['scores'];
				$reqRho = empty($_POST['rho']) ? null : $_POST['rho'];
				$level = empty($_POST['lvl']) ? null : $_POST['lvl'];
				if ($uid && $auth && $lat && $long) {				
					if ($this->authIsValid($uid, $auth)) {
						$engine = new DBEngine();
						$engine->putUserOnline($uid, $lat, $long);
						//$resp = array('code' => 'auth worked');
						//$this->respond($resp);
						$shouts = $engine->checkInbox($uid);
						$scores = null;
						$rho = null;
						if ($reqScores) {
							$scores = $engine->getScores($reqScores);
						}
						if ($reqRho == "1") {
							$rho = $engine->getPopulationDensity($lat, $long, false); // false = do not increment count for density
						}
						$levelUpInfo = $mem->get(Config::$PRE_USER_PENDING_LEVEL_UP . $uid);
						if ($shouts || $scores || $rho || $levelUpInfo) {
							$resp = array('code' => 'shouts');
							if ($shouts) {
								$resp['shouts'] = $shouts;
							}
							if ($scores) {
								$resp['scores'] = $scores;
							}
							if ($rho) {
								$resp['rho'] = $rho;
							}
							if ($levelUpInfo) {
								if ($level && $level == $levelUpInfo['level']) {
									// user acknowledged level up, remove
									$engine->clearLevelUpInfo($uid);		
								} else {
									// give level up info
									$resp['level_change'] = array('lvl' => $levelUpInfo['level'], 'pts' => $levelUpInfo['pts'], 'next_lvl_at' => Config::pointsRequiredForLevel($levelUpInfo['level'] + 1));
								}
							}
							$this->respond($resp);
						}
					}
				}
				break;
				
			case Config::$ACTION_SHOUT:
				$uid = empty($_POST['uid']) ? null : $_POST['uid'];
				$auth = empty($_POST['auth']) ? null : $_POST['auth'];
				$lat = empty($_POST['lat']) ? null : $_POST['lat'];
				$long = empty($_POST['long']) ? null : $_POST['long'];
				$txt = empty($_POST['txt']) ? null : $_POST['txt'];
				$power = empty($_POST['power']) ? null : $_POST['power'];
				if ($uid && $auth && $lat && $long && $txt && $power) {
					if ($this->authIsValid($uid, $auth)) {
						$engine = new DBEngine();
						$engine->shout($uid, $lat, $long, $txt, $power);
					}
				}
				break;
		
			case Config::$ACTION_VOTE:
				$uid = empty($_POST['uid']) ? null : $_POST['uid'];
				$auth = empty($_POST['auth']) ? null : $_POST['auth'];
				$shoutID = empty($_POST['shout_id']) ? null : $_POST['shout_id'];
				$vote = empty($_POST['vote']) ? null : $_POST['vote'];
				if ($uid && $auth && $shoutID && $vote) {
					if ($this->authIsValid($uid, $auth)) {
						$engine = new DBEngine();
						$engine->vote($uid, $shoutID, $vote);	
					}
				}
				break;
				
			case Config::$ACTION_CRON_LIVE_USER_CULL:
				// TODO: secure 
				$engine = new DBEngine();
				$engine->cron_cullLiveUsers();
				break;
				
			case Config::$ACTION_CRON_CLOSE_SHOUTS:
				$engine = new DBEngine();
				$engine->cron_closeShouts();
				break;
				
			case 'admin_view_shouts':
				$engine = new DBEngine();
				$engine->admin_viewShouts();
				break;
			case 'admin_metadata':
				$engine = new DBEngine();
				$engine->admin_tableMetadata();
				break;
			case 'admin_view_users':
				$engine = new DBEngine();
				$engine->admin_viewUsers();
				break;
			case 'admin_view_live_users':
				$engine = new DBEngine();
				$engine->admin_viewLiveUsers();
				break;	
			case 'admin_delete_user':
				$engine = new DBEngine();
				$engine->admin_deleteUser($_POST['uid']);
				break;
			case 'admin_cull_live_users':
				$engine = new DBEngine();
				$engine->admin_cullLiveUsers();
				break;
			case 'admin_reset_tables':
				$engine = new DBEngine();
				$engine->admin_resetTables();
				break;
						
		}
	}
	
	public function authIsValid($uid, $auth) {		
		// REMOVE THIS!
		global $debug;
		if ($debug) { return true; }
		
		global $mem, $log;
		$validAuth = false;
		//$log->LogInfo("authIsValid($uid, $auth)");	
		$hit = $mem->get(Config::$PRE_ACTIVE_AUTH . $uid);
		// $hit = $auth_info = array('pw_hash' => $pw_hash, 'pw_salt' => $pw_salt, 'nonce' => $nonce);
		e("auth in memcache = $hit");

		if ($hit) {
			if (strlen($auth) > Config::$PASSWORD_LENGTH) {
			
				$submittedPw = substr($auth, 0, Config::$PASSWORD_LENGTH);
				$submittedHashChunk = substr($auth, Config::$PASSWORD_LENGTH, strlen($auth));
				$pwHash = $hit['pw_hash'];
				$pwSalt = $hit['pw_salt'];
				$nonce = $hit['nonce'];
				
				//$log->LogError('$pwHash = ' . $pwHash);
				//$log->LogError('$pwSalt = ' . $pwSalt);
				//$log->LogError('$nonce = ' . $nonce);
				//$log->LogError('$submittedPw = ' . $submittedPw);
				//$log->LogError('$submittedHashChunk = ' . $submittedHashChunk);
				//$log->LogError('hash(Config::$HASHING_ALGORITHM, $submittedPw . $pwSalt) = ' . hash(Config::$HASHING_ALGORITHM, $submittedPw . $pwSalt));
				//$log->LogError('hash(Config::$HASHING_ALGORITHM, $submittedPw . $nonce . $uid) = ' . hash(Config::$HASHING_ALGORITHM, $submittedPw . $nonce . $uid));
				
				// does password match?
				if (hash(Config::$HASHING_ALGORITHM, $submittedPw . $pwSalt) == $pwHash) {
					// does nonce match?
					if ($submittedHashChunk == hash(Config::$HASHING_ALGORITHM, $submittedPw . $nonce . $uid)) {
						$validAuth = true;
						return true;
					}
				}
			}
		}
		if (!$validAuth) {
			$log->LogInfo("invalid auth");	
			// expired auth
			// TODO: cap the amount of nonce challenges we'll send
			$engine = new DBEngine();
			$nonce = $engine->generateNonce($uid);
			if ($nonce) {
				$resp = array('code' => 'expired_auth', 'nonce' => $nonce);
				$log->LogInfo("expired auth. nonce = $nonce | real auth is $hit | given auth is $auth | uid is $uid");	
				$this->respond($resp);
			}
			return false;
		}
	}
	
	public function respond($r) {
		global $log;
		$log->LogWarn(json_encode($r));	
		echo json_encode($r);
		ob_flush();
	}
	
}

?>