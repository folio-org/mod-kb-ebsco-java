package org.folio.rmapi.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = Proxies.Deserializer.class)
public class Proxies {

  private List<ProxyWithUrl> proxyList;

  
  public static class Deserializer extends JsonDeserializer<Proxies> {

    @Override
    public Proxies deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      List<ProxyWithUrl> proxyList = new ArrayList<>();

      Iterator<List<ProxyWithUrl>> itr = p.readValuesAs(new TypeReference<List<ProxyWithUrl>>() {});
      itr.forEachRemaining(proxyList::addAll);

      return new Proxies(proxyList);
    }
  }
}
