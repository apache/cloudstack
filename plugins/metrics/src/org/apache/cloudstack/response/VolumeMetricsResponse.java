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

import com.cloud.serializer.Param;
import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.response.VolumeResponse;

public class VolumeMetricsResponse extends VolumeResponse {
    @SerializedName("sizegb")
    @Param(description = "disk size in GiB")
    private String diskSizeGB;

    public void setStorageType(final String storageType, final String volumeType) {
        if (!Strings.isNullOrEmpty(storageType) && !Strings.isNullOrEmpty(volumeType)) {
            this.setStorageType(String.format("%s (%s)", storageType.substring(0, 1).toUpperCase() + storageType.substring(1), volumeType));
        }
    }

    public void setDiskSizeGB(final Long size) {
        if (size != null) {
            this.diskSizeGB = String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
