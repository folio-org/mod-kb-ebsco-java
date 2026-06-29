package org.folio.rest.converter.packages;

import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.TokenInfo;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ManagedPackagePutRequestConverter
  extends CommonPackagePutRequestConverter
  implements Converter<PackagePutRequest, PackagePut> {

  @Override
  public PackagePut convert(PackagePutRequest request) {
    var attributes = request.getData().getAttributes();
    var builder = convertCommonAttributes(attributes);
    builder.allowEbscoToAddTitles(attributes.getAllowKbToAddTitles());
    if (attributes.getPackageToken() != null) {
      var tokenInfo = TokenInfo.builder()
        .value(attributes.getPackageToken().getValue())
        .build();
      builder.packageToken(tokenInfo);
    }
    return builder.build();
  }
}
