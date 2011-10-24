/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.manager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteSnapshotBackupAnswer;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.CreatePrivateTemplateCommand;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.utils.component.Manager;

public interface MockStorageManager extends Manager {
    public static final long DEFAULT_HOST_STORAGE_SIZE = 1 * 1024 * 1024 * 1024 * 1024L; //1T
    public static final long DEFAULT_TEMPLATE_SIZE = 1 * 1000 * 1000 * 1000L; //1G
    
	public PrimaryStorageDownloadAnswer primaryStorageDownload(PrimaryStorageDownloadCommand cmd);
	
	public CreateAnswer createVolume(CreateCommand cmd);
	public AttachVolumeAnswer AttachVolume(AttachVolumeCommand cmd);
	public Answer AttachIso(AttachIsoCommand cmd);
	
	public Answer DeleteStoragePool(DeleteStoragePoolCommand cmd);
	public Answer ModifyStoragePool(ModifyStoragePoolCommand cmd);
	public Answer CreateStoragePool(CreateStoragePoolCommand cmd);
	
	public Answer SecStorageSetup(SecStorageSetupCommand cmd);
	public Answer ListTemplates(ListTemplateCommand cmd);
	public Answer Destroy(DestroyCommand cmd);
	public Answer Download(DownloadCommand cmd);
	public Answer DownloadProcess(DownloadProgressCommand cmd);
	public GetStorageStatsAnswer GetStorageStats(GetStorageStatsCommand cmd);
	public Answer ManageSnapshot(ManageSnapshotCommand cmd);
	public Answer BackupSnapshot(BackupSnapshotCommand cmd, SimulatorInfo info);
	public Answer DeleteSnapshotBackup(DeleteSnapshotBackupCommand cmd);
	public Answer CreateVolumeFromSnapshot(CreateVolumeFromSnapshotCommand cmd);
	public Answer DeleteTemplate(DeleteTemplateCommand cmd);
	public Answer SecStorageVMSetup(SecStorageVMSetupCommand cmd);
	
	public void preinstallTemplates(String url, long zoneId);

    StoragePoolInfo getLocalStorage(String hostGuid);

    public Answer CreatePrivateTemplateFromSnapshot(CreatePrivateTemplateFromSnapshotCommand cmd);

    public Answer ComputeChecksum(ComputeChecksumCommand cmd);

    public Answer CreatePrivateTemplateFromVolume(CreatePrivateTemplateFromVolumeCommand cmd);

    StoragePoolInfo getLocalStorage(String hostGuid, Long storageSize);

	CopyVolumeAnswer CopyVolume(CopyVolumeCommand cmd);
}
