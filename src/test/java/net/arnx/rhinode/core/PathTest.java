package net.arnx.rhinode.core;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import net.arnx.rhinode.core.RhinodeEngine;

public class PathTest {
  private RhinodeEngine engine;

  public PathTest() throws Exception {
    engine = new RhinodeEngine(getClass().getClassLoader(), "META-INF/nodejs");
  }

  @Test
  public void testBasename() throws Exception {
    assertEquals(".", (String)engine.eval("require('path').basename('.')"));
    assertEquals("text.txt", (String)engine.eval("require('path').basename('aaa\\\\text.txt')"));
    assertEquals("text", (String)engine.eval("require('path').basename('aaa\\\\text.txt', '.txt')"));
    assertEquals("text.", (String)engine.eval("require('path').basename('aaa\\\\text.txt', 'txt')"));
    assertEquals("text.txt", (String)engine.eval("require('path').basename('.\\\\aaa\\\\text.txt')"));
    assertEquals("text.txt", (String)engine.eval("require('path').basename('C:\\\\aaa\\\\text.txt')"));
    assertEquals("text.txt", (String)engine.eval("require('path').basename('\\\\aaa\\\\text.txt')"));
    assertEquals("aaa", (String)engine.eval("require('path').basename('aaa')"));
    assertEquals("aaa", (String)engine.eval("require('path').basename('aaa\\\\')"));
  }

  @Test
  public void testExtname() throws Exception {
    assertEquals("", (String)engine.eval("require('path').extname('.')"));
    assertEquals(".txt", (String)engine.eval("require('path').extname('aaa\\\\text.txt')"));
    assertEquals("", (String)engine.eval("require('path').extname('aaa\\\\text')"));
    assertEquals(".txt", (String)engine.eval("require('path').extname('text.bat.txt')"));
    assertEquals("", (String)engine.eval("require('path').extname('.txt')"));
  }

  @Test
  public void testDirname() throws Exception {
    assertEquals(".", (String)engine.eval("require('path').dirname('.')"));
    assertEquals("aaa", (String)engine.eval("require('path').dirname('aaa\\\\text.txt')"));
    assertEquals(".\\aaa", (String)engine.eval("require('path').dirname('.\\\\aaa\\\\text.txt')"));
    assertEquals("C:\\aaa", (String)engine.eval("require('path').dirname('C:\\\\aaa\\\\text.txt')"));
    assertEquals("\\aaa", (String)engine.eval("require('path').dirname('\\\\aaa\\\\text.txt')"));
    assertEquals(".", (String)engine.eval("require('path').dirname('aaa')"));
    assertEquals(".", (String)engine.eval("require('path').dirname('aaa\\\\')"));
  }

  @Test
  public void testIsAbsolute() throws Exception {
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('path').isAbsolute('.')"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('path').isAbsolute('aaa\\\\text.txt')"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('path').isAbsolute('.\\\\aaa\\\\text.txt')"));
    assertEquals(Boolean.TRUE, (Boolean)engine.eval("require('path').isAbsolute('C:\\\\aaa\\\\text.txt')"));
    assertEquals(Boolean.TRUE, (Boolean)engine.eval("require('path').isAbsolute('\\\\aaa\\\\text.txt')"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('path').isAbsolute('aaa')"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('path').isAbsolute('aaa\\\\')"));
  }

  @Test
  public void testNormalize() throws Exception {
    assertEquals(".", (String)engine.eval("require('path').normalize('.')"));
    assertEquals("aaa\\text.txt", (String)engine.eval("require('path').normalize('aaa\\\\.\\\\text.txt')"));
    assertEquals("text.txt", (String)engine.eval("require('path').normalize('aaa\\\\..\\\\text.txt')"));
    assertEquals("C:\\aaa\\text.txt", (String)engine.eval("require('path').normalize('C:\\\\aaa\\\\text.txt')"));
    assertEquals("aaa\\text.txt", (String)engine.eval("require('path').normalize('.\\\\aaa\\\\text.txt')"));
    assertEquals("aaa", (String)engine.eval("require('path').normalize('aaa')"));
    assertEquals("aaa\\", (String)engine.eval("require('path').normalize('aaa\\\\')"));
    assertEquals("aaa", (String)engine.eval("require('path').normalize('aaa\\\\.')"));
    assertEquals(".", (String)engine.eval("require('path').normalize('aaa\\\\..')"));
  }

  @Test
  public void testResolve() throws Exception {
    Path cdir = Paths.get(".").toAbsolutePath().normalize();

    assertEquals(cdir.toString(), (String)engine.eval("require('path').resolve()"));
    assertEquals(cdir.resolve("./aaa").normalize().toString(), (String)engine.eval("require('path').resolve('aaa')"));
    assertEquals(cdir.resolve("./aaa/bbb").normalize().toString(), (String)engine.eval("require('path').resolve('aaa', 'bbb')"));
    assertEquals(cdir.resolve("./aaa/bbb").normalize().toString(), (String)engine.eval("require('path').resolve('aaa', './bbb')"));
    assertEquals(cdir.resolve("./aaa/bbb").normalize().toString(), (String)engine.eval("require('path').resolve('aaa', './bbb/')"));
    assertEquals("C:\\ccc", (String)engine.eval("require('path').resolve('aaa', './bbb', '/ccc')"));
    assertEquals(cdir.resolve("..").normalize().toString(), (String)engine.eval("require('path').resolve('aaa', './bbb', '../../../')"));
  }

  @Test
  public void testJoin() throws Exception {
    assertEquals(".", (String)engine.eval("require('path').join()"));
    assertEquals("aaa\\bbb\\ccc", (String)engine.eval("require('path').join('aaa', 'bbb', 'ccc')"));
    assertEquals("aaa\\bbb", (String)engine.eval("require('path').join('aaa', './bbb')"));
    assertEquals("aaa\\bbb\\", (String)engine.eval("require('path').join('aaa', './bbb/')"));
    assertEquals("aaa\\ccc", (String)engine.eval("require('path').join('aaa', './bbb', '../ccc')"));
    assertEquals("aaa\\bbb\\ccc", (String)engine.eval("require('path').join('aaa', './bbb', '/ccc')"));
  }

  @Test
  public void testRelative() throws Exception {
    assertEquals("", (String)engine.eval("require('path').relative('', '')"));
    assertEquals("..\\bbb", (String)engine.eval("require('path').relative('aaa', 'bbb')"));
    assertEquals("..\\ccc", (String)engine.eval("require('path').relative('/aaa/bbb', '/aaa/ccc')"));
    assertEquals("..\\..\\..\\..\\aaa\\ccc", (String)engine.eval("require('path').relative('.', '/aaa/ccc')"));
  }
}
