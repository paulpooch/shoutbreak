<?php
class Filter {
	
	public static function sanitize($post) {
		
		$clean = array();
		
		if (key_exists($post, 'a')) {
			$clean['a'] = filter_var($post['a'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);				
		}
		
		if (key_exists($post, 'uid')) {
			$clean['uid'] = filter_var($post['uid'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (key_exists($post, 'android_id')) {
			$clean['android_id'] = filter_var($post['android_id'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (key_exists($post, 'device_id')) {
			$clean['device_id'] = filter_var($post['device_id'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (key_exists($post, 'phone_num')) {
			$clean['phone_num'] = filter_var($post['phone_num'], FILTER_SANITIZE_NUMBER_INT);	
		}
		
		if (key_exists($post, 'auth')) {
			$clean['auth'] = filter_var($post['auth'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (key_exists($post, 'lat')) {
			$clean['lat'] = filter_var($post['lat'], FILTER_SANITIZE_NUMBER_FLOAT);	
		}
		
		if (key_exists($post, 'long')) {
			$clean['long'] = filter_var($post['long'], FILTER_SANITIZE_NUMBER_FLOAT);	
		}
		
		if (key_exists($post, 'scores')) {
			$clean['scores'] = filter_var_array($post['scores'], array('filter' => FILTER_SANITIZE_STRING, 'flags' => FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH));
		}
		
		if (key_exists($post, 'rho')) {
			if ($post['rho'] == 1) {
				$clean['rho'] = 1;
			}
		}
		
		if (key_exists($post, 'lvl')) {
			$clean['lvl'] = filter_var($post['lvl'], FILTER_SANITIZE_NUMBER_INT);	
		}
		
		if (key_exists($post, 'txt')) {
			$clean['txt'] = filter_var($post['txt'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (key_exists($post, 'power')) {
			$clean['power'] = filter_var($post['power'], FILTER_SANITIZE_NUMBER_INT);	
		}
		
		if (key_exists($post, 'shout_id')) {
			$clean['uid'] = filter_var($post['uid'], FILTER_SANITIZE_STRING, FILTER_FLAG_STRIP_LOW | FILTER_FLAG_STRIP_HIGH);	
		}
		
		if (key_exists($post, 'vote')) {
			if ($post['vote'] == 1 || $post['vote'] == -1) {
				$clean['vote'] = $post['vote'];
			}
		}
		
		return $clean;
	}

	public static function validate($post) {
		$clean = array();
		
		if (key_exists($post, 'a')) {
			$action = $post['a'];
			$valid_actions = array('create_account', 'user_ping', 'shout', 'vote');
			if (in_array($action, $valid_actions)) {
				$clean['a'] = $action;
			}
		}

		if (key_exists($post, 'uid')) {
			$v = $post['uid'];
			if (strlen($v) == 36) {
				$regex = '[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}';
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['uid'] = $v;
				}
			}
		}
		
		if (key_exists($post, 'android_id')) {
			$v = $post['android_id'];
			if (strlen($v) == 16) {
				$regex = '[0-9A-Fa-f]{16}';
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['android_id'] = $v;
				}
			}
		}
		
		if (key_exists($post, 'device_id')) {
			$v = $post['device_id'];
			if (strlen($v) < 65) {
				$regex = '[0-9A-Fa-f]{8, 64}'; // we'll allow 8-64 hexchars
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['device_id'] = $v;
				}
			}
		}
		
		if (key_exists($post, 'phone_num')) {
			$v = $post['phone_num'];
			if (strlen($v) == 10) {
				$v = filter_var($v, FILTER_VALIDATE_INT, array('options' => array('min_range' => 0, 'max_range' => 9999999999)));
				if ($v !== false) { // we don't want 0 seeming false
					$clean['phone_num'] = $v;
				}
			}
		}
		
		if (key_exists($post, 'auth')) {
			$v = $post['auth'];
			if (strlen($v) == 160) {
				$clean['auth'] = $v;
			}
		}
		
		if (key_exists($post, 'lat')) {
			$v = $post['lat'];
			$v = filter_var($v, FILTER_VALIDATE_FLOAT);
			if ($v !== false) {
				if ($v >= -90 && $v <= 90) {
					$clean['lat'] = $v;	
				}
			}
		}
		
		if (key_exists($post, 'long')) {
			$v = $post['long'];
			$v = filter_var($v, FILTER_VALIDATE_FLOAT);
			if ($v !== false) {
				if ($v >= -180 && $v <= 180) {
					$clean['long'] = $v;	
				}
			}
		}
		
		if (key_exists($post, 'scores')) {
			$v = $post['scores'];
			$regex = '[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}';
			$v = filter_var_array($v, array('filter' => FILTER_VALIDATE_REGEXP, 'options' => array('regexp' => $regex)));
			if ($v !== false) {
				$clean['scores'] = $v;
			}
		}
		
		if (key_exists($post, 'rho')) {
			$clean['rho'] = $post['rho']; // can only be 1 at this point
		}
		
		if (key_exists($post, 'lvl')) {
			$v = $post['lvl'];
			$v = filter_var($v, FILTER_VALIDATE_INT, array('options' => array('min_range' => 0, 'max_range' => 9999)));
			if ($v !== false) { // we don't want 0 seeming false
				$clean['lvl'] = $v;
			}
		}
		
		if (key_exists($post, 'txt')) {
			$v = $post['txt'];
			if (strlen($v) <= 256) {
				$clean['txt'] = $v;
			}
		}
		
		if (key_exists($post, 'power')) {
			$v = $post['power'];
			$v = filter_var($v, FILTER_VALIDATE_INT, array('options' => array('min_range' => 0, 'max_range' => 9999)));
			if ($v !== false) { // we don't want 0 seeming false
				$clean['power'] = $v;
			}
		}
		
		if (key_exists($post, 'shout_id')) {
			$v = $post['shout_id'];
			if (strlen($v) == 36) {
				$regex = '[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}';
				$v = filter_var($v, FILTER_VALIDATE_REGEXP, array('options' => array('regexp' => $regex)));
				if ($v !== false) {
					$clean['shout_id'] = $v;
				}
			}
		}
		
		if (key_exists($post, 'vote')) {
			$clean['rho'] = $post['rho']; // can only be 1 at this point
		}
		
		return $clean;
	}
	
}
?>