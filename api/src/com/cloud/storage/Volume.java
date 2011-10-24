/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.cloud.acl.ControlledEntity;
import com.cloud.template.BasedOn;
import com.cloud.utils.fsm.FiniteState;
import com.cloud.utils.fsm.StateMachine;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;
import com.cloud.vm.VirtualMachine;

public interface Volume extends ControlledEntity, BasedOn, StateObject<Volume.State> {
    enum Type {
        UNKNOWN, ROOT, SWAP, DATADISK, ISO
    };

    enum State {
        Allocated("The volume is allocated but has not been created yet."),
        Creating("The volume is being created.  getPoolId() should reflect the pool where it is being created."),
        Ready("The volume is ready to be used."),
        Migrating("The volume is migrating to other storage pool"),
        Snapshotting("There is a snapshot created on this volume, not backed up to secondary storage yet"),
        Expunging("The volume is being expunging"),
        Destroy("The volume is destroyed, and can't be recovered.");

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
            s_fsm.addTransition(Ready, Event.DestroyRequested, Destroy);
            s_fsm.addTransition(Destroy, Event.ExpungingRequested, Expunging);
            s_fsm.addTransition(Ready, Event.SnapshotRequested, Snapshotting);
            s_fsm.addTransition(Snapshotting, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Snapshotting, Event.OperationFailed, Ready);
            s_fsm.addTransition(Ready, Event.MigrationRequested, Migrating);
            s_fsm.addTransition(Migrating, Event.OperationSucceeded, Ready);
            s_fsm.addTransition(Migrating, Event.OperationFailed, Ready);
            s_fsm.addTransition(Destroy, Event.OperationSucceeded, Destroy);
        }
    }

    enum Event {
        CreateRequested, 
        OperationFailed, 
        OperationSucceeded, 
        OperationRetry,
        MigrationRequested,
        SnapshotRequested,
        DestroyRequested,
        ExpungingRequested;
    }

    long getId();

    /**
     * @return the volume name
     */
    String getName();

    /**
     * @return total size of the partition
     */
    long getSize();

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

    long getDiskOfferingId();

    String getChainInfo();

    boolean isRecreatable();
    
    public long getUpdatedCount();
    
    public void incrUpdatedCount();
    
    public Date getUpdated();
}
