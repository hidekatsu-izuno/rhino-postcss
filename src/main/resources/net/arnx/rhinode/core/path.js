var JFile = Java.type('java.io.File');
var JPaths = Java.type('java.nio.file.Paths');

function isString(value) {
	return (typeof value === 'string' || value instanceof String);
}

var Path = function() {
	this.sep = String(JFile.separator);
	this.delimiter = String(JFile.pathSeparator);
};
Path.prototype.basename = function(path, ext) {
	if (!isString(path)) {
		throw new Error('Type Error: path');
	}
	if (ext !== undefined && !isString(ext)) {
		throw new Error('Type Error: ext');
	}

	var basename = JPaths.get(path).getFileName().toString();
	if (ext && new RegExp(ext.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&') + '$').test(basename)) {
		basename = basename.substring(0, basename.length() - ext.length);
	}
	return String(basename);
};
Path.prototype.dirname = function(path) {
	if (!isString(path)) {
		throw new Error('Type Error: path');
	}
	return String(JPaths.get(path).resolveSibling('').toString()) || '.';
};
Path.prototype.extname = function(path) {
	var basename = this.basename(path);
	var index = basename.lastIndexOf('.');
	if (index > 1) {
		return basename.substring(index);
	}
	return '';
};
Path.prototype.isAbsolute = function(path) {
	if (!isString(path)) {
		throw new Error('Type Error: path');
	}
	return /^([\/\\]|[A-Z]:[\/\\])/i.test(path);
};
Path.prototype.normalize = function(path) {
	if (!isString(path)) {
		throw new Error('Type Error: path');
	}
	var result = String(JPaths.get(path).normalize().toString());
	if (/[\/\\]$/.test(path)) {
		result += this.sep;
	}
	return result || '.';
};
Path.prototype.resolve = function() {
	var args = [];
	for (var i = 0; i < arguments.length; i++) {
		if (!isString(arguments[i])) {
			throw new Error('Type Error: paths[' + i + ']');
		}

		if (arguments[i].length) {
			args.push(String(arguments[i]));
		}
	}
	if (!args.length) {
		args.push('.');
	}

	var path;
	for (var i = 0; i < args.length; i++) {
		var segment = JPaths.get(args[i]);
		if (!path || this.isAbsolute(args[i])) {
			path = segment.toAbsolutePath();
		} else {
			path = path.resolve(segment);
		}
	}
	return String(path.normalize().toString());
};

Path.prototype.join = function() {
	if (!arguments.length) {
		return '.'
	}
	var first;
	var paths = [];
	for (var i = 0; i < arguments.length; i++) {
		if (!isString(arguments[i])) {
			throw new Error('Type Error: path');
		}
		if (i == 0) {
			first = arguments[i];
		} else {
			paths.push(arguments[i]);
		}
	}
	var result = String(JPaths.get(first, paths).normalize().toString());
	if (/[\/\\]$/.test(arguments[arguments.length - 1])) {
		result += this.sep;
	}
	return result || '.';
};
Path.prototype.relative = function() {
	var args = [];
	for (var i = 0; i < 2; i++) {
		if (!isString(arguments[i])) {
			throw new Error('Type Error: paths[' + i + ']');
		}
		args.push(String(arguments[i]));
	}
	var from = JPaths.get(args[0] || '.');
	var to = JPaths.get(args[1] || '.');
	if (this.isAbsolute(args[1] || '.')) {
		from = from.toAbsolutePath().normalize();
		to = to.toAbsolutePath().normalize();
	}
	return String(from.relativize(to).normalize().toString());
};

module.exports = new Path();