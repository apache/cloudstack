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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeProfile;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.springframework.stereotype.Component;

import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.State;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class VolumeManagerImpl implements VolumeManager {
    @Inject
    protected VolumeDao2 _volumeDao;
    private final StateMachine2<State, Event, VolumeVO> s_fsm = new StateMachine2<State, Event, VolumeVO>();
    public VolumeManagerImpl() {
        initStateMachine();
    }

    @Override
    public VolumeVO allocateDuplicateVolume(VolumeVO oldVol) {
        /*
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(), oldVol.getName(), oldVol.getDataCenterId(), oldVol.getDomainId(), oldVol.getAccountId(), oldVol.getDiskOfferingId(), oldVol.getSize());
        newVol.setTemplateId(oldVol.getTemplateId());
        newVol.setDeviceId(oldVol.getDeviceId());
        newVol.setInstanceId(oldVol.getInstanceId());
        newVol.setRecreatable(oldVol.isRecreatable());
        newVol.setReservationId(oldVol.getReservationId());
         */
        return null;
        // return _volumeDao.persist(newVol);
    }

    private void initStateMachine() {
        s_fsm.addTransition(Volume.State.Allocated, Event.CreateRequested, Volume.State.Creating);
        s_fsm.addTransition(Volume.State.Allocated, Event.DestroyRequested, Volume.State.Destroying);
        s_fsm.addTransition(Volume.State.Creating, Event.OperationRetry, Volume.State.Creating);
        s_fsm.addTransition(Volume.State.Creating, Event.OperationFailed, Volume.State.Allocated);
        s_fsm.addTransition(Volume.State.Creating, Event.OperationSucceeded, Volume.State.Ready);
        s_fsm.addTransition(Volume.State.Creating, Event.DestroyRequested, Volume.State.Destroying);
        s_fsm.addTransition(Volume.State.Creating, Event.CreateRequested, Volume.State.Creating);            
        s_fsm.addTransition(Volume.State.Allocated, Event.UploadRequested, Volume.State.UploadOp);
        s_fsm.addTransition(Volume.State.UploadOp, Event.CopyRequested, Volume.State.Creating);// CopyRequested for volume from sec to primary storage            
        s_fsm.addTransition(Volume.State.Creating, Event.CopySucceeded, Volume.State.Ready);
        s_fsm.addTransition(Volume.State.Creating, Event.CopyFailed, Volume.State.UploadOp);// Copying volume from sec to primary failed.  
        s_fsm.addTransition(Volume.State.UploadOp, Event.DestroyRequested, Volume.State.Destroying);
        s_fsm.addTransition(Volume.State.Ready, Event.DestroyRequested, Volume.State.Destroying);
        s_fsm.addTransition(Volume.State.Destroy, Event.ExpungingRequested, Volume.State.Expunging);
        s_fsm.addTransition(Volume.State.Ready, Event.SnapshotRequested, Volume.State.Snapshotting);
        s_fsm.addTransition(Volume.State.Snapshotting, Event.OperationSucceeded, Volume.State.Ready);
        s_fsm.addTransition(Volume.State.Snapshotting, Event.OperationFailed, Volume.State.Ready);
        s_fsm.addTransition(Volume.State.Ready, Event.MigrationRequested, Volume.State.Migrating);
        s_fsm.addTransition(Volume.State.Migrating, Event.OperationSucceeded, Volume.State.Ready);
        s_fsm.addTransition(Volume.State.Migrating, Event.OperationFailed, Volume.State.Ready);
        s_fsm.addTransition(Volume.State.Destroy, Event.OperationSucceeded, Volume.State.Destroy);
        s_fsm.addTransition(Volume.State.Destroying, Event.OperationSucceeded, Volume.State.Destroy);
        s_fsm.addTransition(Volume.State.Destroying, Event.OperationFailed, Volume.State.Destroying);
        s_fsm.addTransition(Volume.State.Destroying, Event.DestroyRequested, Volume.State.Destroying);
    }

    @Override
    public StateMachine2<State, Event, VolumeVO> getStateMachine() {
        return s_fsm;
    }

    @Override
    public VolumeVO processEvent(Volume vol, Volume.Event event) throws NoTransitionException {
        // _volStateMachine.transitTo(vol, event, null, _volumeDao);
        return _volumeDao.findById(vol.getId());
    }

    @Override
    public VolumeProfile getProfile(long volumeId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeVO getVolume(long volumeId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeVO updateVolume(VolumeVO volume) {
        // TODO Auto-generated method stub
        return null;
    }
}
