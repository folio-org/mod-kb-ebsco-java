package org.folio.rest.aspect;

import org.folio.rest.annotations.RestValidator;

public aspect PrecedenceAspect
{
  declare precedence : ValidationErrorHandlerAspect, RestValidator;
}
