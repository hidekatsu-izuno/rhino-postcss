var JString = Java.type('java.lang.String');
var JPaths = Java.type('java.nio.file.Paths');
var JFiles = Java.type('java.nio.file.Files');

function isString(value) {
	return (typeof value === 'string' || value instanceof String);
}

var Buffer = function(buf) {
	this.buf = buf;
};
Buffer.prototype.toString = function(encoding) {
	return new JString(this.buf, encoding || 'UTF-8');
}

var FileSystem = function() {

};

FileSystem.Stats = function(path) {
	if (!JFiles.exists(path)) {
		throw new Error('ENOENT: no such file or directory, stat \'' + path + '\'');
	}
	this.path = path;
};
FileSystem.Stats.prototype.isDirectory = function() {
	return Boolean(JFiles.isDirectory(this.path));
};
FileSystem.Stats.prototype.isFile = function() {
	return !this.isDirectory();
};

FileSystem.prototype.existsSync = function(file) {
	if (!isString(file)) {
		throw new Error('Type Error: file must be string');
	}
	return JFiles.exists(JPaths.get(file));
};
FileSystem.prototype.statSync = function(file) {
	if (!isString(file)) {
		throw new Error('Type Error: file must be string');
	}
	return new FileSystem.Stats(JPaths.get(file));
};
FileSystem.prototype.readFileSync = function(file, options) {
	if (!isString(file)) {
		throw new Error('Type Error: file must be string');
	}

	var encoding, flag;
	if (isString(options)) {
		encoding = options;
	} else if (options != null) {
		encoding = options.encoding;
		flag = options.flag || 'r';
	}

	var buf = JFiles.readAllBytes(JPaths.get(file));
	if (encoding) {
		return new JString(buf, encoding);
	}
	return new Buffer(buf);
};

module.exports = new FileSystem();