var JSystem = Java.type('java.lang.System');

var Process = function() {
	this.env = {};

	var osname = JSystem.getProperty('os.name');
	this.platform = /windows/i.test(osname) ? 'win32' :
		/mac/i.test(osname) ? 'darwin' :
		/freebsd/i.test(osname) ? 'freebsd' :
		/sunos/i.test(osname) ? 'sunos' :
		'linux';
};

module.exports = new Process();