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
package org.apache.cloudstack.storage.volume;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.StorageEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public class VolumeEntityImpl implements VolumeEntity {
    private VolumeInfo volumeInfo;

    public VolumeEntityImpl(VolumeInfo volumeObject) {
        this.volumeInfo = volumeObject;
    }

    public VolumeInfo getVolumeInfo() {
        return volumeInfo;
    }

    public void setVolumeInfo(VolumeInfo vi) {
        this.volumeInfo = vi;
    }

    @Override 
    public String getUuid() {
        return volumeInfo.getUuid();
    }

    @Override
    public long getId() {
        return volumeInfo.getId();
    }

    @Override
    public String getCurrentState() {
        return volumeInfo.getCurrentState().toString();
    }

    @Override
    public String getDesiredState() {
        return volumeInfo.getDesiredState().toString();
    }

    @Override
    public Date getCreatedTime() {
        return volumeInfo.getCreatedData();
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
    public Map<String, String> getDetails(String source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getDetailSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDetail(String source, String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotEntity takeSnapshotOf(boolean full) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String reserveForMigration(long expirationTime) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void migrate(String reservationToken) {
        // TODO Auto-generated method stub

    }

    @Override
    public VolumeEntity setupForCopy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copy(VolumeEntity dest) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attachTo(String vm, long deviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void detachFrom() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getTemplatePath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTemplateUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeDiskType getDiskType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StorageEntity getDataStore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPath(String path) {
        // TODO Auto-generated method stub

    }

}
