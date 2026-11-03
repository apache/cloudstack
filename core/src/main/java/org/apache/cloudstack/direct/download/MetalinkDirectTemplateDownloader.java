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
package org.apache.cloudstack.direct.download;

import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.collections.CollectionUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MetalinkDirectTemplateDownloader extends DirectTemplateDownloaderImpl {
    private List<String> metalinkUrls;
    private List<String> metalinkChecksums;
    private Random random = new Random();
    protected DirectTemplateDownloader downloader;
    private Map<String, String> headers;
    private Integer connectTimeout;
    private Integer soTimeout;
    private List<String> allowedCidrs = Collections.emptyList();

    protected DirectTemplateDownloader createDownloaderForMetalinks(String url, Long templateId,
                String destPoolPath, String checksum, Map<String, String> headers, Integer connectTimeout,
                Integer soTimeout, Integer connectionRequestTimeout, String temporaryDownloadPath) {
        if (url.toLowerCase().startsWith("https:")) {
            return new HttpsDirectTemplateDownloader(url, templateId, destPoolPath, checksum, headers,
                    connectTimeout, soTimeout, connectionRequestTimeout, temporaryDownloadPath,
                    this.isFollowRedirects());
        } else if (url.toLowerCase().startsWith("http:")) {
            return new HttpDirectTemplateDownloader(url, templateId, destPoolPath, checksum, headers,
                    connectTimeout, soTimeout, temporaryDownloadPath, this.isFollowRedirects());
        } else if (url.toLowerCase().startsWith("nfs:")) {
            return new NfsDirectTemplateDownloader(url, destPoolPath, templateId, checksum, temporaryDownloadPath);
        } else {
            logger.error(String.format("Cannot find a suitable downloader to handle the metalink URL %s."
                    + " Only http and https schemes are permitted inside metalink files.", url));
            return null;
        }
    }

    protected MetalinkDirectTemplateDownloader(String url, Integer connectTimeout, Integer socketTimeout, boolean followRedirects) {
        this(url, null, null, null, null, connectTimeout, socketTimeout, null, followRedirects, java.util.Collections.emptyList());
    }

    public MetalinkDirectTemplateDownloader(String url, String destPoolPath, Long templateId, String checksum,
                Map<String, String> headers, Integer connectTimeout, Integer soTimeout, String downloadPath,
                boolean followRedirects) {
        this(url, destPoolPath, templateId, checksum, headers, connectTimeout, soTimeout, downloadPath, followRedirects,
                java.util.Collections.emptyList());
    }

    public MetalinkDirectTemplateDownloader(String url, String destPoolPath, Long templateId, String checksum,
                Map<String, String> headers, Integer connectTimeout, Integer soTimeout, String downloadPath,
                boolean followRedirects, List<String> allowedCidrs) {
        super(url, destPoolPath, templateId, checksum, downloadPath, followRedirects);
        this.headers = headers;
        this.connectTimeout = connectTimeout;
        this.soTimeout = soTimeout;
        this.allowedCidrs = allowedCidrs != null ? allowedCidrs : java.util.Collections.emptyList();
        downloader = createDownloaderForMetalinks(url, templateId, destPoolPath, checksum, headers,
                connectTimeout, soTimeout, null, downloadPath);
        metalinkUrls = downloader.getMetalinkUrls(url);
        metalinkChecksums = downloader.getMetalinkChecksums(url);
        if (CollectionUtils.isEmpty(metalinkUrls)) {
            logger.error(String.format("No URLs found on metalink file: %s. Not possible to download Template %s ", url, templateId));
        } else {
            setUrl(metalinkUrls.get(0));
            logger.info("Metalink downloader created, metalink URL: " + url + " parsed - " +
                    metalinkUrls.size() + " urls and " +
                    (CollectionUtils.isNotEmpty(metalinkChecksums) ? metalinkChecksums.size() : "0") + " checksums found");
        }
    }

    public List<String> getAllowedCidrs() {
        return allowedCidrs;
    }

    @Override
    public Pair<Boolean, String> downloadTemplate() {
        if (StringUtils.isBlank(getUrl())) {
            throw new CloudRuntimeException("Download url has not been set, aborting");
        }
        boolean downloaded = false;
        int i = 0;
        String downloadDir = getDirectDownloadTempPath(getTemplateId());
        do {
            if (!isRedownload()) {
                setUrl(metalinkUrls.get(i));
            }
            try {
                UriUtils.validateMetalinkInnerUrl(getUrl(), allowedCidrs);
            } catch (IllegalArgumentException e) {
                logger.warn(String.format("Skipping metalink inner URL that failed SSRF validation: %s - %s", getUrl(), e.getMessage()));
                i++;
                continue;
            }
            logger.info("Trying to download Template from URL: " + getUrl());
            try {
                DirectTemplateDownloader urlDownloader = createDownloaderForMetalinks(getUrl(), getTemplateId(), getDestPoolPath(),
                        getChecksum(), headers, connectTimeout, soTimeout, null, temporaryDownloadPath);
                setDownloadedFilePath(downloadDir + File.separator + getTemporaryFileName());
                File f = new File(getDownloadedFilePath());
                if (f.exists()) {
                    f.delete();
                    f.createNewFile();
                }
                Pair<Boolean, String> downloadResult = urlDownloader.downloadTemplate();
                downloaded = downloadResult.first();
                if (downloaded) {
                    logger.info("Successfully downloaded Template from URL: " + getUrl());
                }
            } catch (Exception e) {
                logger.error(String.format("Error downloading Template: %s from URL: %s due to: %s", getTemplateId(), getUrl(), e.getMessage()), e);
            }
            i++;
        }
        while (!downloaded && !isRedownload() && i < metalinkUrls.size());
        return new Pair<>(downloaded, getDownloadedFilePath());
    }

    @Override
    public boolean validateChecksum() {
        if (StringUtils.isBlank(getChecksum()) && CollectionUtils.isNotEmpty(metalinkChecksums)) {
            String chk = metalinkChecksums.get(random.nextInt(metalinkChecksums.size()));
            setChecksum(chk);
            logger.info("Checksum not provided but " + metalinkChecksums.size() + " found on metalink file, performing checksum using one of them: " + chk);
        }
        return super.validateChecksum();
    }

    @Override
    public boolean checkUrl(String metalinkUrl) {
        if (!downloader.checkUrl(metalinkUrl)) {
            logger.error(String.format("Metalink URL check failed for: %s", metalinkUrl));
            return false;
        }

        List<String> metalinkUrls = downloader.getMetalinkUrls(metalinkUrl);
        for (String url : metalinkUrls) {
            if (url.endsWith(".torrent")) {
                continue;
            }
            try {
                UriUtils.validateMetalinkInnerUrl(url, allowedCidrs);
            } catch (IllegalArgumentException e) {
                logger.warn(String.format("Skipping metalink inner URL that failed SSRF validation in checkUrl: %s - %s", url, e.getMessage()));
                continue;
            }
            DirectTemplateDownloader urlDownloader;
            try {
                urlDownloader = createDownloaderForMetalinks(url, null, null, null, headers, connectTimeout, soTimeout, null, null);
            } catch (Exception e) {
                logger.warn(String.format("Skipping metalink inner URL that failed validation in checkUrl: %s - %s", url, e.getMessage()));
                continue;
            }
            if (urlDownloader == null || !urlDownloader.checkUrl(url)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Long getRemoteFileSize(String metalinkUrl, String format) {
        List<String> urls = downloader.getMetalinkUrls(metalinkUrl);
        for (String url : urls) {
            if (url.endsWith("torrent")) {
                continue;
            }
            try {
                UriUtils.validateMetalinkInnerUrl(url, allowedCidrs);
            } catch (IllegalArgumentException e) {
                logger.warn(String.format("Skipping metalink inner URL that failed SSRF validation in getRemoteFileSize: %s - %s ", url, e.getMessage()));
                continue;
            }
            if (downloader.checkUrl(url)) {
                return downloader.getRemoteFileSize(url, format);
            }
        }
        return null;
    }

    @Override
    public List<String> getMetalinkUrls(String metalinkUrl) {
        return downloader.getMetalinkUrls(metalinkUrl);
    }

    @Override
    public List<String> getMetalinkChecksums(String metalinkUrl) {
        return downloader.getMetalinkChecksums(metalinkUrl);
    }

}
