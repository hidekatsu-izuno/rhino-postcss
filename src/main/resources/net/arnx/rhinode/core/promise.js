var JThreadPoolManager = Java.type('net.arnx.rhinode.core.ThreadPoolManager');
var JCompletableFuture = Java.type('java.util.concurrent.CompletableFuture');
var JPromiseException = Java.type('net.arnx.rhinode.core.PromiseException');
var JCompletionException = Java.type('java.util.concurrent.CompletionException');

var Promise = function(resolver, futures) {
	var that = this;
	if (resolver instanceof JCompletableFuture) {
		that._future = resolver;
		that._futures = futures;
	} else {
		var global = Function('return this')();
		var executor = function() {
			var status, result;
			(0, Java.synchronized(resolver, global))(function(value) {
				status = 'fulfilled';
				result = value;
			}, function(reason) {
				status = 'rejected';
				result = reason;
			});
			if (status == 'fulfilled') {
				return {
					result: result
				};
			} else if (status == 'rejected') {
				throw new JPromiseException(result);
			}
		};

		that._future = JCompletableFuture.supplyAsync(executor, JThreadPoolManager.get());
	}
};

Promise.all = function(array) {
	var futures = array.map(function(elem) {
		if (elem instanceof Promise) {
			return elem._future;
		}
		return Promise.resolve(elem)._future;
	});
	return new Promise(JCompletableFuture.allOf(futures), futures);
};

Promise.race = function(array) {
	var futures = array.map(function(elem) {
		if (elem instanceof Promise) {
			return elem._future;
		}
		return Promise.resolve(elem)._future;
	});
	return new Promise(JCompletableFuture.anyOf(futures));
};

Promise.resolve = function(value) {
	if (value instanceof Promise) {
		return value;
	} else if (value != null
			&& (typeof value === 'function' || typeof value === 'object')
			&& typeof value.then === 'function') {
		return new Promise(function(fulfill, reject) {
			try {
				return {
					result: value.then(fulfill, reject)
				}
			} catch (e) {
				throw new JPromiseException(e);
			}
		});
	} else {
		return new Promise(JCompletableFuture.completedFuture({
			result: value
		}));
	}
};

Promise.reject = function(value) {
	return new Promise(function(fulfill, reject) {
		reject(value);
	});
};

Promise.prototype.then = function(onFulfillment, onRejection) {
	var that = this;
	return new Promise(that._future.handle(new java.util.function.BiFunction({
		apply: function(success, error) {
			if (success == null && error == null && that._futures != null) {
				success = {
					result: that._futures.map(function(elem) {
						return elem.get().result;
					})
				};
			}

			if (success != null) {
				if (typeof onFulfillment === 'function') {
					try {
						return {
							result: (0, onFulfillment)(success.result)
						};
					} catch (e) {
						return {
							result: (0, onRejection)(e)
						};
					}
				}
				return success;
			} else if (error != null) {
				if (error instanceof JCompletionException) {
					error = error.getCause();
				}
				var cerror = error;
				do {
					if (cerror instanceof JPromiseException) {
						break;
					}
				} while ((cerror = cerror.getCause()) != null);

				if (typeof onRejection === 'function') {
					var reason  = error;
					if (cerror instanceof JPromiseException) {
						reason = cerror.getResult();
					}

					return {
						result: (0, onRejection)(reason)
					};
				}

				throw error;
			}
		}
	})));
};

Promise.prototype.catch = function(onRejection) {
	return this.then(null, onRejection);
};

module.exports = Promise;