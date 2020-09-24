package org.folio.spring.config;

import static org.folio.rest.util.ExceptionMappers.error400BadRequestMapper;
import static org.folio.rest.util.ExceptionMappers.error400ConstraintViolationMapper;
import static org.folio.rest.util.ExceptionMappers.error400DatabaseMapper;
import static org.folio.rest.util.ExceptionMappers.error401AuthorizationMapper;
import static org.folio.rest.util.ExceptionMappers.error401NotAuthorizedMapper;
import static org.folio.rest.util.ExceptionMappers.error404NotFoundMapper;
import static org.folio.rest.util.ExceptionMappers.error409ProcessInProgressMapper;
import static org.folio.rest.util.ExceptionMappers.error422ConfigurationInvalidMapper;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.ExceptionMappers.errorServiceResponseMapper;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;

import org.folio.cache.VertxCache;
import org.folio.config.ModConfiguration;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.db.exc.AuthorizationException;
import org.folio.db.exc.ConstraintViolationException;
import org.folio.db.exc.DatabaseException;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.db.exc.translation.DBExceptionTranslatorFactory;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.holdingsiq.service.exception.ConfigurationInvalidException;
import org.folio.holdingsiq.service.exception.ServiceResponseException;
import org.folio.holdingsiq.service.impl.ConfigurationServiceCache;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.holdingsiq.service.validator.TitleParametersValidator;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.repository.uc.DbUCSettings;
import org.folio.repository.uc.UCSettingsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.CurrencyCollection;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.util.ErrorHandler;
import org.folio.rmapi.LocalConfigurationServiceImpl;
import org.folio.rmapi.cache.PackageCacheKey;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.cache.TitleCacheKey;
import org.folio.rmapi.cache.VendorCacheKey;
import org.folio.service.holdings.LoadServiceFacade;
import org.folio.service.holdings.exception.ProcessInProgressException;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.folio.service.kbcredentials.KbCredentialsServiceImpl;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.folio.service.kbcredentials.UserKbCredentialsServiceImpl;
import org.folio.service.uc.UCSettingsService;
import org.folio.service.uc.UCSettingsServiceImpl;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.rest.converter",
  "org.folio.rest.validator",
  "org.folio.rest.util.template",
  "org.folio.repository",
  "org.folio.service",
  "org.folio.client",
  "org.folio.common"})
public class ApplicationConfig {

  @Bean
  public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setLocation(new ClassPathResource("application.properties"));
    return configurer;
  }

  @Bean
  public ConversionService conversionService(List<Converter<?, ?>> converters) {
    DefaultConversionService conversionService = new DefaultConversionService();
    converters.forEach(conversionService::addConverter);
    return conversionService;
  }

  @Bean
  public VertxCache<String, org.folio.holdingsiq.model.Configuration> rmApiConfigurationCache(Vertx vertx,
      @Value("${configuration.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "rmApiConfigurationCache");
  }

  @Bean
  public VertxCache<VendorIdCacheKey, Long> vendorIdCache(Vertx vertx,
      @Value("${vendor.id.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "vendorIdCache");
  }

  @Bean
  public VertxCache<PackageCacheKey, PackageByIdData> packageCache(Vertx vertx,
      @Value("${package.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "packageCache");
  }

  @Bean
  public VertxCache<VendorCacheKey, VendorById> vendorCache(Vertx vertx,
      @Value("${vendor.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "vendorCache");
  }

  @Bean
  public VertxCache<TitleCacheKey, Title> titleCache(Vertx vertx, @Value("${title.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "titleCache");
  }

  @Bean
  public VertxCache<ResourceCacheKey, Title> resourceCache(Vertx vertx,
      @Value("${resource.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "resourceCache");
  }

  @Bean
  public VertxCache<String, CurrencyCollection> currenciesCache(Vertx vertx,
                                                                @Value("${currencies.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "currenciesCache");
  }

  @Bean
  public VertxCache<String, String> ucTokenCache(Vertx vertx,
                                                 @Value("${uc.token.cache.expire}") long expirationTime) {
    return new VertxCache<>(vertx, expirationTime, "ucTokenCache");
  }

  @Bean
  public ConfigurationService configurationService(
      @Qualifier("nonSecuredUserCredentialsService") UserKbCredentialsService userKbCredentialsService,
      Converter<KbCredentials, org.folio.holdingsiq.model.Configuration> converter, Vertx vertx,
      @Value("${configuration.cache.expire}") long expirationTime) {
    return new ConfigurationServiceCache(new LocalConfigurationServiceImpl(userKbCredentialsService, converter, vertx),
        new VertxCache<>(vertx, expirationTime, "rmApiConfigurationCache"));
  }

  @Bean
  public TitleParametersValidator titleParametersValidator() {
    return new TitleParametersValidator();
  }

  @Bean
  public PackageParametersValidator packageParametersValidator() {
    return new PackageParametersValidator();
  }

  @Bean
  public LoadServiceFacade loadServiceFacade(@Value("${holdings.load.implementation.qualifier}") String qualifier,
      ApplicationContext context) {
    return (LoadServiceFacade) context.getBean(qualifier);
  }

  @Bean
  public DBExceptionTranslator excTranslator(@Value("${db.exception.translator.name}") String translatorName) {
    DBExceptionTranslatorFactory factory = DBExceptionTranslatorFactory.instance();
    return factory.create(translatorName);
  }

  @Bean
  public ErrorHandler errorHandler() {
    return new ErrorHandler()
      .add(ConstraintViolationException.class, error400ConstraintViolationMapper())
      .add(BadRequestException.class, error400BadRequestMapper())
      .add(NotFoundException.class, error404NotFoundMapper())
      .add(NotAuthorizedException.class, error401NotAuthorizedMapper())
      .add(AuthorizationException.class, error401AuthorizationMapper())
      .add(DatabaseException.class, error400DatabaseMapper())
      .add(InputValidationException.class, error422InputValidationMapper())
      .add(ConfigurationInvalidException.class, error422ConfigurationInvalidMapper())
      .add(ServiceResponseException.class, errorServiceResponseMapper());
  }

  @Bean
  public ErrorHandler loadHoldingsErrorHandler() {
    return new ErrorHandler()
      .add(ConstraintViolationException.class, error400ConstraintViolationMapper())
      .add(NotFoundException.class, error404NotFoundMapper())
      .add(NotAuthorizedException.class, error401NotAuthorizedMapper())
      .add(AuthorizationException.class, error401AuthorizationMapper())
      .add(ProcessInProgressException.class, error409ProcessInProgressMapper())
      .add(ConfigurationInvalidException.class, error422ConfigurationInvalidMapper())
      .add(DatabaseException.class, error400DatabaseMapper())
      .add(ServiceResponseException.class, errorServiceResponseMapper());

  }
  @Bean
  public org.folio.config.Configuration configuration(@Value("${kb.ebsco.java.configuration.module}") String module) {
    return new ModConfiguration(module);
  }

  @Bean
  public UserKbCredentialsService securedUserCredentialsService(KbCredentialsRepository credentialsRepository,
      AssignedUserRepository assignedUserRepository,
      @Qualifier("secured") Converter<DbKbCredentials, KbCredentials> converter) {
    return new UserKbCredentialsServiceImpl(credentialsRepository, assignedUserRepository, converter);
  }

  @Bean("securedUCSettingsService")
  public UCSettingsService securedUCSettingsService(@Qualifier("securedUCSettingsConverter") Converter<DbUCSettings, UCSettings> converter,
                                                    UCSettingsRepository repository) {
    return new UCSettingsServiceImpl(repository, converter);
  }

  @Bean("nonSecuredUCSettingsService")
  public UCSettingsService nonSecuredUCSettingsService(@Qualifier("nonSecuredUCSettingsConverter") Converter<DbUCSettings, UCSettings> converter,
                                                       UCSettingsRepository repository) {
    return new UCSettingsServiceImpl(repository, converter);
  }

  @Bean("securedCredentialsService")
  public KbCredentialsService securedCredentialsService(
      @Qualifier("secured") Converter<DbKbCredentials, KbCredentials> converter,
      @Qualifier("securedUserCredentialsService") UserKbCredentialsService userKbCredentialsService,
      @Qualifier("securedCredentialsCollection") Converter<Collection<DbKbCredentials>, KbCredentialsCollection> credentialsCollectionConverter) {
    return new KbCredentialsServiceImpl(converter, userKbCredentialsService, credentialsCollectionConverter);
  }

  @Bean
  public UserKbCredentialsService nonSecuredUserCredentialsService(KbCredentialsRepository repository,
      AssignedUserRepository assignedUserRepository,
      @Qualifier("nonSecured") Converter<DbKbCredentials, KbCredentials> converter) {
    return new UserKbCredentialsServiceImpl(repository, assignedUserRepository, converter);
  }

  @Bean("nonSecuredCredentialsService")
  public KbCredentialsService nonSecuredCredentialsService(
      @Qualifier("nonSecured") Converter<DbKbCredentials, KbCredentials> converter,
      @Qualifier("nonSecuredUserCredentialsService") UserKbCredentialsService userKbCredentialsService,
      @Qualifier("nonSecuredCredentialsCollection") Converter<Collection<DbKbCredentials>, KbCredentialsCollection> credentialsCollectionConverter) {
    return new KbCredentialsServiceImpl(converter, userKbCredentialsService, credentialsCollectionConverter);
  }
}
