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
package com.cloud.usage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "usage_vm_disk")
public class UsageVmDiskVO {
    @Id
    @Column(name = "account_id")
    private long accountId;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "volume_id")
    private Long volumeId;

    @Column(name = "io_read")
    private long ioRead;

    @Column(name = "io_write")
    private long ioWrite;

    @Column(name = "agg_io_write")
    private long aggIOWrite;

    @Column(name = "agg_io_read")
    private long aggIORead;

    @Column(name = "bytes_read")
    private long bytesRead;

    @Column(name = "bytes_write")
    private long bytesWrite;

    @Column(name = "agg_bytes_write")
    private long aggBytesWrite;

    @Column(name = "agg_bytes_read")
    private long aggBytesRead;

    @Column(name = "event_time_millis")
    private long eventTimeMillis = 0;

    protected UsageVmDiskVO() {
    }

    public UsageVmDiskVO(Long accountId, long zoneId, Long vmId, Long volumeId, long ioRead, long ioWrite, long aggIORead, long aggIOWrite, long bytesRead,
            long bytesWrite, long aggBytesRead, long aggBytesWrite, long eventTimeMillis) {
        this.accountId = accountId;
        this.zoneId = zoneId;
        this.vmId = vmId;
        this.volumeId = volumeId;
        this.ioRead = ioRead;
        this.ioWrite = ioWrite;
        this.aggIOWrite = aggIOWrite;
        this.aggIORead = aggIORead;
        this.bytesRead = bytesRead;
        this.bytesWrite = bytesWrite;
        this.aggBytesWrite = aggBytesWrite;
        this.aggBytesRead = aggBytesRead;
        this.eventTimeMillis = eventTimeMillis;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getIORead() {
        return ioRead;
    }

    public void setIORead(Long ioRead) {
        this.ioRead = ioRead;
    }

    public Long getIOWrite() {
        return ioWrite;
    }

    public void setIOWrite(Long ioWrite) {
        this.ioWrite = ioWrite;
    }

    public Long getBytesRead() {
        return bytesRead;
    }

    public void setBytesRead(Long bytesRead) {
        this.bytesRead = bytesRead;
    }

    public Long getBytesWrite() {
        return bytesWrite;
    }

    public void setBytesWrite(Long bytesWrite) {
        this.bytesWrite = bytesWrite;
    }

    public long getEventTimeMillis() {
        return eventTimeMillis;
    }

    public void setEventTimeMillis(long eventTimeMillis) {
        this.eventTimeMillis = eventTimeMillis;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public long getAggIOWrite() {
        return aggIOWrite;
    }

    public void setAggIOWrite(long aggIOWrite) {
        this.aggIOWrite = aggIOWrite;
    }

    public long getAggIORead() {
        return aggIORead;
    }

    public void setAggIORead(long aggIORead) {
        this.aggIORead = aggIORead;
    }

    public long getAggBytesWrite() {
        return aggBytesWrite;
    }

    public void setAggBytesWrite(long aggBytesWrite) {
        this.aggBytesWrite = aggBytesWrite;
    }

    public long getAggBytesRead() {
        return aggBytesRead;
    }

    public void setAggBytesRead(long aggBytesRead) {
        this.aggBytesRead = aggBytesRead;
    }
}
