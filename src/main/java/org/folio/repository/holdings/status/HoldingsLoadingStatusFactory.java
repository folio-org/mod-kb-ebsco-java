package org.folio.repository.holdings.status;

import static org.folio.rest.util.DateTimeUtil.getTimeNow;
import static org.folio.rest.util.RestConstants.JSONAPI;

import java.util.List;
import org.folio.rest.jaxrs.model.HoldingsLoadingStatus;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;
import org.folio.rest.jaxrs.model.LoadStatusAttributes;
import org.folio.rest.jaxrs.model.LoadStatusData;
import org.folio.rest.jaxrs.model.LoadStatusInformation;
import org.folio.rest.jaxrs.model.LoadStatusNameDetailEnum;
import org.folio.rest.jaxrs.model.LoadStatusNameEnum;

public final class HoldingsLoadingStatusFactory {

  private HoldingsLoadingStatusFactory() { }

  public static HoldingsLoadingStatus getStatusNotStarted() {
    return new HoldingsLoadingStatus()
      .withData(new LoadStatusData()
        .withType(LoadStatusData.Type.STATUS)
        .withAttributes(new LoadStatusAttributes()
          .withStatus(new LoadStatusInformation()
            .withName(LoadStatusNameEnum.NOT_STARTED))
          .withUpdated(getTimeNow())
        ))
      .withJsonapi(JSONAPI);
  }

  public static HoldingsLoadingStatus getStatusPopulatingStagingArea() {
    return new HoldingsLoadingStatus()
      .withData(new LoadStatusData()
        .withType(LoadStatusData.Type.STATUS)
        .withAttributes(new LoadStatusAttributes()
          .withStarted(getTimeNow())
          .withStatus(new LoadStatusInformation()
            .withName(LoadStatusNameEnum.IN_PROGRESS)
            .withDetail(LoadStatusNameDetailEnum.POPULATING_STAGING_AREA))
          .withUpdated(getTimeNow())
          .withErrors(null)))
      .withJsonapi(JSONAPI);
  }

  public static HoldingsLoadingStatus getStatusLoadingHoldings(int totalCount, int importedCount, int totalPages,
                                                               int importedPages) {
    return new HoldingsLoadingStatus()
      .withData(new LoadStatusData()
        .withType(LoadStatusData.Type.STATUS)
        .withAttributes(new LoadStatusAttributes()
          .withStatus(new LoadStatusInformation()
            .withName(LoadStatusNameEnum.IN_PROGRESS)
            .withDetail(LoadStatusNameDetailEnum.LOADING_HOLDINGS))
          .withUpdated(getTimeNow())
          .withTotalCount(totalCount)
          .withImportedCount(importedCount)
          .withTotalPages(totalPages)
          .withImportedPages(importedPages))
      )
      .withJsonapi(JSONAPI);
  }

  public static HoldingsLoadingStatus getStatusCompleted(int totalCount) {
    return new HoldingsLoadingStatus()
      .withData(new LoadStatusData()
        .withType(LoadStatusData.Type.STATUS)
        .withAttributes(new LoadStatusAttributes()
          .withStatus(new LoadStatusInformation()
            .withName(LoadStatusNameEnum.COMPLETED))
          .withTotalCount(totalCount)
          .withUpdated(getTimeNow())
          .withFinished(getTimeNow())))
      .withJsonapi(JSONAPI);
  }

  public static HoldingsLoadingStatus getLoadStatusFailed(List<JsonapiErrorResponse> errors) {
    return new HoldingsLoadingStatus()
      .withData(new LoadStatusData()
        .withType(LoadStatusData.Type.STATUS)
        .withAttributes(new LoadStatusAttributes()
          .withStatus(new LoadStatusInformation()
            .withName(LoadStatusNameEnum.FAILED))
          .withUpdated(getTimeNow())
          .withErrors(errors)))
      .withJsonapi(JSONAPI);
  }
}
