/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.storage;

import java.util.Date;

public interface Upload {

    public static enum Status {
        UNKNOWN, ABANDONED, UPLOADED, NOT_UPLOADED, UPLOAD_ERROR, UPLOAD_IN_PROGRESS, NOT_COPIED, COPY_IN_PROGRESS, COPY_ERROR, COPY_COMPLETE, DOWNLOAD_URL_CREATED, DOWNLOAD_URL_NOT_CREATED, ERROR
    }

    public static enum Type {
        VOLUME, TEMPLATE, ISO
    }

    public static enum Mode {
        FTP_UPLOAD, HTTP_DOWNLOAD
    }

    long getHostId();

    long getId();

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
