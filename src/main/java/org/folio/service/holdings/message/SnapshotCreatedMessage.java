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
public class SnapshotCreatedMessage {
  private Configuration configuration;
  private String transactionId;
  private Integer totalCount;
  private Integer totalPages;
  private String credentialsId;
  private String tenantId;

  public SnapshotCreatedMessage() { }

  public SnapshotCreatedMessage(JsonObject jsonObject) {
    SnapshotCreatedMessage message = jsonObject.mapTo(SnapshotCreatedMessage.class);
    this.totalCount = message.getTotalCount();
    this.transactionId = message.getTransactionId();
    this.totalPages = message.getTotalPages();
    this.tenantId = message.getTenantId();
    this.configuration = message.getConfiguration();
    this.credentialsId = message.getCredentialsId();
  }

  public JsonObject toJson() {
    return mapFrom(this);
  }
}
