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

package org.apache.cloudstack.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.serializer.Param;
import org.apache.commons.lang3.StringUtils;
import com.google.gson.annotations.SerializedName;

public class VolumeMetricsResponse extends VolumeResponse {
    @SerializedName(ApiConstants.SIZEGB)
    @Param(description = "disk size in GiB")
    private String diskSizeGB;

    @SerializedName(ApiConstants.DISK_IO_PSTOTAL)
    @Param(description = "the total disk iops")
    private Long diskIopsTotal;

    public void setStorageType(final String storageType, final String volumeType) {
        if (StringUtils.isNoneEmpty(storageType, volumeType)) {
            this.setStorageType(String.format("%s (%s)", storageType.substring(0, 1).toUpperCase() + storageType.substring(1), volumeType));
        }
    }

    public void setDiskSizeGB(final Long size) {
        if (size != null) {
            this.diskSizeGB = String.format("%.2f GiB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setDiskIopsTotal(final Long diskIoRead, final Long diskIoWrite) {
        if (diskIoRead != null && diskIoWrite != null) {
            this.diskIopsTotal = diskIoRead + diskIoWrite;
        }
    }
}
