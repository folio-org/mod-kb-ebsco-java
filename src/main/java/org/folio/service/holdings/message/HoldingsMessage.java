package org.folio.service.holdings.message;

import static io.vertx.core.json.JsonObject.mapFrom;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.folio.holdingsiq.model.Holding;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@DataObject
public class HoldingsMessage {
  private List<Holding> holdingList;
  private String tenantId;
  private String transactionId;
  private String credentialsId;

  public HoldingsMessage() {}

  public HoldingsMessage(JsonObject jsonObject) {
    HoldingsMessage message = jsonObject.mapTo(HoldingsMessage.class);
    this.holdingList = message.getHoldingList();
    this.tenantId = message.getTenantId();
    this.transactionId = message.getTransactionId();
    this.credentialsId = message.getCredentialsId();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
