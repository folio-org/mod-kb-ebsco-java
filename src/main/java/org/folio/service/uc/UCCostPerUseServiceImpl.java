package org.folio.service.uc;

import static java.lang.String.valueOf;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.folio.client.uc.GetTitleUCConfiguration;
import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.model.UCTitleCost;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.util.IdParser;

@Service
public class UCCostPerUseServiceImpl implements UCCostPerUseService {

  @Autowired
  private UCAuthService authService;
  @Autowired
  @Qualifier("nonSecuredUCSettingsService")
  private UCSettingsService settingsService;
  @Autowired
  private UCApigeeEbscoClient client;
  @Autowired
  private ResourceCostPerUseConverter converter;

  @Override
  public CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, int fiscalYear,
                                                                     Map<String, String> okapiHeaders) {
    ResourceId id = IdParser.parseResourceId(resourceId);
    AtomicReference<PlatformType> platformType = new AtomicReference<>();
    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(okapiHeaders),
        (authToken, ucSettings) -> {
          if (platform == null) {
            platformType.set(ucSettings.getAttributes().getPlatformType());
          } else {
            platformType.set(PlatformType.valueOf(platform));
          }
          return createConfiguration(fiscalYear, ucSettings, authToken);
        })
      .thenCompose(configuration -> getTitleCost(id, configuration))
      .thenApply(ucTitleCost -> convert(ucTitleCost, platformType.get()));
  }

  private ResourceCostPerUse convert(UCTitleCost ucTitleCost, PlatformType platformType) {
    return converter.convert(ucTitleCost, platformType);
  }

  private CompletableFuture<UCTitleCost> getTitleCost(ResourceId id, GetTitleUCConfiguration configuration) {
    return client.getTitleCost(valueOf(id.getTitleIdPart()), valueOf(id.getPackageIdPart()), configuration);
  }

  private GetTitleUCConfiguration createConfiguration(int fiscalYear, UCSettings ucSettings, String authToken) {
    return GetTitleUCConfiguration.builder()
      .aggregatedFullText(true)
      .accessToken(authToken)
      .analysisCurrency(ucSettings.getAttributes().getCurrency())
      .customerKey(ucSettings.getAttributes().getCustomerKey())
      .fiscalMonth(ucSettings.getAttributes().getStartMonth().value())
      .fiscalYear(valueOf(fiscalYear))
      .build();
  }
}
