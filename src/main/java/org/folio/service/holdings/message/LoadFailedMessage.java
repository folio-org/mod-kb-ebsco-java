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
public class LoadFailedMessage {
  private Configuration configuration;
  private String errorMessage;
  private String credentialsId;
  private String tenantId;
  private String transactionId;
  private Integer totalCount;
  private Integer totalPages;

  public LoadFailedMessage() { }

  public LoadFailedMessage(JsonObject jsonObject) {
    LoadFailedMessage message = jsonObject.mapTo(LoadFailedMessage.class);
    this.errorMessage = message.getErrorMessage();
    this.tenantId = message.getTenantId();
    this.credentialsId = message.getCredentialsId();
    this.transactionId = message.getTransactionId();
    this.totalCount = message.getTotalCount();
    this.totalPages = message.getTotalPages();
    this.configuration = message.getConfiguration();
  }

  public JsonObject toJson() {
    return mapFrom(this);
  }
}
