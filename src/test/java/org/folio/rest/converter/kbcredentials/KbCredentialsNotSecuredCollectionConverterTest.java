package org.folio.rest.converter.kbcredentials;

import static org.folio.util.KbCredentialsTestUtil.STUB_API_KEY;
import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_CUSTOMER_ID;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsCollection;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsCollectionNoUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collection;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.spring.config.TestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class KbCredentialsNotSecuredCollectionConverterTest {

  @Autowired
  @Qualifier("nonSecuredCredentialsCollection")
  private KbCredentialsCollectionConverter.NonSecuredKbCredentialsCollectionConverter nonSecuredConverter;
  @Value("${kb.ebsco.credentials.url.default}")
  private String defaultUrl;

  @Test
  public void shouldConvertKbCredentialsCollectionWithDefaultUrl() {
    Collection<DbKbCredentials> credentialsCollection = getCredentialsCollectionNoUrl();
    final KbCredentialsCollection kbCredentials = nonSecuredConverter.convert(credentialsCollection);
    assertThat(kbCredentials, notNullValue());
    assertThat(kbCredentials.getMeta().getTotalResults(), equalTo(1));
    var credentials = kbCredentials.getData().getFirst();
    assertThat(credentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(credentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(credentials.getAttributes().getApiKey(), equalTo(STUB_API_KEY));
    assertThat(credentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(credentials.getAttributes().getUrl(), equalTo(defaultUrl));
  }

  @Test
  public void shouldConvertKbCredentialsCollection() {
    Collection<DbKbCredentials> credentialsCollection = getCredentialsCollection();
    final KbCredentialsCollection kbCredentials = nonSecuredConverter.convert(credentialsCollection);
    assertThat(kbCredentials, notNullValue());
    assertThat(kbCredentials.getMeta().getTotalResults(), equalTo(1));
    var credentials = kbCredentials.getData().getFirst();
    assertThat(credentials.getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(credentials.getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(credentials.getAttributes().getApiKey(), equalTo(STUB_API_KEY));
    assertThat(credentials.getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(credentials.getAttributes().getUrl(), equalTo(STUB_API_URL));
  }
}
