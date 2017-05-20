package net.arnx.rhinode.core;

import static org.junit.Assert.*;

import org.junit.Test;

import net.arnx.rhinode.core.RhinodeEngine;

public class FileSystemTest {
  private RhinodeEngine engine;
  private String cdir = "./src/test/java/net/arnx/nashorn/postcss/lib";

  public FileSystemTest() throws Exception {
    engine = new RhinodeEngine(getClass().getClassLoader(), "META-INF/nodejs");
  }

  @Test
  public void testExistsSync() throws Exception {
    assertEquals(Boolean.TRUE, (Boolean)engine.eval("require('fs').existsSync('" + cdir + "/FileSystemTest.java')"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('fs').existsSync('" + cdir + "/FileSystemText.java')"));
  }

  @Test
  public void testStatSync() throws Exception {
    try {
      engine.eval("require('fs').statSync('" + cdir + "/FileSystemText.java')");
      fail();
    } catch (Exception e) {
    }

    assertEquals(Boolean.TRUE, (Boolean)engine.eval("require('fs').statSync('" + cdir + "/FileSystemTest.java').isFile()"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('fs').statSync('" + cdir + "/').isFile()"));
    assertEquals(Boolean.TRUE, (Boolean)engine.eval("require('fs').statSync('" + cdir + "/').isDirectory()"));
    assertEquals(Boolean.FALSE, (Boolean)engine.eval("require('fs').statSync('" + cdir + "/FileSystemTest.java').isDirectory()"));
  }

  @Test
  public void testReadFileSync() throws Exception {
    assertEquals("テスト", engine.eval("require('fs').readFileSync('" + cdir + "/test.txt', 'UTF-8')"));
    assertEquals("テスト", engine.eval("require('fs').readFileSync('" + cdir + "/test.txt', { encoding: 'UTF-8'})"));
    assertEquals("テスト", engine.eval("require('fs').readFileSync('" + cdir + "/test.txt').toString()"));
    assertEquals("テスト", engine.eval("require('fs').readFileSync('" + cdir + "/test.txt').toString('UTF-8')"));
  }
}
