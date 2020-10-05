package org.folio.service.uc;

import static java.lang.String.valueOf;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.jaxrs.model.CostPerUseParameters;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.util.IdParser;

@Service
public class UCCostPerUseServiceImpl implements UCCostPerUseService {

  private final UCAuthService authService;
  private final UCSettingsService settingsService;
  private final UCApigeeEbscoClient client;
  private final ResourceCostPerUseConverter converter;

  public UCCostPerUseServiceImpl(@Qualifier("nonSecuredUCSettingsService") UCAuthService authService,
                                 UCSettingsService settingsService, UCApigeeEbscoClient client,
                                 ResourceCostPerUseConverter converter) {
    this.authService = authService;
    this.settingsService = settingsService;
    this.client = client;
    this.converter = converter;
  }

  @Override
  public CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, int fiscalYear,
                                                                     Map<String, String> okapiHeaders) {
    ResourceId id = IdParser.parseResourceId(resourceId);
    MutableObject<PlatformType> platformType = new MutableObject<>();
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(okapiHeaders),
        (authToken, ucSettings) -> {
          if (platform == null) {
            platformType.setValue(ucSettings.getAttributes().getPlatformType());
          } else {
            platformType.setValue(PlatformType.valueOf(platform));
          }
          return createConfiguration(fiscalYear, ucSettings, authToken);
        })
      .thenCompose(configuration -> getTitleCost(id, configuration)
        .thenApply(ucTitleCostPerUse -> convert(ucTitleCostPerUse, platformType.getValue(), id, configuration))
      );
  }

  private ResourceCostPerUse convert(UCTitleCostPerUse ucTitleCostPerUse, PlatformType platformType, ResourceId resourceId,
                                     GetTitleUCConfiguration configuration) {
    ResourceCostPerUse resourceCostPerUse = converter.convert(ucTitleCostPerUse, platformType);
    resourceCostPerUse.getAttributes().setParameters(new CostPerUseParameters()
      .withCurrency(configuration.getAnalysisCurrency())
      .withStartMonth(Month.fromValue(configuration.getFiscalMonth()))
    );
    return resourceCostPerUse.withResourceId(resourceId.toString());
  }

  private CompletableFuture<UCTitleCostPerUse> getTitleCost(ResourceId id, GetTitleUCConfiguration configuration) {
    return client.getTitleCostPerUse(valueOf(id.getTitleIdPart()), valueOf(id.getPackageIdPart()), configuration);
  }

  private GetTitleUCConfiguration createConfiguration(int fiscalYear, UCSettings ucSettings, String authToken) {
    return GetTitleUCConfiguration.builder()
      .accessToken(authToken)
      .customerKey(ucSettings.getAttributes().getCustomerKey())
      .analysisCurrency(ucSettings.getAttributes().getCurrency())
      .fiscalMonth(ucSettings.getAttributes().getStartMonth().value())
      .fiscalYear(valueOf(fiscalYear))
      .aggregatedFullText(true)
      .build();
  }
}
