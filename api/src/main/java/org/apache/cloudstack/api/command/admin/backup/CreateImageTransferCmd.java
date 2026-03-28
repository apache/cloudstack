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

package org.apache.cloudstack.api.command.admin.backup;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.ImageTransferResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.backup.ImageTransfer;
import org.apache.cloudstack.backup.KVMBackupExportService;
import org.apache.cloudstack.context.CallContext;

import com.cloud.utils.EnumUtils;

@APICommand(name = "createImageTransfer",
        description = "Create image transfer for a disk in backup",
        responseObject = ImageTransferResponse.class,
        since = "4.22.0",
        authorized = {RoleType.Admin})
public class CreateImageTransferCmd extends BaseCmd implements AdminCmd {

    @Inject
    private KVMBackupExportService kvmBackupExportService;

    @Parameter(name = ApiConstants.BACKUP_ID,
            type = CommandType.UUID,
            entityType = BackupResponse.class,
            description = "ID of the backup")
    private Long backupId;

    @Parameter(name = ApiConstants.VOLUME_ID,
            type = CommandType.UUID,
            entityType = VolumeResponse.class,
            required = true,
            description = "ID of the disk/volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.DIRECTION,
            type = CommandType.STRING,
            required = true,
            description = "Direction of the transfer: upload, download")
    private String direction;

    @Parameter(name = ApiConstants.FORMAT,
            type = CommandType.STRING,
            description = "Format of the image: cow/raw. Currently only raw is supported for download. Defaults to raw if not provided")
    private String format;

    public Long getBackupId() {
        return backupId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public ImageTransfer.Direction getDirection() {
        return ImageTransfer.Direction.valueOf(direction);
    }

    public ImageTransfer.Format getFormat() {
        return EnumUtils.fromString(ImageTransfer.Format.class, format);
    }

   @Override
    public void execute() {
        ImageTransferResponse response = kvmBackupExportService.createImageTransfer(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
