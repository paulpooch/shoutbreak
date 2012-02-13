////////////////////////////////////////////////////////////////////////////////
// 
// LOG
//
////////////////////////////////////////////////////////////////////////////////
module.exports = (function() {

	var FileSystem = require('fs'),
		Logger = require('log'),
		logFile = new Logger('debug', FileSystem.createWriteStream('../logs/log.txt')),
		// Not Utils, native util.
		Util = require('util');

	this.e = function(text) { 
		console.error(text);
	};

	this.i = function(text) { 
		console.info(text); 
	};
	
	this.l = function(text) {
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

	this.l('Log initiated.');

	return this;
})();