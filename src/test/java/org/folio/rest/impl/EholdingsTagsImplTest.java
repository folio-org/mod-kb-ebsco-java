package org.folio.rest.impl;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.tag.RecordType;
import org.folio.tag.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.util.TagsTestUtil.clearTags;
import static org.folio.util.TagsTestUtil.insertTags;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(VertxUnitRunner.class)
public class EholdingsTagsImplTest extends WireMockTestBase {

  private static final String PROVIDER_ID = "1111";
  private static final String PACKAGE_ID = PROVIDER_ID + "-" + "3964";
  private static final String TITLE_ID = "12345";
  private static final String RESOURCE_ID = PACKAGE_ID + "-" + TITLE_ID;

  private static final Tag PROVIDER_TAG = tag(PROVIDER_ID, RecordType.PROVIDER, "provider-tag");
  private static final Tag PACKAGE_TAG = tag(PACKAGE_ID, RecordType.PACKAGE, "package-tag");
  private static final Tag TITLE_TAG = tag(TITLE_ID, RecordType.TITLE, "title-tag");
  private static final Tag RESOURCE_TAG = tag(RESOURCE_ID, RecordType.RESOURCE, "resource-tag");

  private static final List<Tag> ALL_TAGS = asList(PROVIDER_TAG, PACKAGE_TAG, TITLE_TAG, RESOURCE_TAG);

  @Autowired
  private Converter<Tag, TagCollectionItem> tagConverter;


  @Test
  public void shouldReturnAllTagsSortedIfNotFilteredOnGet() {
    List<Tag> tags = insertTags(ALL_TAGS, vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags").as(TagCollection.class);

      TagCollection expected = buildTagCollection(tags);
      assertEquals(expected, col);
    } finally {
      clearTags(vertx);
    }
  }

  @Test
  public void shouldReturnEmptyCollectionIfNoTagsOnGet() {
    TagCollection col = getOkResponse("eholdings/tags").as(TagCollection.class);

    TagCollection expected = buildTagCollection(Collections.emptyList());
    assertEquals(expected, col);
  }

  @Test
  public void shouldFilterByRecordTypeOnGet() {
    List<Tag> tags = insertTags(ALL_TAGS, vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags?filter[rectype]=provider").as(TagCollection.class);

      TagCollection expected = buildTagCollection(filter(tags, similarTo(PROVIDER_TAG)));
      assertEquals(expected, col);
    } finally {
      clearTags(vertx);
    }
  }

  @Test
  public void shouldFilterBySeveralRecordTypesOnGet() {
    List<Tag> tags = insertTags(ALL_TAGS, vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags?filter[rectype]=provider&filter[rectype]=title")
          .as(TagCollection.class);

      TagCollection expected = buildTagCollection(filter(tags, similarTo(PROVIDER_TAG).or(similarTo(TITLE_TAG))));
      assertEquals(expected, col);
    } finally {
      clearTags(vertx);
    }
  }

  @Test
  public void shouldReturnEmptyCollectionIfFilteredOutOnGet() {
    insertTags(asList(PROVIDER_TAG, PACKAGE_TAG), vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags?filter[rectype]=title")
        .as(TagCollection.class);

      TagCollection expected = buildTagCollection(Collections.emptyList());
      assertEquals(expected, col);
    } finally {
      clearTags(vertx);
    }
  }

  @Test
  public void shouldFailOnInvalidRecordTypeOnGet() {
    JsonapiError error = getResponseWithStatus("eholdings/tags?filter[rectype]=INVALID&filter[rectype]=title",
        HttpStatus.SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("Invalid 'filter[rectype]' parameter value"));
  }

  private static Tag tag(String recordId, RecordType recordType, String value) {
    return Tag.builder()
              .recordId(recordId)
              .recordType(recordType)
              .value(value).build();
  }

  private List<TagCollectionItem> toTagCollectionItems(List<Tag> tags) {
    return mapItems(tags, tagConverter::convert);
  }

  private List<TagCollectionItem> sort(List<TagCollectionItem> items) {
    ArrayList<TagCollectionItem> result = new ArrayList<>(items);
    result.sort(Comparator.comparing(o -> o.getAttributes().getValue()));
    return result;
  }

  private TagCollection buildTagCollection(List<Tag> tags) {
    return new TagCollection()
      .withData(sort(toTagCollectionItems(tags)))
      .withMeta(new MetaTotalResults().withTotalResults(tags.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }

  private List<Tag> filter(List<Tag> tags, Predicate<Tag> filter) {
    List<Tag> found = tags.stream().filter(filter).collect(Collectors.toList());

    if (CollectionUtils.isEmpty(found)) {
      throw new IllegalArgumentException("Cannot find any tag matching the filter predicate");
    } else {
      return found;
    }
  }

  private Predicate<Tag> similarTo(Tag expected) {
    return tag -> expected.getValue().equals(tag.getValue()) &&
      expected.getRecordId().equals(tag.getRecordId()) &&
      expected.getRecordType().equals(tag.getRecordType());
  }
}
