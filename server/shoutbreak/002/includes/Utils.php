<?php
///////////////////////////////////////////////////////////////////////////////
// Utils
///////////////////////////////////////////////////////////////////////////////
class Utils {
	
	public static function pad($val, $digits) {
		// should we trim if length > $digits?
		if (strlen($val) > $digits) {
			if ($val < 0) {
				// account for '-'
				$val = substr($val, 0, $digits + 1);
			} else {
				$val = substr($val, 0, $digits);
			}
		}
		return str_pad($val, $digits, '0', STR_PAD_LEFT);
	}
	
	public static function sanitizeVars() {
		$_GET = Utils::stripslashes_array($_GET);
		$_POST = Utils::stripslashes_array($_POST);
		//		$_SERVER = Utils::stripslashes_array($_SERVER);
	//		$_FILES = Utils::stripslashes_array($_FILES);
	//		$_REQUEST = Utils::stripslashes_array($_REQUEST);
	//		if (isset($_SESSION)) {
	//			$_SESSION = Utils::stripslashes_array($_SESSION);
	//		}
	//		$_ENV = Utils::stripslashes_array($_ENV);
	//		$_COOKIE = Utils::stripslashes_array($_COOKIE);
	}
	
	//http://www.ajaxray.com/blog/2008/02/06/php-uuid-generator-function/
	public static function uuid($prefix = '') {
		$chars = md5(uniqid(mt_rand(), true));
		$uuid = substr($chars, 0, 8) . '-';
		$uuid .= substr($chars, 8, 4) . '-';
		$uuid .= substr($chars, 12, 4) . '-';
		$uuid .= substr($chars, 16, 4) . '-';
		$uuid .= substr($chars, 20, 12);
		return $prefix . $uuid;
	}
	
	public static function stripslashes_array($data) {
		if (is_array($data)) {
			foreach ($data as $key => $value) {
				$data[$key] = Utils::stripslashes_array($value);
			}
			return $data;
		} else {
			return stripslashes($data);
		}
	}
	
	// length must = Config::$PASSWORD_LENGTH
	public static function generatePassword($length = 32, $level = 3) {
		$validchars[1] = "0123456789abcdfghjkmnpqrstvwxyz";
		$validchars[2] = "0123456789abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		$validchars[3] = "0123456789_!@#$%&*()-=+/abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		$password = "";
		$counter = 0;
		while ( $counter < $length ) {
			$actChar = substr($validchars[$level], mt_rand(0, strlen($validchars[$level]) - 1), 1);
			$password .= $actChar;
			$counter++;
		}
		return $password;
	}
	
// this came with some sample code, looks important
//	public static function GetSQLValueString($theValue, $theType, $theDefinedValue = "", $theNotDefinedValue = "") {
//		$theValue = get_magic_quotes_gpc () ? stripslashes ( $theValue ) : $theValue;
//		$theValue = function_exists ( "mysql_real_escape_string" ) ? mysql_real_escape_string ( $theValue ) : mysql_escape_string ( $theValue );
//		switch ($theType) {
//			case "text" :
//				$theValue = ($theValue != "") ? "'" . $theValue . "'" : "NULL";
//				break;
//			case "long" :
//			case "int" :
//				$theValue = ($theValue != "") ? intval ( $theValue ) : "NULL";
//				break;
//			case "double" :
//				$theValue = ($theValue != "") ? "'" . doubleval ( $theValue ) . "'" : "NULL";
//				break;
//			case "date" :
//				$theValue = ($theValue != "") ? "'" . $theValue . "'" : "NULL";
//				break;
//			case "defined" :
//				$theValue = ($theValue != "") ? $theDefinedValue : $theNotDefinedValue;
//				break;
//		}
//		return $theValue;
//	}

}

?>