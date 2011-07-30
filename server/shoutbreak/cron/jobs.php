<?php
set_include_path('/home/webuser/shoutbreak/htdocs/includes/');
require_once("SimpleDB.php");
require_once("KLogger.php");
require_once("Shout.php");
require_once("Config.php");
require_once("App.php");
require_once("DBEngine.php");
require_once("Utils.php");
$job = 0;
$argv = $_SERVER['argv'];
//$log = new KLogger("logs/log.txt", KLogger::DEBUG);

if (count($argv) > 0 && isset($argv[1])) {
	$mem = new Memcache();
	$mem->connect('localhost', 11211) or die('memcache failure');
	//$log = new KLogger("/home/webuser/shoutbreak/htdocs/logs/log.txt", KLogger::DEBUG);
	$engine = new DBEngine();
	$job = $argv[1];
	if ($job == "cull_live_users") {
		$engine->cron_cullLiveUsers();
	} else if ($job == "close_shouts") {
		$engine->cron_closeShouts();
	}
}

$crontext = "$job @ ".date("r")." by ".$_SERVER['USER']."\r\n" ;
$folder = substr($_SERVER['SCRIPT_FILENAME'],0,strrpos($_SERVER['SCRIPT_FILENAME'],"/")+1);
$folder .= "cronlogs/";
$filename = $folder."cronlog.txt";
$fp = fopen($filename,"a") or die("Open error!");
fwrite($fp, $crontext) or die("Write error!");
fclose($fp);
?>