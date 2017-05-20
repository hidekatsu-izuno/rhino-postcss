package net.arnx.rhinode.postcss;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.arnx.rhinode.core.RhinodeEngine;
import net.arnx.rhinode.util.FileUtil;
import net.arnx.rhinode.util.JsonWriter;

public class PostCSS implements AutoCloseable {
  public static void main(String[] args) throws IOException {
    try (PostCSS postcss = new PostCSS()) {
      postcss.use("autoprefixer");
      postcss.option(Option.CREATE_SOURCE_MAP, true);
      postcss.option(Option.SOURCE_MAP_WITH_SOURCES_CONTENT, true);
      postcss.option(Option.ADD_SOURCE_MAPPING_URL, false);
      Result result = postcss.process("test.css", "test.min.css");
      System.out.println("CSS:\n" + result.css() + "\nSource Map:\n" + result.map());
    }
  }

  private final RhinodeEngine engine;
  private final URI srcDir;
  private final Path destDir;
  private Map<String, String> plugins = new LinkedHashMap<>();
  private Map<Option<?>, Object> options = new LinkedHashMap<>();

  public PostCSS() {
    this(Paths.get(".").toUri(), Paths.get("."));
  }

  public PostCSS(URL srcDir, Path destDir) {
    this(((Supplier<URI>)() -> {
      try {
        return srcDir.toURI();
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(e);
      }
    }).get(), destDir);
  }

  public PostCSS(URI srcDir, Path destDir) {
    this.engine = new RhinodeEngine(getClass().getClassLoader(), "net/arnx/rhinode/modules");
    this.engine.init("require('postcss')");

    this.srcDir = Objects.requireNonNull(srcDir);
    this.destDir = Objects.requireNonNull(destDir);
  }

  public PostCSS use(String plugin, String options) {
    plugins.put(plugin, options);
    return this;
  }

  public PostCSS use(String plugin) {
    plugins.put(plugin, null);
    return this;
  }

  public <T> PostCSS option(PostCSS.Option<T> option, T value) {
    options.put(option, value);
    return this;
  }

  public Result process(String from, String to) throws IOException {
    String inputCss = FileUtil.toString(srcDir.resolve(from).toURL().openStream());

    Map<String, Object> params = new HashMap<>();
    params.put("inputCss", inputCss);

    Map<String, Class<?>> resultTypes = new HashMap<>();
    resultTypes.put("resultCss", String.class);
    resultTypes.put("resultMap", String.class);
    resultTypes.put("error", Throwable.class);

    StringBuilder sb = new StringBuilder();
    sb.append("var that = this;");
    sb.append("require('postcss')([");
    boolean first = true;
    for (Map.Entry<String, String> entry : plugins.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
      sb.append("require(");
      new JsonWriter(sb).value(entry.getKey());
      sb.append(")");
      String poptions = entry.getValue();
      if (poptions != null) {
        sb.append("(").append(poptions).append(")");
      }
    }
    sb.append("]).process(inputCss,");
    JsonWriter json = new JsonWriter(sb);
    json.beginObject();
    {
      json.name("from").value(from.replace('\\', '/'));
      json.name("to").value(to.replace('\\', '/'));
      if (Boolean.TRUE.equals(options.get(Option.PARSE_SAFE))) {
        json.name("safe").value(true);
      }
      if (Boolean.TRUE.equals(options.get(Option.CREATE_SOURCE_MAP))) {
        json.name("map").beginObject();
        Object mapSourcesContent = options.get(Option.SOURCE_MAP_WITH_SOURCES_CONTENT);
        if (mapSourcesContent != null) {
          json.name("sourcesContent").value(mapSourcesContent);
        }
        Object mapFrom = options.get(Option.SOURCE_MAP_FROM);
        if (mapFrom != null) {
          json.name("from").value(mapFrom);
        }
        Object mapAddSourceMappingURL = options.get(Option.ADD_SOURCE_MAPPING_URL);
        if (Boolean.FALSE.equals(mapAddSourceMappingURL)) {
          json.name("annotation").value(mapAddSourceMappingURL);
        } else {
          Object mapInline = options.get(Option.SOURCE_MAPPING_URL_INLINE);
          if (Boolean.TRUE.equals(mapInline)) {
            json.name("inline").value(true);
          } else {
            Object mapTo = options.get(Option.SOURCE_MAP_TO);
            if (mapTo != null) {
              json.name("annotation").value(mapTo);
            }
          }
        }
        json.endObject();
      }
    }
    json.endObject();
    sb.append(").then(function(result){");
    sb.append("that.resultCss = result.css ? result.css.toString() : null;;");
    sb.append("that.resultMap = result.map ? result.map.toString() : null;");
    sb.append("}).catch(function(error) {");
    sb.append("that.error = error;");
    sb.append("});");
    Map<String, Object> result = engine.run(new StringReader(sb.toString()), "<main>", params, resultTypes);
    Throwable error = (Throwable)result.get("error");
    if (error != null) {
      throw new PostCSSException(error);
    }
    return new Result(to, (String)result.get("resultCss"), to + ".map", (String)result.get("resultMap"));
  }

  @Override
  public void close() {
    engine.close();
  }

  public static class Option<T> {
    public static Option<Boolean> PARSE_SAFE = new Option<>("PARSE_SAFE", Boolean.class);

    public static Option<Boolean> CREATE_SOURCE_MAP = new Option<>("SOURCE_MAP_CREATION", Boolean.class);
    public static Option<Boolean> SOURCE_MAP_WITH_SOURCES_CONTENT = new Option<>("SOURCE_MAP_WITH_SOURCES_CONTENT", Boolean.class);
    public static Option<String> SOURCE_MAP_FROM = new Option<>("SOURCE_MAP_FROM", String.class);
    public static Option<String> SOURCE_MAP_TO = new Option<>("SOURCE_MAP_TO", String.class);

    public static Option<Boolean> ADD_SOURCE_MAPPING_URL = new Option<>("SOURCE_MAPPING_URL_ADDING", Boolean.class);
    public static Option<Boolean> SOURCE_MAPPING_URL_INLINE = new Option<>("SOURCE_MAPPING_URL_INLINE", Boolean.class);

    private final String name;

    private Option(String name, Class<T> cls) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public class Result {
    private final String cssPath;
    private final String css;
    private final String mapPath;
    private final String map;

    private Result(String cssPath, String css, String mapPath, String map) {
      this.cssPath = cssPath;
      this.css = css;
      this.mapPath = mapPath;
      this.map = map;
    }

    public String css() {
      return css;
    }

    public String map() {
      return map;
    }

    public void writeCSS() throws IOException {
      if (css == null) {
        throw new IllegalStateException("css is not created.");
      }

      try (BufferedWriter out = Files.newBufferedWriter(destDir.resolve(cssPath), StandardCharsets.UTF_8)) {
        out.append(css);
      }
    }

    public void writeMap() throws IOException {
      if (map == null) {
         throw new IllegalStateException("source map is not created.");
      }

      try (BufferedWriter out = Files.newBufferedWriter(destDir.resolve(mapPath), StandardCharsets.UTF_8)) {
        out.append(map);
      }
    }
  }
}
