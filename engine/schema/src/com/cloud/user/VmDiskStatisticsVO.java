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
package com.cloud.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vm_disk_statistics")
public class VmDiskStatisticsVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "data_center_id", updatable = false)
    private long dataCenterId;

    @Column(name = "account_id", updatable = false)
    private long accountId;

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "volume_id")
    private Long volumeId;

    @Column(name = "net_io_read")
    private long netIORead;

    @Column(name = "net_io_write")
    private long netIOWrite;

    @Column(name = "current_io_read")
    private long currentIORead;

    @Column(name = "current_io_write")
    private long currentIOWrite;

    @Column(name = "agg_io_read")
    private long aggIORead;

    @Column(name = "agg_io_write")
    private long aggIOWrite;

    @Column(name = "net_bytes_read")
    private long netBytesRead;

    @Column(name = "net_bytes_write")
    private long netBytesWrite;

    @Column(name = "current_bytes_read")
    private long currentBytesRead;

    @Column(name = "current_bytes_write")
    private long currentBytesWrite;

    @Column(name = "agg_bytes_read")
    private long aggBytesRead;

    @Column(name = "agg_bytes_write")
    private long aggBytesWrite;

    protected VmDiskStatisticsVO() {
    }

    public VmDiskStatisticsVO(long accountId, long dcId, Long vmId, Long volumeId) {
        this.accountId = accountId;
        this.dataCenterId = dcId;
        this.vmId = vmId;
        this.volumeId = volumeId;
        this.netBytesRead = 0;
        this.netBytesWrite = 0;
        this.currentBytesRead = 0;
        this.currentBytesWrite = 0;
        this.netBytesRead = 0;
        this.netBytesWrite = 0;
        this.currentBytesRead = 0;
        this.currentBytesWrite = 0;
    }

    public long getAccountId() {
        return accountId;
    }

    public Long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public long getCurrentIORead() {
        return currentIORead;
    }

    public void setCurrentIORead(long currentIORead) {
        this.currentIORead = currentIORead;
    }

    public long getCurrentIOWrite() {
        return currentIOWrite;
    }

    public void setCurrentIOWrite(long currentIOWrite) {
        this.currentIOWrite = currentIOWrite;
    }

    public long getNetIORead() {
        return netIORead;
    }

    public long getNetIOWrite() {
        return netIOWrite;
    }

    public void setNetIORead(long netIORead) {
        this.netIORead = netIORead;
    }

    public void setNetIOWrite(long netIOWrite) {
        this.netIOWrite = netIOWrite;
    }

    public long getAggIORead() {
        return aggIORead;
    }

    public void setAggIORead(long aggIORead) {
        this.aggIORead = aggIORead;
    }

    public long getAggIOWrite() {
        return aggIOWrite;
    }

    public void setAggIOWrite(long aggIOWrite) {
        this.aggIOWrite = aggIOWrite;
    }

    public long getCurrentBytesRead() {
        return currentBytesRead;
    }

    public void setCurrentBytesRead(long currentBytesRead) {
        this.currentBytesRead = currentBytesRead;
    }

    public long getCurrentBytesWrite() {
        return currentBytesWrite;
    }

    public void setCurrentBytesWrite(long currentBytesWrite) {
        this.currentBytesWrite = currentBytesWrite;
    }

    public long getNetBytesRead() {
        return netBytesRead;
    }

    public long getNetBytesWrite() {
        return netBytesWrite;
    }

    public void setNetBytesRead(long netBytesRead) {
        this.netBytesRead = netBytesRead;
    }

    public void setNetBytesWrite(long netBytesWrite) {
        this.netBytesWrite = netBytesWrite;
    }

    public long getAggBytesRead() {
        return aggBytesRead;
    }

    public void setAggBytesRead(long aggBytesRead) {
        this.aggBytesRead = aggBytesRead;
    }

    public long getAggBytesWrite() {
        return aggBytesWrite;
    }

    public void setAggBytesWrite(long aggBytesWrite) {
        this.aggBytesWrite = aggBytesWrite;
    }

}
