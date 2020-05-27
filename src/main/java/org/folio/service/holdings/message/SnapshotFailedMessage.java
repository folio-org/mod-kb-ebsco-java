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
public class SnapshotFailedMessage {
  private Configuration configuration;
  private String errorMessage;
  private String credentialsId;
  private String tenantId;

  public SnapshotFailedMessage() {
  }

  public SnapshotFailedMessage(JsonObject jsonObject) {
    SnapshotFailedMessage message = jsonObject.mapTo(SnapshotFailedMessage.class);
    this.errorMessage = message.getErrorMessage();
    this.tenantId = message.getTenantId();
    this.configuration = message.getConfiguration();
    this.credentialsId = message.getCredentialsId();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
