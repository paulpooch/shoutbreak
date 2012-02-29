////////////////////////////////////////////////////////////////////////////////
// 
// LOG
//
////////////////////////////////////////////////////////////////////////////////
module.exports = (function() {

	var FileSystem = require('fs'),
		Logger = require('log'),
		Util = require('util');

	var now = new Date();
	var writeFlags = { flags: 'a' };
	var logName = now.getFullYear() + '-' + (now.getMonth() + 1) + '-' + now.getDate() + '.log';
	var logFile = new Logger('debug', FileSystem.createWriteStream('../logs/' + logName, writeFlags));
	var errorLog = new Logger('debug', FileSystem.createWriteStream('../logs/exceptions.log', writeFlags));
	var cronLog = new Logger('debug', FileSystem.createWriteStream('../logs/cron.log', writeFlags));

	this.l = function(text) {
		if (typeof(text) != 'string') {
			console.dir(text);
			logFile.debug(Util.inspect(text));
		} else {
			console.log(text);
			logFile.debug(text);
		}
	};

	this.exception = function(text) {
		if (typeof(text) != 'string') {
			console.dir(text);
			errorLog.debug(Util.inspect(text));
		} else {
			console.log(text);
			errorLog.debug(text);
		}
	};

	this.logCron = function(text) {
		if (typeof(text) != 'string') {
			console.dir(text);
			cronLog.debug(Util.inspect(text));
		} else {
			console.log(text);
			cronLog.debug(text);
		}
	};

	this.e = function(text) { 
		if (typeof(text) != 'string') {
			console.error(text);
			errorLog.debug(Util.inspect(text));
		} else {
			console.error(text);
			errorLog.debug(text);
		}
	};

	this.i = function(text) { 
		console.info(text); 
	};
	
	this.w = function(text) { 
		console.warn(text); 
	};
	
	this.obj = function(obj) { 
		console.dir(obj); 
	};

	return this;
})();