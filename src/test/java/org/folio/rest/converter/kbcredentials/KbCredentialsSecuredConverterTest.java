package org.folio.rest.converter.kbcredentials;

import static org.folio.util.KbCredentialsTestUtil.API_URL;
import static org.folio.util.KbCredentialsTestUtil.CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.CUSTOMER_ID;
import static org.folio.util.KbCredentialsTestUtil.getCredentials;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsNoUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.spring.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class KbCredentialsSecuredConverterTest {

  @Autowired
  private KbCredentialsConverter.KbCredentialsFromDbSecuredConverter securedConverter;
  @Value("${kb.ebsco.credentials.url.default}")
  private String defaultUrl;

  @Test
  void shouldConvertKbCredentialsWithDefaultUrl() {
    var dbKbCredentials = getCredentialsNoUrl();
    var kbCredentials = securedConverter.convert(dbKbCredentials);
    assertEquals(KbCredentials.Type.KB_CREDENTIALS, kbCredentials.getType());
    assertEquals(CREDENTIALS_NAME, kbCredentials.getAttributes().getName());
    assertTrue(kbCredentials.getAttributes().getApiKey().contains("*"));
    assertEquals(CUSTOMER_ID, kbCredentials.getAttributes().getCustomerId());
    assertEquals(kbCredentials.getAttributes().getUrl(), defaultUrl);
  }

  @Test
  void shouldConvertKbCredentials() {
    var dbKbCredentials = getCredentials();
    var kbCredentials = securedConverter.convert(dbKbCredentials);
    assertEquals(KbCredentials.Type.KB_CREDENTIALS, kbCredentials.getType());
    assertEquals(CREDENTIALS_NAME, kbCredentials.getAttributes().getName());
    assertTrue(kbCredentials.getAttributes().getApiKey().contains("*"));
    assertEquals(CUSTOMER_ID, kbCredentials.getAttributes().getCustomerId());
    assertEquals(API_URL, kbCredentials.getAttributes().getUrl());
  }
}
