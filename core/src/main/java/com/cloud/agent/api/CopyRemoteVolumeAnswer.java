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

import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;

import java.util.Map;

public class CopyRemoteVolumeAnswer extends Answer {

    private String remoteIp;
    private String filename;

    private long size;
    private Map<VolumeOnStorageTO.Detail, String> volumeDetails;

    CopyRemoteVolumeAnswer() {
    }

    public CopyRemoteVolumeAnswer(CopyRemoteVolumeCommand cmd, final boolean success, String details, String filename, long size,
                                  Map<VolumeOnStorageTO.Detail, String> volumeDetails) {
        super(cmd, success, details);
        this.remoteIp = cmd.getRemoteIp();
        this.filename = filename;
        this.size = size;
        this.volumeDetails = volumeDetails;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public Map<VolumeOnStorageTO.Detail, String> getVolumeDetails() {
        return volumeDetails;
    }

    public String getString() {
        return "CopyRemoteVolumeAnswer [remoteIp=" + remoteIp + "]";
    }
}
