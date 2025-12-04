package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import java.util.List;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.result.TitleResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class ResourceCollectionItemConverters {

  private ResourceCollectionItemConverters() { }

  @Component
  public static class FromTitle implements Converter<Title, ResourceCollectionItem> {

    private final CommonResourceConverter commonResourceConverter;

    public FromTitle(CommonResourceConverter commonResourceConverter) {
      this.commonResourceConverter = commonResourceConverter;
    }

    @Override
    public ResourceCollectionItem convert(Title title) {
      CustomerResources resource = title.getCustomerResourcesList().getFirst();
      return new ResourceCollectionItem()
        .withId(resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId())
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withRelationships(createEmptyRelationship())
        .withAttributes(commonResourceConverter.createResourceDataAttributes(title, resource));
    }
  }

  @Component
  public static class FromTitleResult implements Converter<TitleResult, ResourceCollectionItem> {

    private final Converter<Title, ResourceCollectionItem> titleConverter;
    private final Converter<List<DbTag>, Tags> tagsConverter;
    private final Converter<DbAccessType, AccessType> accessTypeConverter;

    public FromTitleResult(Converter<Title, ResourceCollectionItem> titleConverter,
                           Converter<List<DbTag>, Tags> tagsConverter,
                           Converter<DbAccessType, AccessType> accessTypeConverter) {
      this.titleConverter = titleConverter;
      this.tagsConverter = tagsConverter;
      this.accessTypeConverter = accessTypeConverter;
    }

    @Override
    public ResourceCollectionItem convert(TitleResult titleResult) {
      ResourceCollectionItem result = requireNonNull(titleConverter.convert(titleResult.getTitle()));
      result.getAttributes().setTags(tagsConverter.convert(emptyIfNull(titleResult.getResourceTagList())));
      if (titleResult.getResourceAccessType() != null) {
        result.getIncluded().add(accessTypeConverter.convert(titleResult.getResourceAccessType()));
      }
      return result;
    }
  }
}
