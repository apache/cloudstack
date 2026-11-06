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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.storage.MigrateObjectStoreAccountCmd;
import org.apache.cloudstack.api.command.admin.storage.RotateObjectStoreAccountKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.api.command.user.bucket.MigrateBucketCredentialCmd;
import org.apache.cloudstack.api.command.user.bucket.RevokeBucketKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.RotateBucketKeyCmd;
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
import com.cloud.agent.api.to.BucketCredentialTO;
import com.cloud.agent.api.to.BucketKeyTO;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.configuration.Resource;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.resourcelimit.CheckedReservation;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.storage.BucketCredentialKeyVO;
import com.cloud.storage.BucketCredentialVO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.dao.BucketCredentialDao;
import com.cloud.storage.dao.BucketCredentialKeyDao;
import com.cloud.storage.dao.BucketDao;
import com.cloud.usage.BucketStatisticsVO;
import com.cloud.usage.dao.BucketStatisticsDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
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
    @Inject
    private BucketCredentialDao _bucketCredentialDao;
    @Inject
    private BucketCredentialKeyDao _bucketCredentialKeyDao;

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
                DefaultMaxDomainObjectStorage,
                PerBucketCredentials
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
        boolean credentialCreated = false;
        if(cmd.isObjectLocking()) {
            objectLock = true;
        }
        try {
            bucketTO = new BucketTO(objectStore.createBucket(bucket, objectLock));
            bucketCreated = true;

            if (isPerBucketCredentialsEnabled(objectStore, bucket.getAccountId())) {
                provisionBucketCredential(objectStore, bucket);
                credentialCreated = true;
                bucket = _bucketDao.findById(bucket.getId());
                bucketTO = toBucketTO(bucket);
            }

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

            bucket.setState(Bucket.State.Created);
            _bucketDao.update(bucket.getId(), bucket);
            if (cmd.getQuota() != null) {
                _objectStoreDao.updateAllocatedSize(objectStoreVO, cmd.getQuota() * Resource.ResourceType.bytesToGiB);
            }
        } catch (Exception e) {
            logger.debug("Failed to create bucket with name: {}", bucket.getName(), e);
            if (credentialCreated) {
                removeBucketCredential(objectStore, bucket, bucketTO, false);
            }
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
            BucketTO bucketTO = toBucketTO(bucket);
            if (objectStore.deleteBucket(bucketTO)) {
                removeBucketCredential(objectStore, bucket, bucketTO, true);
                resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.bucket);
                if (bucket.getQuota() != null) {
                    resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage, (bucket.getQuota() * Resource.ResourceType.bytesToGiB));
                    _objectStoreDao.updateAllocatedSize(objectStoreVO, -(bucket.getQuota() * Resource.ResourceType.bytesToGiB));
                }
                _bucketDao.remove(bucket.getId());
                return true;
            }
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_UPDATE, eventDescription = "updating bucket")
    public boolean updateBucket(UpdateBucketCmd cmd, Account caller) throws ResourceAllocationException {
        BucketVO bucket = _bucketDao.findById(cmd.getId());
        if (bucket == null) {
            throw new InvalidParameterValueException("Unable to find bucket with ID: " + cmd.getId());
        }
        BucketTO bucketTO = toBucketTO(bucket);
        _accountMgr.checkAccess(caller, null, true, bucket);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(bucket.getObjectStoreId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);

        try {
            if (cmd.getEncryption() != null) {
                if (cmd.getEncryption()) {
                    objectStore.setBucketEncryption(bucketTO);
                } else {
                    objectStore.deleteBucketEncryption(bucketTO);
                }
                bucket.setEncryption(cmd.getEncryption());
            }

            if (cmd.getVersioning() != null) {
                if (cmd.getVersioning()) {
                    objectStore.setBucketVersioning(bucketTO);
                } else {
                    objectStore.deleteBucketVersioning(bucketTO);
                }
                bucket.setVersioning(cmd.getVersioning());
            }

            if (cmd.getPolicy() != null) {
                objectStore.setBucketPolicy(bucketTO, cmd.getPolicy());
                bucket.setPolicy(cmd.getPolicy());
            }

            updateBucketQuota(cmd, bucket, objectStore, objectStoreVO, bucketTO);

            _bucketDao.update(bucket.getId(), bucket);
        } catch (Exception e) {
            throw new CloudRuntimeException("Error while updating bucket: " +bucket.getName() +". "+e.getMessage());
        }

        return true;
    }

    private void updateBucketQuota(UpdateBucketCmd cmd, BucketVO bucket, ObjectStoreEntity objectStore, ObjectStoreVO objectStoreVO, BucketTO bucketTO) throws ResourceAllocationException {
        Integer quota = cmd.getQuota();
        if (quota == null) {
            return;
        }

        int quotaDelta = quota - bucket.getQuota();
        objectStore.setQuota(bucketTO, quota);
        bucket.setQuota(quota);

        long diff = quotaDelta * Resource.ResourceType.bytesToGiB;

        if (quotaDelta < 0) {
            resourceLimitManager.decrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage, Math.abs(diff));
            _objectStoreDao.updateAllocatedSize(objectStoreVO, diff);
            return;
        }

        Account owner = _accountMgr.getActiveAccountById(bucket.getAccountId());
        try (CheckedReservation objectStorageReservation = new CheckedReservation(owner, Resource.ResourceType.object_storage, diff, reservationDao, resourceLimitManager)) {
            resourceLimitManager.incrementResourceCount(bucket.getAccountId(), Resource.ResourceType.object_storage, diff);
            _objectStoreDao.updateAllocatedSize(objectStoreVO, diff);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_KEY_ROTATE, eventDescription = "rotating bucket key")
    public BucketCredentialKey rotateBucketKey(RotateBucketKeyCmd cmd, Account caller) {
        BucketVO bucket = getCheckedBucket(cmd.getId(), caller);
        BucketCredentialVO credential = getRequiredCredential(bucket);
        ObjectStoreEntity objectStore = getObjectStore(bucket);
        BucketTO bucketTO = getBucketTO(bucket, credential);

        List<BucketCredentialKeyVO> keys = _bucketCredentialKeyDao.listByCredentialId(credential.getId());
        int slot = resolveRotationSlot(cmd.getKeySlot(), keys);
        BucketCredentialKeyVO target = findKeyInSlot(keys, slot);
        BucketCredentialKeyVO other = findKeyInSlot(keys, slot == BucketCredentialKey.KEY_SLOT_ONE ? BucketCredentialKey.KEY_SLOT_TWO : BucketCredentialKey.KEY_SLOT_ONE);

        // Identities are commonly limited to two keys, so a slot can only be replaced in place
        // when the other slot is also active. Everywhere else the new key is created first so the
        // credential never has zero valid keys and the mirror never points at a dead key.
        boolean targetActive = target != null && target.getState() == BucketCredentialKey.State.Active;
        boolean otherActive = other != null && other.getState() == BucketCredentialKey.State.Active;
        String oldAccessKey = targetActive ? target.getAccessKey() : null;
        if (targetActive && otherActive) {
            objectStore.removeBucketCredentialKey(bucketTO, oldAccessKey);
            revokeKeyRow(target);
            updateBucketKeyMirror(bucket, credential);
            oldAccessKey = null;
        }

        Set<String> knownAccessKeys = new HashSet<>();
        for (BucketCredentialKeyVO key : keys) {
            if (key.getAccessKey() != null && key.getState() == BucketCredentialKey.State.Active) {
                knownAccessKeys.add(key.getAccessKey());
            }
        }
        BucketKeyTO newKey = objectStore.createBucketCredentialKey(bucketTO, knownAccessKeys);
        BucketCredentialKeyVO keyVO;
        try {
            keyVO = persistKeyInSlot(credential, slot, target, newKey);
            updateBucketKeyMirror(bucket, credential);
        } catch (RuntimeException e) {
            logger.warn("Failed to record new key {} for bucket {}; removing it from the backend", newKey.getAccessKey(), bucket.getName(), e);
            objectStore.removeBucketCredentialKey(bucketTO, newKey.getAccessKey());
            throw e;
        }

        if (oldAccessKey != null) {
            objectStore.removeBucketCredentialKey(bucketTO, oldAccessKey);
        }
        return keyVO;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_KEY_REVOKE, eventDescription = "revoking bucket key")
    public boolean revokeBucketKey(RevokeBucketKeyCmd cmd, Account caller) {
        BucketVO bucket = getCheckedBucket(cmd.getId(), caller);
        BucketCredentialVO credential = getRequiredCredential(bucket);
        ObjectStoreEntity objectStore = getObjectStore(bucket);
        BucketTO bucketTO = getBucketTO(bucket, credential);

        List<BucketCredentialKeyVO> keys = _bucketCredentialKeyDao.listByCredentialId(credential.getId());
        BucketCredentialKeyVO target = findKeyInSlot(keys, cmd.getKeySlot());
        if (target == null || target.getState() != BucketCredentialKey.State.Active) {
            throw new InvalidParameterValueException("Key slot " + cmd.getKeySlot() + " of bucket " + bucket.getName() + " holds no active key");
        }
        if (countActiveKeys(keys) <= 1) {
            throw new InvalidParameterValueException("Key slot " + cmd.getKeySlot() + " holds the only active key of bucket " + bucket.getName() + ". A bucket always keeps one active key: create a key in the other slot before revoking this one");
        }

        objectStore.removeBucketCredentialKey(bucketTO, target.getAccessKey());
        revokeKeyRow(target);
        updateBucketKeyMirror(bucket, credential);
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_CREDENTIAL_MIGRATE, eventDescription = "migrating bucket to a dedicated credential")
    public Bucket migrateBucketCredential(MigrateBucketCredentialCmd cmd, Account caller) {
        BucketVO bucket = getCheckedBucket(cmd.getId(), caller);
        if (bucket.getState() != Bucket.State.Created) {
            throw new InvalidParameterValueException("Bucket " + bucket.getName() + " is not in the Created state");
        }
        if (_bucketCredentialDao.findByBucketId(bucket.getId()) != null) {
            throw new InvalidParameterValueException("Bucket " + bucket.getName() + " already has a dedicated credential");
        }
        ObjectStoreEntity objectStore = getObjectStore(bucket);
        if (!objectStore.supportsBucketCredentials()) {
            throw new InvalidParameterValueException("The object store hosting bucket " + bucket.getName() + " does not support per-bucket credentials");
        }
        if (!objectStore.accountSupportsBucketCredentials(bucket.getAccountId())) {
            throw new InvalidParameterValueException("The account owning bucket " + bucket.getName() + " has not been migrated to per-bucket credentials on this object store yet. An administrator can migrate it from the Object Storage tab under the Account");
        }
        provisionBucketCredential(objectStore, bucket);
        return _bucketDao.findById(bucket.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_OBJECT_STORE_ACCOUNT_MIGRATE, eventDescription = "migrating account on object store for per-bucket credentials")
    public boolean migrateObjectStoreAccount(MigrateObjectStoreAccountCmd cmd, Account caller) {
        Account account = _accountMgr.getActiveAccountById(cmd.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account with ID: " + cmd.getAccountId());
        }
        _accountMgr.checkAccess(caller, null, true, account);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(cmd.getObjectStoreId());
        if (objectStoreVO == null) {
            throw new InvalidParameterValueException("Unable to find object store with ID: " + cmd.getObjectStoreId());
        }
        ObjectStoreEntity objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        if (!objectStore.supportsBucketCredentials()) {
            // the cause names the gateway's own admin credential and its capabilities, which belong
            // to whoever runs the platform; a domain admin is told to ask them rather than shown it
            String reason = _accountMgr.isRootAdmin(caller.getId()) ? objectStore.bucketCredentialsUnsupportedReason() : null;
            throw new InvalidParameterValueException("Object store " + objectStoreVO.getName()
                    + " does not support per-bucket credentials"
                    + (reason != null ? ": " + reason : ". Contact your platform administrator"));
        }
        return withAccountStoreLock(account, objectStoreVO, "migrate the account", () -> objectStore.migrateAccountForBucketCredentials(account.getId()));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_OBJECT_STORE_ACCOUNT_KEY_ROTATE, eventDescription = "rotating account key on object store")
    public boolean rotateObjectStoreAccountKey(RotateObjectStoreAccountKeyCmd cmd, Account caller) {
        Account account = _accountMgr.getActiveAccountById(cmd.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("Unable to find account with ID: " + cmd.getAccountId());
        }
        _accountMgr.checkAccess(caller, null, true, account);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(cmd.getObjectStoreId());
        if (objectStoreVO == null) {
            throw new InvalidParameterValueException("Unable to find object store with ID: " + cmd.getObjectStoreId());
        }
        ObjectStoreEntity objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        if (!objectStore.accountSupportsBucketCredentials(account.getId())) {
            throw new InvalidParameterValueException("Account " + account.getAccountName() + " has not been migrated to per-bucket credentials on object store " + objectStoreVO.getName() + " yet. Migrate the account on this store first");
        }
        List<String> legacyBuckets = new ArrayList<>();
        for (BucketVO bucket : _bucketDao.listByObjectStoreIdAndAccountId(objectStoreVO.getId(), account.getId())) {
            if (_bucketCredentialDao.findByBucketId(bucket.getId()) == null) {
                legacyBuckets.add(bucket.getName());
            }
        }
        if (!legacyBuckets.isEmpty()) {
            throw new InvalidParameterValueException("Cannot rotate the account key yet: " + legacyBuckets.size() + " bucket(s) still use it (" + String.join(", ", legacyBuckets) + "). Move each of them to a per-bucket credential first");
        }
        return withAccountStoreLock(account, objectStoreVO, "rotate the account key", () -> {
            objectStore.rotateAccountKey(account.getId());
            return true;
        });
    }

    /**
     * Run an account-level object store operation under a lock covering that account on that
     * store. Two of these running at once, whether from two API calls or two management servers,
     * would each issue a key and then revoke the other's, leaving the account with a key nobody
     * holds. Refusing the second is better than repairing that afterwards.
     */
    protected boolean withAccountStoreLock(Account account, ObjectStoreVO objectStore, String operation, Supplier<Boolean> work) {
        GlobalLock lock = GlobalLock.getInternLock("ObjectStoreAccount-" + account.getId() + "-" + objectStore.getId());
        if (!lock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
            throw new CloudRuntimeException("Another object storage operation is already running for account "
                    + account.getAccountName() + " on " + objectStore.getName() + "; unable to " + operation + " right now");
        }
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long countAccountScopedBuckets(long accountId, long objectStoreId) {
        long count = 0;
        for (BucketVO bucket : _bucketDao.listByObjectStoreIdAndAccountId(objectStoreId, accountId)) {
            if (_bucketCredentialDao.findByBucketId(bucket.getId()) == null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<? extends BucketCredentialKey> listBucketKeys(long bucketId) {
        BucketCredentialVO credential = _bucketCredentialDao.findByBucketId(bucketId);
        if (credential == null) {
            return null;
        }
        List<BucketCredentialKeyVO> keys = _bucketCredentialKeyDao.listByCredentialId(credential.getId());
        keys.sort(Comparator.comparingInt(BucketCredentialKeyVO::getKeySlot));
        return keys;
    }

    /**
     * Whether a new bucket of this account gets its own credential. An account that has been
     * migrated always does, whatever the global setting says: a shared-key bucket carries the
     * account's root key on the bucket row, where every user of the account can read it, which
     * would undo the migration. The setting therefore only decides how an account is set up to
     * begin with. If the backend cannot issue a per-bucket credential, creation fails the way any
     * backend failure does, rather than falling back to the shared credential.
     */
    protected boolean isPerBucketCredentialsEnabled(ObjectStoreEntity objectStore, long accountId) {
        return objectStore.accountSupportsBucketCredentials(accountId);
    }

    /**
     * Provision a dedicated backend identity for the bucket, record it with its first key in
     * slot one, and mirror that key onto the bucket row. The backend call is idempotent, so a
     * failure after it can be retried safely; a failure to record it removes the identity again.
     */
    protected void provisionBucketCredential(ObjectStoreEntity objectStore, BucketVO bucket) {
        BucketTO bucketTO = new BucketTO(bucket);
        BucketCredentialTO credentialTO = objectStore.createBucketCredential(bucketTO);
        if (credentialTO == null || credentialTO.getKeys() == null || credentialTO.getKeys().isEmpty()) {
            throw new CloudRuntimeException("Object store returned no credential for bucket " + bucket.getName());
        }
        BucketKeyTO firstKey = credentialTO.getKeys().get(0);
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    BucketCredentialVO credential = _bucketCredentialDao.persist(new BucketCredentialVO(bucket.getId(), credentialTO.getProviderCredentialId()));
                    _bucketCredentialKeyDao.persist(new BucketCredentialKeyVO(credential.getId(), BucketCredentialKey.KEY_SLOT_ONE, firstKey.getAccessKey(), firstKey.getSecretKey()));
                    bucket.setAccessKey(firstKey.getAccessKey());
                    bucket.setSecretKey(firstKey.getSecretKey());
                    _bucketDao.update(bucket.getId(), bucket);
                }
            });
        } catch (RuntimeException e) {
            logger.warn("Failed to record dedicated credential for bucket {}; removing it from the backend", bucket.getName(), e);
            bucketTO.setProviderCredentialId(credentialTO.getProviderCredentialId());
            try {
                objectStore.deleteBucketCredential(bucketTO);
            } catch (Exception cleanup) {
                logger.warn("Failed to remove backend credential {} for bucket {}; it needs manual cleanup", credentialTO.getProviderCredentialId(), bucket.getName(), cleanup);
            }
            throw e;
        }
    }

    /**
     * Remove the bucket's dedicated backend identity (if any) and its rows. When
     * {@code propagateFailure} is false the backend failure is logged and the rows are removed
     * anyway, which is what a rollback wants; otherwise the failure is raised so the operation
     * can be retried with the rows still in place.
     */
    protected void removeBucketCredential(ObjectStoreEntity objectStore, Bucket bucket, BucketTO bucketTO, boolean propagateFailure) {
        BucketCredentialVO credential = _bucketCredentialDao.findByBucketId(bucket.getId());
        if (credential == null) {
            return;
        }
        bucketTO.setProviderCredentialId(credential.getProviderCredentialId());
        try {
            objectStore.deleteBucketCredential(bucketTO);
        } catch (RuntimeException e) {
            if (propagateFailure) {
                throw e;
            }
            logger.warn("Failed to remove backend credential {} for bucket {}; it needs manual cleanup", credential.getProviderCredentialId(), bucket.getName(), e);
        }
        for (BucketCredentialKeyVO key : _bucketCredentialKeyDao.listByCredentialId(credential.getId())) {
            _bucketCredentialKeyDao.expunge(key.getId());
        }
        _bucketCredentialDao.expunge(credential.getId());
    }

    /**
     * Keep the bucket row's access/secret key columns pointing at the most recently created
     * active key, so every existing consumer of those columns keeps working across rotations.
     */
    protected void updateBucketKeyMirror(BucketVO bucket, BucketCredentialVO credential) {
        BucketCredentialKeyVO newest = null;
        for (BucketCredentialKeyVO key : _bucketCredentialKeyDao.listByCredentialId(credential.getId())) {
            if (key.getState() != BucketCredentialKey.State.Active) {
                continue;
            }
            if (newest == null || (key.getCreated() != null && newest.getCreated() != null && key.getCreated().after(newest.getCreated()))) {
                newest = key;
            }
        }
        if (newest == null) {
            throw new CloudRuntimeException("Bucket " + bucket.getName() + " has no active key left");
        }
        bucket.setAccessKey(newest.getAccessKey());
        bucket.setSecretKey(newest.getSecretKey());
        _bucketDao.update(bucket.getId(), bucket);
    }

    private BucketCredentialKeyVO persistKeyInSlot(BucketCredentialVO credential, int slot, BucketCredentialKeyVO existing, BucketKeyTO newKey) {
        if (existing == null) {
            return _bucketCredentialKeyDao.persist(new BucketCredentialKeyVO(credential.getId(), slot, newKey.getAccessKey(), newKey.getSecretKey()));
        }
        existing.setAccessKey(newKey.getAccessKey());
        existing.setSecretKey(newKey.getSecretKey());
        existing.setState(BucketCredentialKey.State.Active);
        existing.setCreated(new Date());
        existing.setLastUsed(null);
        _bucketCredentialKeyDao.update(existing.getId(), existing);
        return existing;
    }

    private void revokeKeyRow(BucketCredentialKeyVO key) {
        key.setState(BucketCredentialKey.State.Revoked);
        key.setSecretKey(null);
        _bucketCredentialKeyDao.update(key.getId(), key);
    }

    private int resolveRotationSlot(Integer requested, List<BucketCredentialKeyVO> keys) {
        if (requested != null) {
            if (requested != BucketCredentialKey.KEY_SLOT_ONE && requested != BucketCredentialKey.KEY_SLOT_TWO) {
                throw new InvalidParameterValueException("Key slot must be " + BucketCredentialKey.KEY_SLOT_ONE + " or " + BucketCredentialKey.KEY_SLOT_TWO);
            }
            return requested;
        }
        for (int slot : new int[] {BucketCredentialKey.KEY_SLOT_ONE, BucketCredentialKey.KEY_SLOT_TWO}) {
            BucketCredentialKeyVO key = findKeyInSlot(keys, slot);
            if (key == null || key.getState() != BucketCredentialKey.State.Active) {
                return slot;
            }
        }
        throw new InvalidParameterValueException("Both key slots hold active keys; specify the slot to rotate");
    }

    private static BucketCredentialKeyVO findKeyInSlot(List<BucketCredentialKeyVO> keys, int slot) {
        for (BucketCredentialKeyVO key : keys) {
            if (key.getKeySlot() == slot) {
                return key;
            }
        }
        return null;
    }

    private static int countActiveKeys(List<BucketCredentialKeyVO> keys) {
        int active = 0;
        for (BucketCredentialKeyVO key : keys) {
            if (key.getState() == BucketCredentialKey.State.Active) {
                active++;
            }
        }
        return active;
    }

    private BucketVO getCheckedBucket(long bucketId, Account caller) {
        BucketVO bucket = _bucketDao.findById(bucketId);
        if (bucket == null) {
            throw new InvalidParameterValueException("Unable to find bucket with ID: " + bucketId);
        }
        _accountMgr.checkAccess(caller, null, true, bucket);
        return bucket;
    }

    private BucketCredentialVO getRequiredCredential(BucketVO bucket) {
        BucketCredentialVO credential = _bucketCredentialDao.findByBucketId(bucket.getId());
        if (credential == null) {
            throw new InvalidParameterValueException("Bucket " + bucket.getName() + " still uses the account key. Move it to a per-bucket credential first (the Migrate to Per-Bucket Credential action on the bucket)");
        }
        return credential;
    }

    private ObjectStoreEntity getObjectStore(BucketVO bucket) {
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(bucket.getObjectStoreId());
        return (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
    }

    private static BucketTO getBucketTO(BucketVO bucket, BucketCredentialVO credential) {
        BucketTO bucketTO = new BucketTO(bucket);
        bucketTO.setProviderCredentialId(credential.getProviderCredentialId());
        return bucketTO;
    }

    /**
     * A BucketTO that carries the bucket's dedicated credential reference when it has one, so
     * drivers can keep their per-credential grants intact on bucket-level operations.
     */
    protected BucketTO toBucketTO(Bucket bucket) {
        BucketTO bucketTO = new BucketTO(bucket);
        BucketCredentialVO credential = _bucketCredentialDao.findByBucketId(bucket.getId());
        if (credential != null) {
            bucketTO.setProviderCredentialId(credential.getProviderCredentialId());
        }
        return bucketTO;
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
