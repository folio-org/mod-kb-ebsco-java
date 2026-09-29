package org.folio.service.sanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

  @ParameterizedTest
  @MethodSource("testCasesWithoutChanges")
  void shouldKeepContentWithoutChanges(String desc, String content) {
    assertEquals(content, sanitizer.sanitize(content), desc);
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
  void shouldStripDisallowedAttribute() {
    assertEquals("<p>text</p>", sanitizer.sanitize("<p onclick=\"doEvil()\">text</p>"));
  }

  @Test
  void shouldStripHrefAttributeOnNonAnchorTag() {
    assertEquals("<p>text</p>", sanitizer.sanitize("<p href=\"https://example.com\">text</p>"));
  }

  private static Stream<Arguments> testCasesWithoutChanges() {
    return Stream.of(
      Arguments.of("should keep not html content as is", "some custom description"),
      Arguments.of("should keep class and style attributes on allowed tag",
        "<p class=\"note\" style=\"color:red\">text</p>"),
      Arguments.of("should keep href rel and target attributes on anchor",
        "<a href=\"https://example.com\" rel=\"noopener\" target=\"_blank\">link</a>"),
      Arguments.of("should not pretty print output", "<ul><li>one</li><li>two</li></ul>"),
      Arguments.of("should keep allowed tags",
        "<p>paragraph</p><strong>bold</strong><em>italic</em><u>underline</u>"
        + "<ol><li>one</li></ol><ul><li>two</li></ul><h1>h1</h1><h2>h2</h2><h3>h3</h3>line<br>break")
    );
  }
}
