// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.object;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.api.command.user.bucket.UpdateBucketCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.IllegalBucketNameException;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.configuration.Resource;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.resourcelimit.CheckedReservation;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.storage.BucketVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.dao.BucketDao;
import com.cloud.usage.BucketStatisticsVO;
import com.cloud.usage.dao.BucketStatisticsDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public class BucketApiServiceImpl extends ManagerBase implements BucketApiService, Configurable {

    @Inject
    private ObjectStoreDao _objectStoreDao;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    private BucketDao _bucketDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private ResourceLimitManagerImpl resourceLimitManager;

    @Inject
    private BucketStatisticsDao _bucketStatisticsDao;
    @Inject
    ReservationDao reservationDao;

    private ScheduledExecutorService _executor = null;

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3;

    protected BucketApiServiceImpl() {

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Bucket-Usage"));
        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new BucketUsageTask(), 60L, 3600L, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        _executor.shutdown();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return BucketApiService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                DefaultMaxAccountBuckets,
                DefaultMaxAccountObjectStorage,
                DefaultMaxProjectBuckets,
                DefaultMaxProjectObjectStorage,
                DefaultMaxDomainBuckets,
                DefaultMaxDomainObjectStorage
        };
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_CREATE, eventDescription = "creating bucket", create = true)
    public Bucket allocBucket(CreateBucketCmd cmd) throws ResourceAllocationException {
        try {
            BucketNameUtils.validateBucketName(cmd.getBucketName());
        } catch (IllegalBucketNameException e) {
            logger.error("Invalid Bucket Name: " +cmd.getBucketName(), e);
            throw new InvalidParameterValueException("Invalid Bucket Name: "+e.getMessage());
        }
        if (cmd.getQuota() != null && cmd.getQuota() < 0) {
            throw new InvalidParameterValueException("Bucket quota cannot be negative: " + cmd.getQuota());
        }
        //ToDo check bucket exists
        long ownerId = cmd.getEntityOwnerId();
        Account owner = _accountMgr.getActiveAccountById(ownerId);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(cmd.getObjectStoragePoolId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        try {
            if(!objectStore.createUser(ownerId)) {
                logger.error("Failed to create user in objectstore {}", objectStore);
                return null;
            }
        } catch (CloudRuntimeException e) {
            logger.error("Error while checking object store user.", e);
            return null;
        }

        long size = ObjectUtils.defaultIfNull(cmd.getQuota(), 0) * Resource.ResourceType.bytesToGiB;
        return createCheckedBucket(cmd, owner, size, ownerId);
    }

    @NotNull
    private BucketVO createCheckedBucket(CreateBucketCmd cmd, Account owner, long size, long ownerId) throws ResourceAllocationException {
        try (CheckedReservation bucketReservation = new CheckedReservation(owner, Resource.ResourceType.bucket,
                1L, reservationDao, resourceLimitManager);
             CheckedReservation objectStorageReservation = new CheckedReservation(owner,
                     Resource.ResourceType.object_storage, size, reservationDao, resourceLimitManager)) {
            BucketVO bucket = new BucketVO(ownerId, owner.getDomainId(), cmd.getObjectStoragePoolId(), cmd.getBucketName(), cmd.getQuota(),
                    cmd.isVersioning(), cmd.isEncryption(), cmd.isObjectLocking(), cmd.getPolicy());
            _bucketDao.persist(bucket);
            resourceLimitManager.incrementResourceCount(bucket.getAccountId(), Resource.ResourceType.bucket);
            if (size > 0) {
                resourceLimitManager.incrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage,
                        (cmd.getQuota() * Resource.ResourceType.bytesToGiB));
            }
            return bucket;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_CREATE, eventDescription = "creating bucket", async = true)
    public Bucket createBucket(CreateBucketCmd cmd) {
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(cmd.getObjectStoragePoolId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        BucketVO bucket = _bucketDao.findById(cmd.getEntityId());
        BucketTO bucketTO = new BucketTO(bucket);
        boolean objectLock = false;
        boolean bucketCreated = false;
        if(cmd.isObjectLocking()) {
            objectLock = true;
        }
        try {
            bucketTO = new BucketTO(objectStore.createBucket(bucket, objectLock));
            bucketCreated = true;

            if (cmd.isVersioning()) {
                objectStore.setBucketVersioning(bucketTO);
            }

            if (cmd.isEncryption()) {
                objectStore.setBucketEncryption(bucketTO);
            }

            if (cmd.getQuota() != null) {
                objectStore.setQuota(bucketTO, cmd.getQuota());
                if (objectStoreVO.getTotalSize() != null && objectStoreVO.getTotalSize() != 0 && objectStoreVO.getAllocatedSize() != null) {
                    Long allocatedSize = objectStoreVO.getAllocatedSize() / Resource.ResourceType.bytesToGiB;
                    Long totalSize = objectStoreVO.getTotalSize() / Resource.ResourceType.bytesToGiB;
                    if (cmd.getQuota() + allocatedSize > totalSize) {
                        logger.error("Object store {}'s allocated size has reached the total size limit of {}GiB.", objectStoreVO.getName(), totalSize);
                        throw new CloudRuntimeException("Not enough space in object store to create the bucket");
                    }
                }
            }

            if (cmd.getPolicy() != null) {
                objectStore.setBucketPolicy(bucketTO, cmd.getPolicy());
            }

            // Re-read the row so fields the provider persisted during
            // createBucket (access key, secret key, bucket URL) are not
            // clobbered by this state update and are present in the API
            // response. Providers such as SeaweedFS and Cloudian HyperStore
            // write the per-account credentials to the BucketVO themselves.
            BucketVO createdBucket = _bucketDao.findById(bucket.getId());
            if (createdBucket != null) {
                bucket = createdBucket;
            }
            bucket.setState(Bucket.State.Created);
            _bucketDao.update(bucket.getId(), bucket);
            if (cmd.getQuota() != null) {
                _objectStoreDao.updateAllocatedSize(objectStoreVO, cmd.getQuota() * Resource.ResourceType.bytesToGiB);
            }
        } catch (Exception e) {
            logger.debug("Failed to create bucket with name: {}", bucket.getName(), e);
            if(bucketCreated) {
                objectStore.deleteBucket(bucketTO);
            }
            _bucketDao.remove(bucket.getId());
            resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.bucket);
            if (bucket.getQuota() != null) {
                resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage,
                        (bucket.getQuota() * Resource.ResourceType.bytesToGiB));
            }
            throw new CloudRuntimeException("Failed to create bucket with name: "+bucket.getName()+". "+e.getMessage());
        }
        return bucket;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_DELETE, eventDescription = "deleting bucket")
    public boolean deleteBucket(long bucketId, Account caller) throws ResourceAllocationException {
        Bucket bucket = _bucketDao.findById(bucketId);
        if (bucket == null) {
            throw new InvalidParameterValueException("Unable to find bucket with ID: " + bucketId);
        }
        _accountMgr.checkAccess(caller, null, true, bucket);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(bucket.getObjectStoreId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        return deleteCheckedBucket(objectStore, bucket, objectStoreVO);
    }

    private boolean deleteCheckedBucket(ObjectStoreEntity objectStore, Bucket bucket, ObjectStoreVO objectStoreVO) throws ResourceAllocationException {
        Account owner = _accountMgr.getAccount(bucket.getAccountId());
        try (CheckedReservation bucketReservation = new CheckedReservation(owner, Resource.ResourceType.bucket,
                bucket.getId(), null, -1L, reservationDao, resourceLimitManager);
             CheckedReservation objectStorageReservation = new CheckedReservation(owner,
                     Resource.ResourceType.object_storage, bucket.getId(), null,
                     -1*(ObjectUtils.defaultIfNull(bucket.getQuota(), 0) * Resource.ResourceType.bytesToGiB), reservationDao, resourceLimitManager)) {
            BucketTO bucketTO = new BucketTO(bucket);
            if (objectStore.deleteBucket(bucketTO)) {
                return removeBucketAndUpdateResourceAccounting(bucket, objectStoreVO);
            }
            return false;
        }
    }

    private boolean removeBucketAndUpdateResourceAccounting(Bucket bucket, ObjectStoreVO objectStoreVO) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.bucket);
                Integer quota = bucket.getQuota();
                if (quota != null) {
                    long quotaBytes = (long) quota * Resource.ResourceType.bytesToGiB;
                    resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage, quotaBytes);
                    if (!Boolean.TRUE.equals(_objectStoreDao.updateAllocatedSize(objectStoreVO, -quotaBytes))) {
                        throw new CloudRuntimeException("Failed to update allocated size on object store " + objectStoreVO.getName());
                    }
                }
                if (!_bucketDao.remove(bucket.getId())) {
                    throw new CloudRuntimeException("Failed to remove bucket " + bucket.getName());
                }
                return true;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_UPDATE, eventDescription = "updating bucket")
    public boolean updateBucket(UpdateBucketCmd cmd, Account caller) throws ResourceAllocationException {
        BucketVO bucket = _bucketDao.findById(cmd.getId());
        if (bucket == null) {
            throw new InvalidParameterValueException("Unable to find bucket with ID: " + cmd.getId());
        }
        BucketTO bucketTO = new BucketTO(bucket);
        _accountMgr.checkAccess(caller, null, true, bucket);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(bucket.getObjectStoreId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);

        // Validate quota before applying any remote side effects so a
        // negative value does not leave encryption/versioning/policy changes
        // applied while the API returns an error.
        if (cmd.getQuota() != null && cmd.getQuota() < 0) {
            throw new InvalidParameterValueException("Bucket quota cannot be negative: " + cmd.getQuota());
        }

        // Capture the pre-update remote state so a failure after a partial
        // update can roll back the remote mutations that already succeeded.
        // Without this, a quota reservation rejection (or any later failure)
        // would leave encryption/versioning/policy changes applied remotely
        // while the BucketVO retains the old values and the API returns an
        // error, leaving CloudStack and the backend out of sync.
        Boolean previousEncryption = bucket.isEncryption();
        Boolean previousVersioning = bucket.isVersioning();
        String previousPolicy = bucket.getPolicy();
        boolean encryptionApplied = false;
        boolean versioningApplied = false;
        boolean policyApplied = false;

        try {
            if (cmd.getEncryption() != null) {
                if (cmd.getEncryption()) {
                    objectStore.setBucketEncryption(bucketTO);
                } else {
                    objectStore.deleteBucketEncryption(bucketTO);
                }
                bucket.setEncryption(cmd.getEncryption());
                encryptionApplied = true;
            }

            if (cmd.getVersioning() != null) {
                if (cmd.getVersioning()) {
                    objectStore.setBucketVersioning(bucketTO);
                } else {
                    objectStore.deleteBucketVersioning(bucketTO);
                }
                bucket.setVersioning(cmd.getVersioning());
                versioningApplied = true;
            }

            if (cmd.getPolicy() != null) {
                objectStore.setBucketPolicy(bucketTO, cmd.getPolicy());
                bucket.setPolicy(cmd.getPolicy());
                policyApplied = true;
            }

            boolean bucketPersisted = updateBucketQuota(cmd, bucket, objectStore, objectStoreVO, bucketTO);

            if (!bucketPersisted && !_bucketDao.update(bucket.getId(), bucket)) {
                throw new CloudRuntimeException("Failed to update bucket " + bucket.getName());
            }
        } catch (Exception e) {
            // Roll back the remote encryption/versioning/policy mutations that
            // were applied before the failure (e.g. a quota reservation
            // rejection) so the remote bucket and the BucketVO stay consistent.
            // The quota path unwinds its own mutations internally.
            rollbackBucketUpdateMutations(objectStore, bucketTO, bucket,
                    previousEncryption, encryptionApplied,
                    previousVersioning, versioningApplied,
                    previousPolicy, policyApplied, e);
            if (e instanceof CloudRuntimeException) {
                throw (CloudRuntimeException) e;
            }
            throw new CloudRuntimeException("Error while updating bucket: " + bucket.getName() + ". " + e.getMessage(), e);
        }

        return true;
    }

    /**
     * Best-effort compensation for the remote encryption, versioning, and
     * policy mutations applied during {@link #updateBucket} when a later step
     * fails. Each remote mutation is reverted to its pre-update value in
     * reverse order; the in-memory {@link BucketVO} fields are restored too so
     * a retry starts from the same state the backend is in. Failures are
     * logged and attached to the original exception rather than masking it.
     */
    private void rollbackBucketUpdateMutations(ObjectStoreEntity objectStore, BucketTO bucketTO, BucketVO bucket,
            Boolean previousEncryption, boolean encryptionApplied,
            Boolean previousVersioning, boolean versioningApplied,
            String previousPolicy, boolean policyApplied, Exception cause) {
        if (policyApplied) {
            try {
                if (previousPolicy == null || "private".equalsIgnoreCase(previousPolicy)) {
                    objectStore.setBucketPolicy(bucketTO, "private");
                } else {
                    objectStore.setBucketPolicy(bucketTO, previousPolicy);
                }
                bucket.setPolicy(previousPolicy);
            } catch (Exception ex) {
                logger.error("Failed to roll back bucket policy for {} while compensating a failed update",
                        bucket.getName(), ex);
                cause.addSuppressed(ex);
            }
        }
        if (versioningApplied) {
            try {
                if (Boolean.TRUE.equals(previousVersioning)) {
                    objectStore.setBucketVersioning(bucketTO);
                } else {
                    objectStore.deleteBucketVersioning(bucketTO);
                }
                bucket.setVersioning(previousVersioning);
            } catch (Exception ex) {
                logger.error("Failed to roll back bucket versioning for {} while compensating a failed update",
                        bucket.getName(), ex);
                cause.addSuppressed(ex);
            }
        }
        if (encryptionApplied) {
            try {
                if (Boolean.TRUE.equals(previousEncryption)) {
                    objectStore.setBucketEncryption(bucketTO);
                } else {
                    objectStore.deleteBucketEncryption(bucketTO);
                }
                bucket.setEncryption(previousEncryption);
            } catch (Exception ex) {
                logger.error("Failed to roll back bucket encryption for {} while compensating a failed update",
                        bucket.getName(), ex);
                cause.addSuppressed(ex);
            }
        }
    }

    private boolean updateBucketQuota(UpdateBucketCmd cmd, BucketVO bucket, ObjectStoreEntity objectStore, ObjectStoreVO objectStoreVO, BucketTO bucketTO) throws ResourceAllocationException {
        Integer quota = cmd.getQuota();
        if (quota == null) {
            return false;
        }

        Integer previousQuota = bucket.getQuota();
        int previousQuotaValue = ObjectUtils.defaultIfNull(previousQuota, 0);
        long quotaDelta = (long) quota - previousQuotaValue;
        long diff = quotaDelta * Resource.ResourceType.bytesToGiB;

        if (diff <= 0) {
            // A decrease (or no change) cannot exceed a limit, so no reservation
            // is needed.
            applyQuotaChange(bucket, objectStore, objectStoreVO, bucketTO, quota, previousQuota, previousQuotaValue, diff);
            return true;
        }

        // Reserve BEFORE mutating the remote quota. Applying it first meant an
        // increase that exceeded the account or store limit left the backend
        // with the new quota while the BucketVO and resource counts kept the
        // old value. If anything inside fails, the reservation is released by
        // try-with-resources and applyQuotaChange unwinds its own mutations.
        Account owner = _accountMgr.getActiveAccountById(bucket.getAccountId());
        try (CheckedReservation objectStorageReservation = new CheckedReservation(owner, Resource.ResourceType.object_storage, diff, reservationDao, resourceLimitManager)) {
            applyQuotaChange(bucket, objectStore, objectStoreVO, bucketTO, quota, previousQuota, previousQuotaValue, diff);
        }
        return true;
    }

    /**
     * Apply a quota change to the storage backend, the account resource count, the
     * object store allocated size and the BucketVO, unwinding every step that
     * completed if a later one fails so the four cannot end up disagreeing.
     *
     * The BucketVO is persisted here rather than being left to the caller. When a
     * quota is supplied, the caller skips its final bucket update so there is no
     * later DAO failure after the backend quota and resource counters have been
     * committed.
     */
    private void applyQuotaChange(BucketVO bucket, ObjectStoreEntity objectStore, ObjectStoreVO objectStoreVO,
            BucketTO bucketTO, int quota, Integer previousQuota, int previousQuotaValue, long diff) {
        long accountId = bucket.getAccountId();
        boolean remoteApplied = false;
        boolean countApplied = false;
        boolean allocatedApplied = false;
        try {
            objectStore.setQuota(bucketTO, quota);
            remoteApplied = true;

            if (diff != 0) {
                if (diff > 0) {
                    resourceLimitManager.incrementResourceCount(accountId, Resource.ResourceType.object_storage, diff);
                } else {
                    resourceLimitManager.decrementResourceCount(accountId, Resource.ResourceType.object_storage, Math.abs(diff));
                }
                countApplied = true;

                // updateAllocatedSize reports a failed DAO update by returning
                // false rather than throwing, so the result must be checked or
                // the failure passes silently.
                if (!Boolean.TRUE.equals(_objectStoreDao.updateAllocatedSize(objectStoreVO, diff))) {
                    throw new CloudRuntimeException("Failed to update allocated size on object store " + objectStoreVO.getName());
                }
                allocatedApplied = true;
            }

            bucket.setQuota(quota);
            if (!_bucketDao.update(bucket.getId(), bucket)) {
                bucket.setQuota(previousQuota);
                throw new CloudRuntimeException("Failed to persist quota on bucket " + bucket.getName());
            }
        } catch (RuntimeException e) {
            rollbackQuotaChange(bucket, objectStore, objectStoreVO, bucketTO, previousQuota, previousQuotaValue, diff,
                    remoteApplied, countApplied, allocatedApplied, e);
            throw e;
        }
    }

    /**
     * Best-effort compensation for a partially applied quota change, unwinding in
     * reverse order. Failures are logged and attached to the original exception
     * rather than masking it.
     */
    private void rollbackQuotaChange(BucketVO bucket, ObjectStoreEntity objectStore, ObjectStoreVO objectStoreVO,
            BucketTO bucketTO, Integer previousQuota, int previousQuotaValue, long diff, boolean remoteApplied, boolean countApplied,
            boolean allocatedApplied, RuntimeException cause) {
        if (allocatedApplied) {
            try {
                if (!Boolean.TRUE.equals(_objectStoreDao.updateAllocatedSize(objectStoreVO, -diff))) {
                    throw new CloudRuntimeException("Failed to restore allocated size on object store " + objectStoreVO.getName());
                }
            } catch (Exception ex) {
                logger.error("Failed to restore allocated size on object store {} while rolling back a quota change",
                        objectStoreVO.getName(), ex);
                cause.addSuppressed(ex);
            }
        }
        if (countApplied) {
            try {
                if (diff > 0) {
                    resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage, diff);
                } else {
                    resourceLimitManager.incrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage, Math.abs(diff));
                }
            } catch (Exception ex) {
                logger.error("Failed to restore the object_storage resource count for account {} while rolling back a quota change",
                        bucket.getAccountId(), ex);
                cause.addSuppressed(ex);
            }
        }
        if (remoteApplied) {
            try {
                objectStore.setQuota(bucketTO, previousQuotaValue);
            } catch (Exception ex) {
                logger.error("Failed to restore quota {} on bucket {} while rolling back a quota change; "
                        + "the backend quota and CloudStack accounting may be inconsistent", previousQuotaValue, bucket.getName(), ex);
                cause.addSuppressed(ex);
            }
        }
        bucket.setQuota(previousQuota);
    }

    public void getBucketUsage() {
        //ToDo track usage one last time when object store or bucket is removed
        List<ObjectStoreVO> objectStores = _objectStoreDao.listObjectStores();
        for(ObjectStoreVO objectStoreVO: objectStores) {
            ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
            Map<String, Long> bucketSizes = objectStore.getAllBucketsUsage();
            List<BucketVO> buckets = _bucketDao.listByObjectStoreId(objectStoreVO.getId());
            for(BucketVO bucket : buckets) {
                Long size = bucketSizes.get(bucket.getName());
                if( size != null){
                    bucket.setSize(size);
                    _bucketDao.update(bucket.getId(), bucket);
                }
            }
        }
    }

    private class BucketUsageTask extends ManagedContextRunnable {
        public BucketUsageTask() {
        }

        @Override
        protected void runInContext() {
            GlobalLock scanLock = GlobalLock.getInternLock("BucketUsage");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    try {
                        List<ObjectStoreVO> objectStores = _objectStoreDao.listObjectStores();
                        for(ObjectStoreVO objectStoreVO: objectStores) {
                            logger.debug("Getting bucket usage for Object Store \"{}\"", objectStoreVO.getName());
                            ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
                            Map<String, Long> bucketSizes;
                            try {
                                bucketSizes = objectStore.getAllBucketsUsage();
                            } catch (CloudRuntimeException e) {
                                logger.error(String.format("Failed to get bucket usage for Object Store \"%s\". Skipping this store.", objectStoreVO.getName()), e);
                                continue;
                            }
                            List<BucketVO> buckets = _bucketDao.listByObjectStoreId(objectStoreVO.getId());
                            Long objectStoreUsedBytes = 0L;
                            for(BucketVO bucket : buckets) {
                                Long size = bucketSizes.get(bucket.getName());
                                if( size != null) {
                                    objectStoreUsedBytes += size;
                                    bucket.setSize(size);
                                    _bucketDao.update(bucket.getId(), bucket);

                                    //Update Bucket Usage stats
                                    BucketStatisticsVO bucketStatisticsVO = _bucketStatisticsDao.findBy(bucket.getAccountId(), bucket.getId());
                                    if(bucketStatisticsVO != null) {
                                        bucketStatisticsVO.setSize(size);
                                        _bucketStatisticsDao.update(bucketStatisticsVO.getId(), bucketStatisticsVO);
                                    } else {
                                        bucketStatisticsVO = new BucketStatisticsVO(bucket.getAccountId(), bucket.getId());
                                        bucketStatisticsVO.setSize(size);
                                        _bucketStatisticsDao.persist(bucketStatisticsVO);
                                    }
                                }
                            }
                            objectStoreVO.setUsedSize(objectStoreUsedBytes);
                            _objectStoreDao.persist(objectStoreVO);
                        }
                        logger.debug("Completed updating bucket usage for all object stores");
                    } catch (Exception e) {
                        logger.error("Error while fetching bucket usage", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }
        }
    }
}
