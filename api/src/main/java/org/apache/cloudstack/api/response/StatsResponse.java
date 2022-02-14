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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class StatsResponse extends BaseResponse {

    @SerializedName("timestamp")
    @Param(description = "the time when the VM stats were collected. The format is \"yyyy-MM-dd hh:mm:ss\"")
    private Date timestamp;

    @SerializedName("cpuused")
    @Param(description = "the amount (percentage) of the VM's CPU currently used")
    private String cpuUsed;

    @SerializedName(ApiConstants.DISK_IO_READ)
    @Param(description = "the VM's disk read (IO)")
    protected Long diskIORead;

    @SerializedName(ApiConstants.DISK_IO_WRITE)
    @Param(description = "the VM's disk write (IO)")
    protected Long diskIOWrite;

    @SerializedName(ApiConstants.DISK_IO_PSTOTAL)
    @Param(description = "the total disk iops")
    protected Long diskIopsTotal = 0L;

    @SerializedName(ApiConstants.DISK_KBS_READ)
    @Param(description = "the VM's disk read (bytes)")
    private Long diskKbsRead;

    @SerializedName(ApiConstants.DISK_KBS_WRITE)
    @Param(description = "the VM's disk write (bytes)")
    private Long diskKbsWrite;

    @SerializedName("memoryintfreekbs")
    @Param(description = "the internal memory free of the VM or zero if it cannot be calculated")
    private Long memoryIntFreeKBs;

    @SerializedName("memorykbs")
    @Param(description = "the memory used by the VM in Kbps")
    private Long memoryKBs;

    @SerializedName("memorytargetkbs")
    @Param(description = "the target memory in VM in Kbps")
    private Long memoryTargetKBs;

    @SerializedName("networkkbsread")
    @Param(description = "the incoming network traffic on the VM")
    protected Long networkKbsRead;

    @SerializedName("networkkbswrite")
    @Param(description = "the outgoing network traffic on the host")
    protected Long networkKbsWrite;

    @SerializedName("networkread")
    @Param(description = "the network read in MiB")
    protected String networkRead;

    @SerializedName("networkwrite")
    @Param(description = "the network write in MiB")
    protected String networkWrite;

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setCpuUsed(String cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public void setDiskIORead(Long diskIORead) {
        this.diskIORead = diskIORead;
        accumulateDiskIopsTotal(diskIORead);
    }

    public void setDiskIOWrite(Long diskIOWrite) {
        this.diskIOWrite = diskIOWrite;
        accumulateDiskIopsTotal(diskIOWrite);
    }

    public void setDiskKbsRead(Long diskKbsRead) {
        this.diskKbsRead = diskKbsRead;
    }

    public void setDiskKbsWrite(Long diskKbsWrite) {
        this.diskKbsWrite = diskKbsWrite;
    }

    public void setMemoryIntFreeKBs(Long memoryIntFreeKBs) {
        this.memoryIntFreeKBs = memoryIntFreeKBs;
    }

    public void setMemoryKBs(Long memoryKBs) {
        this.memoryKBs = memoryKBs;
    }

    public void setMemoryTargetKBs(Long memoryTargetKBs) {
        this.memoryTargetKBs = memoryTargetKBs;
    }

    public void setNetworkKbsRead(Long networkKbsRead) {
        this.networkKbsRead = networkKbsRead;
        if (networkKbsRead != null) {
            this.networkRead = String.format("%.2f MB", networkKbsRead / 1024.0);
        }
    }

    public void setNetworkKbsWrite(Long networkKbsWrite) {
        this.networkKbsWrite = networkKbsWrite;
        if (networkKbsWrite != null) {
            this.networkWrite = String.format("%.2f MB", networkKbsWrite / 1024.0);
        }
    }

    /**
     * Accumulate disk IOPS (Input/Output Operations Per Second)
     * in {@code diskIopsTotal} attribute.
     * @param diskIo the IOPS value to increment in {@code diskIopsTotal}.
     */
    protected void accumulateDiskIopsTotal(Long diskIo) {
        if (diskIo != null) {
            this.diskIopsTotal += diskIo;
        }
    }
}