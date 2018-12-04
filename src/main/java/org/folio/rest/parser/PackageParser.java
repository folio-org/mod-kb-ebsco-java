package org.folio.rest.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ValidationException;
import org.folio.rest.model.PackageId;

public class PackageParser {

  private static final String PACKAGE_ID_REGEX = "([^-]+)-([^-]+)";
  private static final Pattern PACKAGE_ID_PATTERN = Pattern.compile(PACKAGE_ID_REGEX);
  private static final String PACKAGE_ID_MISSING_ERROR = "Package and provider id are required";
  private static final String PACKAGE_ID_INVALID_ERROR = "Package or provider id are invalid";

  public PackageId parsePackageId(String packageIdString) {
    try {
      long providerId;
      long packageId;
      Matcher matcher = PACKAGE_ID_PATTERN.matcher(packageIdString);

      if (matcher.find() && matcher.hitEnd()) {
        providerId = Long.parseLong(matcher.group(1));
        packageId = Long.parseLong(matcher.group(2));
      } else {
        throw new ValidationException(PACKAGE_ID_MISSING_ERROR);
      }

      return new PackageId(providerId, packageId);
    } catch (NumberFormatException e) {
      throw new ValidationException(PACKAGE_ID_INVALID_ERROR);
    }
  }

}
