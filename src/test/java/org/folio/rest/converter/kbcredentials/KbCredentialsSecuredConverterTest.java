package org.folio.rest.converter.kbcredentials;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_CUSTOMER_ID;
import static org.folio.util.KbCredentialsTestUtil.getCredentials;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsNoUrl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class KbCredentialsSecuredConverterTest {

  @Autowired
  private KbCredentialsConverter.KbCredentialsFromDbSecuredConverter securedConverter;
  @Value("${kb.ebsco.credentials.url.default}")
  private String defaultUrl;

  @Test
  public void shouldConvertKbCredentialsWithDefaultUrl() {
    DbKbCredentials holding = getCredentialsNoUrl();
    final KbCredentials kbCredentials = securedConverter.convert(holding);
    assertThat(kbCredentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(kbCredentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(kbCredentials.getAttributes().getApiKey(), containsString("*"));
    assertThat(kbCredentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(kbCredentials.getAttributes().getUrl(), equalTo(defaultUrl));
  }

  @Test
  public void shouldConvertKbCredentials() {
    DbKbCredentials holding = getCredentials();
    final KbCredentials kbCredentials = securedConverter.convert(holding);
    assertThat(kbCredentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(kbCredentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(kbCredentials.getAttributes().getApiKey(), containsString("*"));
    assertThat(kbCredentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(kbCredentials.getAttributes().getUrl(), equalTo(STUB_API_URL));
  }

}
