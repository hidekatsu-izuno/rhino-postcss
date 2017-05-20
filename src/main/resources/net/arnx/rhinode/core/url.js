var JURI = Java.type('java.net.URI');

function isString(value) {
	return (typeof value === 'string' || value instanceof String);
}

var Url = function(urlString) {
	var uri = new JURI(urlString);

	this.protocol = (uri.scheme && uri.scheme.length()) ? String(uri.scheme + ':') : null;
	this.auth = (uri.rawUserInfo && uri.rawUserInfo.length()) ? String(uri.rawUserInfo) : null;
	this.hostname = (uri.host && uri.host.length()) ? String(uri.host) : null;
	this.port = (uri.port >= 0) ? String(uri.port) : null;
	this.host = this.port ? (this.hostname || '') + ':' + this.port : this.hostname;
	this.slashes = this.hostname ? true : null;
	this.hash = (uri.rawFragment && uri.rawFragment.length()) ? String('#' + uri.rawFragment) : null;
	this.query = (uri.rawQuery && uri.rawQuery.length()) ? String(uri.rawQuery) : null;
	this.pathname = (uri.rawPath && uri.rawPath.length()) ? String(uri.rawPath) : null;
	this.search = this.query ? '?' + this.query : null;
	this.path = this.query ? (this.pathname || '') + '?' + this.query : this.pathname;
	this.href = String(uri);
};
Url.parse = function(urlString) {
	if (!isString(urlString)) {
		throw new Error('Type Error: url is not string' + urlString);
	}
	return new Url(urlString);
};
Url.format = function(obj) {
	if (isString(obj)) {
		obj = Url.parse(obj);
	}
	return String(new JURI(
			obj.protocol ? obj.protocol.replace(/:$/, '') : null,
			obj.auth,
			obj.hostname,
			obj.port ? parseInt(obj.port, 10) : -1,
			obj.pathname,
			obj.query,
			obj.hash ? obj.hash.replace(/^#/, '') : null
		).toString());
};
Url.prototype.toString = function() {
	return 'Url {\n' +
		'  protocol: ' + (this.protocol != null ? '\'' + this.protocol + '\'' : 'null') + ',\n' +
		'  slashes: ' + this.slashes + ',\n' +
		'  auth: ' + (this.auth != null ? '\'' + this.auth + '\'' : 'null') + ',\n' +
		'  host: ' + (this.host != null ? '\'' + this.host + '\'' : 'null') + ',\n' +
		'  port: ' + (this.port != null ? '\'' + this.port + '\'' : 'null') + ',\n' +
		'  hostname: ' + (this.hostname != null ? '\'' + this.hostname + '\'' : 'null') + ',\n' +
		'  hash: ' + (this.hash != null ? '\'' + this.hash + '\'' : 'null') + ',\n' +
		'  search: ' + (this.search != null ? '\'' + this.search + '\'' : 'null') + ',\n' +
		'  query: ' + (this.query != null ? '\'' + this.query + '\'' : 'null') + ',\n' +
		'  pathname: ' + (this.pathname != null ? '\'' + this.pathname + '\'' : 'null') + ',\n' +
		'  path: ' + (this.path != null ? '\'' + this.path + '\'' : 'null') + ',\n' +
		'  href: ' + (this.href != null ? '\'' + this.href + '\'' : 'null') + ' }';
};

module.exports = Url;