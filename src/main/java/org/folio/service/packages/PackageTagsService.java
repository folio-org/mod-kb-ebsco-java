package org.folio.service.packages;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.util.IdParser.packageIdToString;
import static org.folio.rest.util.IdParser.parsePackageId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.PackageId;
import org.folio.repository.RecordType;
import org.folio.repository.packages.DbPackage;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.tag.DbTag;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.result.TitleResult;
import org.springframework.stereotype.Service;

/**
 * Encapsulates local storage of packages and their tags, hiding {@link PackageRepository}
 * and {@link TagRepository} from {@link PackageService}.
 */
@Service
class PackageTagsService {

  private final PackageRepository packageRepository;
  private final TagRepository tagRepository;

  PackageTagsService(PackageRepository packageRepository, TagRepository tagRepository) {
    this.packageRepository = packageRepository;
    this.tagRepository = tagRepository;
  }

  CompletableFuture<Void> updatePackageTags(String packageId, UUID credentialsId,
                                            PackageTagsDataAttributes attributes, String tenant) {
    Tags tags = attributes.getTags();
    if (tags == null) {
      return completedFuture(null);
    }
    DbPackage dbPackage = createDbPackage(packageId, credentialsId, attributes);
    PackageId id = dbPackage.getId();
    return updateStoredPackage(tags, dbPackage, tenant)
      .thenCompose(
        o -> tagRepository.updateRecordTags(tenant, packageIdToString(id), RecordType.PACKAGE, tags.getTagList()))
      .thenApply(updated -> null);
  }

  CompletableFuture<Void> deletePackageTags(PackageId packageId, UUID credentialsId, String tenant) {
    return packageRepository.delete(packageId, credentialsId, tenant)
      .thenCompose(o -> tagRepository.deleteRecordTags(tenant, packageIdToString(packageId), RecordType.PACKAGE))
      .thenCompose(v -> completedFuture(null));
  }

  CompletableFuture<Void> loadResourceTags(String tenant, Map<String, TitleResult> resourceIdToTitle) {
    return tagRepository.findPerRecord(tenant, new ArrayList<>(resourceIdToTitle.keySet()), RecordType.RESOURCE)
      .thenAccept(tagMap -> populateResourceTags(resourceIdToTitle, tagMap));
  }

  private void populateResourceTags(Map<String, TitleResult> resourceIdToTitle, Map<String, List<DbTag>> tagMap) {
    tagMap.forEach((id, tags) -> resourceIdToTitle.get(id).setResourceTagList(tags));
  }

  private CompletableFuture<Void> updateStoredPackage(Tags tags, DbPackage pkg, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return packageRepository.save(pkg, tenant);
    }
    return packageRepository.delete(pkg.getId(), pkg.getCredentialsId(), tenant);
  }

  private DbPackage createDbPackage(String packageId, UUID credentialsId, PackageTagsDataAttributes attributes) {
    return DbPackage.builder()
      .id(parsePackageId(packageId))
      .credentialsId(credentialsId)
      .name(attributes.getName())
      .contentType(ConverterConsts.CONTENT_TYPES.inverseBidiMap().get(attributes.getContentType()))
      .build();
  }
}
