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
import com.cloud.storage.Upload;

public class UploadAnswer extends Answer {

    private String jobId;
    private int uploadPct;
    private String errorString;
    private Upload.Status uploadStatus;
    private String uploadPath;
    private String installPath;
    public Long templateSize = 0L;

    public int getUploadPct() {
        return uploadPct;
    }

    public String getErrorString() {
        return errorString;
    }

    public String getUploadStatusString() {
        return uploadStatus.toString();
    }

    public Upload.Status getUploadStatus() {
        return uploadStatus;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    protected UploadAnswer() {

    }

    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public UploadAnswer(String jobId, int uploadPct, String errorString, Upload.Status uploadStatus, String fileSystemPath, String installPath, long templateSize) {
        super();
        this.jobId = jobId;
        this.uploadPct = uploadPct;
        this.errorString = errorString;
        this.details = errorString;
        this.uploadStatus = uploadStatus;
        this.uploadPath = fileSystemPath;
        this.installPath = fixPath(installPath);
        this.templateSize = templateSize;
    }

    public UploadAnswer(String jobId, int uploadPct, Command command, Upload.Status uploadStatus, String fileSystemPath, String installPath) {
        super(command);
        this.jobId = jobId;
        this.uploadPct = uploadPct;
        this.uploadStatus = uploadStatus;
        this.uploadPath = fileSystemPath;
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

    public void setUploadStatus(Upload.Status uploadStatus) {
        this.uploadStatus = uploadStatus;
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

}
