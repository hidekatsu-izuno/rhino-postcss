var JSystem = Java.type('java.lang.System');

var Console = function() {

};
Console.prototype.log = function() {
	var message = '';
	for (var i = 0; i < arguments.length; i++) {
		if (i > 0) {
			message += ' ';
		}
		message += String(arguments[i]);
	}
	JSystem.out.println(message);
};
Console.prototype.error = function() {
	var message = '';
	for (var i = 0; i < arguments.length; i++) {
		if (i > 0) {
			message += ' ';
		}
		message += String(arguments[i]);
	}
	JSystem.err.println(message);
};

module.exports = new Console();