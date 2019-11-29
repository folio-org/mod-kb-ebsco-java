package org.folio.rest.converter.resources;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.tag.Tag;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.result.TitleResult;


public class ResourceCollectionItemConverters {

  @Component
  public static class FromTitle implements Converter<Title, ResourceCollectionItem> {

    @Autowired
    private CommonResourceConverter commonResourceConverter;

    @Override
    public ResourceCollectionItem convert(@NonNull Title title) {
      CustomerResources resource = title.getCustomerResourcesList().get(0);
      return new ResourceCollectionItem()
        .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withRelationships(createEmptyRelationship())
        .withAttributes(commonResourceConverter.createResourceDataAttributes(title, resource));
    }

  }

  @Component
  public static class FromTitleResult implements Converter<TitleResult, ResourceCollectionItem> {

    @Autowired
    private Converter<Title, ResourceCollectionItem> titleConverter;
    @Autowired
    private Converter<List<Tag>, Tags> tagsConverter;

    @Override
    public ResourceCollectionItem convert(@NonNull TitleResult titleResult) {
      ResourceCollectionItem result = titleConverter.convert(titleResult.getTitle());
      result.getAttributes().setTags(tagsConverter.convert(emptyIfNull(titleResult.getResourceTagList())));
      return result;
    }
  }

  private ResourceCollectionItemConverters() { }
}
