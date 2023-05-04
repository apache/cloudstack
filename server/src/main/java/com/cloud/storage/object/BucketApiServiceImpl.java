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
package com.cloud.storage.object;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Bucket;
import com.cloud.storage.BucketVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.ObjectStoreEntity;
import org.apache.log4j.Logger;

import javax.inject.Inject;

public class BucketApiServiceImpl extends ManagerBase implements BucketApiService, Configurable {
    private final static Logger s_logger = Logger.getLogger(BucketApiServiceImpl.class);
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private ObjectStoreDao _objectStoreDao;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    private BucketDao _bucketDao;
    @Inject
    private AccountManager _accountMgr;

    protected BucketApiServiceImpl() {

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
    public Bucket allocBucket(CreateBucketCmd cmd) throws ResourceAllocationException {
        //ToDo check bucket exists
        long ownerId = cmd.getEntityOwnerId();
        Account owner = _accountMgr.getActiveAccountById(ownerId);
        BucketVO bucket = new BucketVO(ownerId, owner.getDomainId(), cmd.getObjectStoragePoolId(), cmd.getBucketName());
        _bucketDao.persist(bucket);
        return bucket;
    }

    @Override
    public Bucket createBucket(CreateBucketCmd cmd) {
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(cmd.getObjectStoragePoolId());
        ObjectStoreEntity  objectStore = (ObjectStoreEntity)_dataStoreMgr.getDataStore(objectStoreVO.getId(), DataStoreRole.Object);
        objectStore.createBucket(cmd.getBucketName());
        BucketVO bucket = _bucketDao.findById(cmd.getEntityId());
        bucket.setState(Bucket.State.Created);
        _bucketDao.update(bucket.getId(), bucket);
        return bucket;
    }

    @Override
    public boolean deleteBucket(long bucketId, Account caller) {
        return false;
    }
}