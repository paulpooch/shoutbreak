<?php
require_once ('index.php');
?>

<html>
<head>
<style type="text/css">

body { font-family: "Lucida Sans Unicode"; }

</style>
</head>
<body>

<br/><br/>
<table border="1" cellpadding="3">
	<tr><th>PROTOCOL TOOLS</th><th><a href="http://app.shoutbreak.co/logs/log.txt">log</a></th></tr>
	<form method="post">
	<tr>
		<td>
			<b>create_account</b>
		</td>
		<td>
			a: <input type="text" name="a" value="create_account"/>
		</td>
		<td>
			uid: <input type="text" name="uid"/>
		</td>
				<td>
			android_id: <input type="text" name="android_id"/>
		</td>
				<td>
			device_id: <input type="text" name="device_id"/>
		</td>
				<td>
			phone_num: <input type="text" name="phone_num"/>
		</td>
		<td>
			<input type="submit" name="debug"/>
		</td>
	</tr>
	</form>
	<form method="post">
	<tr>
		<td>
			<b>user_ping</b>
		</td>
		<td>
			a: <input type="text" name="a" value="user_ping"/>
		</td>
		<td>
			uid: <input type="text" name="uid"/>
		</td>
				<td>
			auth: <input type="text" name="auth"/>
		</td>
				<td>
			lat: <input type="text" name="lat"/>
		</td>
				<td>
			long: <input type="text" name="long"/>
		</td>
		<td>
			scores: <input type="text" name="scores"/>
		</td>
		<td>
			<input type="submit" name="debug"/>
		</td>
	</tr>
	</form>
	<form method="post">
	<tr>
		<td>
			<b>shout</b>
		</td>
		<td>
			a: <input type="text" name="a" value="shout"/>
		</td>
		<td>
			uid: <input type="text" name="uid"/>
		</td>
		<td>
			auth: <input type="text" name="auth"/>
		</td>
		<td>
			lat: <input type="text" name="lat"/>
		</td>
		<td>
			long: <input type="text" name="long"/>
		</td>
		<td>
			txt: <input type="text" name="txt"/>
		</td>
		<td>
			power: <input type="text" name="power"/>
		</td>
		<td>
			<input type="submit" name="debug"/>
		</td>
	</tr>
	</form>
	<form method="post">
	<tr>
		<td>
			<b>vote</b>
		</td>
		<td>
			a: <input type="text" name="a" value="vote"/>
		</td>
		<td>
			uid: <input type="text" name="uid"/>
		</td>
		<td>
			auth: <input type="text" name="auth"/>
		</td>
		<td>
			shout_id: <input type="text" name="shout_id"/>
		</td>
		<td>
			vote: <input type="text" name="vote"/>
		</td>
		<td>
			<input type="submit" name="debug"/>
		</td>
	</tr>
	</form>
</table>
</form>

<br/><br/>
<table border="1" cellpadding="3">
	<tr><th>SIMPLE DB TOOLS</th></tr>
	<tr>
		<td>
			<b>shouts</b>
		</td>
		<td>
			<form action="admin.php" method="post">
			<input type="hidden" name="a" value="admin_view_shouts"/>		
			<input type="submit" name="debug" value="show shouts"/>
			</form>
		</td>
	</tr>
	<tr>
		<td>
			<b>cull live users</b>
		</td>
		<td>
			<form action="admin.php" method="post">
			<input type="hidden" name="a" value="admin_cull_live_users"/>		
			<input type="submit" name="debug" value="cull live users"/>
			</form>
		</td>
	</tr>
	<tr>
		<td>
			<b>metadata</b>
		</td>
		<td>
			<form action="admin.php" method="post">
			<input type="hidden" name="a" value="admin_metadata"/>		
			<input type="submit" name="debug" value="show table metadata"/>
			</form>
		</td>
	</tr>
	<tr>
		<td>
			<b>users</b>
		</td>
		<td>
			<form action="admin.php" method="post">
			<input type="hidden" name="a" value="admin_view_users"/>		
			<input type="submit" name="debug" value="show users"/>
			</form>

		</td>
	</tr>
	<tr>
		<td>
			<b>live users</b>
		</td>
		<td>
			<form action="admin.php" method="post">
			<input type="hidden" name="a" value="admin_view_live_users"/>		
			<input type="submit" name="debug" value="show live users"/>
			</form>

		</td>
	</tr>
		<tr>
		<td>
			<b>reset tables</b>
		</td>
		<td>
			<form action="admin.php" method="post">
			<input type="hidden" name="a" value="admin_reset_tables"/>		
			<input type="submit" name="debug" value="reset tables"/>
			</form>
		</td>
	</tr>
</table>
</form>
<br/><br/>

<?php 

$engine = new DBEngine();
if (empty($_POST['a'])) {
	$_POST = $_GET; // DEBUGGING
}
$a = empty($_POST['a']) ? null : $_POST['a'];
switch ($a) {
	case 'admin_view_shouts':
		$engine->admin_viewShouts();
		break;
	case 'admin_metadata':
		$engine->admin_tableMetadata();
		break;
	case 'admin_view_users':
		$engine->admin_viewUsers();
		break;
	case 'admin_view_live_users':
		$engine->admin_viewLiveUsers();
		break;	
	case 'admin_delete_user':
		$engine->admin_deleteUser($_POST['uid']);
		break;
	case 'admin_cull_live_users':
		$engine->admin_cullLiveUsers();
		break;
	case 'admin_reset_tables':
		$engine->admin_resetTables();
		break;
}
?>

</body>
</html>
