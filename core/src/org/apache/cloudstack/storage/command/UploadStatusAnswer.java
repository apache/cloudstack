//
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
//

package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;

public class UploadStatusAnswer extends Answer {
    public static enum UploadStatus {
        UNKNOWN, IN_PROGRESS, COMPLETED, ERROR
    }

    private UploadStatus status;
    private long virtualSize = 0;
    private long physicalSize = 0;
    private String installPath = null;
    private int downloadPercent = 0;

    protected UploadStatusAnswer() {
    }

    public UploadStatusAnswer(UploadStatusCommand cmd, UploadStatus status, String msg) {
        super(cmd, false, msg);
        this.status = status;
    }

    public UploadStatusAnswer(UploadStatusCommand cmd, Exception e) {
        super(cmd, false, e.getMessage());
        this.status = UploadStatus.ERROR;
    }

    public UploadStatusAnswer(UploadStatusCommand cmd, UploadStatus status) {
        super(cmd, true, null);
        this.status = status;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public void setPhysicalSize(long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public int getDownloadPercent() {
        return downloadPercent;
    }

    public void setDownloadPercent(int downloadPercent) {
        this.downloadPercent = downloadPercent;
    }
}
