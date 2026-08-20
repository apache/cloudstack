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
import com.cloud.utils.script.Script;
import com.cloud.utils.storage.QCOW2Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

public class NfsDirectTemplateDownloader extends DirectTemplateDownloaderImpl {

    private String srcHost;
    private String srcPath;
    private String fileName;

    private static final String mountCommand = "mount -t nfs %s %s";

    /**
     * Parse NFS URL and split the path into the export directory (mountable) and file name.
     * For example, nfs://host/export/templates/file.qcow2 -> srcPath=/export/templates, fileName=file.qcow2
     */
    private void parseUrl() {
        URI uri = null;
        String url = getUrl();
        try {
            uri = new URI(UriUtils.encodeURIComponent(url));
            if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("nfs")) {
                srcHost = uri.getHost();
                srcPath = uri.getPath();
                if (srcPath != null) {
                    int lastSlash = srcPath.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        fileName = srcPath.substring(lastSlash + 1);
                        srcPath = srcPath.substring(0, lastSlash);
                    }
                }
                if (srcPath == null || srcPath.isEmpty()) {
                    srcPath = "/";
                }
            }
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Invalid NFS url " + url + " caused error: " + e.getMessage());
        }
    }

    protected NfsDirectTemplateDownloader(String url) {
        this(url, null, null, null, null);
    }

    public NfsDirectTemplateDownloader(String url, String destPool, Long templateId, String checksum,
               String downloadPath) {
        super(url, destPool, templateId, checksum, downloadPath, false);
        parseUrl();
    }

    @Override
    public Pair<Boolean, String> downloadTemplate() {
        String mountSrcUuid = UUID.randomUUID().toString();
        String mountPoint = "/mnt/" + mountSrcUuid;
        String mount = String.format(mountCommand, srcHost + ":" + srcPath, mountPoint);
        Script.runSimpleBashScript(mount);
        String downloadDir = getDestPoolPath() + File.separator + getDirectDownloadTempPath(getTemplateId());
        String destPath = downloadDir + File.separator + getTemporaryFileName();
        setDownloadedFilePath(destPath);
        if (fileName != null && !fileName.isEmpty()) {
            Script.runSimpleBashScript("cp " + mountPoint + "/" + fileName + " " + destPath);
        } else {
            Script.runSimpleBashScript("cp " + mountPoint + " " + destPath);
        }
        Script.runSimpleBashScript("umount " + mountPoint);
        return new Pair<>(true, getDownloadedFilePath());
    }

    @Override
    public boolean checkUrl(String url) {
        try {
            parseUrl();
            return true;
        } catch (CloudRuntimeException e) {
            logger.error(String.format("Cannot check URL %s is reachable due to: %s", url, e.getMessage()), e);
            return false;
        }
    }

    @Override
    public Long getRemoteFileSize(String url, String format) {
        String mountSrcUuid = UUID.randomUUID().toString();
        String mountPoint = "/mnt/" + mountSrcUuid;
        Script.runSimpleBashScript("mkdir -p " + mountPoint);
        Script.runSimpleBashScript(String.format(mountCommand, srcHost + ":" + srcPath, mountPoint));
        try {
            File file = new File(mountPoint + "/" + (fileName != null ? fileName : ""));
            if (!file.exists()) {
                logger.error(String.format("File not found on NFS mount: %s", file.getAbsolutePath()));
                return null;
            }
            if ("qcow2".equalsIgnoreCase(format) && fileName != null && !fileName.isEmpty()) {
                try (InputStream is = new FileInputStream(file)) {
                    return QCOW2Utils.getVirtualSize(is, false);
                } catch (IOException e) {
                    logger.warn(String.format("Could not read qcow2 virtual size for NFS file %s, falling back to file length: %s", url, e.getMessage()));
                }
            }
            return file.length();
        } catch (Exception e) {
            logger.error(String.format("Could not get remote file size for NFS URL: %s due to: %s", url, e.getMessage()), e);
            return null;
        } finally {
            Script.runSimpleBashScript("umount -l " + mountPoint + " 2>/dev/null");
            Script.runSimpleBashScript("rmdir " + mountPoint + " 2>/dev/null");
        }
    }

    @Override
    public List<String> getMetalinkUrls(String metalinkUrl) {
        return null;
    }

    @Override
    public List<String> getMetalinkChecksums(String metalinkUrl) {
        return null;
    }
}