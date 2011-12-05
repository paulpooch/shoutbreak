<?php
/*
 * TODO
 * 
 * what are our assumptions when using memcache?  are we writing to it every time we touch db?  we better.
 * 
 * do not use memcache for logic
 * never rely on memcache existing, pull if it doesn't
 * all methods should return stuff (t/f?)
 * memcached synced with db?
 * concurrency issues?  read memcache RIGHT before writing?  writing stale data / overwrites?
 * 
 * make sure you always keep TABLE_SHOUT_INDEX up to date
 * 
 */

$version = '004';
set_include_path('/home/webuser/shoutbreak/htdocs/' . $version . '/includes/');
require_once("SimpleDB.php");
require_once("KLogger.php");
require_once("Shout.php");
require_once("Config.php");
require_once("App.php");
require_once("DBEngine.php");
require_once("Utils.php");
require_once("Filter.php");

error_reporting(E_ALL | E_STRICT);
ini_set('display_errors', 1);
date_default_timezone_set('UTC');
Utils::sanitizeVars();

$debug = empty($_POST['debug']) ? false : true;
function e($s) {
	global $debug;
	if ($debug) {
		echo 'DEBUG: ' . $s . '<br/>';
	}
}

$mem = null;
$log = new KLogger($_SERVER['DOCUMENT_ROOT'] . '/logs/log.txt', KLogger::DEBUG);
$app = new App();
$app->handleRequest();
?>