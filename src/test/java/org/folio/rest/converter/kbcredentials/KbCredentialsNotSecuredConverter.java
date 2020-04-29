package org.folio.rest.converter.kbcredentials;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import static org.folio.util.KbCredentialsTestUtil.*;

import org.folio.spring.config.TestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class KbCredentialsNotSecuredConverter {

  @Autowired
  @Qualifier("non-secured")
  private KbCredentialsConverter.KbCredentialsFromDbNonSecuredConverter notSecuredConverter;
  @Value("${kb.ebsco.credentials.url.default}")
  private String defaultUrl;

  @Test
  public void shouldConvertKbCredentialsWithDefaultUrl() {
    DbKbCredentials holding = getCredentialsNoUrl();
    final KbCredentials kbCredentials = notSecuredConverter.convert(holding);
    assertThat(kbCredentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(kbCredentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(kbCredentials.getAttributes().getApiKey(), equalTo(STUB_API_KEY));
    assertThat(kbCredentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(kbCredentials.getAttributes().getUrl(), equalTo(defaultUrl));
  }

  @Test
  public void shouldConvertKbCredentials() {
    DbKbCredentials holding = getCredentials();
    final KbCredentials kbCredentials = notSecuredConverter.convert(holding);
    assertThat(kbCredentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(kbCredentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(kbCredentials.getAttributes().getApiKey(), equalTo(STUB_API_KEY));
    assertThat(kbCredentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(kbCredentials.getAttributes().getUrl(), equalTo(STUB_API_URL));
  }


}
