package org.folio.rest.converter.packages;

import static org.folio.util.TestUtil.readJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.holdingsiq.model.Title;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rmapi.result.ResourceResult;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class PackageResponseConverterTest {

  @Autowired
  private ConversionService conversionService;

  @Test
  void shouldReturnCustomCoverageInDescendingOrder() {
    var title = readJsonFile("responses/rmapi/titles/get-custom-title-with-coverage-dates-asc.json", Title.class);

    var resourceResult = new ResourceResult(title, null, null, false);
    var resource = conversionService.convert(resourceResult, Resource.class);
    assertNotNull(resource);

    var customCoverages = resource.getData().getAttributes().getCustomCoverages();
    assertEquals(2, customCoverages.size());
    assertEquals("2004-03-01", customCoverages.get(0).getBeginCoverage());
    assertEquals("2004-03-04", customCoverages.get(0).getEndCoverage());

    assertEquals("2001-01-01", customCoverages.get(1).getBeginCoverage());
    assertEquals("2004-02-01", customCoverages.get(1).getEndCoverage());
  }
}
