package org.folio.rest.validator;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePostDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePostRequest;

@Component
public class ResourcePostValidator {

  public void validate(ResourcePostRequest request) {
    ResourcePostDataAttributes attributes = request.getData().getAttributes();
    String url = attributes.getUrl();
    if(!StringUtils.isEmpty(url) && !ValidatorUtil.isUrlValid(url)){
      throw new InputValidationException("Invalid url","Url has invalid format");
    }
    ValidatorUtil.checkIsNotEmpty("Package Id", attributes.getPackageId());
    ValidatorUtil.checkIsNotEmpty("Title Id", attributes.getTitleId());
  }

  public void validateRelatedObjects(PackageByIdData packageData, Title title, Titles existingTitles) {
    if(BooleanUtils.isNotTrue(packageData.getIsCustom())){
      throw new InputValidationException("Invalid PackageId", "Packageid Cannot associate Title with a managed Package");
    }
    if(titleExists(title, existingTitles)){
      throw new InputValidationException("Invalid Title", "Package already associated with Title");
    }
  }

  private boolean titleExists(Title title, Titles existingTitles) {
    return existingTitles.getTitleList().stream()
      .anyMatch(existingTitle -> existingTitle.getTitleId().equals(title.getTitleId()));
  }
}
