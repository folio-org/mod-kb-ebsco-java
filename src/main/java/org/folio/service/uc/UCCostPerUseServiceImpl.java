package org.folio.service.uc;

import static java.lang.String.valueOf;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

  private final UCAuthService authService;
  private final UCSettingsService settingsService;
  private final UCApigeeEbscoClient client;

  public UCCostPerUseServiceImpl(@Qualifier("nonSecuredUCSettingsService") UCSettingsService settingsService,
                                 UCAuthService authService, UCApigeeEbscoClient client) {
    this.settingsService = settingsService;
    this.authService = authService;
    this.client = client;
  }

  @Override
  public CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, int fiscalYear,
                                                                     Map<String, String> okapiHeaders) {
    ResourceId id = IdParser.parseResourceId(resourceId);

    return authService.authenticate(okapiHeaders)
      .thenCombine(settingsService.fetchByUser(okapiHeaders),
        (authToken, ucSettings) -> {
          PlatformType platformType;
          if (platform == null) {
            platformType = ucSettings.getAttributes().getPlatformType();
          } else {
            platformType = PlatformType.valueOf(platform);
          }
          switch (platformType) {
            case NON_PUBLISHER:
              return List.of(createConfiguration(true, fiscalYear, ucSettings, authToken));
            case PUBLISHER:
              return List.of(createConfiguration(false, fiscalYear, ucSettings, authToken));
            default:
              return List.of(createConfiguration(true, fiscalYear, ucSettings, authToken),
                createConfiguration(false, fiscalYear, ucSettings, authToken));
          }
        }).thenApply(configurations -> getTitleCosts(id, configurations))
      .thenCompose(futures ->
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
          .thenApply(unused -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
      ).thenCompose(this::combine);
  }

  private List<CompletableFuture<UCTitleCost>> getTitleCosts(ResourceId id, List<GetTitleUCConfiguration> configurations) {
    return configurations.stream()
      .map(configuration -> getTitleCost(id, configuration))
      .collect(Collectors.toList());
  }

  private CompletableFuture<ResourceCostPerUse> combine(List<UCTitleCost> ucTitleCosts) {
    return null;
  }

  private CompletableFuture<UCTitleCost> getTitleCost(ResourceId id, GetTitleUCConfiguration configuration) {
    return client.getTitleCost(valueOf(id.getTitleIdPart()), valueOf(id.getPackageIdPart()), configuration);
  }

  private GetTitleUCConfiguration createConfiguration(boolean platform, int fiscalYear, UCSettings ucSettings,
                                                      String authToken) {
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
