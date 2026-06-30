package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.PackageConverterUtils.CONTENT_TYPE_TO_RMAPI_CODE;

import org.folio.holdingsiq.model.PackagePut;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CustomPackagePutRequestConverter
  extends CommonPackagePutRequestConverter
  implements Converter<PackagePutRequest, PackagePut> {

  @Override
  public PackagePut convert(PackagePutRequest request) {
    var attributes = request.getData().getAttributes();
    var builder = convertCommonAttributes(attributes);
    builder.packageName(attributes.getName());
    var contentType = CONTENT_TYPE_TO_RMAPI_CODE.get(attributes.getContentType());
    builder.contentType(contentType != null ? contentType : 6);
    return builder.build();
  }
}
