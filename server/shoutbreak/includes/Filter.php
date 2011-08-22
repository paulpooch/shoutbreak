<?php
class Filter {
	
	public static function sanitize($post) {
		
		$clean = array();
		
		if (array_key_exists('a', $post)) {
			$clean['a'] = filter_var($post['a'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);				
		}
		
		if (array_key_exists('uid', $post)) {
			$clean['uid'] = filter_var($post['uid'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('android_id', $post)) {
			$clean['android_id'] = filter_var($post['android_id'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('device_id', $post)) {
			$clean['device_id'] = filter_var($post['device_id'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('carrier', $post)) {
			$clean['carrier'] = filter_var($post['carrier'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('phone_num', $post)) {
			$clean['phone_num'] = filter_var($post['phone_num'], FILTER_SANITIZE_NUMBER_INT);	
		}
		
		if (array_key_exists('auth', $post)) {
			$clean['auth'] = filter_var($post['auth'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('lat', $post)) {
			$clean['lat'] = filter_var($post['lat'], FILTER_SANITIZE_NUMBER_FLOAT, FILTER_FLAG_ALLOW_FRACTION);	
		}
		
		if (array_key_exists('long', $post)) {
			$clean['long'] = filter_var($post['long'], FILTER_SANITIZE_NUMBER_FLOAT, FILTER_FLAG_ALLOW_FRACTION);	
		}
		
		if (array_key_exists('scores', $post)) {
			$scoresArray = json_decode($post['scores']);
			$cleanScores = array();
			foreach($scoresArray as $shoutId) {
				$cleanId = filter_var($shoutId, FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);
				if ($cleanId !== false) {
					array_push($cleanScores, $cleanId);
				}
				// TODO: begin here.  also clean the validate part.  Make sure the app for reqScores if using the right datatype.
				
				
				$tempLog .= $var . ' : ' . $value . ', ';
			}
			$log->LogWarn("UNCLEAN SCORES = " . $tempLog);
			
			$scores = array('scores' => $scoresArray);
			$args = array(
				'scores' => array(
					'filter'	=> FILTER_SANITIZE_STRING, 
					'flags'		=>  FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH
				)
			);
			$clean['scores'] = filter_var_array($scores, $args);
			
			$tempLog = "";
			foreach($clean['scores'] as $var => $value) {
				$tempLog .= $var . ' : ' . $value . ', ';
			}
			$log->LogWarn("CLEAN SCORES = " . $tempLog);
			
		}
		
		if (array_key_exists('rho', $post)) {
			if ($post['rho'] == 1) {
				$clean['rho'] = 1;
			}
		}
		
		if (array_key_exists('lvl', $post)) {
			$clean['lvl'] = filter_var($post['lvl'], FILTER_SANITIZE_NUMBER_INT);	
		}
		
		if (array_key_exists('txt', $post)) {
			$clean['txt'] = filter_var($post['txt'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('power', $post)) {
			$clean['power'] = filter_var($post['power'], FILTER_SANITIZE_NUMBER_INT);	
		}
		
		if (array_key_exists('shout_id', $post)) {
			$clean['uid'] = filter_var($post['uid'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (array_key_exists('vote', $post)) {
			if ($post['vote'] == 1 || $post['vote'] == -1) {
				$clean['vote'] = $post['vote'];
			}
		}
		
		return $clean;
	}

	public static function validate($post) {
		$clean = array();
		
		if (array_key_exists('a', $post)) {
			$action = $post['a'];
			$valid_actions = array('create_account', 'user_ping', 'shout', 'vote');
			if (in_array($action, $valid_actions)) {
				$clean['a'] = $action;
			}
		}

		if (array_key_exists('uid', $post)) {
			$v = $post['uid'];
			if (strlen($v) == 36) {
				$regex = '/[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}/';
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['uid'] = $v;
				}
			}
		}
		
		if (array_key_exists('android_id', $post)) {
			$v = $post['android_id'];
			if (strlen($v) == 16) {
				$regex = '/[0-9A-Fa-f]{16}/';
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['android_id'] = $v;
				}
			}
		}
		
		if (array_key_exists('device_id', $post)) {
			$v = $post['device_id'];
			if (strlen($v) < 65) {
				$regex = '/[0-9A-Fa-f]{8,64}/'; // we'll allow 8-64 hexchars
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['device_id'] = $v;
				}
			}
		}
		
		if (array_key_exists('phone_num', $post)) {
			$v = $post['phone_num'];
			if (strlen($v) == 10) {
				$v = filter_var($v, FILTER_VALIDATE_INT);
				if ($v !== false) { // we don't want 0 seeming false
					$clean['phone_num'] = $v;
				}
			}
		}
		
		if (array_key_exists('carrier', $post)) {
			$v = $post['carrier'];
			if (strlen($v) < 100) {
				$clean['carrier'] = $v;
			}
		}
		
		if (array_key_exists('auth', $post)) {
			$v = $post['auth'];
			if (strlen($v) == 160 || strlen($v) == 7) { // 7 is the length of 'default'
				$clean['auth'] = $v;
			}
		}
		
		if (array_key_exists('lat', $post)) {
			$v = $post['lat'];
			$v = filter_var($v, FILTER_VALIDATE_FLOAT);
			if ($v !== false) {
				if ($v >= -90 && $v <= 90) {
					$clean['lat'] = $v;	
				}
			}
		}
		
		if (array_key_exists('long', $post)) {
			$v = $post['long'];
			$v = filter_var($v, FILTER_VALIDATE_FLOAT);
			if ($v !== false) {
				if ($v >= -180 && $v <= 180) {
					$clean['long'] = $v;	
				}
			}
		}
		
		if (array_key_exists('scores', $post)) {
			$v = $post['scores'];
			$regex = '/[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}/';
			$v = filter_var_array($v, array('filter' => FILTER_VALIDATE_REGEXP, 'options' => array('regexp' => $regex)));
			if ($v !== false) {
				$clean['scores'] = $v;
			}
		}
		
		if (array_key_exists('rho', $post)) {
			$clean['rho'] = $post['rho']; // can only be 1 at this point
		}
		
		if (array_key_exists('lvl', $post)) {
			$v = $post['lvl'];
			$v = filter_var($v, FILTER_VALIDATE_INT, array('options' => array('min_range' => 0, 'max_range' => 9999)));
			if ($v !== false) { // we don't want 0 seeming false
				$clean['lvl'] = $v;
			}
		}
		
		if (array_key_exists('txt', $post)) {
			$v = $post['txt'];
			if (strlen($v) <= 256) {
				$clean['txt'] = $v;
			}
		}
		
		if (array_key_exists('power', $post)) {
			$v = $post['power'];
			$v = filter_var($v, FILTER_VALIDATE_INT, array('options' => array('min_range' => 0, 'max_range' => 9999)));
			if ($v !== false) { // we don't want 0 seeming false
				$clean['power'] = $v;
			}
		}
		
		if (array_key_exists('shout_id', $post)) {
			$v = $post['shout_id'];
			if (strlen($v) == 36) {
				$regex = '/[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}/';
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['shout_id'] = $v;
				}
			}
		}
		
		if (array_key_exists('vote', $post)) {
			$clean['rho'] = $post['rho']; // can only be 1 at this point
		}
		
		return $clean;
	}
	
}
?>