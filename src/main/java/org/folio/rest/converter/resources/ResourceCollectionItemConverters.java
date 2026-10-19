package org.folio.rest.converter.resources;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.rest.converter.resources.ResourceConverterUtils.createEmptyRelationship;

import java.util.List;
import org.folio.holdingsiq.model.Resource;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.IdParser;
import org.folio.rmapi.result.ResourceItemResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class ResourceCollectionItemConverters {

  private ResourceCollectionItemConverters() { }

  @Component
  public static class FromResource implements Converter<Resource, ResourceCollectionItem> {

    private final CommonResourceConverter commonResourceConverter;

    public FromResource(CommonResourceConverter commonResourceConverter) {
      this.commonResourceConverter = commonResourceConverter;
    }

    @Override
    public ResourceCollectionItem convert(Resource resource) {
      return new ResourceCollectionItem()
        .withId(IdParser.getResourceId(resource))
        .withType(ResourceCollectionItem.Type.RESOURCES)
        .withRelationships(createEmptyRelationship())
        .withAttributes(commonResourceConverter.createResourceDataAttributes(resource));
    }
  }

  @Component
  public static class FromResourceItemResult implements Converter<ResourceItemResult, ResourceCollectionItem> {

    private final Converter<Resource, ResourceCollectionItem> resourceConverter;
    private final Converter<List<DbTag>, Tags> tagsConverter;
    private final Converter<DbAccessType, AccessType> accessTypeConverter;

    public FromResourceItemResult(Converter<Resource, ResourceCollectionItem> resourceConverter,
                                  Converter<List<DbTag>, Tags> tagsConverter,
                                  Converter<DbAccessType, AccessType> accessTypeConverter) {
      this.resourceConverter = resourceConverter;
      this.tagsConverter = tagsConverter;
      this.accessTypeConverter = accessTypeConverter;
    }

    @Override
    public ResourceCollectionItem convert(ResourceItemResult itemResult) {
      ResourceCollectionItem result = requireNonNull(resourceConverter.convert(itemResult.getResource()));
      result.getAttributes().setTags(tagsConverter.convert(emptyIfNull(itemResult.getTagList())));
      if (itemResult.getAccessType() != null) {
        result.getIncluded().add(accessTypeConverter.convert(itemResult.getAccessType()));
      }
      return result;
    }
  }
}
