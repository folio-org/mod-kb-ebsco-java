package org.folio.service.holdings;

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
public class ConfigurationMessage {
  private Configuration configuration;
  private String tenantId;

  public ConfigurationMessage() {}

  public ConfigurationMessage(JsonObject jsonObject) {
    ConfigurationMessage message = jsonObject.mapTo(ConfigurationMessage.class);
    this.configuration = message.getConfiguration();
    this.tenantId = message.getTenantId();
  }

  public JsonObject toJson(){
    return mapFrom(this);
  }
}
