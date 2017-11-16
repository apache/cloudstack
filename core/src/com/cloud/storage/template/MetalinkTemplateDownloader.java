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
        _toFile = toDir + File.separator + filename;
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (!status.equals(Status.NOT_STARTED)) {
            // Only start downloading if we haven't started yet.
            LOGGER.debug("Template download is already started, not starting again. Template: " + _downloadUrl);
            return 0;
        }
        status = Status.IN_PROGRESS;
        Script.runSimpleBashScript("aria2c " + _downloadUrl + " -d " + _toDir);
        status = Status.DOWNLOAD_FINISHED;
        String sizeResult = Script.runSimpleBashScript("ls -als " + _toFile + " | awk '{print $1}'");
        long size = Long.parseLong(sizeResult);
        return size;
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

}
