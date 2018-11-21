package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.Token;
import org.folio.rmapi.model.TokenInfo;

import java.util.Objects;

public class CommonAttributesConverter {

  public Token convertToken(TokenInfo tokenInfo) {
    if(Objects.isNull(tokenInfo)){
      return null;
    }
    return new Token()
      .withFactName(tokenInfo.getFactName())
      .withHelpText(tokenInfo.getHelpText())
      .withPrompt(tokenInfo.getPrompt())
      .withValue(tokenInfo.getValue() == null ? null : (String) tokenInfo.getValue());
  }
}
