package org.folio.service.holdings.message;

import static io.vertx.core.json.JsonObject.mapFrom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.folio.holdingsiq.model.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@DataObject
public class LoadHoldingsMessage {
  private Configuration configuration;
  private String credentialsId;
  private String tenantId;
  private Integer totalCount;
  private Integer totalPages;
  private String currentTransactionId;
  private String previousTransactionId;

  public LoadHoldingsMessage() {
  }

  public LoadHoldingsMessage(JsonObject jsonObject) {
    LoadHoldingsMessage message = jsonObject.mapTo(LoadHoldingsMessage.class);
    this.tenantId = message.getTenantId();
    this.credentialsId = message.getCredentialsId();
    this.totalCount = message.getTotalCount();
    this.totalPages = message.getTotalPages();
    this.currentTransactionId = message.getCurrentTransactionId();
    this.previousTransactionId = message.getPreviousTransactionId();
    this.configuration = message.getConfiguration();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
