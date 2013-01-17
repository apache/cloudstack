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
package org.apache.cloudstack.storage.volume;

import java.io.File;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.log4j.Logger;

import com.cloud.storage.Volume;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class VolumeObject implements VolumeInfo {
    private static final Logger s_logger = Logger.getLogger(VolumeObject.class);
    protected VolumeVO volumeVO;
    private StateMachine2<Volume.State, Volume.Event, VolumeVO> _volStateMachine;
    protected DataStore dataStore;
    @Inject
    VolumeDao2 volumeDao;
    @Inject
    VolumeManager volumeMgr;

    private VolumeObject(DataStore dataStore, VolumeVO volumeVO) {
        this.volumeVO = volumeVO;
        this.dataStore = dataStore;
    }

    public static VolumeObject getVolumeObject(DataStore dataStore,
            VolumeVO volumeVO) {
        VolumeObject vo = new VolumeObject(dataStore, volumeVO);
        vo = ComponentContext.inject(vo);
        return vo;
    }

    @Override
    public String getUuid() {
        return volumeVO.getUuid();
    }

    public void setPath(String uuid) {
        volumeVO.setPath(uuid);
    }

    public Volume.State getState() {
        return volumeVO.getState();
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public long getSize() {
        return volumeVO.getSize();
    }

    public long getVolumeId() {
        return volumeVO.getId();
    }

    public boolean stateTransit(Volume.Event event) {
        boolean result = false;
        _volStateMachine = volumeMgr.getStateMachine();
        try {
            result = _volStateMachine.transitTo(volumeVO, event, null,
                    volumeDao);
        } catch (NoTransitionException e) {
            String errorMessage = "Failed to transit volume: "
                    + this.getVolumeId() + ", due to: " + e.toString();
            s_logger.debug(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }
        return result;
    }

    public void update() {
        volumeDao.update(volumeVO.getId(), volumeVO);
        volumeVO = volumeDao.findById(volumeVO.getId());
    }

    @Override
    public long getId() {
        return this.volumeVO.getId();
    }

    @Override
    public boolean isAttachedVM() {
        return (this.volumeVO.getInstanceId() == null) ? false : true;
    }

    @Override
    public String getUri() {
        return this.dataStore.getUri() + File.separator + "?type=volume&path="
                + this.volumeVO.getPath();
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.VOLUME;
    }

    @Override
    public DiskFormat getFormat() {
        // TODO Auto-generated method stub
        return null;
    }
}
