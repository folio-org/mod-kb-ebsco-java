package org.folio.rest.converter.kbcredentials;

import static org.folio.util.KbCredentialsTestUtil.API_URL;
import static org.folio.util.KbCredentialsTestUtil.CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.CUSTOMER_ID;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsCollection;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsCollectionNoUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class KbCredentialsSecuredCollectionConverterTest {

  @Autowired
  private KbCredentialsCollectionConverter.SecuredKbCredentialsCollectionConverter securedConverter;
  @Value("${kb.ebsco.credentials.url.default}")
  private String defaultUrl;

  @Test
  void shouldConvertKbCredentialsCollectionWithDefaultUrl() {
    var credentialsCollection = getCredentialsCollectionNoUrl();
    var kbCredentials = securedConverter.convert(credentialsCollection);
    assertNotNull(kbCredentials);
    assertEquals(1, kbCredentials.getMeta().getTotalResults());
    var credentials = kbCredentials.getData().getFirst();
    assertEquals(KbCredentials.Type.KB_CREDENTIALS, credentials.getType());
    assertEquals(CREDENTIALS_NAME, credentials.getAttributes().getName());
    assertTrue(credentials.getAttributes().getApiKey().contains("*"));
    assertEquals(CUSTOMER_ID, credentials.getAttributes().getCustomerId());
    assertEquals(credentials.getAttributes().getUrl(), defaultUrl);
  }

  @Test
  void shouldConvertKbCredentialsCollection() {
    var credentialsCollection = getCredentialsCollection();
    var kbCredentials = securedConverter.convert(credentialsCollection);
    assertNotNull(kbCredentials);
    assertEquals(1, kbCredentials.getMeta().getTotalResults());
    var credentials = kbCredentials.getData().getFirst();
    assertEquals(KbCredentials.Type.KB_CREDENTIALS, credentials.getType());
    assertEquals(CREDENTIALS_NAME, credentials.getAttributes().getName());
    assertTrue(credentials.getAttributes().getApiKey().contains("*"));
    assertEquals(CUSTOMER_ID, credentials.getAttributes().getCustomerId());
    assertEquals(API_URL, credentials.getAttributes().getUrl());
  }
}
