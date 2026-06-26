package org.folio.rest.impl;

import static java.util.Arrays.asList;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.RecordType.PACKAGE;
import static org.folio.repository.RecordType.PROVIDER;
import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.RecordType.TITLE;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.TagsTestUtil.buildTag;
import static org.folio.util.TagsTestUtil.saveTags;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.rest.jaxrs.model.TagUniqueCollection;
import org.folio.rest.util.RestConstants;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

class EholdingsTagsImplIntegrationTest extends IntegrationTestBase {

  private static final DbTag PACKAGE_TAG = buildTag(FULL_PACKAGE_ID, PACKAGE, STUB_TAG_VALUE);
  private static final DbTag RESOURCE_TAG = buildTag(STUB_MANAGED_RESOURCE_ID, RESOURCE, STUB_TAG_VALUE_2);
  private static final DbTag PROVIDER_TAG = buildTag(STUB_VENDOR_ID, PROVIDER, STUB_TAG_VALUE_3);
  private static final DbTag TITLE_TAG = buildTag(STUB_TITLE_ID, TITLE, STUB_TAG_VALUE_4);
  private static final List<DbTag> ALL_TAGS = asList(PROVIDER_TAG, PACKAGE_TAG, TITLE_TAG, RESOURCE_TAG);
  private static final List<DbTag> UNIQUE_TAGS = asList(PROVIDER_TAG, PACKAGE_TAG, PACKAGE_TAG, TITLE_TAG, RESOURCE_TAG,
    RESOURCE_TAG);
  private static final String TAGS_PATH = "eholdings/tags";

  @Autowired
  private Converter<DbTag, TagCollectionItem> tagConverter;

  @AfterEach
  void tearDown() {
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
  }

  @Test
  void shouldReturnAllTagsSortedIfNotFilteredOnGet() {
    var tags = saveTags(ALL_TAGS, vertx);

    var col = getWithOk(TAGS_PATH).as(TagCollection.class);

    var expected = buildTagCollection(tags);
    assertEquals(expected, col);
  }

  @Test
  void shouldReturnEmptyCollectionIfNoTagsOnGet() {
    var col = getWithOk(TAGS_PATH).as(TagCollection.class);

    var expected = buildTagCollection(Collections.emptyList());
    assertEquals(expected, col);
  }

  @Test
  void shouldFilterByRecordTypeOnGet() {
    var tags = saveTags(ALL_TAGS, vertx);

    var col = getWithOk(TAGS_PATH + "?filter[rectype]=provider").as(TagCollection.class);

    var expected = buildTagCollection(filter(tags, similarTo(PROVIDER_TAG)));
    assertEquals(expected, col);
  }

  @Test
  void shouldFilterBySeveralRecordTypesOnGet() {
    var tags = saveTags(ALL_TAGS, vertx);

    var col = getWithOk(TAGS_PATH + "?filter[rectype]=provider&filter[rectype]=title").as(
      TagCollection.class);

    var expected = buildTagCollection(filter(tags, similarTo(PROVIDER_TAG).or(similarTo(TITLE_TAG))));
    assertEquals(expected, col);
  }

  @Test
  void shouldReturnEmptyCollectionIfFilteredOutOnGet() {
    saveTags(asList(PROVIDER_TAG, PACKAGE_TAG), vertx);

    var col = getWithOk(TAGS_PATH + "?filter[rectype]=title").as(TagCollection.class);

    var expected = buildTagCollection(Collections.emptyList());
    assertEquals(expected, col);
  }

  @Test
  void shouldFailOnInvalidRecordTypeOnGet() {
    var error = getWithStatus(TAGS_PATH + "?filter[rectype]=INVALID&filter[rectype]=title",
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid 'filter[rectype]' parameter value");
  }

  @Test
  void shouldReturnAllUniqueTags() {
    var tags = mapItems(saveTags(UNIQUE_TAGS, vertx), DbTag::getValue);

    var col = getWithOk(TAGS_PATH + "/summary").as(TagUniqueCollection.class);

    assertEquals(4, col.getData().size());
    assertEquals(Integer.valueOf(4), col.getMeta().getTotalResults());
    assertTrue(checkContainingOfUniqueTags(tags, col));
  }

  @Test
  void shouldReturnEmptyUniqueTagsCollection() {
    var col = getWithOk(TAGS_PATH + "/summary").as(TagUniqueCollection.class);

    assertEquals(0, col.getData().size());
    assertEquals(Integer.valueOf(0), col.getMeta().getTotalResults());
  }

  @Test
  void shouldReturnListOfUniqueTagsWithParamsResources() {
    var tags = mapItems(saveTags(UNIQUE_TAGS, vertx), DbTag::getValue);

    var col = getWithOk(TAGS_PATH + "/summary?filter[rectype]=resource").as(
      TagUniqueCollection.class);

    assertEquals(1, col.getData().size());
    assertEquals(Integer.valueOf(1), col.getMeta().getTotalResults());
    assertTrue(checkContainingOfUniqueTags(tags, col));
  }

  @Test
  void shouldReturnListOfUniqueTagsWithMultipleParams() {
    var tags = mapItems(saveTags(UNIQUE_TAGS, vertx), DbTag::getValue);

    var col = getWithOk(TAGS_PATH + "/summary?filter[rectype]=resource&filter[rectype]=provider").as(
      TagUniqueCollection.class);

    assertEquals(2, col.getData().size());
    assertEquals(Integer.valueOf(2), col.getMeta().getTotalResults());
    assertTrue(checkContainingOfUniqueTags(tags, col));
  }

  @Test
  void shouldReturnBadRequestWithInvalidParams() {
    var error = getWithStatus(TAGS_PATH + "/summary?filter[rectype]=INVALID&filter[rectype]=title",
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid 'filter[rectype]' parameter value");
  }

  private boolean checkContainingOfUniqueTags(List<String> source, TagUniqueCollection collection) {
    return source.containsAll(mapItems(collection.getData(),
      tagUniqueCollectionItem -> tagUniqueCollectionItem.getAttributes().getValue()));
  }

  private List<TagCollectionItem> toTagCollectionItems(List<DbTag> tags) {
    return mapItems(tags, tagConverter::convert);
  }

  private List<TagCollectionItem> sort(List<TagCollectionItem> items) {
    var result = new ArrayList<TagCollectionItem>(items);
    result.sort(Comparator.comparing(o -> o.getAttributes().getValue()));
    return result;
  }

  private TagCollection buildTagCollection(List<DbTag> tags) {
    return new TagCollection()
      .withData(sort(toTagCollectionItems(tags)))
      .withMeta(new MetaTotalResults().withTotalResults(tags.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }

  private List<DbTag> filter(List<DbTag> tags, Predicate<DbTag> filter) {
    var found = tags.stream().filter(filter).toList();

    if (CollectionUtils.isEmpty(found)) {
      throw new IllegalArgumentException("Cannot find any tag matching the filter predicate");
    } else {
      return found;
    }
  }

  private Predicate<DbTag> similarTo(DbTag expected) {
    return tag -> expected.getValue().equals(tag.getValue())
                  && expected.getRecordId().equals(tag.getRecordId())
                  && expected.getRecordType().equals(tag.getRecordType());
  }
}
