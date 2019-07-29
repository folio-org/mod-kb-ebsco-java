package org.folio.service.holdings;


import static io.vertx.core.json.JsonObject.mapFrom;

import java.util.List;

import org.folio.holdingsiq.model.Holding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(
  ignoreUnknown = true
)
@Getter
@Setter
@AllArgsConstructor
@DataObject
public class HoldingsMessage {
  private List<Holding> holdingList;
  private String tenantId;

  public HoldingsMessage() {}

  public HoldingsMessage(JsonObject jsonObject) {
    HoldingsMessage message = jsonObject.mapTo(HoldingsMessage.class);
    this.holdingList = message.getHoldingList();
    this.tenantId = message.getTenantId();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
