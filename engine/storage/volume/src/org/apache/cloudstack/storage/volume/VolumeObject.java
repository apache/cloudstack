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

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.storage.encoding.EncodingType;

public class VolumeObject implements VolumeInfo {
    private static final Logger s_logger = Logger.getLogger(VolumeObject.class);
    protected VolumeVO volumeVO;
    private StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    protected DataStore dataStore;
    @Inject
    VolumeDao volumeDao;
    @Inject
    ObjectInDataStoreManager ojbectInStoreMgr;
    private Object payload;

    public VolumeObject() {
        _volStateMachine = Volume.State.getStateMachine();
    }
    
    protected void configure(DataStore dataStore, VolumeVO volumeVO) {
        this.volumeVO = volumeVO;
        this.dataStore = dataStore;
    }

    public static VolumeObject getVolumeObject(DataStore dataStore, VolumeVO volumeVO) {
        VolumeObject vo = ComponentContext.inject(VolumeObject.class);
        vo.configure(dataStore, volumeVO);
        return vo;
    }

    @Override
    public String getUuid() {
        return volumeVO.getUuid();
    }

    public void setPath(String uuid) {
        volumeVO.setPath(uuid);
    }
    
    public void setSize(Long size) {
    	volumeVO.setSize(size);
    }

    public Volume.State getState() {
        return volumeVO.getState();
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public Long getSize() {
        return volumeVO.getSize();
    }

    public long getVolumeId() {
        return volumeVO.getId();
    }
    public boolean stateTransit(Volume.Event event) {
        boolean result = false;
        try {
            result = _volStateMachine.transitTo(volumeVO, event, null, volumeDao);
            volumeVO = volumeDao.findById(volumeVO.getId());
        } catch (NoTransitionException e) {
            String errorMessage = "Failed to transit volume: " + this.getVolumeId() + ", due to: " + e.toString();
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
        if (this.dataStore == null) {
            throw new CloudRuntimeException("datastore must be set before using this object");
        }
        DataObjectInStore obj = ojbectInStoreMgr.findObject(this.volumeVO.getUuid(), DataObjectType.VOLUME, this.dataStore.getUuid(), this.dataStore.getRole());
        if (obj.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            return this.dataStore.getUri() + 
                    "&" + EncodingType.OBJTYPE + "=" + DataObjectType.VOLUME + 
                    "&" + EncodingType.SIZE + "=" + this.volumeVO.getSize() + 
                    "&" + EncodingType.NAME + "=" + this.volumeVO.getName();
        } else {
            return this.dataStore.getUri() +
                    "&" + EncodingType.OBJTYPE + "=" + DataObjectType.VOLUME + 
                    "&" + EncodingType.PATH + "=" + obj.getInstallPath();
        }
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

    @Override
    public void processEvent(
            ObjectInDataStoreStateMachine.Event event) {
        if (this.dataStore == null) {
            return;
        }
        try {
            Volume.Event volEvent = null;
            if (this.dataStore.getRole() == DataStoreRole.Image) {
                ojbectInStoreMgr.update(this, event);
                if (event == ObjectInDataStoreStateMachine.Event.CreateRequested) {
                    volEvent = Volume.Event.UploadRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.OperationSuccessed) {
                    volEvent = Volume.Event.CopySucceeded;
                } else if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                    volEvent = Volume.Event.CopyFailed;
                }
            } else {
                if (event == ObjectInDataStoreStateMachine.Event.CreateRequested ||
                        event == ObjectInDataStoreStateMachine.Event.CreateOnlyRequested) {
                    volEvent = Volume.Event.CreateRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.CopyingRequested) {
                    volEvent = Volume.Event.CopyRequested;
                }
            }
            
            if (event == ObjectInDataStoreStateMachine.Event.DestroyRequested) {
                volEvent = Volume.Event.DestroyRequested;
            } else if (event == ObjectInDataStoreStateMachine.Event.ExpungeRequested) {
                volEvent = Volume.Event.ExpungingRequested;
            } else if (event == ObjectInDataStoreStateMachine.Event.OperationSuccessed) {
                volEvent = Volume.Event.OperationSucceeded;
            } else if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                volEvent = Volume.Event.OperationFailed;
            } else if (event == ObjectInDataStoreStateMachine.Event.ResizeRequested) {
            	volEvent = Volume.Event.ResizeRequested;
            }
            this.stateTransit(volEvent);
        } catch (Exception e) {
            s_logger.debug("Failed to update state", e);
            throw new CloudRuntimeException("Failed to update state:" + e.toString());
        }

    }

    @Override
    public String getName() {
        return this.volumeVO.getName();
    }

    @Override
    public Long getInstanceId() {
        return this.volumeVO.getInstanceId();
    }

    @Override
    public String getFolder() {
        return this.volumeVO.getFolder();
    }

    @Override
    public String getPath() {
        return this.volumeVO.getPath();
    }

    @Override
    public Long getPodId() {
        return this.volumeVO.getPodId();
    }

    @Override
    public long getDataCenterId() {
        return this.volumeVO.getDataCenterId();
    }

    @Override
    public Type getVolumeType() {
        return this.volumeVO.getVolumeType();
    }

    @Override
    public Long getPoolId() {
        return this.volumeVO.getPoolId();
    }

    @Override
    public Date getAttached() {
        return this.volumeVO.getAttached();
    }

    @Override
    public Long getDeviceId() {
        return this.volumeVO.getDeviceId();
    }

    @Override
    public Date getCreated() {
        return this.volumeVO.getCreated();
    }

    @Override
    public long getDiskOfferingId() {
        return this.volumeVO.getDiskOfferingId();
    }

    @Override
    public String getChainInfo() {
        return this.volumeVO.getChainInfo();
    }

    @Override
    public boolean isRecreatable() {
        return this.volumeVO.isRecreatable();
    }

    @Override
    public long getUpdatedCount() {
        return this.volumeVO.getUpdatedCount();
    }

    @Override
    public void incrUpdatedCount() {
        this.volumeVO.incrUpdatedCount();
    }

    @Override
    public Date getUpdated() {
        return this.volumeVO.getUpdated();
    }

    @Override
    public String getReservationId() {
        return this.volumeVO.getReservationId();
    }

    @Override
    public void setReservationId(String reserv) {
        this.volumeVO.setReservationId(reserv);
    }

    @Override
    public long getAccountId() {
        return this.volumeVO.getAccountId();
    }

    @Override
    public long getDomainId() {
        return this.volumeVO.getDomainId();
    }

    @Override
    public Long getTemplateId() {
        return this.volumeVO.getTemplateId();
    }

    @Override
    public void addPayload(Object data) {
        this.payload = data;
    }

    @Override
    public Object getpayload() {
       return this.payload;
    }

	@Override
	public HypervisorType getHypervisorType() {
		return this.volumeDao.getHypervisorType(this.volumeVO.getId());
	}

	@Override
	public Long getLastPoolId() {
		return this.volumeVO.getLastPoolId();
	}
}
