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

package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.backup.ImageTransfer;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ImageTransfer.class)
public class ImageTransferResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the image transfer")
    private String id;

    @SerializedName("backupid")
    @Param(description = "the backup ID")
    private String backupId;

    @SerializedName("vmid")
    @Param(description = "the VM ID")
    private String vmId;

    @SerializedName(ApiConstants.VOLUME_ID)
    @Param(description = "the disk/volume ID")
    private String diskId;

    @SerializedName("devicename")
    @Param(description = "the device name (vda, vdb, etc)")
    private String deviceName;

    @SerializedName("transferurl")
    @Param(description = "the transfer URL")
    private String transferUrl;

    @SerializedName("phase")
    @Param(description = "the transfer phase")
    private String phase;

    @SerializedName("direction")
    @Param(description = "the image transfer direction: upload / download")
    private String direction;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date created")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setTransferUrl(String transferUrl) {
        this.transferUrl = transferUrl;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
