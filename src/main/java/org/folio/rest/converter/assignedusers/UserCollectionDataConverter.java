package org.folio.rest.converter.assignedusers;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.Value;
import org.folio.rest.jaxrs.model.AssignedUser;
import org.folio.rest.jaxrs.model.AssignedUserCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.folio.service.users.Group;
import org.folio.service.users.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserCollectionDataConverter
  implements Converter<UserCollectionDataConverter.UsersResult, AssignedUserCollection> {

  private final Converter<User, AssignedUser> itemConverter;

  public UserCollectionDataConverter(Converter<User, AssignedUser> itemConverter) {
    this.itemConverter = itemConverter;
  }

  @Override
  public AssignedUserCollection convert(UsersResult source) {
    var assignedUserCollection = new AssignedUserCollection()
      .withData(mapItems(source.getUsers(), itemConverter::convert))
      .withMeta(new MetaTotalResults().withTotalResults(source.getUsers().size()))
      .withJsonapi(RestConstants.JSONAPI);
    var groupMap = source.getGroups().stream()
      .collect(Collectors.toMap(Group::getId, Group::getGroup));

    assignedUserCollection.getData().stream()
      .map(AssignedUser::getAttributes)
      .filter(assignedUserDataAttributes -> assignedUserDataAttributes.getPatronGroup() != null)
      .forEach(userData -> userData.setPatronGroup(groupMap.get(userData.getPatronGroup())));

    return assignedUserCollection;
  }

  @Value
  public static class UsersResult {
    Collection<User> users;
    Collection<Group> groups;
  }
}
