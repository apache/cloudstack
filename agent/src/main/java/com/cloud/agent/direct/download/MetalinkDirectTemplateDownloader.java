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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;

public class MetalinkDirectTemplateDownloader extends HttpDirectTemplateDownloader {

    private String metalinkUrl;
    private List<String> metalinkUrls;
    private List<String> metalinkChecksums;
    private Random random = new Random();
    private static final Logger s_logger = Logger.getLogger(MetalinkDirectTemplateDownloader.class.getName());

    public MetalinkDirectTemplateDownloader(String url, String destPoolPath, Long templateId, String checksum, Map<String, String> headers, Integer connectTimeout, Integer soTimeout) {
        super(url, templateId, destPoolPath, checksum, headers, connectTimeout, soTimeout);
        metalinkUrl = url;
        metalinkUrls = UriUtils.getMetalinkUrls(metalinkUrl);
        metalinkChecksums = UriUtils.getMetalinkChecksums(metalinkUrl);
        if (CollectionUtils.isEmpty(metalinkUrls)) {
            throw new CloudRuntimeException("No urls found on metalink file: " + metalinkUrl + ". Not possible to download template " + templateId);
        }
        setUrl(metalinkUrls.get(0));
        s_logger.info("Metalink downloader created, metalink url: " + metalinkUrl + " parsed - " +
                metalinkUrls.size() + " urls and " +
                (CollectionUtils.isNotEmpty(metalinkChecksums) ? metalinkChecksums.size() : "0") + " checksums found");
    }

    @Override
    public boolean downloadTemplate() {
        if (StringUtils.isBlank(getUrl())) {
            throw new CloudRuntimeException("Download url has not been set, aborting");
        }
        String downloadDir = getDirectDownloadTempPath(getTemplateId());
        boolean downloaded = false;
        int i = 0;
        do {
            if (!isRedownload()) {
                setUrl(metalinkUrls.get(i));
            }
            s_logger.info("Trying to download template from url: " + getUrl());
            try {
                File f = new File(getDestPoolPath() + File.separator + downloadDir + File.separator + getFileNameFromUrl());
                if (f.exists()) {
                    f.delete();
                    f.createNewFile();
                }
                setDownloadedFilePath(f.getAbsolutePath());
                request = createRequest(getUrl(), reqHeaders);
                downloaded = super.downloadTemplate();
                if (downloaded) {
                    s_logger.info("Successfully downloaded template from url: " + getUrl());
                }

            } catch (Exception e) {
                s_logger.error("Error downloading template: " + getTemplateId() + " from " + getUrl() + ": " + e.getMessage());
            }
            i++;
        }
        while (!downloaded && !isRedownload() && i < metalinkUrls.size());
        return downloaded;
    }

    @Override
    public boolean validateChecksum() {
        if (StringUtils.isBlank(getChecksum()) && CollectionUtils.isNotEmpty(metalinkChecksums)) {
            String chk = metalinkChecksums.get(random.nextInt(metalinkChecksums.size()));
            setChecksum(chk);
            s_logger.info("Checksum not provided but " + metalinkChecksums.size() + " found on metalink file, performing checksum using one of them: " + chk);
        }
        return super.validateChecksum();
    }
}
