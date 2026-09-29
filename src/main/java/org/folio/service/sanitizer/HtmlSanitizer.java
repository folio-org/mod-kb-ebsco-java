package org.folio.service.sanitizer;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Strips HTML content down to a fixed set of allowed tags and attributes using {@link Jsoup}.
 *
 * <p>The allowed set is hardcoded rather than externally configurable: {@code p}, {@code strong}, {@code em},
 * {@code a}, {@code u}, {@code ol}, {@code ul}, {@code li}, {@code h1}, {@code h2}, {@code h3} and {@code br} tags,
 * plus {@code class}/{@code style} attributes on any allowed tag and {@code href}/{@code rel}/{@code target} on
 * {@code a}. Any other tag or attribute is removed, though the text content of a stripped tag is kept (with the
 * exception of tags like {@code script} whose content is data, not text, and is discarded).</p>
 */
@Component
public class HtmlSanitizer implements Sanitizer<String> {

  private static final Safelist SAFELIST = new Safelist()
    .addTags("p", "strong", "em", "a", "u", "ol", "ul", "li", "h1", "h2", "h3", "br")
    .addAttributes(":all", "class", "style")
    .addAttributes("a", "href", "rel", "target");

  private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().prettyPrint(false);

  /**
   * Sanitizes the given HTML content, removing any tag or attribute not on the allowed list.
   *
   * @param target the HTML content to sanitize; a blank or {@code null} value is returned unchanged.
   * @return the sanitized content, with output pretty-printing disabled so it is not reformatted.
   */
  @Override
  public String sanitize(String target) {
    return StringUtils.isNotBlank(target)
      ? Jsoup.clean(target, "", SAFELIST, OUTPUT_SETTINGS)
      : target;
  }
}
