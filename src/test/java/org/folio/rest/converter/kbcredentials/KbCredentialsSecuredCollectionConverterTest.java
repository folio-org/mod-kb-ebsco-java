package org.folio.rest.converter.kbcredentials;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import static org.folio.util.KbCredentialsTestUtil.STUB_API_URL;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.STUB_CUSTOMER_ID;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsCollection;
import static org.folio.util.KbCredentialsTestUtil.getCredentialsCollectionNoUrl;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.spring.config.TestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class KbCredentialsSecuredCollectionConverterTest {

  @Autowired
  private KbCredentialsCollectionConverter.SecuredKbCredentialsCollectionConverter securedConverter;
  @Value("${kb.ebsco.credentials.url.default}")
  private String defaultUrl;

  @Test
  public void shouldConvertKbCredentialsCollectionWithDefaultUrl() {
    Collection<DbKbCredentials> credentialsCollection = getCredentialsCollectionNoUrl();
    final KbCredentialsCollection kbCredentials = securedConverter.convert(credentialsCollection);
    assertThat(kbCredentials.getMeta().getTotalResults(), equalTo(1));
    assertThat(kbCredentials.getData().get(0).getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(kbCredentials.getData().get(0).getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(kbCredentials.getData().get(0).getAttributes().getApiKey(), containsString("*"));
    assertThat(kbCredentials.getData().get(0).getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(kbCredentials.getData().get(0).getAttributes().getUrl(), equalTo(defaultUrl));
  }

  @Test
  public void shouldConvertKbCredentialsCollection() {
    Collection<DbKbCredentials> credentialsCollection = getCredentialsCollection();
    final KbCredentialsCollection kbCredentials = securedConverter.convert(credentialsCollection);
    assertThat(kbCredentials.getMeta().getTotalResults(), equalTo(1));
    assertThat(kbCredentials.getData().get(0).getType(), equalTo(KbCredentials.Type.KB_CREDENTIALS));
    assertThat(kbCredentials.getData().get(0).getAttributes().getName(), equalTo(STUB_CREDENTIALS_NAME));
    assertThat(kbCredentials.getData().get(0).getAttributes().getApiKey(), containsString("*"));
    assertThat(kbCredentials.getData().get(0).getAttributes().getCustomerId(), equalTo(STUB_CUSTOMER_ID));
    assertThat(kbCredentials.getData().get(0).getAttributes().getUrl(), equalTo(STUB_API_URL));
  }
}
