package org.folio.rest.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import static org.folio.rest.impl.TagsTestUtil.clearTags;
import static org.folio.rest.impl.TagsTestUtil.insertTags;

import java.util.ArrayList;
import java.util.List;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.TagCollection;
import org.folio.tag.RecordType;
import org.folio.tag.Tag;

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

  private static final Tag[] ALL_TAGS = new Tag[] {PROVIDER_TAG, PACKAGE_TAG, TITLE_TAG, RESOURCE_TAG};

  @Test
  public void shouldReturnAllTagsSortedIfNotFilteredOnGet() {
    insertTags(asList(ALL_TAGS), vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags").as(TagCollection.class);

      assertNotNull(col);
      assertEquals(sort(values(ALL_TAGS)), col.getData());
      assertEquals(ALL_TAGS.length, col.getMeta().getTotalResults().intValue());
    } finally {
      clearTags(vertx);
    }
  }

  @Test
  public void shouldReturnEmptyCollectionIfNoTagsOnGet() {
    TagCollection col = getOkResponse("eholdings/tags").as(TagCollection.class);

    assertNotNull(col);
    assertThat(col.getData(), is(emptyCollectionOf(String.class)));
    assertEquals(0, col.getMeta().getTotalResults().intValue());
  }

  @Test
  public void shouldFilterByRecordTypeOnGet() {
    insertTags(asList(ALL_TAGS), vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags?filter[rectype]=provider").as(TagCollection.class);

      assertNotNull(col);
      assertEquals(values(PROVIDER_TAG), col.getData());
      assertEquals(1, col.getMeta().getTotalResults().intValue());
    } finally {
      clearTags(vertx);
    }
  }

  @Test
  public void shouldFilterBySeveralRecordTypesOnGet() {
    insertTags(asList(ALL_TAGS), vertx);

    try {
      TagCollection col = getOkResponse("eholdings/tags?filter[rectype]=provider&filter[rectype]=title")
          .as(TagCollection.class);

      assertNotNull(col);
      assertEquals(sort(values(PROVIDER_TAG, TITLE_TAG)), col.getData());
      assertEquals(2, col.getMeta().getTotalResults().intValue());
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

      assertNotNull(col);
      assertThat(col.getData(), is(emptyCollectionOf(String.class)));
      assertEquals(0, col.getMeta().getTotalResults().intValue());
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

  private static List<String> sort(List<String> list) {
    ArrayList<String> result = new ArrayList<>(list);
    result.sort(String::compareTo);
    return result;
  }

  private static List<String> values(Tag ... tags) {
    List<String> result = new ArrayList<>(tags.length);
    for (Tag tag : tags) {
      result.add(tag.getValue());
    }
    return result;
  }

  private static Tag tag(String recordId, RecordType recordType, String value) {
    return Tag.builder()
              .recordId(recordId)
              .recordType(recordType)
              .value(value).build();
  }

}
