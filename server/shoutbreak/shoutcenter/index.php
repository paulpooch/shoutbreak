<!DOCTYPE html>
<html>
<head>
<style type="text/css">

html { width: 100%; margin: 0px; padding: 0px; font-size: 1.0em;  font-family: "Lucida Grande", "Lucida Sans Unicode", "Lucida Sans", Verdana, Tahoma, sans-serif;  }
body { width: 100%; margin: 0px; padding: 0px; }

#header { height: 70px; background: #fff url("/shoutcenter/img/s_logo_h60.png") 4px 4px no-repeat; padding: 4px 4px 4px 100px;  }
#header a { margin-left: 20px; }
#content { background: #3c005a url("/shoutcenter/img/body_bg.png") 0px 0px repeat-x; }
#shout_table { background: #fff; width: 1400px; }
#user_table { background: #fff; width: 1400px; }

.header.row { font-family: "Courier New"; background: #8000ff; color: #fff; font-weight: 700; }
.row { font-size: .8em; background: #f2e5ff; height: 20px; margin-bottom: 1px; width: 100%; clear: both; cursor: pointer; }
.row:hover { background: #e5ccff; }
.col { float: left; width: 100px; height: 100%; overflow: hidden; margin-left: 10px; }
.code { font-family: "Courier New"; }
#detailrow { margin: 10px; display: none; }
.cssform p { width: 300px; clear: left; margin: 0; padding: 5px 0 8px 0; padding-left: 155px; border-top: 1px dashed gray; height: 1%; }
.cssform label{ float: left; margin-left: -155px; width: 150px; }
.cssform input[type="text"]{ width: 300px; }
.cssform textarea{ width: 250px; height: 150px; }

</style>


</style>
<link type="text/css" href="css/eggplant/jquery-ui-1.8.13.custom.css" rel="stylesheet" />	
<script type="text/javascript" src="js/jquery-1.5.1.min.js"></script>
<script type="text/javascript" src="js/jquery-ui-1.8.13.custom.min.js"></script>
<script type="text/javascript">

var ShoutCenter = (function() {

	var self = this;
	var shouts = new Object();
	var users = new Object();
	
	var Shout = function(jsonObj) {
		this.open = jsonObj['Attributes'].open;
		this.time = jsonObj['Attributes'].time;
		this.shout_id = jsonObj['Attributes'].shout_id;
		this.hit = jsonObj['Attributes'].hit;
		this.power = jsonObj['Attributes'].power;
		this.user_id = jsonObj['Attributes'].user_id;
		this.downs =jsonObj['Attributes'].downs;
		this.ups = jsonObj['Attributes'].ups;
		this.lat = jsonObj['Attributes'].lat;
		this.long = jsonObj['Attributes'].long;
		this.txt = jsonObj['Attributes'].txt;
		shouts[this.shout_id] = this;
	};

	var User = function(jsonObj) {
		this.user_id = jsonObj['Attributes'].user_id;
		this.user_pw_hash = jsonObj['Attributes'].user_pw_hash;
		this.user_pw_salt = jsonObj['Attributes'].user_pw_salt;
		this.android_id = jsonObj['Attributes'].android_id;
		this.device_id = jsonObj['Attributes'].device_id;
		this.phone_num = jsonObj['Attributes'].phone_num;
		this.carrier = jsonObj['Attributes'].carrier;
		this.creation_time = jsonObj['Attributes'].creation_time;
		this.last_activity_time = jsonObj['Attributes'].last_activity_time;
		this.points = jsonObj['Attributes'].points;
		this.level = jsonObj['Attributes'].level;
		this.pending_level_up = jsonObj['Attributes'].pending_level_up;
		users[this.user_id] = this;
	};
	
	var populateShoutForm = function(shout) {
		$('#shout_form #time').val(shout.time);
		$('#shout_form #shout_id').val(shout.shout_id);
		$('#shout_form #txt').val(shout.txt);
		$('#shout_form #lat').val(shout.lat);
		$('#shout_form #long').val(shout.long);
		$('#shout_form #open').val(shout.open);
		$('#shout_form #power').val(shout.power);
		$('#shout_form #user_id').val(shout.user_id);
		$('#shout_form #ups').val(shout.ups);
		$('#shout_form #downs').val(shout.downs);
		$('#shout_form #hit').val(shout.hit);
	};
	
	var populateUserForm = function(user) {
		$('#user_form #user_id').val(user.user_id);
		$('#user_form #user_pw_hash').val(user.user_pw_hash);
		$('#user_form #user_pw_salt').val(user.user_pw_salt);
		$('#user_form #android_id').val(user.android_id);
		$('#user_form #device_id').val(user.device_id);
		$('#user_form #phone_num').val(user.phone_num);
		$('#user_form #carrier').val(user.carrier);
		$('#user_form #creation_time').val(user.creation_time);
		$('#user_form #last_activity_time').val(user.last_activity_time);
		$('#user_form #points').val(user.points);
		$('#user_form #level').val(user.level);
		$('#user_form #pending_level_up').val(user.pending_level_up);
	};

	var displayShouts = function(data) {
		$('#shouts').remove();
		var items = [];
		$.each(data, function(key, val) {
			var shout = new Shout(val);
			var html = '<div class="row">' +
				'<div class="col">' + shout.time + '</div>' +
				'<div class="col shout_id">' + shout.shout_id + '</div>' +
				'<div class="col">' + shout.txt + '</div>' +
				'<div class="col">' + shout.lat + '</div>' +
				'<div class="col">' + shout.long + '</div>' +
				'<div class="col">' + shout.open + '</div>' +
				'<div class="col">' + shout.power + '</div>' +
				'<div class="col">' + shout.user_id + '</div>' +
				'<div class="col">' + shout.ups + '</div>' +
				'<div class="col">' + shout.downs + '</div>' +
				'<div class="col">' + shout.hit + '</div>' +
				'</div>';
			items.push(html);
		});
		$('<div/>', {
			'id': 'shouts',
			html: items.join('')
		}).insertAfter('#shout_table .header.row');
		$('#shouts .row').click(function() {
			var shoutId = $(this).find('.shout_id').text();
			var shout = shouts[shoutId];
			populateShoutForm(shout);
			$('#shout_table .detailrow').insertAfter($(this)).show();
		});
	};
	
	var displayUsers = function(data) {
		$('#users').remove();
		var items = [];
		$.each(data, function(key, val) {
			var user = new User(val);
			var html = '<div class="row">' +
				'<div class="col">' + user.creation_time + '</div>' + 
				'<div class="col">' + user.last_activity_time + '</div>' + 
				'<div class="col">' + user.level + '</div>' + 
				'<div class="col">' + user.pending_level_up + '</div>' + 
				'<div class="col">' + user.phone_num + '</div>' + 
				'<div class="col">' + user.points + '</div>' + 
				'<div class="col">' + user.android_id + '</div>' + 
				'<div class="col">' + user.carrier + '</div>' + 
				'<div class="col">' + user.device_id + '</div>' + 
				'<div class="col user_id">' + user.user_id + '</div>' + 
				'<div class="col">' + user.user_pw_hash + '</div>' + 
				'<div class="col">' + user.user_pw_salt + '</div>' + 
				'</div>';
			items.push(html);
		});
		$('<div/>', {
			'id': 'users',
			html: items.join('')
		}).insertAfter('#user_table .header.row');
		$('#users .row').click(function() {
			var userId = $(this).find('.user_id').text();
			var user = users[userId];
			populateUserForm(user);
			$('#user_table .detailrow').insertAfter($(this)).show();
		});
	};

	$(document).ready(function() {
		
		$('#recent_shouts').click(function() {
			$.getJSON('/shoutcenter/backend.php?a=get_recent_shouts', function(data) {
				displayShouts(data);				
			});
		});

		$('#all_shouts').click(function() {
			$.getJSON('/shoutcenter/backend.php?a=get_all_shouts', function(data) {
				displayShouts(data);				
			});
		});

		$('#show_users').click(function() {
			$.getJSON('/shoutcenter/backend.php?a=get_users', function(data) {
				displayUsers(data);
			});
		});

		$('#cron_close_shouts').click(function(data) {
			$.getJSON('/shoutcenter/backend.php?a=cron_close_shouts', function(data) {
				var resultCode = "";
				$.each(data, function(key, val) {
					resultCode += key + " : " + val;
				});
				alert(resultCode);
			});
		});
		
	});
	return self;
	
})();


</script>
</head>
<body>

<div id="header">
	<a href="http://app.shoutbreak.co/logs/log.txt">log</a>
	<a href="#" id="recent_shouts">recent shouts</a>
	<a href="#" id="all_shouts">all shouts (use sparingly please)</a>
	<a href="#" id="show_users">show users</a>
	<a href="#" id="cron_close_shouts">trigger close shout cron</a>
</div>

<div id="content">

	<div id="shout_table">	
		<div class="header row">
			<div class="col">time</div>
			<div class="col code">shout_id</div>
			<div class="col">txt</div>
			<div class="col">lat</div>
			<div class="col">long</div>
			<div class="col">open</div>
			<div class="col">power</div>
			<div class="col">user_id</div>
			<div class="col">ups</div>
			<div class="col">downs</div>
			<div class="col">hit</div>
		</div>
		<div class="detailrow">
			<form id="shout_form" class="cssform" action="">
				<div style="float: left;">
					<p>
						<label for="time">time</label>
						<input id="time" type="text" value="" />
					</p>
					<p>
						<label for="shout_id">shout_id</label>
						<input id="shout_id" type="text" value="" />
					</p>
					<p>
						<label for="txt">txt</label>
						<input id="txt" type="text" value="" />
					</p>
					<p>
						<label for="lat">lat</label>
						<input id="lat" type="text" value="" />
					</p>
					<p>
						<label for="long">long</label>
						<input id="long" type="text" value="" />
					</p>
					<p>
						<label for="time">open</label>
						<input id="open" type="text" value="" />
					</p>
					<p>
						<label for="power">power</label>
						<input id="power" type="text" value="" />
					</p>
					<p>
						<label for="user_id">user_id</label>
						<input id="user_id" type="text" value="" />
					</p>
					<p>
						<label for="ups">ups</label>
						<input id="ups" type="text" value="" />
					</p>
					<p>
						<label for="downs">downs</label>
						<input id="downs" type="text" value="" />
					</p>
					<p>
						<label for="hit">hit</label>
						<input id="hit" type="text" value="" />
					</p>
					<div style="margin-left: 150px;">
						<input type="submit" value="Submit" />
					</div>
				</div>
			</form>
		</div>
	</div>

	<div id="user_table">	
		<div class="header row">
			<div class="col">creation_time</div>
			<div class="col">last_activity_time</div>
			<div class="col">level</div>
			<div class="col">pending_level_up</div>
			<div class="col">phone_num</div>
			<div class="col">points</div>
			<div class="col">android_id</div>
			<div class="col">carrier</div>
			<div class="col">device_id</div>
			<div class="col code">user_id</div>
			<div class="col">user_pw_hash</div>
			<div class="col">user_pw_salt</div>
		</div>
		<div class="detailrow">
			<form id="user_form" class="cssform" action="">
				<div style="float: left;">
					<p>
						<label for="creation_time">creation_time</label>
						<input id="creation_time" type="text" value="" />
					</p>
					<p>
						<label for="last_activity_time">last_activity_time</label>
						<input id="last_activity_time" type="text" value="" />
					</p>
					<p>
						<label for="level">level</label>
						<input id="level" type="text" value="" />
					</p>
					<p>
						<label for="pending_level_up">pending_level_up</label>
						<input id="pending_level_up" type="text" value="" />
					</p>
					<p>
						<label for="phone_num">phone_num</label>
						<input id="phone_num" type="text" value="" />
					</p>
					<p>
						<label for="points">points</label>
						<input id="points" type="text" value="" />
					</p>
					<p>
						<label for="android_id">android_id</label>
						<input id="android_id" type="text" value="" />
					</p>
					<p>
						<label for="carrier">carrier</label>
						<input id="carrier" type="text" value="" />
					</p>
					<p>
						<label for="device_id">device_id</label>
						<input id="device_id" type="text" value="" />
					</p>
					<p>
						<label for="user_id">user_id</label>
						<input id="user_id" type="text" value="" />
					</p>
					<p>
						<label for="user_pw_hash">user_pw_hash</label>
						<input id="user_pw_hash" type="text" value="" />
					</p>
					<p>
						<label for="user_pw_salt">user_pw_salt</label>
						<input id="user_pw_salt" type="text" value="" />
					</p>
					<div style="margin-left: 150px;">
						<input type="submit" value="Submit" />
					</div>
				</div>
			</form>
		</div>
	</div>
	
</div>
</body>
</html>