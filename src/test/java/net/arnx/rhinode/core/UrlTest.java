package net.arnx.rhinode.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class UrlTest {
  private RhinodeEngine engine;

  public UrlTest() throws Exception {
    engine = new RhinodeEngine(getClass().getClassLoader(), "META-INF/nodejs");
  }

  @Test
  public void testParse() {
    assertEquals("Url {\n"
        + "  protocol: null,\n"
        + "  slashes: null,\n"
        + "  auth: null,\n"
        + "  host: null,\n"
        + "  port: null,\n"
        + "  hostname: null,\n"
        + "  hash: null,\n"
        + "  search: null,\n"
        + "  query: null,\n"
        + "  pathname: null,\n"
        + "  path: null,\n"
        + "  href: '' }",
        (String)engine.eval("String(require('url').parse(''))"));
    assertEquals("Url {\n"
        + "  protocol: 'http:',\n"
        + "  slashes: true,\n"
        + "  auth: 'user:pass',\n"
        + "  host: 'sub.host.com:8080',\n"
        + "  port: '8080',\n"
        + "  hostname: 'sub.host.com',\n"
        + "  hash: '#hash',\n"
        + "  search: '?query=string',\n"
        + "  query: 'query=string',\n"
        + "  pathname: '/p/a/t/h',\n"
        + "  path: '/p/a/t/h?query=string',\n"
        + "  href: 'http://user:pass@sub.host.com:8080/p/a/t/h?query=string#hash' }",
        (String)engine.eval("String(require('url').parse('http://user:pass@sub.host.com:8080/p/a/t/h?query=string#hash'))"));
  }

  @Test
  public void testFormat() {
    assertEquals("",
        (String)engine.eval("require('url').format({\n"
        + "  protocol: null,\n"
        + "  slashes: null,\n"
        + "  auth: null,\n"
        + "  host: null,\n"
        + "  port: null,\n"
        + "  hostname: null,\n"
        + "  hash: null,\n"
        + "  search: null,\n"
        + "  query: null,\n"
        + "  pathname: null,\n"
        + "  path: null })"));
    assertEquals("http://user:pass@sub.host.com:8080/p/a/t/h?query=string#hash",
        (String)engine.eval("require('url').format({\n"
        + "  protocol: 'http:',\n"
        + "  slashes: true,\n"
        + "  auth: 'user:pass',\n"
        + "  host: 'sub.host.com:8080',\n"
        + "  port: '8080',\n"
        + "  hostname: 'sub.host.com',\n"
        + "  hash: '#hash',\n"
        + "  search: '?query=string',\n"
        + "  query: 'query=string',\n"
        + "  pathname: '/p/a/t/h',\n"
        + "  path: '/p/a/t/h?query=string' })"));
  }
}
