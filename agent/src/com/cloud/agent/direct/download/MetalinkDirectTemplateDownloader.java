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
package com.cloud.agent.direct.download;

import com.cloud.utils.script.Script;

import java.io.File;

public class MetalinkDirectTemplateDownloader extends DirectTemplateDownloaderImpl {

    private String downloadDir;

    public MetalinkDirectTemplateDownloader(String url, String destPoolPath, Long templateId, String checksum) {
        super(url, destPoolPath, templateId, checksum);
        String relativeDir = getDirectDownloadTempPath(templateId);
        downloadDir = getDestPoolPath() + File.separator + relativeDir;
        createFolder(downloadDir);
    }

    @Override
    public boolean downloadTemplate() {
        String downloadCommand = "aria2c " + getUrl() + " -d " + downloadDir + " --check-integrity=true";
        Script.runSimpleBashScript(downloadCommand);
        //Remove .metalink file
        Script.runSimpleBashScript("rm -f " + downloadDir + File.separator + getFileNameFromUrl());
        String fileName = Script.runSimpleBashScript("ls " + downloadDir);
        if (fileName == null) {
            return false;
        }
        setDownloadedFilePath(downloadDir + File.separator + fileName);
        return true;
    }
}
