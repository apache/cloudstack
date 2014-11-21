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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface Upload extends InternalIdentity, Identity {

    public static enum Status {
        UNKNOWN,
        ABANDONED,
        UPLOADED,
        NOT_UPLOADED,
        UPLOAD_ERROR,
        UPLOAD_IN_PROGRESS,
        NOT_COPIED,
        COPY_IN_PROGRESS,
        COPY_ERROR,
        COPY_COMPLETE,
        DOWNLOAD_URL_CREATED,
        DOWNLOAD_URL_NOT_CREATED,
        ERROR
    }

    public static enum Type {
        VOLUME, TEMPLATE, ISO
    }

    public static enum Mode {
        FTP_UPLOAD, HTTP_DOWNLOAD
    }

    long getDataStoreId();

    Date getCreated();

    Date getLastUpdated();

    String getErrorString();

    String getJobId();

    int getUploadPercent();

    Status getUploadState();

    long getTypeId();

    Type getType();

    Mode getMode();

    String getUploadUrl();

    void setId(Long id);

    void setCreated(Date created);

    String getInstallPath();

    void setInstallPath(String installPath);

}
