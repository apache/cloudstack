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
package com.cloud.agent.api;

import com.cloud.storage.Storage.StoragePoolType;

public class AttachVolumeCommand extends Command {
    private boolean attach;
    private boolean _managed;
    private String vmName;
    private StoragePoolType pooltype;
    private String volumePath;
    private String volumeName;
    private Long volumeSize;
    private Long deviceId;
    private String chainInfo;
    private String poolUuid;
    private String _storageHost;
    private int _storagePort;
    private String _iScsiName;
    private String _chapInitiatorUsername;
    private String _chapInitiatorPassword;
    private String _chapTargetUsername;
    private String _chapTargetPassword;
    private Long bytesReadRate;
    private Long bytesWriteRate;
    private Long iopsReadRate;
    private Long iopsWriteRate;

    protected AttachVolumeCommand() {
    }

    public AttachVolumeCommand(boolean attach, boolean managed, String vmName,
            StoragePoolType pooltype, String volumePath, String volumeName,
            Long volumeSize, Long deviceId, String chainInfo) {
        this.attach = attach;
        this._managed = managed;
        this.vmName = vmName;
        this.pooltype = pooltype;
        this.volumePath = volumePath;
        this.volumeName = volumeName;
        this.volumeSize = volumeSize;
        this.deviceId = deviceId;
        this.chainInfo = chainInfo;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public boolean getAttach() {
        return attach;
    }

    public String getVmName() {
        return vmName;
    }

    public StoragePoolType getPooltype() {
        return pooltype;
    }

    public void setPooltype(StoragePoolType pooltype) {
        this.pooltype = pooltype;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public String getVolumeName() {
	    return volumeName;
    }

    public Long getVolumeSize() {
        return volumeSize;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getChainInfo() {
        return chainInfo;
    }

    public void setStorageHost(String storageHost) {
        _storageHost = storageHost;
    }

    public String getStorageHost() {
        return _storageHost;
    }

    public void setStoragePort(int storagePort) {
        _storagePort = storagePort;
    }

    public int getStoragePort() {
        return _storagePort;
    }

    public boolean isManaged() {
        return _managed;
    }

    public void set_iScsiName(String iScsiName) {
        this._iScsiName = iScsiName;
    }

    public String get_iScsiName() {
        return _iScsiName;
    }

    public void setChapInitiatorUsername(String chapInitiatorUsername) {
        _chapInitiatorUsername = chapInitiatorUsername;
    }

    public String getChapInitiatorUsername() {
        return _chapInitiatorUsername;
    }

    public void setChapInitiatorPassword(String chapInitiatorPassword) {
        _chapInitiatorPassword = chapInitiatorPassword;
    }

    public String getChapInitiatorPassword() {
        return _chapInitiatorPassword;
    }

    public void setChapTargetUsername(String chapTargetUsername) {
        _chapTargetUsername = chapTargetUsername;
    }

    public String getChapTargetUsername() {
        return _chapTargetUsername;
    }

    public void setChapTargetPassword(String chapTargetPassword) {
        _chapTargetPassword = chapTargetPassword;
    }

    public String getChapTargetPassword() {
        return _chapTargetPassword;
    }

    public void setBytesReadRate(Long bytesReadRate) {
        this.bytesReadRate = bytesReadRate;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public void setBytesWriteRate(Long bytesWriteRate) {
        this.bytesWriteRate = bytesWriteRate;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public void setIopsReadRate(Long iopsReadRate) {
        this.iopsReadRate = iopsReadRate;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public void setIopsWriteRate(Long iopsWriteRate) {
        this.iopsWriteRate = iopsWriteRate;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }
}
