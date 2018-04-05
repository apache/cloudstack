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
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.response.StoragePoolResponse;

public class StoragePoolMetricsResponse extends StoragePoolResponse {
    @SerializedName("disksizeusedgb")
    @Param(description = "disk size used in GiB")
    private String diskSizeUsedGB;

    @SerializedName("disksizetotalgb")
    @Param(description = "disk size in GiB")
    private String diskSizeTotalGB;

    @SerializedName("disksizeallocatedgb")
    @Param(description = "disk size allocated in GiB")
    private String diskSizeAllocatedGB;

    @SerializedName("disksizeunallocatedgb")
    @Param(description = "disk size unallocated in GiB")
    private String diskSizeUnallocatedGB;

    @SerializedName("storageusagethreshold")
    @Param(description = "storage usage notification threshold exceeded")
    private Boolean storageUsedThreshold;

    @SerializedName("storageusagedisablethreshold")
    @Param(description = "storage usage disable threshold exceeded")
    private Boolean storageUsedDisableThreshold;

    @SerializedName("storageallocatedthreshold")
    @Param(description = "storage allocated notification threshold exceeded")
    private Boolean storageAllocatedThreshold;

    @SerializedName("storageallocateddisablethreshold")
    @Param(description = "storage allocated disable threshold exceeded")
    private Boolean storageAllocatedDisableThreshold;

    public void setDiskSizeUsedGB(final Long diskSizeUsed) {
        if (diskSizeUsed != null) {
            this.diskSizeUsedGB = String.format("%.2f GB", diskSizeUsed / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setDiskSizeTotalGB(final Long totalDiskSize, final String overProvisionFactor) {
        if (totalDiskSize != null && overProvisionFactor != null) {
            this.diskSizeTotalGB = String.format("%.2f GB (x%s)", totalDiskSize / (1024.0 * 1024.0 * 1024.0), overProvisionFactor);
        }
    }

    public void setDiskSizeAllocatedGB(final Long diskSizeAllocated) {
        if (diskSizeAllocated != null) {
            this.diskSizeAllocatedGB = String.format("%.2f GB", diskSizeAllocated / (1024.0 * 1024.0 * 1024.0));

        }
    }

    public void setDiskSizeUnallocatedGB(final Long totalDiskSize, final Long diskSizeAllocated, final String overProvisionFactor) {
        if (totalDiskSize != null && diskSizeAllocated != null && overProvisionFactor != null) {
            this.diskSizeUnallocatedGB = String.format("%.2f GB", ((Double.valueOf(overProvisionFactor) * totalDiskSize) - diskSizeAllocated) / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public void setStorageUsedThreshold(final Long totalDiskSize, final Long diskSizeUsed, final String overProvisionFactor, final Double threshold) {
        if (totalDiskSize != null && diskSizeUsed != null && overProvisionFactor != null && threshold != null) {
            this.storageUsedThreshold = diskSizeUsed > (totalDiskSize * Double.valueOf(overProvisionFactor) * threshold) ;
        }
    }

    public void setStorageUsedDisableThreshold(final Long totalDiskSize, final Long diskSizeUsed, final String overProvisionFactor, final Double threshold) {
        if (totalDiskSize != null && diskSizeUsed != null && overProvisionFactor != null && threshold != null) {
            this.storageUsedDisableThreshold = diskSizeUsed > (totalDiskSize * Double.valueOf(overProvisionFactor) * threshold);
        }
    }

    public void setStorageAllocatedThreshold(final Long totalDiskSize, final Long diskSizeAllocated, final String overProvisionFactor, final Double threshold) {
        if (totalDiskSize != null && diskSizeAllocated != null && overProvisionFactor != null && threshold != null) {
            this.storageAllocatedThreshold = diskSizeAllocated > (totalDiskSize * Double.valueOf(overProvisionFactor) * threshold);
        }
    }

    public void setStorageAllocatedDisableThreshold(final Long totalDiskSize, final Long diskSizeAllocated, final String overProvisionFactor, final Double threshold) {
        if (totalDiskSize != null && diskSizeAllocated != null && overProvisionFactor != null && threshold != null) {
            this.storageAllocatedDisableThreshold = diskSizeAllocated > (totalDiskSize * Double.valueOf(overProvisionFactor) * threshold);
        }
    }
}
