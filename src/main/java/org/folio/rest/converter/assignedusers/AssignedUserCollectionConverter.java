package org.folio.rest.converter.assignedusers;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collection;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AssignedUserCollectionConverter implements Converter<Collection<DbAssignedUser>, AssignedUserCollection> {

  @Autowired
  private Converter<DbAssignedUser, AssignedUser> itemConverter;

  @Override
  public AssignedUserCollection convert(@NotNull Collection<DbAssignedUser> source) {
    return new AssignedUserCollection()
      .withData(mapItems(source, itemConverter::convert))
      .withMeta(new MetaTotalResults().withTotalResults(source.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
