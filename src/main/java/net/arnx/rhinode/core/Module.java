package net.arnx.rhinode.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;

import net.arnx.rhinode.util.FileUtil;

public class Module extends ScriptableObject implements Function {
  private static final ThreadLocal<Map<String, Scriptable>> LOCAL = new ThreadLocal<Map<String, Scriptable>>() {
    protected Map<String,Scriptable> initialValue() {
      return new HashMap<>();
    }
  };

  private Context cx;
  private Scriptable global;
  private ClassLoader cl;
  private String basedir;

  private Module main;
  private Scriptable module;
  private ModuleCache cache;
  private List<Scriptable> children = new ArrayList<>();
  private Scriptable exports;

  public Module(Context cx,
      Scriptable global,
      ClassLoader cl,
      String basedir,
      ModuleCache cache,
      String filename,
      Scriptable module,
      Scriptable exports,
      Module parent,
      Module main) {

    this.cx = cx;
    this.global = global;
    this.cl = cl;
    this.basedir = basedir;

    this.main = (main != null) ? main : this;
    this.cache = (cache != null) ? cache : new ModuleCache();
    this.module = module;
    this.exports = exports;

    ScriptableObject.putProperty(this, "main", this.main.module);

    ScriptableObject.putProperty(module, "loaded", false);
    ScriptableObject.putProperty(module, "parent", (parent != null) ? parent.module : null);
    ScriptableObject.putProperty(module, "children", children);
    ScriptableObject.putProperty(module, "filename", filename);
    ScriptableObject.putProperty(module, "id", filename);
    ScriptableObject.putProperty(module, "exports", exports);
  }

  @Override
  public String getClassName() {
    return "Module";
  }

  @Override
  public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
    return null;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String path;
    if (args.length == 0 || args[0] == null || (path = Context.toString(args[0])).isEmpty()) {
      throw ScriptRuntime.constructError("Error", "Cannot find module ''");
    }

    String fullpath = FileUtil.normalize(basedir, path);
    Scriptable cachedExports = LOCAL.get().get(fullpath);
    if (cachedExports != null) {
      return cachedExports;
    } else {
      LOCAL.get().put(fullpath, cx.newObject(scope));
    }

    Module found = null;
    try {
      if (path.startsWith("/") || path.startsWith("../") || path.startsWith("./")) {
        found = findModule(basedir, path);
      } else if (path.startsWith("classpath:")) {
        found = findModule("", path.substring(10));
      } else if (path.equals("fs") || path.equals("path") || path.equals("url")) {
        found = findModule("net/arnx/rhinode/core", path);
      } else {
        String current = basedir;
        do {
          found = findModule(FileUtil.normalize(current, "node_modules"), path);
        } while (found == null && (current = FileUtil.normalize(current, "..")) != null);
      }

      if (found == null) {
        throw ScriptRuntime.constructError("Error", "Cannot find module '" + path + "'");
      }

      children.add(found.module);

      return found.exports;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      LOCAL.remove();
    }
  }

  void setLoaded() {
    ScriptableObject.putProperty(module, "loaded", true);
  }

  private Module findModule(String basedir, String path) throws IOException {
    if (path == null) {
      return null;
    }

    String fullPath = FileUtil.normalize(basedir, path);

    Module found = cache.get(fullPath);
    if (found != null) {
      return found;
    }

    URL url = null;
    String targetPath = null;
    if (FileUtil.isExtension(fullPath, ".js") || FileUtil.isExtension(fullPath, ".json")) {
      targetPath = fullPath;
      url = cl.getResource(targetPath);
    } else {
      targetPath = fullPath + ".js";
      url = cl.getResource(targetPath);
      if (url == null) {
        targetPath = fullPath + ".json";
        url = cl.getResource(targetPath);
      }
    }

    if (url == null) {
      URL purl = cl.getResource(FileUtil.normalize(fullPath, "package.json"));
      if (purl != null) {
        String json = FileUtil.toString(purl.openStream());
        Object mainModule = ScriptableObject.getProperty(cache.parseJson(json), "main");
        if (mainModule != UniqueTag.NOT_FOUND && mainModule != null) {
          return findModule(fullPath, Context.toString(mainModule));
        }
      }

      targetPath = FileUtil.normalize(fullPath, "index.js");
      url = cl.getResource(targetPath);
      if (url == null) {
        targetPath = FileUtil.normalize(fullPath, "index.json");
        url = cl.getResource(targetPath);
      }
    }

    if (url != null) {
      found = compileModule(targetPath, url);
      cache.put(fullPath, found);
    }

    return found;
  }

  private Module compileModule(String fullpath, URL url) throws IOException {
    Scriptable module = cx.newObject(global);
    module.setPrototype(global);
    module.setParentScope(null);

    Scriptable exports = LOCAL.get().get(fullpath);
    if (exports == null) {
      exports = cx.newObject(global);
    }

    Module created = new Module(cx, global, cl, FileUtil.dirname(fullpath), cache, fullpath, module, exports, this, this.main);

    if (FileUtil.isExtension(fullpath, ".json")) {
      String code = FileUtil.toString(url.openStream());
      created.exports = cache.parseJson(code);
    } else {
      String className = FileUtil.toClassName(fullpath);
      if (cl.getResource(className.replace('.', '/') + ".class") != null) {
        try {
          Script script = (Script)Class.forName(className, true, cl).newInstance();
          Function function = (Function)script.exec(cx, global);
          function.call(cx, global, created, new Object[] { created.exports, created, created.module, fullpath, FileUtil.dirname(fullpath) });
          created.exports = (Scriptable)ScriptableObject.getProperty(created.module, "exports");
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      } else {
        String code = FileUtil.toString(url.openStream());
        Function function = cx.compileFunction(global, "function (exports, require, module, __filename, __dirname) {" + code + "}", fullpath, 0, null);
        function.call(cx, global, created, new Object[] { created.exports, created, created.module, fullpath, FileUtil.dirname(fullpath) });
        created.exports = (Scriptable)ScriptableObject.getProperty(created.module, "exports");
      }
    }

    created.setLoaded();
    return created;
  }

  private class ModuleCache {
    private Map<String, Module> modules = new HashMap<>();
    private JsonParser jsonParser;

    public ModuleCache() {
      jsonParser = new JsonParser(cx, global);
    }

    public Module get(String fullPath) {
      return modules.get(fullPath);
    }

    public void put(String fullPath, Module module) {
      modules.put(fullPath, module);
    }

    public Scriptable parseJson(String json) {
      try {
        return (Scriptable)jsonParser.parseValue(json);
      } catch (ParseException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
