package org.folio.rest.converter.packages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.Package;
import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.result.PackageResult;

@Component
public class PackageByIdConverter implements Converter<PackageByIdData, Package> {
  @Autowired
  private Converter<PackageResult, Package> packageConverter;

  @Override
  public Package convert(PackageByIdData packageByIdData) {
    return packageConverter.convert(new PackageResult(packageByIdData, null, null));
  }
}
