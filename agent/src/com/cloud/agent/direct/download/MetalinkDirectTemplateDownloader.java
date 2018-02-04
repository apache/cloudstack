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

import com.cloud.utils.UriUtils;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MetalinkDirectTemplateDownloader extends HttpDirectTemplateDownloader {

    public MetalinkDirectTemplateDownloader(String url, String destPoolPath, Long templateId, String checksum, Map<String, String> headers) {
        super(url, templateId, destPoolPath, checksum, headers);
    }

    @Override
    public boolean downloadTemplate() {
        s_logger.debug("Retrieving metalink file from: " + getUrl() + " to file: " + getDownloadedFilePath());
        List<String> metalinkUrls = UriUtils.getMetalinkUrls(getUrl());
        if (CollectionUtils.isNotEmpty(metalinkUrls)) {
            String downloadDir = getDirectDownloadTempPath(getTemplateId());
            boolean downloaded = false;
            int i = 0;
            while (!downloaded && i < metalinkUrls.size()) {
                try {
                    setUrl(metalinkUrls.get(i));
                    s_logger.debug("Trying to download template from metalink url: " + getUrl());
                    File f = new File(getDestPoolPath() + File.separator + downloadDir + File.separator + getFileNameFromUrl());
                    if (f.exists()) {
                        f.delete();
                        f.createNewFile();
                    }
                    setDownloadedFilePath(f.getAbsolutePath());
                    request = createRequest(getUrl(), reqHeaders);
                    downloaded = super.downloadTemplate();
                    if (downloaded) {
                        s_logger.debug("Successfully downloaded template from metalink url: " + getUrl());
                        break;
                    }
                } catch (Exception e) {
                    s_logger.error("Error downloading template: " + getTemplateId() + " from " + getUrl() + ": " + e.getMessage());
                }
                i++;
            }
            return downloaded;
        }
        return true;
    }
}
