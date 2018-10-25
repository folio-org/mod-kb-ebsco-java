package org.folio.properties;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class PropertyConfiguration {
  private static final String CLASSPATH_PROPERTY_FILE_NAME = "application.properties";
  private static final String EXTERNAL_CONFIGURATION_FILE_PROPERTY = "config.file";
  private static final PropertyConfiguration INSTANCE = new PropertyConfiguration();

  private Configuration configuration;

  private PropertyConfiguration() {
    try
    {
      Configurations configs = new Configurations();
      String externalConfigFile = System.getProperty(EXTERNAL_CONFIGURATION_FILE_PROPERTY);
      if(externalConfigFile != null){
        configuration = configs.properties(new File(externalConfigFile));
      }
      else{
        configuration = configs.properties(PropertyConfiguration.class.getClassLoader()
          .getResource(CLASSPATH_PROPERTY_FILE_NAME));
      }
    }
    catch (ConfigurationException ex)
    {
      throw new IllegalStateException("Failed to read properties", ex);
    }
  }

  public static PropertyConfiguration getInstance(){
    return INSTANCE;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
