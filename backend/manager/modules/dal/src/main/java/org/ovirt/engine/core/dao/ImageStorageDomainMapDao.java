package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap;
import org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMapId;
import org.ovirt.engine.core.compat.Guid;

/**
 * Interface for having DB related operations on {@link org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap} entities
 *
 */
public interface ImageStorageDomainMapDao extends GenericDao<ImageStorageDomainMap, ImageStorageDomainMapId> {

    /**
     * Removes the {@link org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap} entries that have the given image Id
     *
     * @param imageId
     *            Id of {@link DiskImage} that the removed entries were created for
     */
    void remove(Guid imageId);

    /**
     * Gets a list of {@link org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap} entries that have the given id of {@link StorageDomain}
     *
     * @param storageDomainId
     *            ID of {@link StorageDomain} entity that the returned entities are associated with
     * @return list of entities
     */
    List<ImageStorageDomainMap> getAllByStorageDomainId(Guid storageDomainId);

    /**
     * Gets a list of {@link org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap} entries that have the given id of {@link DiskImage} entity
     *
     * @param imageId
     *            ID of {@link DiskImage} entity that the returned entities are associated with
     * @return list of entities
     */
    List<ImageStorageDomainMap> getAllByImageId(Guid imageId);

    /**
     * updates images quota of a specific disk on a specific storage domain
     *
     * @param diskId
     * @param storageDomainId
     * @param quotaId
     */
    void updateQuotaForImageAndSnapshots(Guid diskId, Guid storageDomainId, Guid quotaId);

    /**
     * updates images disk profile of a specific disk on a specific storage domain
     *
     * @param diskId
     * @param storageDomainId
     * @param diskProfileId
     */
    void updateDiskProfileByImageGroupIdAndStorageDomainId(Guid diskId, Guid storageDomainId, Guid diskProfileId);
}
