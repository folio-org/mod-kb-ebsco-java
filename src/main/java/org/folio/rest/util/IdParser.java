package org.folio.rest.util;

import static org.folio.common.ListUtils.mapItems;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ValidationException;

import org.apache.commons.lang3.StringUtils;

import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.packages.PackageInfoInDB;
import org.folio.repository.resources.ResourceInfoInDB;

public final class IdParser {

  private static final String RESOURCE_ID_INVALID_ERROR = "Resource id is invalid";
  private static final String TITLE_ID_IS_INVALID_ERROR = "Title id is invalid - %s";
  private static final String PACKAGE_ID_MISSING_ERROR = "Package and provider id are required";
  private static final String PACKAGE_ID_INVALID_ERROR = "Package or provider id are invalid";
  private static final String INVALID_PROVIDER_ID_ERROR = "Provider id is invalid - %s";

  private IdParser() {
  }

  public static ResourceId parseResourceId(String id) {
    List<Long> parts = parseId(id, 3, RESOURCE_ID_INVALID_ERROR, RESOURCE_ID_INVALID_ERROR);
    return ResourceId.builder()
      .providerIdPart(parts.get(0))
      .packageIdPart(parts.get(1))
      .titleIdPart(parts.get(2)).build();
  }

  public static PackageId parsePackageId(String id) {
    List<Long> parts = parseId(id, 2, PACKAGE_ID_MISSING_ERROR, PACKAGE_ID_INVALID_ERROR);
    return PackageId.builder().providerIdPart(parts.get(0)).packageIdPart(parts.get(1)).build();
  }

  public static Long parseTitleId(String id) {
    return parseId(id, 1, TITLE_ID_IS_INVALID_ERROR, TITLE_ID_IS_INVALID_ERROR).get(0);
  }

  public static Long parseProviderId(String id) {
    return parseId(id, 1, INVALID_PROVIDER_ID_ERROR, INVALID_PROVIDER_ID_ERROR).get(0);
  }

  public static String packageIdToString(PackageId packageId) {
    return concat(packageId.getProviderIdPart(), packageId.getPackageIdPart());
  }

  public static String resourceIdToString(ResourceId resourceId) {
    return concat(resourceId.getProviderIdPart(), resourceId.getPackageIdPart(), resourceId.getTitleIdPart());
  }

  public static String getResourceId(CustomerResources resource) {
    return concat(resource.getVendorId(), resource.getPackageId(), resource.getTitleId());
  }

  public static List<PackageId> getPackageIds(List<PackageInfoInDB> packageIds) {
    return mapItems(packageIds, PackageInfoInDB::getId);
  }

  public static List<ResourceId> getTitleIds(List<ResourceInfoInDB> resources) {
    return mapItems(resources, ResourceInfoInDB::getId);
  }

  public static List<ResourceId> getResourceIds(List<HoldingInfoInDB> holdings) {
    return mapItems(holdings, resource ->
      ResourceId.builder()
        .providerIdPart(resource.getVendorId())
        .packageIdPart(Long.parseLong(resource.getPackageId()))
        .titleIdPart(Long.parseLong(resource.getTitleId()))
        .build());
  }

  public static List<PackageId> getPackageIds(Packages packages) {
    return mapItems(packages.getPackagesList(), packageData ->
      PackageId.builder()
        .packageIdPart(packageData.getPackageId())
        .providerIdPart(packageData.getVendorId())
        .build());
  }

  private static String concat(long... parts) {
    return StringUtils.join(parts, '-');
  }

  private static List<Long> parseId(String id, int partCount, String wrongCountErrorMessage,
                                    String numberFormatErrorMessage) {
    String[] parts = id.split("-");
    if (parts.length != partCount) {
      throw new ValidationException(String.format(wrongCountErrorMessage, id));
    }
    List<Long> parsedParts = new ArrayList<>();
    try {
      for (String part : parts) {
        parsedParts.add(Long.parseLong(part));
      }
      return parsedParts;
    } catch (NumberFormatException e) {
      throw new ValidationException(String.format(numberFormatErrorMessage, id));
    }
  }
}
