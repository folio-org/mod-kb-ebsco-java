package org.folio.rest.converter.assignedusers;



import static org.folio.common.ListUtils.mapItems;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.folio.service.users.User;

@Component
public class UserCollectionDataConverter implements Converter<Collection<User>, AssignedUserCollection> {

  @Autowired
  private Converter<User, AssignedUser> itemConverter;

  @Override
  public AssignedUserCollection convert(Collection<User> source) {
    return new AssignedUserCollection()
      .withData(mapItems(source, user -> itemConverter.convert(user)))
      .withMeta(new MetaTotalResults().withTotalResults(source.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }
}