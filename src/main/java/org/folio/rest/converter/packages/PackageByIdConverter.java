package org.folio.rest.converter.packages;

import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rmapi.result.PackageResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageByIdConverter implements Converter<PackageData, Package> {
  private final Converter<PackageResult, Package> packageConverter;

  public PackageByIdConverter(Converter<PackageResult, Package> packageConverter) {
    this.packageConverter = packageConverter;
  }

  @Override
  public Package convert(PackageData packageData) {
    return packageConverter.convert(new PackageResult(packageData));
  }
}
