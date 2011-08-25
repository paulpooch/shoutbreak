<?php
require_once($_SERVER['DOCUMENT_ROOT'] . '/index.php');

$engine = new DBEngine();
if (empty($_POST['a'])) {
	$_POST = $_GET; // DEBUGGING
}
$a = empty($_POST['a']) ? null : $_POST['a'];
switch ($a) {
	case 'get_recent_shouts':
		$engine->admin_getRecentShouts();
		break;
	case 'get_users':
		$engine->admin_showUsers();
}

?>