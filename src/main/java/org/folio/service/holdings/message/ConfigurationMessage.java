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
public class ConfigurationMessage {
  private Configuration configuration;
  private String credentialsId;
  private String tenantId;

  public ConfigurationMessage() { }

  public ConfigurationMessage(JsonObject jsonObject) {
    ConfigurationMessage message = jsonObject.mapTo(ConfigurationMessage.class);
    this.configuration = message.getConfiguration();
    this.credentialsId = message.getCredentialsId();
    this.tenantId = message.getTenantId();
  }

  public JsonObject toJson() {
    return mapFrom(this);
  }
}
