package org.folio.service.holdings.message;


import static io.vertx.core.json.JsonObject.mapFrom;

import org.folio.holdingsiq.model.Configuration;

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
public class LoadHoldingsMessage {
  private Configuration configuration;
  private String tenantId;
  private Integer totalCount;
  private Integer totalPages;

  public LoadHoldingsMessage() {
  }

  public LoadHoldingsMessage(JsonObject jsonObject) {
    LoadHoldingsMessage message = jsonObject.mapTo(LoadHoldingsMessage.class);
    this.tenantId = message.getTenantId();
    this.totalCount = message.getTotalCount();
    this.totalPages = message.getTotalPages();
    this.configuration = message.getConfiguration();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
