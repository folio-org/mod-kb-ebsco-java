package org.folio.rest.impl;

import java.util.Collections;

import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePutData;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.util.RestConstants;


public class ResourcesTestData {
  
  public static ResourcePutRequest getResourcePutRequest(ResourceDataAttributes attributes) {
    return new ResourcePutRequest()
      .withData(new ResourcePutData()
        .withType(ResourcePutData.Type.RESOURCES)
        .withAttributes(attributes));
  }
  
  public static Resource getExpectedManagedResource() {
    return new Resource().withData(new ResourceCollectionItem()
      .withId("583-4345-762169")
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withAttributes(new ResourceDataAttributes()
          .withDescription(null)
          .withEdition(null)
          .withIsPeerReviewed(false)
          .withIsTitleCustom(false)
          .withPublisherName("Praeger")
          .withTitleId(762169)
          .withContributors(Collections.emptyList())
          .withIdentifiers(Collections.emptyList())
          .withName("\"Great Satan\" Vs. the \"Mad Mullahs\": How the United States and Iran Demonize Each Other")
          .withPublicationType(PublicationType.BOOK)
          .withSubjects(Collections.emptyList())
          .withCoverageStatement("test coverage statement")
          .withCustomEmbargoPeriod(new EmbargoPeriod()
              .withEmbargoUnit(EmbargoUnit.YEARS)
              .withEmbargoValue(10))
          .withIsPackageCustom(false)
          .withIsSelected(true)
          .withIsTokenNeeded(false)
          .withLocationId(6926225)
          .withManagedEmbargoPeriod(new EmbargoPeriod()
              .withEmbargoValue(0))
          .withPackageId("583-4345")
          .withPackageName("OhioLINK Electronic Book Center: EBC")
          .withUrl("https://publisher.abc-clio.com/9780313068003/")
          .withProviderId(583)
          .withProviderName("OhioLINK")
          .withVisibilityData(new VisibilityData()
              .withIsHidden(false)
              .withReason(""))
          .withManagedCoverages(Collections.emptyList())
          .withCustomCoverages(Collections.emptyList())
          .withProxy(new Proxy()
              .withId("Test-proxy-id")
              .withInherited(false))
        ))
      .withJsonapi(RestConstants.JSONAPI);
  }
  
  public static Resource getExpectedCustomResource() {
    return new Resource().withData(new ResourceCollectionItem()
      .withId("123356-3157070-19412030")
      .withType(ResourceCollectionItem.Type.RESOURCES)
      .withAttributes(new ResourceDataAttributes()
          .withDescription("test description")
          .withEdition("test edition")
          .withIsPeerReviewed(true)
          .withIsTitleCustom(true)
          .withPublisherName("Not Empty")
          .withTitleId(19412030)
          .withContributors(Collections.emptyList())
          .withIdentifiers(Collections.emptyList())
          .withName("sd-test-java-again")
          .withPublicationType(PublicationType.BOOK)
          .withSubjects(Collections.emptyList())
          .withCoverageStatement("My test statement")
          .withCustomEmbargoPeriod(new EmbargoPeriod()
              .withEmbargoUnit(EmbargoUnit.WEEKS)
              .withEmbargoValue(30))
          .withIsPackageCustom(true)
          .withIsSelected(true)
          .withIsTokenNeeded(false)
          .withLocationId(43006476)
          .withManagedEmbargoPeriod(new EmbargoPeriod()
              .withEmbargoUnit(EmbargoUnit.DAYS)
              .withEmbargoValue(10))
          .withPackageId("123356-3157070")
          .withPackageName("Andrii custom packageC")
          .withUrl("https://hello")
          .withProviderId(123356)
          .withProviderName("API DEV GOVERNMENT CUSTOMER")
          .withVisibilityData(new VisibilityData()
              .withIsHidden(false)
              .withReason(""))
          .withManagedCoverages(Collections.emptyList())
          .withCustomCoverages(Collections.emptyList())
          .withProxy(new Proxy()
              .withId("Proxy-ID-234")
              .withInherited(false))
        ))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
