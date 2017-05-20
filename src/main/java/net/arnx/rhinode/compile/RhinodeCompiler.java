package net.arnx.rhinode.compile;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.mozilla.javascript.tools.ToolErrorReporter;

import net.arnx.rhinode.util.FileUtil;

public class RhinodeCompiler {
  public static void main(String[] args) throws IOException {
    RhinodeCompiler compiler = new RhinodeCompiler();
    compiler.compile(Paths.get(args[0]), Paths.get(args[1]));
  }

  public void compile(Path src, Path dest) throws IOException {
    CompilerEnvirons env = new CompilerEnvirons();
    env.setErrorReporter(new ToolErrorReporter(true));
    env.setOptimizationLevel(9);
    env.setGenerateDebugInfo(false);
    env.setGeneratingSource(false);
    ClassCompiler compiler = new ClassCompiler(env);

    for (Path path : Files.walk(src.normalize()).collect(Collectors.toList())) {
      if (Files.isDirectory(path)) {
        continue;
      }

      String filename = path.getFileName().toString();
      if (!FileUtil.isExtension(filename, ".js")) {
        continue;
      }

      String fullpath = FileUtil.normalize(src.relativize(path).toString().replace('\\', '/'));
      String code = "(function(exports, require, module, __filename, __dirname) {"
          + FileUtil.toString(Files.newInputStream(path))
          + "})";

      String className = FileUtil.toClassName(fullpath);
      Object[] items = compiler.compileToClassFiles(code , fullpath, 0, className);
      for (int i = 0; items != null && i != items.length; i += 2) {
        Path outPath = dest.resolve(items[i].toString().replace('.', '/') + ".class");
        byte[] bytes = (byte[])items[i+1];
        Files.createDirectories(outPath.getParent());
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(outPath))) {
          out.write(bytes);
        }
      }
    }
  }
}
