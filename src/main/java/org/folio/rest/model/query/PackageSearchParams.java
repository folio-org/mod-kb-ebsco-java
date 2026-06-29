package org.folio.rest.model.query;

public record PackageSearchParams(
  String query,
  String queryField,
  String queryType,
  boolean highlight
) { }
