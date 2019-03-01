package org.folio.rest.converter.common.attr;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Identifier;
import org.folio.rest.jaxrs.model.Identifier.Subtype;
import org.folio.rest.jaxrs.model.Identifier.Type;

public class IdentifiersConverterPair {

  private static final BidiMap<Integer, Type> IDENTIFIER_TYPES = new TreeBidiMap<>();
  static {
    IDENTIFIER_TYPES.put(0, Type.ISSN);
    IDENTIFIER_TYPES.put(1, Type.ISBN);
  }

  private static final BidiMap<Integer, Subtype> IDENTIFIER_SUBTYPES = new TreeBidiMap<>();
  static {
    IDENTIFIER_SUBTYPES.put(1, Subtype.PRINT);
    IDENTIFIER_SUBTYPES.put(2, Subtype.ONLINE);
  }

  private IdentifiersConverterPair() {
  }

  @Component
  public static class FromRMApi implements Converter<List<Identifier>, List<org.folio.rest.jaxrs.model.Identifier>> {

    @Override
    public List<org.folio.rest.jaxrs.model.Identifier> convert(@Nullable List<Identifier> identifiersList) {
      if (Objects.isNull(identifiersList)) {
        return Collections.emptyList();
      }

      return identifiersList.stream()
        .filter(identifier -> IDENTIFIER_TYPES.keySet().contains(identifier.getType()) && IDENTIFIER_SUBTYPES.keySet().contains(identifier.getSubtype()))
        .sorted(Comparator.comparing(Identifier::getType).thenComparing(Identifier::getSubtype))
        .map(identifier -> new org.folio.rest.jaxrs.model.Identifier()
          .withId(identifier.getId())
          .withType(IDENTIFIER_TYPES
            .getOrDefault(identifier.getType(), Type.ISBN))
          .withSubtype(IDENTIFIER_SUBTYPES.getOrDefault(identifier.getSubtype(), Subtype.ONLINE)))
        .collect(Collectors.toList());
    }

  }

  @Component
  public static class ToRMApi implements Converter<List<org.folio.rest.jaxrs.model.Identifier>, List<Identifier>> {

    @Override
    public List<Identifier> convert(@NonNull List<org.folio.rest.jaxrs.model.Identifier> identifiersList) {
      return identifiersList.stream()
        .sorted(Comparator.comparing(org.folio.rest.jaxrs.model.Identifier::getSubtype)
                          .thenComparing(org.folio.rest.jaxrs.model.Identifier::getType))
        .map(identifier ->  Identifier.builder()
          .id(identifier.getId())
          .type(IDENTIFIER_TYPES.inverseBidiMap().get(identifier.getType()))
          .subtype(IDENTIFIER_SUBTYPES.inverseBidiMap().get(identifier.getSubtype()))
          .build())
        .collect(Collectors.toList());
    }
    
  }

}
