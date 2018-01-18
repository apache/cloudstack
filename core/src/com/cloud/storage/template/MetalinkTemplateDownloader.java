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

import com.cloud.storage.StorageLayer;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import java.io.File;

public class MetalinkTemplateDownloader extends TemplateDownloaderBase implements TemplateDownloader {

    private TemplateDownloader.Status status = TemplateDownloader.Status.NOT_STARTED;

    private static final Logger LOGGER = Logger.getLogger(MetalinkTemplateDownloader.class.getName());

    public MetalinkTemplateDownloader(StorageLayer storageLayer, String downloadUrl, String toDir, DownloadCompleteCallback callback, long maxTemplateSize) {
        super(storageLayer, downloadUrl, toDir, maxTemplateSize, callback);
        String[] parts = _downloadUrl.split("/");
        String filename = parts[parts.length - 1];
        _callback = callback;
        _toFile = toDir + File.separator + filename;
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (_status == Status.ABORTED || _status == Status.UNRECOVERABLE_ERROR || _status == Status.DOWNLOAD_FINISHED) {
            return 0;
        }

        LOGGER.info("Starting metalink download from: " + _downloadUrl);
        _start = System.currentTimeMillis();

        status = Status.IN_PROGRESS;
        Script.runSimpleBashScript("aria2c " + _downloadUrl + " -d " + _toDir);
        String metalinkFile = _toFile;
        Script.runSimpleBashScript("rm -f " + metalinkFile);
        String templateFileName = Script.runSimpleBashScript("ls " + _toDir);
        String downloadedFile = _toDir + File.separator + templateFileName;
        _toFile = _toDir + File.separator + "tmpdownld_";
        Script.runSimpleBashScript("mv " + downloadedFile + " " + _toFile);

        File file = new File(_toFile);
        if (!file.exists()) {
            _status = Status.UNRECOVERABLE_ERROR;
            LOGGER.error("Error downloading template from: " + _downloadUrl);
            return 0;
        }
        _totalBytes = file.length();
        status = Status.DOWNLOAD_FINISHED;
        _downloadTime = System.currentTimeMillis() - _start;
        if (_callback != null) {
            _callback.downloadComplete(status);
        }
        return _totalBytes;
    }

    @Override
    public int getDownloadPercent() {
        if (status == Status.DOWNLOAD_FINISHED) {
            return 100;
        } else if (status == Status.IN_PROGRESS) {
            return 50;
        } else {
            return 0;
        }
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }
}
