package org.folio.rest.converter.assignedusers;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collection;
import org.folio.repository.assigneduser.DbAssignedUser;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AssignedUserCollectionConverter implements Converter<Collection<DbAssignedUser>, AssignedUserCollection> {

  private final Converter<DbAssignedUser, AssignedUser> itemConverter;

  public AssignedUserCollectionConverter(Converter<DbAssignedUser, AssignedUser> itemConverter) {
    this.itemConverter = itemConverter;
  }

  @Override
  public AssignedUserCollection convert(Collection<DbAssignedUser> source) {
    return new AssignedUserCollection()
      .withData(mapItems(source, itemConverter::convert))
      .withMeta(new MetaTotalResults().withTotalResults(source.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
