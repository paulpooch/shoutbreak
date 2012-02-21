////////////////////////////////////////////////////////////////////////////////
// 
// UTILS
//
////////////////////////////////////////////////////////////////////////////////
module.exports = (function() {
	var self = this;

	// Includes ////////////////////////////////////////////////////////////////
	var Crypto =	require('crypto'),
		Config =	require('./config'),
		Shout = 	require('./shout'),
		Log = 		require('./log');

	this.generatePassword = function(pLength, pLevel) {
		var length = (typeof pLength == 'undefined') ? 32 : pLength;
		var level = (typeof pLevel == 'undefined') ? 3 : pLevel;
		var validChars = [
			'', // Level 0 undefined. 
			'0123456789abcdfghjkmnpqrstvwxyz',
			'0123456789abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ',
			'0123456789_!@#$%*()-=+/abcdfghjkmnpqrstvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
		];
		var password = '';
		var counter = 0;
		while (counter < length) {
			var rand = Math.round(Math.random() * (validChars[level].length - 1));
			var oneChar = validChars[level].substr(rand, 1);
			password += oneChar;
			counter++;
		}
		return password;
	};

	this.hashSha512 = function(text) {
		return Crypto.createHash('sha512').update(text).digest('base64');
	};

	this.sleep = function(seconds, callback) {
		var startTime = new Date().getTime();
		while (new Date().getTime() < startTime + (seconds * 1000));
		callback();
  	};

  	this.getNowISO = function() {
  		return (new Date().toISOString());
  	};

  	this.formatLatForSimpleDB = function(lat) {
  		Log.l('formatLatForSimpleDB : ' + lat + ' => ');
  		lat += Config.OFFSET_LAT;
  		Log.l(self.pad(lat * Config.MULTIPLY_COORDS, Config.PAD_COORDS));
  		return self.pad(lat * Config.MULTIPLY_COORDS, Config.PAD_COORDS);
  	};

	this.formatLngForSimpleDB = function(lng) {
  		lng += Config.OFFSET_LNG;
  		return self.pad(lng * Config.MULTIPLY_COORDS, Config.PAD_COORDS);
  	};

  	this.pad = function(val, digits) {
  		val = Math.round(val);
  		if (val.length > digits) {
  			if (val < 0) {
  				digits++;
  			}
  		}
 		while (val.length < digits) {
 			val = '0' + val;
 		}
 		return String(val);
  	};

 	if (typeof(Number.prototype.toRad) === 'undefined') {
  		Number.prototype.toRad = function() {
    		return this * Math.PI / 180;
  		};
	}

	// http://stackoverflow.com/questions/27928/how-do-i-calculate-distance-between-two-latitude-longitude-points
  	this.distanceBetween = function(lat1, lng1, lat2, lng2) {
  		Log.l('distanceBetween (' + lat1 + ', ' + lng1 + ') - (' + lat2 + ', ' + lng2 + ' ) = ');
  		var R = 6371; // Radius of the earth in km
		var dLat = (lat2 - lat1).toRad();  // Javascript functions in radians
		var dLon = (lng2 - lng1).toRad(); 
		var a = Math.sin(dLat / 2) * Math.sin( dLat / 2) +
			Math.cos(lat1.toRad()) * Math.cos(lat2.toRad()) * 
        	Math.sin(dLon / 2) * Math.sin(dLon / 2); 
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); 
		var d = R * c; // Distance in km
		Log.l(d);
		return d;
  	};

  	// Adapter from simpledb.js
  	this.simpleDBLogger = function(date, type) {
  		var strs = ['simpledb: ', date.toISOString(), type];
  		for (var aI = 2; aI < arguments.length; aI++) {
  			var a = arguments[aI];
  			strs.push('object' == typeof(a) ? JSON.stringify(a) : '' + a);
  		}
  		//util.debug(strs.join(' '));
  		//Log.i(strs.join(' '));
  	};

  	this.buildShoutJson = function(shout, forUserId) {
		var sObj = {
			'shout_id': shout.shoutId,
			'txt': shout.text,
			'ts': shout.time,
			'hit': shout.hit
		};
		if (forUserId == shout.userId) {
			sObj['outbox'] = 1;
		}
		return sObj;
	};

	this.buildScoreJson = function(shout) {
		var sObj = {
			'shout_id': shout.shoutId,
			'ups': shout.ups,
			'downs': shout.downs,
			'hit': shout.hit,
			'open': shout.open
		};
		return sObj;
	};
	
	this.makeShoutFromDynamoItem = function(item) {
		var shout = new Shout();
		shout.shoutId = item['shout_id']['S'];
		shout.userId = item['user_id']['S'];
		shout.time = item['time']['S'];
		if (item['last_activity_time']) {
			shout.lastActivityTime = item['last_activity_time']['S'];
		}
		shout.text = item['text']['S'];
		shout.open = parseInt(item['open']['N']);
		shout.re = item['re']['S'];
		shout.hit = parseInt(item['hit']['N']);
		shout.power = parseInt(item['power']['N']);
		shout.ups = parseInt(item['ups']['N']);
		shout.downs = parseInt(item['downs']['N']);
		shout.lat = parseInt(item['lat']['N']);
		shout.lng = parseInt(item['lng']['N']);
		return shout;
	};

	this.maxTargetsAtLevel = function(level) {
		return Math.min((level), Config.SIMPLEDB_MAX_NUMBER_OF_ITEMS)
	};

	this.reachAtLevel = function(level) {
		return self.maxTargetsAtLevel(level);
	};

	this.workRequiredForLevel = function(level) {
		if (level == 1) {
			return Config.SHOUTBREAK_SCORING_WORK_AT_LEVEL_1;
		} else {
			return Config.SHOUTBREAK_SCORING_COEFFECIENT * self.workRequiredForLevel(level - 1);
		}
	};

	this.pointsRequiredForLevel = function(level) {
		var result = Math.round(self.workRequiredForLevel(level) * self.reachAtLevel(level));
		return result;	
	};

	this.pointsForVote = function(level) {
		return 1;
	};

	return this;
})();