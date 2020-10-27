package org.folio.config.cache;

import io.vertx.core.shareddata.Shareable;
import lombok.Value;

import org.folio.client.uc.configuration.GetTitlePackageUCConfiguration;

@Value
public class UCTitlePackageCacheKey implements Shareable {

  String customerKey;
  String fiscalYear;
  String fiscalMonth;
  String analysisCurrency;
  boolean publisherPlatform;
  boolean previousYear;
  byte[] bodyHash;

  public UCTitlePackageCacheKey(GetTitlePackageUCConfiguration configuration, byte[] bodyHash) {
    this.customerKey = configuration.getCustomerKey();
    this.fiscalYear = configuration.getFiscalYear();
    this.fiscalMonth = configuration.getFiscalMonth();
    this.analysisCurrency = configuration.getAnalysisCurrency();
    this.publisherPlatform = configuration.isPublisherPlatform();
    this.previousYear = configuration.isPreviousYear();
    this.bodyHash = bodyHash;
  }
}
