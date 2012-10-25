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

import com.cloud.utils.fsm.StateMachine2;

public enum VolumeState {
	Allocated("The volume is allocated but has not been created yet."),
    Creating("The volume is being created.  getPoolId() should reflect the pool where it is being created."),
    Ready("The volume is ready to be used."),
    Migrating("The volume is migrating to other storage pool"),
    Snapshotting("There is a snapshot created on this volume, not backed up to secondary storage yet"),
    Expunging("The volume is being expunging"),
    Destroy("The volume is destroyed, and can't be recovered."),        
    UploadOp ("The volume upload operation is in progress or in short the volume is on secondary storage");            

    String _description;

    private VolumeState(String description) {
        _description = description;
    }

    public static StateMachine2<VolumeState, VolumeEvent, Volume> getStateMachine() {
        return s_fsm;
    }

    public String getDescription() {
        return _description;
    }

    private final static StateMachine2<VolumeState, VolumeEvent, Volume> s_fsm = new StateMachine2<VolumeState, VolumeEvent, Volume>();
    static {
        s_fsm.addTransition(Allocated, VolumeEvent.CreateRequested, Creating);
        s_fsm.addTransition(Allocated, VolumeEvent.DestroyRequested, Destroy);
        s_fsm.addTransition(Creating, VolumeEvent.OperationRetry, Creating);
        s_fsm.addTransition(Creating, VolumeEvent.OperationFailed, Allocated);
        s_fsm.addTransition(Creating, VolumeEvent.OperationSucceeded, Ready);
        s_fsm.addTransition(Creating, VolumeEvent.DestroyRequested, Destroy);
        s_fsm.addTransition(Creating, VolumeEvent.CreateRequested, Creating);            
        s_fsm.addTransition(Allocated, VolumeEvent.UploadRequested, UploadOp);
        s_fsm.addTransition(UploadOp, VolumeEvent.CopyRequested, Creating);// CopyRequested for volume from sec to primary storage            
        s_fsm.addTransition(Creating, VolumeEvent.CopySucceeded, Ready);
        s_fsm.addTransition(Creating, VolumeEvent.CopyFailed, UploadOp);// Copying volume from sec to primary failed.  
        s_fsm.addTransition(UploadOp, VolumeEvent.DestroyRequested, Destroy);
        s_fsm.addTransition(Ready, VolumeEvent.DestroyRequested, Destroy);
        s_fsm.addTransition(Destroy, VolumeEvent.ExpungingRequested, Expunging);
        s_fsm.addTransition(Ready, VolumeEvent.SnapshotRequested, Snapshotting);
        s_fsm.addTransition(Snapshotting, VolumeEvent.OperationSucceeded, Ready);
        s_fsm.addTransition(Snapshotting, VolumeEvent.OperationFailed, Ready);
        s_fsm.addTransition(Ready, VolumeEvent.MigrationRequested, Migrating);
        s_fsm.addTransition(Migrating, VolumeEvent.OperationSucceeded, Ready);
        s_fsm.addTransition(Migrating, VolumeEvent.OperationFailed, Ready);
        s_fsm.addTransition(Destroy, VolumeEvent.OperationSucceeded, Destroy);
    }
}
