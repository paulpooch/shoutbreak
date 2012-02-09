////////////////////////////////////////////////////////////////////////////////
// 
// LOG
//
////////////////////////////////////////////////////////////////////////////////
module.exports = (function() {

	this.e = 	function(text) { console.error(text); };
	this.i = 	function(text) { console.info(text); };
	this.l = 	function(text) { console.log(text); };
	this.w = 	function(text) { console.warn(text); };
	this.obj = 	function(obj) { console.dir(obj); };

	return this;
})();