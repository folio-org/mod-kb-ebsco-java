package org.folio.rest.parser;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ValidationException;

import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;

@Component
public class IdParser {
  private static final String RESOURCE_ID_INVALID_ERROR = "Resource id is invalid";
  private static final String TITLE_ID_IS_INVALID_ERROR = "Title id is invalid - %s";
  private static final String PACKAGE_ID_MISSING_ERROR = "Package and provider id are required";
  private static final String PACKAGE_ID_INVALID_ERROR = "Package or provider id are invalid";
  private static final String INVALID_PROVIDER_ID_ERROR = "Provider id is invalid - %s";

  public ResourceId parseResourceId(String id){
    List<Long> parts = parseId(id, 3, RESOURCE_ID_INVALID_ERROR, RESOURCE_ID_INVALID_ERROR);
    return ResourceId.builder()
      .providerIdPart(parts.get(0))
      .packageIdPart(parts.get(1))
      .titleIdPart(parts.get(2)).build();
  }

  public PackageId parsePackageId(String id){
    List<Long> parts = parseId(id, 2, PACKAGE_ID_MISSING_ERROR, PACKAGE_ID_INVALID_ERROR);
    return PackageId.builder().providerIdPart(parts.get(0)).packageIdPart(parts.get(1)).build();
  }

  public Long parseTitleId(String id){
    return parseId(id, 1, TITLE_ID_IS_INVALID_ERROR, TITLE_ID_IS_INVALID_ERROR).get(0);
  }

  public Long parseProviderId(String id){
    return parseId(id, 1, INVALID_PROVIDER_ID_ERROR, INVALID_PROVIDER_ID_ERROR).get(0);
  }

  private List<Long> parseId(String id, int partCount, String wrongCountErrorMessage, String numberFormatErrorMessage) {
    String[] parts = id.split("-");
    if (parts.length != partCount) {
      throw new ValidationException(
        String.format(wrongCountErrorMessage, id));
    }
    List<Long> parsedParts = new ArrayList<>();
    try {
      for (String part : parts) {
        parsedParts.add(Long.parseLong(part));
      }
      return parsedParts;
    } catch (NumberFormatException e) {
      throw new ValidationException(
        String.format(numberFormatErrorMessage, id));
    }
  }
}
