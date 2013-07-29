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
package com.cloud.storage;

import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.template.BasedOn;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

public interface Volume extends ControlledEntity, Identity, InternalIdentity, BasedOn, StateObject<Volume.State> {
    enum Type {
        UNKNOWN, ROOT, SWAP, DATADISK, ISO
    };

    enum State {
        Allocated("The volume is allocated but has not been created yet."),
        Creating("The volume is being created.  getPoolId() should reflect the pool where it is being created."),
        Ready("The volume is ready to be used."),
        Migrating("The volume is migrating to other storage pool"),
        Snapshotting("There is a snapshot created on this volume, not backed up to secondary storage yet"),
        Resizing("The volume is being resized"),
        Expunging("The volume is being expunging"),
        Expunged("The volume is being expunging"),
        Destroy("The volume is destroyed, and can't be recovered."), 
        Destroying("The volume is destroying, and can't be recovered."),  
        UploadOp ("The volume upload operation is in progress or in short the volume is on secondary storage"),
        Uploading("volume is uploading"),
        Copying("volume is copying from image store to primary, in case it's an uploaded volume"),
        Uploaded("volume is uploaded");

        String _description;

        private State(String description) {
            _description = description;
        }

        public static StateMachine2<State, Event, Volume> getStateMachine() {
            return s_fsm;
        }

        public String getDescription() {
            return _description;
        }

        private final static StateMachine2<State, Event, Volume> s_fsm = new StateMachine2<State, Event, Volume>();
        static {
            s_fsm.addTransition(Allocated, Event.CreateRequested, Creating);
            s_fsm.addTransition(Allocated, Event.DestroyRequested, Destroy);
            s_fsm.addTransition(Creating, Event.OperationRetry, Creating);
            s_fsm.addTransition(Creating, Event.OperationFailed, Allocated);
            s_fsm.addTransition(Creating, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Creating, Event.DestroyRequested, Destroy);
            s_fsm.addTransition(Creating, Event.CreateRequested, Creating);            
            s_fsm.addTransition(Ready, Event.ResizeRequested, Resizing);
            s_fsm.addTransition(Resizing, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Resizing, Event.OperationFailed, Ready);          
            s_fsm.addTransition(Allocated, Event.UploadRequested, UploadOp);
            s_fsm.addTransition(Uploaded, Event.CopyRequested, Copying);            
            s_fsm.addTransition(Copying, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Copying, Event.OperationFailed, Uploaded);
            s_fsm.addTransition(UploadOp, Event.DestroyRequested, Destroy);
            s_fsm.addTransition(Ready, Event.DestroyRequested, Destroy);
            s_fsm.addTransition(Destroy, Event.ExpungingRequested, Expunging);
            s_fsm.addTransition(Expunging, Event.ExpungingRequested, Expunging);
            s_fsm.addTransition(Expunging, Event.OperationSucceeded, Expunged);
            s_fsm.addTransition(Expunging, Event.OperationFailed, Expunging);
            s_fsm.addTransition(Ready, Event.SnapshotRequested, Snapshotting);
            s_fsm.addTransition(Snapshotting, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Snapshotting, Event.OperationFailed, Ready);
            s_fsm.addTransition(Ready, Event.MigrationRequested, Migrating);
            s_fsm.addTransition(Migrating, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Migrating, Event.OperationFailed, Ready);
            s_fsm.addTransition(Destroy, Event.OperationSucceeded, Destroy);
            s_fsm.addTransition(UploadOp, Event.OperationSucceeded, Uploaded);
            s_fsm.addTransition(UploadOp, Event.OperationFailed, Allocated);
            s_fsm.addTransition(Uploaded, Event.DestroyRequested, Destroy);
            s_fsm.addTransition(Expunged, Event.ExpungingRequested, Expunged);
            s_fsm.addTransition(Expunged, Event.OperationSucceeded, Expunged);
            s_fsm.addTransition(Expunged, Event.OperationFailed, Expunged);
        }
    }

    enum Event {
        CreateRequested,
        CopyRequested,
        CopySucceeded,
        CopyFailed,
        OperationFailed,
        OperationSucceeded,
        OperationRetry,
        UploadRequested,
        MigrationRequested,
        SnapshotRequested,
        DestroyRequested,
        ExpungingRequested,
        ResizeRequested;
    }

    /**
     * @return the volume name
     */
    String getName();

    /**
     * @return total size of the partition
     */
    Long getSize();

    Long getMinIops();

    Long getMaxIops();

    String get_iScsiName();

    /**
     * @return the vm instance id
     */
    Long getInstanceId();

    /**
     * @return the folder of the volume
     */
    String getFolder();

    /**
     * @return the path created.
     */
    String getPath();

    Long getPodId();

    long getDataCenterId();

    Type getVolumeType();

    Long getPoolId();

    State getState();

    Date getAttached();

    Long getDeviceId();

    Date getCreated();

    Long getDiskOfferingId();

    String getChainInfo();

    boolean isRecreatable();

    public long getUpdatedCount();

    public void incrUpdatedCount();

    public Date getUpdated();

	/**
	 * @return
	 */
	String getReservationId();

	/**
	 * @param reserv
	 */
	void setReservationId(String reserv);
	Storage.ImageFormat getFormat();
	Long getVmSnapshotChainSize();
}
