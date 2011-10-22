/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.manager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;

public interface VmwareStorageManager {
    Answer execute(VmwareHostService hostService, PrimaryStorageDownloadCommand cmd);
	Answer execute(VmwareHostService hostService, BackupSnapshotCommand cmd);
	Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromVolumeCommand cmd);
	Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromSnapshotCommand cmd);
	Answer execute(VmwareHostService hostService, CopyVolumeCommand cmd);
	Answer execute(VmwareHostService hostService, CreateVolumeFromSnapshotCommand cmd);
}
