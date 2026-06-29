package org.folio.rest.model.filter;

import java.util.List;

public record PackageFilterParams(
  String filterCustom,
  String filterSelected,
  String filterType,
  String filterVisibility,
  String filterFreeAccess,
  List<String> filterTags,
  List<String> filterAccessType
) { }
