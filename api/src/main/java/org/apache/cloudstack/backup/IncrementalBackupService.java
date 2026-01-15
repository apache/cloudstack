//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import java.util.List;

import org.apache.cloudstack.api.command.admin.backup.CreateImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.DeleteVmCheckpointCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeBackupCmd;
import org.apache.cloudstack.api.command.admin.backup.FinalizeImageTransferCmd;
import org.apache.cloudstack.api.command.admin.backup.ListImageTransfersCmd;
import org.apache.cloudstack.api.command.admin.backup.ListVmCheckpointsCmd;
import org.apache.cloudstack.api.command.admin.backup.StartBackupCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.CheckpointResponse;
import org.apache.cloudstack.api.response.ImageTransferResponse;

import com.cloud.utils.component.PluggableService;

/**
 * Service for managing oVirt-style incremental backups using libvirt checkpoints
 */
public interface IncrementalBackupService extends PluggableService {

    /**
     * Start a backup session for a VM
     * Creates a new checkpoint and starts NBD server for pull-mode backup
     */
    BackupResponse startBackup(StartBackupCmd cmd);

    /**
     * Finalize a backup session
     * Stops NBD server, updates checkpoint tracking, deletes old checkpoints
     */
    boolean finalizeBackup(FinalizeBackupCmd cmd);

    /**
     * Create an image transfer object for a disk
     * Registers NBD endpoint with ImageIO (stubbed for POC)
     */
    ImageTransferResponse createImageTransfer(CreateImageTransferCmd cmd);

    /**
     * Finalize an image transfer
     * Marks transfer as complete (NBD is closed globally in finalize backup)
     */
    boolean finalizeImageTransfer(FinalizeImageTransferCmd cmd);

    /**
     * List image transfers for a backup
     */
    List<ImageTransferResponse> listImageTransfers(ListImageTransfersCmd cmd);

    /**
     * List checkpoints for a VM
     */
    List<CheckpointResponse> listVmCheckpoints(ListVmCheckpointsCmd cmd);

    /**
     * Delete a VM checkpoint (no-op for normal flow, kept for API parity)
     */
    boolean deleteVmCheckpoint(DeleteVmCheckpointCmd cmd);
}
