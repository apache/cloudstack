/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.object.store;

import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import org.apache.cloudstack.storage.object.Bucket;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.ObjectStoreDriver;
import org.apache.cloudstack.storage.object.ObjectStoreEntity;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ObjectStoreImpl implements ObjectStoreEntity {

    protected ObjectStoreDriver driver;
    protected ObjectStoreVO objectStoreVO;
    protected ObjectStoreProvider provider;

    public ObjectStoreImpl() {
        super();
    }

    protected void configure(ObjectStoreVO objectStoreVO, ObjectStoreDriver objectStoreDriver, ObjectStoreProvider provider) {
        this.driver = objectStoreDriver;
        this.objectStoreVO = objectStoreVO;
        this.provider = provider;
    }

    public static ObjectStoreEntity getDataStore(ObjectStoreVO objectStoreVO, ObjectStoreDriver objectStoreDriver, ObjectStoreProvider provider) {
        ObjectStoreImpl instance = ComponentContext.inject(ObjectStoreImpl.class);
        instance.configure(objectStoreVO, objectStoreDriver, provider);
        return instance;
    }

    @Override
    public DataStoreDriver getDriver() {
        return this.driver;
    }

    @Override
    public DataStoreRole getRole() {
        return null;
    }

    @Override
    public long getId() {
        return this.objectStoreVO.getId();
    }

    @Override
    public String getUri() {
        return this.objectStoreVO.getUrl();
    }

    @Override
    public Scope getScope() {
        return null;
    }


    @Override
    public String getUuid() {
        return this.objectStoreVO.getUuid();
    }

    public Date getCreated() {
        return this.objectStoreVO.getCreated();
    }

    @Override
    public String getName() {
        return objectStoreVO.getName();
    }

    @Override
    public DataObject create(DataObject obj) {
        return null;
    }

    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        return driver.createBucket(bucket, objectLock);
    }

    @Override
    public boolean deleteBucket(BucketTO bucket) {
        return driver.deleteBucket(bucket, objectStoreVO.getId());
    }

    @Override
    public boolean setBucketEncryption(BucketTO bucket) {
        return driver.setBucketEncryption(bucket, objectStoreVO.getId());
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket) {
        return driver.deleteBucketEncryption(bucket, objectStoreVO.getId());
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket) {
        return driver.setBucketVersioning(bucket, objectStoreVO.getId());
    }

    @Override
    public boolean deleteBucketVersioning(BucketTO bucket) {
        return driver.deleteBucketVersioning(bucket, objectStoreVO.getId());
    }

    @Override
    public void setBucketPolicy(BucketTO bucket, String policy) {
        driver.setBucketPolicy(bucket, policy, objectStoreVO.getId());
    }

    @Override
    public void setQuota(BucketTO bucket, int quota) {
        driver.setBucketQuota(bucket, objectStoreVO.getId(), quota);
    }

    @Override
    public Map<String, Long> getAllBucketsUsage() {
        return driver.getAllBucketsUsage(objectStoreVO.getId());
    }

    @Override
    public List<Bucket> listBuckets() {
        return driver.listBuckets(objectStoreVO.getId());
    }

    /*
    Create user if not exists
     */
    @Override
    public boolean createUser(long accountId) {
        return driver.createUser(accountId, objectStoreVO.getId());
    }

    @Override
    public boolean delete(DataObject obj) {
        return false;
    }

    @Override
    public DataStoreTO getTO() {
        return null;
    }

    @Override
    public String getProviderName() {
        return objectStoreVO.getProviderName();
    }

    @Override
    public String getUrl() {
        return objectStoreVO.getUrl();
    }

}
