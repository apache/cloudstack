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
package com.cloud.agent.manager;

import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.command.UploadStatusAnswer;
import org.apache.cloudstack.storage.command.UploadStatusCommand;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.storage.ListVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.utils.component.Manager;

public interface MockStorageManager extends Manager {
    public static final long DEFAULT_HOST_STORAGE_SIZE = 1 * 1024 * 1024 * 1024 * 1024L; //1T
    public static final long DEFAULT_TEMPLATE_SIZE = 1 * 1000 * 1000 * 1000L; //1G

    public PrimaryStorageDownloadAnswer primaryStorageDownload(PrimaryStorageDownloadCommand cmd);

    public CreateAnswer createVolume(CreateCommand cmd);

    public Answer AttachIso(AttachIsoCommand cmd);

    public Answer DeleteStoragePool(DeleteStoragePoolCommand cmd);

    public Answer ModifyStoragePool(ModifyStoragePoolCommand cmd);

    public Answer CreateStoragePool(CreateStoragePoolCommand cmd);

    public Answer SecStorageSetup(SecStorageSetupCommand cmd);

    public Answer ListTemplates(ListTemplateCommand cmd);

    public Answer ListVolumes(ListVolumeCommand cmd);

    public Answer Destroy(DestroyCommand cmd);

    public Answer Download(DownloadCommand cmd);

    public Answer DownloadProcess(DownloadProgressCommand cmd);

    GetVolumeStatsAnswer getVolumeStats(GetVolumeStatsCommand cmd);

    public GetStorageStatsAnswer GetStorageStats(GetStorageStatsCommand cmd);

    public Answer ManageSnapshot(ManageSnapshotCommand cmd);

    public Answer BackupSnapshot(BackupSnapshotCommand cmd, SimulatorInfo info);

    //public Answer DeleteSnapshotBackup(DeleteSnapshotBackupCommand cmd);
    public Answer CreateVolumeFromSnapshot(CreateVolumeFromSnapshotCommand cmd);

    //public Answer DeleteTemplate(DeleteTemplateCommand cmd);
    public Answer Delete(DeleteCommand cmd);

    public Answer SecStorageVMSetup(SecStorageVMSetupCommand cmd);

    public void preinstallTemplates(String url, long zoneId);

    StoragePoolInfo getLocalStorage(String hostGuid);

    public Answer CreatePrivateTemplateFromSnapshot(CreatePrivateTemplateFromSnapshotCommand cmd);

    public Answer ComputeChecksum(ComputeChecksumCommand cmd);

    public Answer CreatePrivateTemplateFromVolume(CreatePrivateTemplateFromVolumeCommand cmd);

    StoragePoolInfo getLocalStorage(String hostGuid, Long storageSize);

    CopyVolumeAnswer CopyVolume(CopyVolumeCommand cmd);

    public UploadStatusAnswer getUploadStatus(UploadStatusCommand cmd);

    Answer handleConfigDriveIso(HandleConfigDriveIsoCommand cmd);

    Answer handleResizeVolume(ResizeVolumeCommand cmd);
}
