package org.folio.rest.impl;

import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;

public class PackagesTestData {

  public static PackageCollection getExpectedPackageCollection() {
    List<PackageCollectionItem> collectionItems = new ArrayList<>();
    PackageCollectionItem collectionItem = new PackageCollectionItem()
      .withId("1111111-2222222")
      .withAttributes(new PackageDataAttributes()
        .withName("TEST_PACKAGE_NAME")
        .withPackageId(2222222)
        .withIsCustom(true)
        .withProviderId(1111111)
        .withProviderName("TEST_VENDOR_NAME")
        .withTitleCount(5)
        .withIsSelected(true)
        .withSelectedCount(5)
        .withPackageType("Custom")
        .withContentType(PackageDataAttributes.ContentType.ONLINE_REFERENCE)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason("")
        ));
    collectionItems.add(collectionItem);
    return new PackageCollection().withData(collectionItems)
      .withMeta(new MetaTotalResults().withTotalResults(1));

  }

  public static Package getExpectedPackage() {
    return new Package().withData(new PackageCollectionItem()
      .withId("111111-3964")
      .withType(PACKAGES_TYPE)
      .withAttributes(new PackageDataAttributes()
        .withName("carole and sams test")
        .withPackageId(3964)
        .withIsCustom(true)
        .withProviderId(111111)
        .withProviderName("APIDEV CORPORATE CUSTOMER")
        .withTitleCount(6)
        .withIsSelected(true)
        .withSelectedCount(6)
        .withPackageType("Custom")
        .withContentType(PackageDataAttributes.ContentType.UNKNOWN)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason("")
        )
        .withProxy(new Proxy()
          .withId("<n>")
          .withInherited(true))
        .withAllowKbToAddTitles(false)
        .withPackageToken(new Token()
          .withFactName("[[gale.customcode.infocust]]")
          .withHelpText("help text")
          .withValue("token value")
          .withPrompt("res_id=info:sid/gale:")
        )))
      .withJsonapi(RestConstants.JSONAPI);
  }

  public static PackageCollection getExpectedCollectionPackageItem() {
    List<PackageCollectionItem> collectionItems = new ArrayList<>();
    PackageCollectionItem collectionItem = new PackageCollectionItem()
      .withId("392-3007")
      .withAttributes(new PackageDataAttributes()
        .withName("American Academy of Family Physicians")
        .withPackageId(3007)
        .withIsCustom(false)
        .withProviderId(392)
        .withProviderName("American Academy of Family Physicians")
        .withTitleCount(3)
        .withIsSelected(false)
        .withSelectedCount(0)
        .withPackageType("Variable")
        .withContentType(PackageDataAttributes.ContentType.E_JOURNAL)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason("")
        ));
    collectionItems.add(collectionItem);
    return new PackageCollection().withData(collectionItems)
      .withMeta(new MetaTotalResults().withTotalResults(414));

  }
}
