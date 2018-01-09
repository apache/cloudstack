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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.utils.security.ChecksumValue;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.UUID;

public abstract class DirectTemplateDownloaderImpl implements DirectTemplateDownloader {

    private String url;
    private String destPoolPath;
    private Long templateId;
    private String downloadedFilePath;
    private String installPath;
    private String checksum;

    protected DirectTemplateDownloaderImpl(final String url, final String destPoolPath, final Long templateId, final String checksum) {
        this.url = url;
        this.destPoolPath = destPoolPath;
        this.templateId = templateId;
        this.checksum = checksum;
    }

    private static String directDownloadDir = "template";

    /**
     * Return direct download temporary path to download template
     */
    protected static String getDirectDownloadTempPath(Long templateId) {
        String templateIdAsString = String.valueOf(templateId);
        return directDownloadDir + File.separator + templateIdAsString.substring(0,1) +
                File.separator + templateIdAsString;
    }

    /**
     * Create folder on path if it does not exist
     */
    protected void createFolder(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public String getUrl() {
        return url;
    }

    public String getDestPoolPath() {
        return destPoolPath;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getDownloadedFilePath() {
        return downloadedFilePath;
    }

    public void setDownloadedFilePath(String filePath) {
        this.downloadedFilePath = filePath;
    }

    /**
     * Return filename from url
     */
    public String getFileNameFromUrl() {
        String[] urlParts = url.split("/");
        return urlParts[urlParts.length - 1];
    }

    /**
     * Checks if downloaded template is extractable
     * @return true if it should be extracted, false if not
     */
    private boolean isTemplateExtractable() {
        String type = Script.runSimpleBashScript("file " + downloadedFilePath + " | awk -F' ' '{print $2}'");
        return type.equalsIgnoreCase("bzip2") || type.equalsIgnoreCase("gzip") || type.equalsIgnoreCase("zip");
    }

    @Override
    public boolean extractAndInstallDownloadedTemplate() {
        installPath = UUID.randomUUID().toString();
        if (isTemplateExtractable()) {
            extractDownloadedTemplate();
        } else {
            Script.runSimpleBashScript("mv " + downloadedFilePath + " " + getInstallFullPath());
        }
        return true;
    }

    /**
     * Return install full path
     */
    private String getInstallFullPath() {
        return destPoolPath + File.separator + installPath;
    }

    /**
     * Return extract command to execute given downloaded file
     */
    private String getExtractCommandForDownloadedFile() {
        if (downloadedFilePath.endsWith(".zip")) {
            return "unzip -p " + downloadedFilePath + " | cat > " + getInstallFullPath();
        } else if (downloadedFilePath.endsWith(".bz2")) {
            return "bunzip2 -c " + downloadedFilePath + " > " + getInstallFullPath();
        } else if (downloadedFilePath.endsWith(".gz")) {
            return "gunzip -c " + downloadedFilePath + " > " + getInstallFullPath();
        } else {
            throw new CloudRuntimeException("Unable to extract template " + templateId + " on " + downloadedFilePath);
        }
    }

    /**
     * Extract downloaded template into installPath, remove compressed file
     */
    private void extractDownloadedTemplate() {
        String extractCommand = getExtractCommandForDownloadedFile();
        Script.runSimpleBashScript(extractCommand);
        Script.runSimpleBashScript("rm -f " + downloadedFilePath);
    }

    @Override
    public DirectTemplateInformation getTemplateInformation() {
        String sizeResult = Script.runSimpleBashScript("ls -als " + getInstallFullPath() + " | awk '{print $1}'");
        long size = Long.parseLong(sizeResult);
        return new DirectTemplateInformation(installPath, size, checksum);
    }

    /**
     * Return checksum command from algorithm
     */
    private String getChecksumCommandFromAlgorithm(String algorithm) {
        if (algorithm.equalsIgnoreCase("MD5")) {
            return "md5sum";
        } else if (algorithm.equalsIgnoreCase("SHA-1")) {
            return "sha1sum";
        } else if (algorithm.equalsIgnoreCase("SHA-224")) {
            return "sha224sum";
        } else if (algorithm.equalsIgnoreCase("SHA-256")) {
            return "sha256sum";
        } else if (algorithm.equalsIgnoreCase("SHA-384")) {
            return "sha384sum";
        } else if (algorithm.equalsIgnoreCase("SHA-512")) {
            return "sha512sum";
        } else {
            throw new CloudRuntimeException("Unknown checksum algorithm: " + algorithm);
        }
    }

    @Override
    public boolean validateChecksum() {
        if (StringUtils.isNotBlank(checksum)) {
            ChecksumValue providedChecksum = new ChecksumValue(checksum);
            String algorithm = providedChecksum.getAlgorithm();
            String checksumCommand = "echo '%s %s' | %s -c --quiet";
            String cmd = String.format(checksumCommand, providedChecksum.getChecksum(), downloadedFilePath, getChecksumCommandFromAlgorithm(algorithm));
            int result = Script.runSimpleBashScriptForExitValue(cmd);
            return result == 0;
        }
        return true;
    }
}
