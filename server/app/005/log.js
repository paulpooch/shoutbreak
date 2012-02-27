////////////////////////////////////////////////////////////////////////////////
// 
// LOG
//
////////////////////////////////////////////////////////////////////////////////
module.exports = (function() {

	var FileSystem = require('fs'),
		Logger = require('log'),
		Util = require('util');

	var logFile = false;

	this.init = function(logName) {
		logFile = new Logger('debug', FileSystem.createWriteStream('../logs/' + logName ));
		this.l('Log initiated.');
		// Not Utils, native util.
	};

	this.e = function(text) { 
		console.error(text);
	};

	this.i = function(text) { 
		console.info(text); 
	};
	
	this.l = function(text) {
		if (logFile === false) {
			logFile = new Logger('debug', FileSystem.createWriteStream('../logs/log.txt'));
		}
		if (typeof(text) != 'string') {
			console.dir(text);
			logFile.debug(Util.inspect(text));
		} else {
			console.log(text);
			logFile.debug(text);
		}
	};
	
	this.w = function(text) { 
		console.warn(text); 
	};
	
	this.obj = function(obj) { 
		console.dir(obj); 
	};

	return this;
})();