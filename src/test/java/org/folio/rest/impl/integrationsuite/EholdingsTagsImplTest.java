package org.folio.rest.impl.integrationsuite;

import static java.util.Arrays.asList;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.TagsTestUtil.insertTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.repository.RecordType;
import org.folio.repository.tag.DbTag;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.rest.jaxrs.model.TagCollectionItem;
import org.folio.rest.jaxrs.model.TagUniqueCollection;
import org.folio.rest.jaxrs.model.TagUniqueCollectionItem;
import org.folio.rest.util.RestConstants;

@RunWith(VertxUnitRunner.class)
public class EholdingsTagsImplTest extends WireMockTestBase {

  private static final String PROVIDER_ID = "1111";
  private static final String PACKAGE_ID = PROVIDER_ID + "-" + "3964";
  private static final String TITLE_ID = "12345";
  private static final String RESOURCE_ID = PACKAGE_ID + "-" + TITLE_ID;

  private static final DbTag PROVIDER_TAG = tag(PROVIDER_ID, RecordType.PROVIDER, "provider-tag");
  private static final DbTag PACKAGE_TAG = tag(PACKAGE_ID, RecordType.PACKAGE, "package-tag");
  private static final DbTag TITLE_TAG = tag(TITLE_ID, RecordType.TITLE, "title-tag");
  private static final DbTag RESOURCE_TAG = tag(RESOURCE_ID, RecordType.RESOURCE, "resource-tag");

  private static final List<DbTag> ALL_TAGS = asList(PROVIDER_TAG, PACKAGE_TAG, TITLE_TAG, RESOURCE_TAG);
  private static final List<DbTag> UNIQUE_TAGS = asList(PROVIDER_TAG, PACKAGE_TAG, PACKAGE_TAG, TITLE_TAG, RESOURCE_TAG,
    RESOURCE_TAG);

  @Autowired
  private Converter<DbTag, TagCollectionItem> tagConverter;
  @Autowired
  private Converter<String, TagUniqueCollectionItem> tagUniqueConverter;

  @After
  public void tearDown() {
    clearDataFromTable(vertx, TAGS_TABLE_NAME);
  }

  @Test
  public void shouldReturnAllTagsSortedIfNotFilteredOnGet() {
    List<DbTag> tags = insertTags(ALL_TAGS, vertx);

    TagCollection col = getWithOk("eholdings/tags").as(TagCollection.class);

    TagCollection expected = buildTagCollection(tags);
    assertEquals(expected, col);
  }

  @Test
  public void shouldReturnEmptyCollectionIfNoTagsOnGet() {
    TagCollection col = getWithOk("eholdings/tags").as(TagCollection.class);

    TagCollection expected = buildTagCollection(Collections.emptyList());
    assertEquals(expected, col);
  }

  @Test
  public void shouldFilterByRecordTypeOnGet() {
    List<DbTag> tags = insertTags(ALL_TAGS, vertx);

    TagCollection col = getWithOk("eholdings/tags?filter[rectype]=provider").as(TagCollection.class);

    TagCollection expected = buildTagCollection(filter(tags, similarTo(PROVIDER_TAG)));
    assertEquals(expected, col);
  }

  @Test
  public void shouldFilterBySeveralRecordTypesOnGet() {
    List<DbTag> tags = insertTags(ALL_TAGS, vertx);

    TagCollection col = getWithOk("eholdings/tags?filter[rectype]=provider&filter[rectype]=title").as(
        TagCollection.class);

    TagCollection expected = buildTagCollection(filter(tags, similarTo(PROVIDER_TAG).or(similarTo(TITLE_TAG))));
    assertEquals(expected, col);
  }

  @Test
  public void shouldReturnEmptyCollectionIfFilteredOutOnGet() {
    insertTags(asList(PROVIDER_TAG, PACKAGE_TAG), vertx);

    TagCollection col = getWithOk("eholdings/tags?filter[rectype]=title").as(TagCollection.class);

    TagCollection expected = buildTagCollection(Collections.emptyList());
    assertEquals(expected, col);
  }

  @Test
  public void shouldFailOnInvalidRecordTypeOnGet() {
    JsonapiError error = getWithStatus("eholdings/tags?filter[rectype]=INVALID&filter[rectype]=title",
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("Invalid 'filter[rectype]' parameter value"));
  }

  @Test
  public void shouldReturnAllUniqueTags() {
    List<String> tags = mapItems(insertTags(UNIQUE_TAGS, vertx), DbTag::getValue);

    TagUniqueCollection col = getWithOk("eholdings/tags/summary").as(TagUniqueCollection.class);

    assertEquals(4, col.getData().size());
    assertEquals(Integer.valueOf(4), col.getMeta().getTotalResults());
    assertTrue(checkContainingOfUniqueTags(tags, col));
  }

  @Test
  public void shouldReturnEmptyUniqueTagsCollection() {
    TagUniqueCollection col = getWithOk("eholdings/tags/summary").as(TagUniqueCollection.class);

    assertEquals(0, col.getData().size());
    assertEquals(Integer.valueOf(0), col.getMeta().getTotalResults());
  }

  @Test
  public void shouldReturnListOfUniqueTagsWithParamsResources() {
    List<String> tags = mapItems(insertTags(UNIQUE_TAGS, vertx), DbTag::getValue);

    TagUniqueCollection col = getWithOk("eholdings/tags/summary?filter[rectype]=resource").as(
        TagUniqueCollection.class);

    assertEquals(1, col.getData().size());
    assertEquals(Integer.valueOf(1), col.getMeta().getTotalResults());
    assertTrue(checkContainingOfUniqueTags(tags, col));
  }

  @Test
  public void shouldReturnListOfUniqueTagsWithMultipleParams() {
    List<String> tags = mapItems(insertTags(UNIQUE_TAGS, vertx), DbTag::getValue);

    TagUniqueCollection col = getWithOk("eholdings/tags/summary?filter[rectype]=resource&filter[rectype]=provider").as(
        TagUniqueCollection.class);

    assertEquals(2, col.getData().size());
    assertEquals(Integer.valueOf(2), col.getMeta().getTotalResults());
    assertTrue(checkContainingOfUniqueTags(tags, col));
  }

  @Test
  public void shouldReturnBadRequestWithInvalidParams() {
    JsonapiError error = getWithStatus("eholdings/tags/summary?filter[rectype]=INVALID&filter[rectype]=title",
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), containsString("Invalid 'filter[rectype]' parameter value"));
  }

  private boolean checkContainingOfUniqueTags(List<String> source, TagUniqueCollection collection){
    return source.containsAll(mapItems(collection.getData(),
      tagUniqueCollectionItem -> tagUniqueCollectionItem.getAttributes().getValue()));
  }

  private static DbTag tag(String recordId, RecordType recordType, String value) {
    return DbTag.builder()
              .recordId(recordId)
              .recordType(recordType)
              .value(value).build();
  }

  private List<TagCollectionItem> toTagCollectionItems(List<DbTag> tags) {
    return mapItems(tags, tagConverter::convert);
  }

  private List<TagCollectionItem> sort(List<TagCollectionItem> items) {
    ArrayList<TagCollectionItem> result = new ArrayList<>(items);
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
    List<DbTag> found = tags.stream().filter(filter).collect(Collectors.toList());

    if (CollectionUtils.isEmpty(found)) {
      throw new IllegalArgumentException("Cannot find any tag matching the filter predicate");
    } else {
      return found;
    }
  }

  private Predicate<DbTag> similarTo(DbTag expected) {
    return tag -> expected.getValue().equals(tag.getValue()) &&
      expected.getRecordId().equals(tag.getRecordId()) &&
      expected.getRecordType().equals(tag.getRecordType());
  }
}
