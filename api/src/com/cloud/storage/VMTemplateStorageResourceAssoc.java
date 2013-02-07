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
package com.cloud.storage;

import java.util.Date;

import org.apache.cloudstack.api.InternalIdentity;

public interface VMTemplateStorageResourceAssoc extends InternalIdentity {
    public static enum Status {
        UNKNOWN, DOWNLOAD_ERROR, NOT_DOWNLOADED, DOWNLOAD_IN_PROGRESS, DOWNLOADED, ABANDONED, UPLOADED, NOT_UPLOADED, UPLOAD_ERROR, UPLOAD_IN_PROGRESS, CREATING, CREATED
    }

    String getInstallPath();

    long getTemplateId();

    void setTemplateId(long templateId);

    int getDownloadPercent();

    void setDownloadPercent(int downloadPercent);

    void setDownloadState(Status downloadState);

    Date getCreated();

    Date getLastUpdated();

    void setLastUpdated(Date date);

    void setInstallPath(String installPath);

    Status getDownloadState();

    void setLocalDownloadPath(String localPath);

    String getLocalDownloadPath();

    void setErrorString(String errorString);

    String getErrorString();

    void setJobId(String jobId);

    String getJobId();;

    long getTemplateSize();

}
