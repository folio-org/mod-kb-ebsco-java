package org.folio.rest.converter.holdings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.util.HoldingsTestUtil.getStubHolding;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class HoldingsCollectionItemConverterTest {

  @Autowired
  private HoldingCollectionItemConverter holdingCollectionItemConverter;

  @Test
  public void shouldConvertHoldingToResource() throws IOException, URISyntaxException {
    DbHoldingInfo holding = getStubHolding();
    final ResourceCollectionItem resourceCollectionItem = holdingCollectionItemConverter.convert(holding);
    assertThat(resourceCollectionItem.getId(), equalTo("123356-3157070-19412030"));
    assertThat(resourceCollectionItem.getAttributes().getName(), equalTo("Test Title"));
    assertThat(resourceCollectionItem.getAttributes().getTitleId(), equalTo(19412030));
    assertThat(resourceCollectionItem.getAttributes().getPublisherName(), equalTo("Test one Press"));
    assertThat(resourceCollectionItem.getAttributes().getPublicationType(), equalTo(PublicationType.BOOK));
  }
}
