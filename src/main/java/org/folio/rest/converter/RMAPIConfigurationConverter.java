package org.folio.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.folio.config.RMAPIConfiguration;
import org.folio.rest.jaxrs.model.EholdingsConfigurationPutApplicationVndApiJsonImpl;

/**
 * Converts internal RMAPIConfiguration object into object that will be returned in response
 */
public class RMAPIConfigurationConverter {
  public EholdingsConfigurationPutApplicationVndApiJsonImpl convert(RMAPIConfiguration rmAPIConfig) {
    EholdingsConfigurationPutApplicationVndApiJsonImpl jsonConfig = new EholdingsConfigurationPutApplicationVndApiJsonImpl();
    jsonConfig.setData(new EholdingsConfigurationPutApplicationVndApiJsonImpl.DataTypeImpl());
    jsonConfig.getData().setType("configurations");
    jsonConfig.getData().setAttributes(new EholdingsConfigurationPutApplicationVndApiJsonImpl.DataTypeImpl.AttributesTypeImpl());
    jsonConfig.getData().getAttributes().setApiKey(StringUtils.repeat("*", 40));
    jsonConfig.getData().getAttributes().setCustomerId(rmAPIConfig.getCustomerId());
    jsonConfig.getData().getAttributes().setRmapiBaseUrl(rmAPIConfig.getUrl());
    return jsonConfig;
  }
}
