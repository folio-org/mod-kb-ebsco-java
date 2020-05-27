package org.folio.service.holdings.message;

import static io.vertx.core.json.JsonObject.mapFrom;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.folio.holdingsiq.model.HoldingInReport;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@DataObject
public class DeltaReportMessage {
  private List<HoldingInReport> holdingList;
  private String tenantId;
  private String transactionId;
  private String credentialsId;

  private DeltaReportMessage() { }

  public DeltaReportMessage(JsonObject jsonObject) {
    DeltaReportMessage message = jsonObject.mapTo(DeltaReportMessage.class);
    this.holdingList = message.getHoldingList();
    this.tenantId = message.getTenantId();
    this.transactionId = message.getTransactionId();
    this.credentialsId = message.getCredentialsId();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
