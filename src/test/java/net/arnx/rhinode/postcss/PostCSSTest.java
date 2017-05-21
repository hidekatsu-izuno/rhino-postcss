package net.arnx.rhinode.postcss;

import static org.junit.Assert.*;

import org.junit.Test;

public class PostCSSTest {

  @Test
  public void test() throws Exception {
    PostCSS.main(new String[] { "-?" });
  }
}
