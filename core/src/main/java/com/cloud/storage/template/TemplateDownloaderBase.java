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

package com.cloud.storage.template;

import java.io.File;

import org.apache.log4j.Logger;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.storage.StorageLayer;

public abstract class TemplateDownloaderBase extends ManagedContextRunnable implements TemplateDownloader {
    private static final Logger s_logger = Logger.getLogger(TemplateDownloaderBase.class);

    protected String _downloadUrl;
    protected String _toFile;
    protected TemplateDownloader.Status _status = TemplateDownloader.Status.NOT_STARTED;
    protected String _errorString = " ";
    protected long _remoteSize = 0;
    protected long _downloadTime = 0;
    protected long _totalBytes;
    protected DownloadCompleteCallback _callback;
    protected boolean _resume = false;
    protected String _toDir;
    protected long _start;
    protected StorageLayer _storage;
    protected boolean _inited = false;
    private long maxTemplateSizeInBytes;

    public TemplateDownloaderBase(StorageLayer storage, String downloadUrl, String toDir, long maxTemplateSizeInBytes, DownloadCompleteCallback callback) {
        _storage = storage;
        _downloadUrl = downloadUrl;
        _toDir = toDir;
        _callback = callback;
        _inited = true;

        this.maxTemplateSizeInBytes = maxTemplateSizeInBytes;
    }

    @Override
    public String getDownloadError() {
        return _errorString;
    }

    @Override
    public String getDownloadLocalPath() {
        File file = new File(_toFile);
        return file.getAbsolutePath();
    }

    @Override
    public int getDownloadPercent() {
        if (_remoteSize == 0) {
            return 0;
        }

        return (int)(100.0 * _totalBytes / _remoteSize);
    }

    @Override
    public long getDownloadTime() {
        return _downloadTime;
    }

    @Override
    public long getDownloadedBytes() {
        return _totalBytes;
    }

    @Override
    public Status getStatus() {
        return _status;
    }

    @Override
    public void setDownloadError(String string) {
        _errorString = string;
    }

    @Override
    public void setStatus(Status status) {
        _status = status;
    }

    @Override
    public boolean stopDownload() {
        switch (getStatus()) {
            case IN_PROGRESS:
            case UNKNOWN:
            case NOT_STARTED:
            case RECOVERABLE_ERROR:
            case UNRECOVERABLE_ERROR:
            case ABORTED:
                _status = TemplateDownloader.Status.ABORTED;
                break;
            case DOWNLOAD_FINISHED:
                break;
            default:
                break;
        }
        File f = new File(_toFile);
        if (f.exists()) {
            f.delete();
        }
        return true;
    }

    @Override
    public long getMaxTemplateSizeInBytes() {
        return this.maxTemplateSizeInBytes;
    }

    @Override
    protected void runInContext() {
        try {
            download(_resume, _callback);
        } catch (Exception e) {
            s_logger.warn("Unable to complete download due to ", e);
            _errorString = "Failed to install: " + e.getMessage();
            _status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
        }
    }

    @Override
    public void setResume(boolean resume) {
        _resume = resume;

    }

    @Override
    public boolean isInited() {
        return _inited;
    }
}
