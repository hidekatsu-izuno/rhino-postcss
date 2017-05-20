package net.arnx.rhinode.core;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

public class RhinodeEngine implements AutoCloseable {
  private Context cx;
  private Scriptable global;

  public RhinodeEngine(ClassLoader cl, String root) {
    cx = Context.enter();
    cx.setOptimizationLevel(9);
    global = cx.initStandardObjects(null, true);

    Scriptable module = cx.newObject(global);
    Scriptable exports = cx.newObject(global);

    Module created = new Module(cx, global, cl, root, null, "<main>", module, exports, null, null);
    created.setLoaded();

    ScriptableObject.putProperty(global, "require", created);
    ScriptableObject.putProperty(global, "module", module);
    ScriptableObject.putProperty(global, "exports", exports);

    init("var Java = require('classpath:net/arnx/rhinode/core/nashorn.js')");
    init("var console = require('classpath:net/arnx/rhinode/core/console.js')");
    init("var process = require('classpath:net/arnx/rhinode/core/process.js')");
    init("var Promise = require('classpath:net/arnx/rhinode/core/promise.js')");
  }

  public void init(String script) {
    cx.evaluateString(global, script, "<init>", 0, null);
  }

  public Map<String, Object> run(Reader reader, String filename, Map<String, Object> params, Map<String, Class<?>> resultTypes) {
    ScriptExecutor se = new ScriptExecutor(params, resultTypes);
    se.execute((cx, scope) -> {
      try {
        return cx.evaluateReader(scope, reader, filename, 0, null);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
    return se.result;
  }

  public Object eval(String script) {
    ScriptExecutor se = new ScriptExecutor(Collections.emptyMap(), Collections.emptyMap());
    se.execute((cx, scope) -> {
      return cx.evaluateString(scope, script, "<eval>", 0, null);
    });
    return se.ret;
  }

  @Override
  public void close() {
    Context.exit();
  }

  private class ScriptExecutor {
    Map<String, Object> params;
    Map<String, Class<?>> resultTypes;
    Map<String, Object> result;
    Object ret;

    public ScriptExecutor(Map<String, Object> params, Map<String, Class<?>> resultTypes) {
      this.params = params;
      this.resultTypes = resultTypes;
    }

    public void execute(BiFunction<Context, Scriptable, Object> fn) {
      Context cx2 = Context.enter();
      try {
        cx2.setOptimizationLevel(9);
        Scriptable scope = cx2.newObject(global);
        scope.setPrototype(global);
        scope.setParentScope(null);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
          ScriptableObject.putProperty(scope, entry.getKey(), Context.javaToJS(entry.getValue(), scope));
        }

        ExecutorService pool = ThreadPoolManager.get();

        ret = fn.apply(cx2, scope);

        ThreadPoolManager.clear();
        pool.shutdown();
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
          throw new IllegalStateException("timeout for execution promises");
        }
        result = new LinkedHashMap<>();
        for (Map.Entry<String, Class<?>> entry : resultTypes.entrySet()) {
          Object value =  ScriptableObject.getProperty(scope, entry.getKey());

          if (value != null && value != UniqueTag.NOT_FOUND && !(value instanceof Undefined)) {
            result.put(entry.getKey(), Context.jsToJava(value, entry.getValue()));
          }
        }
      } catch (InterruptedException e) {
        throw new IllegalStateException("cancel for execution promises", e);
      } finally {
        Context.exit();
      }
    }
  }
}
