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

import java.util.Arrays;
import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.template.BasedOn;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

public interface Volume extends ControlledEntity, Identity, InternalIdentity, BasedOn, StateObject<Volume.State>, Displayable {

    // Managed storage volume parameters (specified in the compute/disk offering for PowerFlex)
    String BANDWIDTH_LIMIT_IN_MBPS = "bandwidthLimitInMbps";
    String IOPS_LIMIT = "iopsLimit";

    enum Type {
        UNKNOWN, ROOT, SWAP, DATADISK, ISO
    };

    enum State {
        Allocated("The volume is allocated but has not been created yet."),
        Creating("The volume is being created.  getPoolId() should reflect the pool where it is being created."),
        Ready("The volume is ready to be used."),
        Migrating("The volume is migrating to other storage pool"),
        Snapshotting("There is a snapshot created on this volume, not backed up to secondary storage yet"),
        RevertSnapshotting("There is a snapshot created on this volume, the volume is being reverting from snapshot"),
        Resizing("The volume is being resized"),
        Expunging("The volume is being expunging"),
        Expunged("The volume has been expunged, and can no longer be recovered"),
        Destroy("The volume is destroyed, and can be recovered."),
        Destroying("The volume is destroying, and can't be recovered."),
        UploadOp("The volume upload operation is in progress or in short the volume is on secondary storage"),
        Copying("Volume is copying from image store to primary, in case it's an uploaded volume"),
        Uploaded("Volume is uploaded"),
        NotUploaded("The volume entry is just created in DB, not yet uploaded"),
        UploadInProgress("Volume upload is in progress"),
        UploadError("Volume upload encountered some error"),
        UploadAbandoned("Volume upload is abandoned since the upload was never initiated within a specified time"),
        Attaching("The volume is attaching to a VM from Ready state."),
        Restoring("The volume is being restored from backup.");

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
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.CreateRequested, Creating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.OperationFailed, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.OperationSucceeded, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.OperationRetry, Creating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.OperationFailed, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.CreateRequested, Creating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.CreateRequested, Creating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.ResizeRequested, Resizing, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Resizing, Event.OperationSucceeded, Ready, Arrays.asList(new StateMachine2.Transition.Impact[]{StateMachine2.Transition.Impact.USAGE})));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Resizing, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.UploadRequested, UploadOp, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Uploaded, Event.CopyRequested, Copying, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Copying, Event.OperationSucceeded, Ready, Arrays.asList(new StateMachine2.Transition.Impact[]{StateMachine2.Transition.Impact.USAGE})));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Copying, Event.OperationFailed, Uploaded, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadOp, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroy, Event.ExpungingRequested, Expunging, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.ExpungingRequested, Expunging, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.OperationSucceeded, Expunged,null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunging, Event.OperationFailed, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.SnapshotRequested, Snapshotting, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Snapshotting, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Snapshotting, Event.OperationFailed, Ready,null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.RevertSnapshotRequested, RevertSnapshotting, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(RevertSnapshotting, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(RevertSnapshotting, Event.OperationFailed, Ready,null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Allocated, Event.MigrationCopyRequested, Creating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.MigrationCopyFailed, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Creating, Event.MigrationCopySucceeded, Ready, Arrays.asList(new StateMachine2.Transition.Impact[]{StateMachine2.Transition.Impact.USAGE})));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.MigrationRequested, Migrating, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Migrating, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Migrating, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroy, Event.OperationSucceeded, Destroy, Arrays.asList(new StateMachine2.Transition.Impact[]{StateMachine2.Transition.Impact.USAGE})));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroy, Event.OperationFailed, Destroy, Arrays.asList(StateMachine2.Transition.Impact.USAGE)));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadOp, Event.OperationSucceeded, Uploaded, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadOp, Event.OperationFailed, Allocated, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Uploaded, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunged, Event.ExpungingRequested, Expunged, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunged, Event.OperationSucceeded, Expunged, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunged, Event.OperationFailed, Expunged, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.OperationTimeout, UploadAbandoned, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.UploadRequested, UploadInProgress, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.OperationSucceeded, Uploaded, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(NotUploaded, Event.OperationFailed, UploadError, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadInProgress, Event.OperationSucceeded, Uploaded, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadInProgress, Event.OperationFailed, UploadError, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadInProgress, Event.OperationTimeout, UploadError, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadError, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(UploadAbandoned, Event.DestroyRequested, Destroy, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.AttachRequested, Attaching, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Attaching, Event.OperationSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Attaching, Event.OperationFailed, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroy, Event.RecoverRequested, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Ready, Event.RestoreRequested, Restoring, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Expunged, Event.RestoreRequested, Restoring, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Destroy, Event.RestoreRequested, Restoring, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Restoring, Event.RestoreSucceeded, Ready, null));
            s_fsm.addTransition(new StateMachine2.Transition<State, Event>(Restoring, Event.RestoreFailed, Ready, null));
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
        MigrationCopyRequested,
        MigrationCopySucceeded,
        MigrationCopyFailed,
        SnapshotRequested,
        RevertSnapshotRequested,
        DestroyRequested,
        RecoverRequested,
        ExpungingRequested,
        ResizeRequested,
        AttachRequested,
        OperationTimeout,
        RestoreRequested,
        RestoreSucceeded,
        RestoreFailed;
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

    @Override
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

    Storage.ProvisioningType getProvisioningType();

    Long getVmSnapshotChainSize();

    Integer getHypervisorSnapshotReserve();

    @Deprecated
    boolean isDisplayVolume();

    boolean isDisplay();

    boolean isDeployAsIs();

    String getExternalUuid();

    void setExternalUuid(String externalUuid);

    public Long getPassphraseId();

    public void setPassphraseId(Long id);

    public String getEncryptFormat();

    public void setEncryptFormat(String encryptFormat);
}
