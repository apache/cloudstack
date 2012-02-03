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

/**
 * @author chiradeep
 * 
 */
public interface VMTemplateStorageResourceAssoc {
    public static enum Status {
        UNKNOWN, DOWNLOAD_ERROR, NOT_DOWNLOADED, DOWNLOAD_IN_PROGRESS, DOWNLOADED, ABANDONED, UPLOADED, NOT_UPLOADED, UPLOAD_ERROR, UPLOAD_IN_PROGRESS
    }

    String getInstallPath();

    long getTemplateId();

    void setTemplateId(long templateId);

    int getDownloadPercent();

    void setDownloadPercent(int downloadPercent);

    void setDownloadState(Status downloadState);

    long getId();

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
