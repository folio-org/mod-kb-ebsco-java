package org.folio.rest.converter.common;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import org.folio.rest.jaxrs.model.PublicationType;

public final class ConverterConsts {

  public static final BidiMap<String, PublicationType> publicationTypes;

  static {
    BidiMap<String, PublicationType> bidiMap = new TreeBidiMap<>();
    bidiMap.put("audiobook", PublicationType.AUDIOBOOK);
    bidiMap.put("book", PublicationType.BOOK);
    bidiMap.put("bookseries", PublicationType.BOOK_SERIES);
    bidiMap.put("database", PublicationType.DATABASE);
    bidiMap.put("journal", PublicationType.JOURNAL);
    bidiMap.put("newsletter", PublicationType.NEWSLETTER);
    bidiMap.put("newspaper", PublicationType.NEWSPAPER);
    bidiMap.put("proceedings", PublicationType.PROCEEDINGS);
    bidiMap.put("report", PublicationType.REPORT);
    bidiMap.put("streamingaudio", PublicationType.STREAMING_AUDIO);
    bidiMap.put("streamingvideo", PublicationType.STREAMING_VIDEO);
    bidiMap.put("thesisdissertation", PublicationType.THESIS_DISSERTATION);
    bidiMap.put("website", PublicationType.WEBSITE);
    bidiMap.put("unspecified", PublicationType.UNSPECIFIED);
    publicationTypes = UnmodifiableBidiMap.unmodifiableBidiMap(bidiMap);
  }

  private ConverterConsts() {
  }
}
