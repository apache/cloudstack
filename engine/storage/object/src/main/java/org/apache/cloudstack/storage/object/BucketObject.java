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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import org.apache.cloudstack.engine.subsystem.api.storage.BucketInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import java.util.Date;

public class BucketObject implements BucketInfo {

    private String name;

    @Override
    public long getDomainId() {
        return 0;
    }

    @Override
    public long getAccountId() {
        return 0;
    }

    @Override
    public Class<?> getEntityType() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addPayload(Object data) {

    }

    @Override
    public Object getPayload() {
        return null;
    }

    @Override
    public Bucket getBucket() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public DataTO getTO() {
        return null;
    }

    @Override
    public DataStore getDataStore() {
        return null;
    }

    @Override
    public Long getSize() {
        return null;
    }

    @Override
    public Integer getQuota() {
        return null;
    }

    @Override
    public boolean isVersioning() {
        return false;
    }

    @Override
    public boolean isEncryption() {
        return false;
    }

    @Override
    public boolean isObjectLock() {
        return false;
    }

    @Override
    public String getPolicy() {
        return null;
    }

    @Override
    public String getBucketURL() {
        return null;
    }

    @Override
    public String getAccessKey() {
        return null;
    }

    @Override
    public String getSecretKey() {
        return null;
    }

    @Override
    public long getPhysicalSize() {
        return 0;
    }

    @Override
    public DataObjectType getType() {
        return null;
    }

    @Override
    public String getUuid() {
        return null;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {

    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {

    }

    @Override
    public void incRefCount() {

    }

    @Override
    public void decRefCount() {

    }

    @Override
    public Long getRefCount() {
        return null;
    }

    @Override
    public long getObjectStoreId() {
        return 0;
    }

    @Override
    public Date getCreated() {
        return null;
    }

    @Override
    public State getState() {
        return null;
    }
}
