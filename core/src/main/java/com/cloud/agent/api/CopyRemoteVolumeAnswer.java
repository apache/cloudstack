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

public class CopyRemoteVolumeAnswer extends Answer {

    private String remoteIp;
    private String filename;

    private long size;

    CopyRemoteVolumeAnswer() {
    }

    public CopyRemoteVolumeAnswer(CopyRemoteVolumeCommand cmd, String details, String filename, long size) {
        super(cmd, true, details);
        this.remoteIp = cmd.getRemoteIp();
        this.filename = filename;
        this.size = size;
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

    public String getString() {
        return "CopyRemoteVolumeAnswer [remoteIp=" + remoteIp + "]";
    }
}
