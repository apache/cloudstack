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
package org.apache.cloudstack.storage.snapshot;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Snapshot;

public class SnapshotEntityImpl implements SnapshotEntity {

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getAccountId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getVolumeId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getSnapshotId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Date getCreated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HypervisorType getHypervisorType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRecursive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public short getSnapshotType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getDomainId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String reserveForBackup(int expiration) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void backup(String reservationToken) {
        // TODO Auto-generated method stub

    }

    @Override
    public void restore(String vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, String> getDetails() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public State getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Type getRecurringType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LocationType getLocationType() {
        return null;
    }

    @Override
    public Class<?> getEntityType() {
        return Snapshot.class;
    }
}
