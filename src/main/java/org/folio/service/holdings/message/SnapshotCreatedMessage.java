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
public class SnapshotCreatedMessage {
  private Configuration configuration;
  private Integer totalCount;
  private Integer totalPages;
  private String tenantId;

  public SnapshotCreatedMessage() {
  }

  public SnapshotCreatedMessage(JsonObject jsonObject) {
    SnapshotCreatedMessage message = jsonObject.mapTo(SnapshotCreatedMessage.class);
    this.totalCount = message.getTotalCount();
    this.totalPages = message.getTotalPages();
    this.tenantId = message.getTenantId();
    this.configuration = message.getConfiguration();
  }

  public JsonObject toJson() {
    return mapFrom(this);
  }
}
