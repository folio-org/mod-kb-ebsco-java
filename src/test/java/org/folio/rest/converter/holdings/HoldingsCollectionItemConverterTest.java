package org.folio.rest.converter.holdings;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import static org.folio.util.HoldingsTestUtil.getHolding;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.repository.holdings.DbHolding;
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
    DbHolding holding = getHolding();
    final ResourceCollectionItem resourceCollectionItem = holdingCollectionItemConverter.convert(holding);
    assertThat(resourceCollectionItem.getId(), equalTo("123356-3157070-19412030"));
    assertThat(resourceCollectionItem.getAttributes().getName(), equalTo("Test Title"));
    assertThat(resourceCollectionItem.getAttributes().getTitleId(), equalTo(19412030));
    assertThat(resourceCollectionItem.getAttributes().getPublisherName(), equalTo("Test one Press"));
    assertThat(resourceCollectionItem.getAttributes().getPublicationType(), equalTo(PublicationType.BOOK));
  }
}
