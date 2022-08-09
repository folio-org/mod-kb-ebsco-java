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
public class DeltaReportCreatedMessage {
  private Configuration configuration;
  private Integer totalCount;
  private Integer totalPages;
  private String tenantId;
  private String credentialsId;

  public DeltaReportCreatedMessage() {
  }

  public DeltaReportCreatedMessage(JsonObject jsonObject) {
    DeltaReportCreatedMessage message = jsonObject.mapTo(DeltaReportCreatedMessage.class);
    this.totalCount = message.getTotalCount();
    this.totalPages = message.getTotalPages();
    this.tenantId = message.getTenantId();
    this.configuration = message.getConfiguration();
    this.credentialsId = message.getCredentialsId();
  }

  public JsonObject toJson() {
    return mapFrom(this);
  }
}
