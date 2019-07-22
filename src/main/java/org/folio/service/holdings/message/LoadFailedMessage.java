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
public class LoadFailedMessage {
  private Configuration configuration;
  private String errorMessage;
  private String tenantId;

  public LoadFailedMessage() {
  }

  public LoadFailedMessage(JsonObject jsonObject) {
    LoadFailedMessage message = jsonObject.mapTo(LoadFailedMessage.class);
    this.errorMessage = message.getErrorMessage();
    this.tenantId = message.getTenantId();
    this.configuration = message.getConfiguration();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
