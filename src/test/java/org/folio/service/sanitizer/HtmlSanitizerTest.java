package org.folio.service.sanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

  private final HtmlSanitizer sanitizer = new HtmlSanitizer();

  @Test
  void shouldReturnNullWhenTargetIsNull() {
    assertNull(sanitizer.sanitize(null));
  }

  @Test
  void shouldReturnBlankValueUnchanged() {
    assertEquals("   ", sanitizer.sanitize("   "));
  }

  @Test
  void shouldKeepNotHtmlContentAsIs() {
    var html = "some custom description";
    assertEquals(html, sanitizer.sanitize(html));
  }

  @Test
  void shouldKeepAllowedTags() {
    var html = "<p>paragraph</p><strong>bold</strong><em>italic</em><u>underline</u>"
      + "<ol><li>one</li></ol><ul><li>two</li></ul><h1>h1</h1><h2>h2</h2><h3>h3</h3>line<br>break";
    assertEquals(html, sanitizer.sanitize(html));
  }

  @Test
  void shouldStripDisallowedTagButKeepItsText() {
    assertEquals("some text", sanitizer.sanitize("<div><span>some text</span></div>"));
  }

  @Test
  void shouldStripScriptTagAndItsContent() {
    assertEquals("<p>safe</p>", sanitizer.sanitize("<script>alert('xss')</script><p>safe</p>"));
  }

  @Test
  void shouldKeepClassAndStyleAttributesOnAllowedTag() {
    var html = "<p class=\"note\" style=\"color:red\">text</p>";
    assertEquals(html, sanitizer.sanitize(html));
  }

  @Test
  void shouldKeepHrefRelAndTargetAttributesOnAnchor() {
    var html = "<a href=\"https://example.com\" rel=\"noopener\" target=\"_blank\">link</a>";
    assertEquals(html, sanitizer.sanitize(html));
  }

  @Test
  void shouldStripDisallowedAttribute() {
    assertEquals("<p>text</p>", sanitizer.sanitize("<p onclick=\"doEvil()\">text</p>"));
  }

  @Test
  void shouldStripHrefAttributeOnNonAnchorTag() {
    assertEquals("<p>text</p>", sanitizer.sanitize("<p href=\"https://example.com\">text</p>"));
  }

  @Test
  void shouldNotPrettyPrintOutput() {
    var html = "<ul><li>one</li><li>two</li></ul>";
    assertEquals(html, sanitizer.sanitize(html));
  }
}
