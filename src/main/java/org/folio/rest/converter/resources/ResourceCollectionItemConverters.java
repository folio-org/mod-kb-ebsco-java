package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import java.util.List;

import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.jaxrs.model.AccessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.result.TitleResult;

public class ResourceCollectionItemConverters {

  private ResourceCollectionItemConverters() { }

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
    private Converter<List<DbTag>, Tags> tagsConverter;
    @Autowired
    private Converter<DbAccessType, AccessType> accessTypeConverter;

    @Override
    public ResourceCollectionItem convert(@NonNull TitleResult titleResult) {
      ResourceCollectionItem result = requireNonNull(titleConverter.convert(titleResult.getTitle()));
      result.getAttributes().setTags(tagsConverter.convert(emptyIfNull(titleResult.getResourceTagList())));
      if (titleResult.getResourceAccessType() != null) {
        result.getIncluded().add(accessTypeConverter.convert(titleResult.getResourceAccessType()));
      }
      return result;
    }
  }
}
