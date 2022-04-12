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

import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VmMetricsResponse extends UserVmResponse {
    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the VM's primary IP address")
    private String ipAddress;

    @SerializedName("cputotal")
    @Param(description = "the total cpu capacity in Ghz")
    private String cpuTotal;

    @SerializedName("memorytotal")
    @Param(description = "the total memory capacity in GiB")
    private String memTotal;

    @SerializedName("networkread")
    @Param(description = "network read in MiB")
    private String networkRead;

    @SerializedName("networkwrite")
    @Param(description = "network write in MiB")
    private String networkWrite;

    @SerializedName("diskread")
    @Param(description = "disk read in MiB")
    private String diskRead;

    @SerializedName("diskwrite")
    @Param(description = "disk write in MiB")
    private String diskWrite;

    @SerializedName(ApiConstants.DISK_IO_PSTOTAL)
    @Param(description = "the total disk iops")
    private Long diskIopsTotal;

    public void setIpAddress(final Set<NicResponse> nics) {
        if (nics != null && nics.size() > 0) {
            this.ipAddress = nics.iterator().next().getIpaddress();
        }
    }

    public void setCpuTotal(final Integer cpuNumber, final Integer cpuSpeed) {
        if (cpuNumber != null && cpuSpeed != null) {
            this.cpuTotal = String.format("%.1f Ghz", cpuNumber * cpuSpeed / 1000.0);
        }
    }

    public void setMemTotal(final Integer memory) {
        if (memory != null) {
            this.memTotal = String.format("%.2f GiB", memory / 1024.0);
        }
    }

    public void setNetworkRead(final Long networkReadKbs) {
        if (networkReadKbs != null) {
            this.networkRead = String.format("%.2f MiB", networkReadKbs / 1024.0);
        }
    }

    public void setNetworkWrite(final Long networkWriteKbs) {
        if (networkWriteKbs != null) {
            this.networkWrite = String.format("%.2f MiB", networkWriteKbs / 1024.0);
        }
    }

    public void setDiskRead(final Long diskReadKbs) {
        if (diskReadKbs != null) {
            this.diskRead = String.format("%.2f MiB", diskReadKbs / 1024.0);
        }
    }

    public void setDiskWrite(final Long diskWriteKbs) {
        if (diskWriteKbs != null) {
            this.diskWrite = String.format("%.2f MiB", diskWriteKbs / 1024.0);
        }
    }

    public void setDiskIopsTotal(final Long diskIoRead, final Long diskIoWrite) {
        if (diskIoRead != null && diskIoWrite != null) {
            this.diskIopsTotal = diskIoRead + diskIoWrite;
        }
    }
}
