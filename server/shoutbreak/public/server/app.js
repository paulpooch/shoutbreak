/* Node.js Shout Map Server */

var PORT		= 8080,
 	AWS_KEY_ID	= 'AKIAINHDEIZ3QVSHQ3PA',
 	AWS_SECRET_KEY	= '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',
 	TABLE_SHOUTS	= 'SHOUTS_0',
	REFRESH_RATE	= 5 * 1000,
	BACKOFF_FACTOR	= 1.2,
	EMIT_MAX	= 500;
	REFRESH_MAX	= 5 * 60 * 1000;
	MONTHS		= ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

var	_io	= require('socket.io');
var _simpledb = require('simpledb');

var _rate = REFRESH_RATE;
var _conn = null;
var _sdb = null;
var _shouts = [];
var _latest = 0;

var _clients = new function() {

	var self = this;
	var clients = [];

	this.add = function(socket) {
		var client = { "latest": 0, "socket": socket };
		clients.push(client);
		self.updateClient(client);
		console.log("stored client " + socket.id + ", currently " + clients.length + " clients connected");
	}

	this.remove = function(socket) {
		var clientIndex = self.getClientIndex(socket);
		clients.splice(clientIndex, 1);
		console.log("removed client " + socket.id + ", currently " + clients.length + " clients connected");
	}

	this.updateClient = function(client) {
		if (_shouts.length > 0) {
			var newShouts = [];
			var socket = client.socket;
			var newShout;
			var start = _shouts.length - EMIT_MAX  > 0 ? _shouts.length - EMIT_MAX : 0;
			var i = (client.latest == 0) ? start : _getShoutIndexByTime(client.latest) + 1;
			for (; i < _shouts.length; i++) {
				newShouts.push(_shouts[i]);
				newShout = newShouts[newShouts.length -1];
				// remove excess info
				delete newShout.$ItemName;
				//delete newShout.lat;
				//delete newShout.long;
				delete newShout.downs;
				delete newShout.hit;
				delete newShout.open;
				delete newShout.power;
				//delete newShout.shout_id;
				//delete newShout.time;
				//delete newShout.date;
				//delete newShout.txt;
				delete newShout.ups;
				delete newShout.user_id;
			}
			if (newShouts.length > 0) {
				socket.emit("update", newShouts);
			}
			client.latest = _latest;
		}
	}

	this.updateAll = function() {
		console.log("updating all clients...");
		for (var i = 0; i < clients.length; i++) {
			self.updateClient(clients[i]);
		}
		console.log("done updating clients");
	}

	this.getClientIndex = function(socket) {
		for (var i = 0; i < clients.length; i++) {
			var client = clients[i];
			if (client.socket.id == socket.id) {
				return i;
			}
		}
		return -1;
	};
};

var _init = (function() {

	var uid = 0;

	console.log("\ninitializing server...");
	
	// connect to simpledb
	_sdb = new _simpledb.SimpleDB({'keyid': AWS_KEY_ID, 'secret': AWS_SECRET_KEY});

	// check for new shouts
	loop = function() {
		console.log("refresh rate set to " + _rate + "ms");
		_pullShouts(_clients.updateAll);
		console.log(_shouts.length + " shouts stored in memory");
		setTimeout(loop, _rate);
	};
	setTimeout(loop, _rate);

	// listen for connections
	_conn = _io.listen(PORT);
	console.log("listening on port " + PORT + " for new connections");

	// handle connections
	_conn.sockets.on('connection', function(socket) {

		console.log("client connected");

		_clients.add(socket);

		socket.on('disconnect', function() {
			console.log("client disconnected");
			var socket = this;
			_clients.remove(socket);
		});

	});

})();


var _getShoutIndexByTime = function(time) {
	for (var i = 0; i < _shouts.length, time != -1; i++) {
		var shout = _shouts[i];
		if (shout.time == time) {
			return i;
		}
	}
	return -1;
}

var _pullShouts = function(callback) {

	console.log("pulling latest shouts from database...");

	var isFirstPass = true;
	var query = "SELECT * FROM " + TABLE_SHOUTS + " WHERE time > '" + _latest + "' ORDER BY time ASC";

	var tokenizedQuery = function(result, token, callback) {
		var date;
		var hour;
		var minutes;
		var apm;
		var month;
		var day;
		if (result != null) {
			for (var i = 0; i < result.length; i++) {
				// clean up and push new shouts
				date = new Date(result[i].time);
				hour = date.getHours();
				if (hour > 12) hour -= 12;
				if (hour == 0) hour = 12;
				minutes = date.getMinutes();
				if (minutes < 10) minutes = "0" + minutes;
				apm = date.getHours() < 12 ? "am" : "pm";
				month = MONTHS[date.getMonth()];
				day = date.getDate();
				result[i].lat = (parseFloat(result[i].lat) / 100000) - 90;
				result[i].lng = (parseFloat(result[i].long) / 100000) - 180;
				delete result[i].long;
				result[i].date = month + " " + day + " at " + hour + ":" + minutes + apm; 
				_shouts.push(result[i]);
			}
			result = null;
			_rate = REFRESH_RATE;
		} else {
			if (_rate > REFRESH_MAX) {
				_rate = REFRESH_MAX;
			} else {
				_rate = Math.round(_rate * BACKOFF_FACTOR);
			}
		}

		if (token != null) {
			_sdb.select(query, token, function(error, result, meta) {
				var token = meta.result.SelectResult.NextToken ? { NextToken: meta.result.SelectResult.NextToken } : null;
				tokenizedQuery(result, token, callback);
			});
		} else if (isFirstPass) {
			_sdb.select(query, function(error, result, meta) {
				var token = meta.result.SelectResult.NextToken ? { NextToken: meta.result.SelectResult.NextToken } : null;
				if (result.length > 0 && result[result.length - 1].time != _latest) {
					tokenizedQuery(result, token, callback);
				} else {
					console.log("shouts up to date!");
				}
			});
			isFirstPass = false;
		} else {
			console.log("done pulling from database");
			_latest = _shouts[_shouts.length - 1].time;
			callback();
		}
	};

	tokenizedQuery(null, null, callback);
	
};
