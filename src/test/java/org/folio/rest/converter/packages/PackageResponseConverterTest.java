package org.folio.rest.converter.packages;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.holdingsiq.model.Title;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rmapi.result.ResourceResult;
import org.folio.spring.config.TestConfig;
import org.folio.util.TestUtil;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class PackageResponseConverterTest {

  @Autowired
  private ConversionService conversionService;

  @Test
  public void shouldReturnCustomCoverageInDescendingOrder() throws URISyntaxException, IOException {

    ObjectMapper mapper = new ObjectMapper();

    Title title = mapper.readValue(TestUtil.getFile("responses/rmapi/titles/get-custom-title-with-coverage-dates-asc.json"), Title.class);

    final ResourceResult resourceResult = new ResourceResult(title, null, null, false);
    final Resource resource = conversionService.convert(resourceResult, Resource.class);

    final List<Coverage> customCoverages = resource.getData().getAttributes().getCustomCoverages();
    assertThat(customCoverages.size(), equalTo(2));
    assertThat(customCoverages.get(0).getBeginCoverage(), equalTo("2004-03-01"));
    assertThat(customCoverages.get(0).getEndCoverage(), equalTo("2004-03-04"));

    assertThat(customCoverages.get(1).getBeginCoverage(), equalTo("2001-01-01"));
    assertThat(customCoverages.get(1).getEndCoverage(), equalTo("2004-02-01"));

  }
}
