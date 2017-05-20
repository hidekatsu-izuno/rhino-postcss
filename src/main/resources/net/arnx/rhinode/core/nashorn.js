
var Java = function() {
};
Java.prototype.type = function(name) {
	var current = Packages;
	var paths = name.split(/\./g);
	for (var i = 0; i < paths.length; i++) {
		current = current[paths[i]];
	}
	return current;
};
Java.prototype.synchronized = function(obj, target) {
	if (target) {
		return 	new Packages.org.mozilla.javascript.Synchronizer(obj, target);
	}
	return 	new Packages.org.mozilla.javascript.Synchronizer(obj);
}
module.exports = new Java();