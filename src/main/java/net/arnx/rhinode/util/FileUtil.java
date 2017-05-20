package net.arnx.rhinode.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class FileUtil {
  public static String normalize(String path) {
    if (path.contains(".")) {
      StringBuilder sb = new StringBuilder(path.length());
      for (String segment : path.split("/")) {
        if (segment.isEmpty() || segment.equals(".")) {
          // skip
        } else if (segment.equals("..")) {
          int sep = sb.lastIndexOf("/");
          if (sep != -1) {
            sb.setLength(sep);
          } else {
            return null;
          }
        } else {
          if (sb.length() > 0) {
            sb.append("/");
          }
          sb.append(segment);
        }
      }
      path = sb.toString();
    }
    return path;
  }

  public static String normalize(String path, String subpath) {
    String result;
    if (path.isEmpty() || subpath.startsWith("/")) {
      result = subpath;
    } else if (path.endsWith("/")) {
      result = path + subpath;
    } else {
      result = path + "/" + subpath;
    }
    return normalize(result);
  }

  public static String dirname(String path) {
    int start = path.lastIndexOf('/');
    if (start == -1) {
      return "";
    } else if (start == path.length() - 1) {
      return path.substring(0, path.length()-1);
    }
    return path.substring(0, start);
  }

  public static boolean isExtension(String path, String ext) {
    return path.toLowerCase(Locale.ENGLISH).endsWith(ext);
  }

  public static String toClassName(String path) {
    StringBuilder sb = new StringBuilder();
    sb.append(normalize(path).replace('-', '_').replace('.', '_').replace('/', '.'));
    return sb.toString();
  }

  public static String toString(InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder(1000);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      int n;
      while ((n = reader.read()) != -1) {
        sb.append((char)n);
      }
    }
    return sb.toString();
  }
}
