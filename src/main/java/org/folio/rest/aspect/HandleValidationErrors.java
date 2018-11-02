package org.folio.rest.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * If method annotated by @HandleValidationErrors throws ValidationException
 * exception will be caught and 400 error will be returned with exception message
 * wrapped into json according to jsonapi.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandleValidationErrors {
}
