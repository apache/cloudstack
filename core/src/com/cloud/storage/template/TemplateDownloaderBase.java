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
package com.cloud.storage.template;

import java.io.File;

import org.apache.log4j.Logger;

import com.cloud.storage.StorageLayer;

public abstract class TemplateDownloaderBase implements TemplateDownloader {
    private static final Logger s_logger = Logger.getLogger(TemplateDownloaderBase.class);
    
    protected String _downloadUrl;
    protected String _toFile;
    protected TemplateDownloader.Status _status= TemplateDownloader.Status.NOT_STARTED;
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
    private long MAX_TEMPLATE_SIZE_IN_BYTES;
    
    public TemplateDownloaderBase(StorageLayer storage, String downloadUrl, String toDir, long maxTemplateSizeInBytes, DownloadCompleteCallback callback) {
    	_storage = storage;
        _downloadUrl = downloadUrl;
        _toDir = toDir;
        _callback = callback;
        _inited = true;
        
        this.MAX_TEMPLATE_SIZE_IN_BYTES  = maxTemplateSizeInBytes;
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
        
        return (int)(100.0*_totalBytes/_remoteSize);
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
    
    public long getMaxTemplateSizeInBytes() { 
		return this.MAX_TEMPLATE_SIZE_IN_BYTES;
	}
    
    @Override
    public void run() {
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
