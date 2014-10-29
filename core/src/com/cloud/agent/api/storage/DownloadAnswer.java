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

package com.cloud.agent.api.storage;

import java.io.File;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class DownloadAnswer extends Answer {
    private String jobId;
    private int downloadPct;
    private String errorString;
    private VMTemplateStorageResourceAssoc.Status downloadStatus;
    private String downloadPath;
    private String installPath;
    private long templateSize = 0L;
    private long templatePhySicalSize = 0L;
    private String checkSum;

    public String getCheckSum() {
        return checkSum;
    }

    public int getDownloadPct() {
        return downloadPct;
    }

    public String getErrorString() {
        return errorString;
    }

    public String getDownloadStatusString() {
        return downloadStatus.toString();
    }

    public VMTemplateStorageResourceAssoc.Status getDownloadStatus() {
        return downloadStatus;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    protected DownloadAnswer() {

    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public DownloadAnswer(String errorString, Status status) {
        super();
        this.downloadPct = 0;
        this.errorString = errorString;
        this.downloadStatus = status;
        this.details = errorString;
    }

    public DownloadAnswer(String jobId, int downloadPct, String errorString, Status downloadStatus, String fileSystemPath, String installPath, long templateSize,
            long templatePhySicalSize, String checkSum) {
        super();
        this.jobId = jobId;
        this.downloadPct = downloadPct;
        this.errorString = errorString;
        this.details = errorString;
        this.downloadStatus = downloadStatus;
        this.downloadPath = fileSystemPath;
        this.installPath = fixPath(installPath);
        this.templateSize = templateSize;
        this.templatePhySicalSize = templatePhySicalSize;
        this.checkSum = checkSum;
    }

    public DownloadAnswer(String jobId, int downloadPct, Command command, Status downloadStatus, String fileSystemPath, String installPath) {
        super(command);
        this.jobId = jobId;
        this.downloadPct = downloadPct;
        this.downloadStatus = downloadStatus;
        this.downloadPath = fileSystemPath;
        this.installPath = installPath;
    }

    private static String fixPath(String path) {
        if (path == null) {
            return path;
        }
        if (path.startsWith(File.separator)) {
            path = path.substring(File.separator.length());
        }
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - File.separator.length());
        }
        return path;
    }

    public void setDownloadStatus(VMTemplateStorageResourceAssoc.Status downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = fixPath(installPath);
    }

    public void setTemplateSize(long templateSize) {
        this.templateSize = templateSize;
    }

    public Long getTemplateSize() {
        return templateSize;
    }

    public void setTemplatePhySicalSize(long templatePhySicalSize) {
        this.templatePhySicalSize = templatePhySicalSize;
    }

    public long getTemplatePhySicalSize() {
        return templatePhySicalSize;
    }

}
