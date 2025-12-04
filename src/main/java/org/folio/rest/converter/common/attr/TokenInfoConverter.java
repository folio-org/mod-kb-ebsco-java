package org.folio.rest.converter.common.attr;

import java.util.Objects;
import org.folio.holdingsiq.model.TokenInfo;
import org.folio.rest.jaxrs.model.Token;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TokenInfoConverter implements Converter<TokenInfo, Token> {

  @Override
  public Token convert(@Nullable TokenInfo tokenInfo) {
    if (Objects.isNull(tokenInfo)) {
      return null;
    }
    return new Token()
      .withFactName(tokenInfo.getFactName())
      .withHelpText(tokenInfo.getHelpText())
      .withPrompt(tokenInfo.getPrompt())
      .withValue(tokenInfo.getValue() == null ? null : (String) tokenInfo.getValue());
  }
}
