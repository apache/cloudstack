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

import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.IllegalBucketNameException;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
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
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.api.command.user.bucket.UpdateBucketCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private BucketStatisticsDao _bucketStatisticsDao;

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
        };
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_CREATE, eventDescription = "creating bucket", create = true)
    public Bucket allocBucket(CreateBucketCmd cmd) {
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
                logger.error("Failed to create user in objectstore "+ objectStore.getName());
                return null;
            }
        } catch (CloudRuntimeException e) {
            logger.error("Error while checking object store user.", e);
            return null;
        }

        BucketVO bucket = new BucketVO(ownerId, owner.getDomainId(), cmd.getObjectStoragePoolId(), cmd.getBucketName(), cmd.getQuota(),
                                    cmd.isVersioning(), cmd.isEncryption(), cmd.isObjectLocking(), cmd.getPolicy());
        _bucketDao.persist(bucket);
        return bucket;
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
            }

            if (cmd.getPolicy() != null) {
                objectStore.setBucketPolicy(bucketTO, cmd.getPolicy());
            }

            bucket.setState(Bucket.State.Created);
            _bucketDao.update(bucket.getId(), bucket);
        } catch (Exception e) {
            logger.debug("Failed to create bucket with name: "+bucket.getName(), e);
            if(bucketCreated) {
                objectStore.deleteBucket(bucketTO);
            }
            _bucketDao.remove(bucket.getId());
            throw new CloudRuntimeException("Failed to create bucket with name: "+bucket.getName()+". "+e.getMessage());
        }
        return bucket;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_DELETE, eventDescription = "deleting bucket")
    public boolean deleteBucket(long bucketId, Account caller) {
        Bucket bucket = _bucketDao.findById(bucketId);
        BucketTO bucketTO = new BucketTO(bucket);
        if (bucket == null) {
            throw new InvalidParameterValueException("Unable to find bucket with ID: " + bucketId);
        }
        _accountMgr.checkAccess(caller, null, true, bucket);
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(bucket.getObjectStoreId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        if (objectStore.deleteBucket(bucketTO)) {
            return _bucketDao.remove(bucketId);
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BUCKET_UPDATE, eventDescription = "updating bucket")
    public boolean updateBucket(UpdateBucketCmd cmd, Account caller) {
        BucketVO bucket = _bucketDao.findById(cmd.getId());
        BucketTO bucketTO = new BucketTO(bucket);
        if (bucket == null) {
            throw new InvalidParameterValueException("Unable to find bucket with ID: " + cmd.getId());
        }
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

            if (cmd.getQuota() != null) {
                objectStore.setQuota(bucketTO, cmd.getQuota());
                bucket.setQuota(cmd.getQuota());
            }
            _bucketDao.update(bucket.getId(), bucket);
        } catch (Exception e) {
            throw new CloudRuntimeException("Error while updating bucket: " +bucket.getName() +". "+e.getMessage());
        }

        return true;
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
                            ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
                            Map<String, Long> bucketSizes = objectStore.getAllBucketsUsage();
                            List<BucketVO> buckets = _bucketDao.listByObjectStoreId(objectStoreVO.getId());
                            for(BucketVO bucket : buckets) {
                                Long size = bucketSizes.get(bucket.getName());
                                if( size != null){
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
