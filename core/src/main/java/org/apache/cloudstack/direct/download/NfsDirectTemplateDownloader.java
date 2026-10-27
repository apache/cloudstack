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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class NfsDirectTemplateDownloader extends DirectTemplateDownloaderImpl {

    private String srcHost;
    private String srcPath;

    // srcHost and srcPath are used to build mount/cp commands; restrict them to safe
    // characters so a crafted NFS url cannot smuggle shell metacharacters into the agent.
    // Host must start with an alphanumeric character and the path must start with '/' so
    // neither can be interpreted as a command-line option (argument confusion).
    private static final Pattern SRC_HOST_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");
    private static final Pattern SRC_PATH_PATTERN = Pattern.compile("^/[A-Za-z0-9/._-]*$");
    // SRC_PATH_PATTERN allows '.' and '/' individually, so a ".." segment would still pass;
    // reject path traversal explicitly.
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(^|/)\\.\\.(/|$)");

    /**
     * Parse url and set srcHost and srcPath
     */
    private void parseUrl() {
        String url = getUrl();
        try {
            URI uri = new URI(UriUtils.encodeURIComponent(url));
            if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("nfs")) {
                srcHost = uri.getHost();
                srcPath = uri.getPath();
                validateHostAndPath(url);
            }
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Invalid NFS url " + url + " caused error: " + e.getMessage());
        }
    }

    private void validateHostAndPath(String url) {
        if (srcHost == null || !SRC_HOST_PATTERN.matcher(srcHost).matches()) {
            throw new CloudRuntimeException("Invalid host in NFS url: " + url);
        }
        if (srcPath == null || !SRC_PATH_PATTERN.matcher(srcPath).matches()
                || PATH_TRAVERSAL_PATTERN.matcher(srcPath).find()) {
            throw new CloudRuntimeException("Invalid path in NFS url: " + url);
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

        // Build each command from discrete arguments (no shell) so srcHost/srcPath cannot be
        // interpreted as shell metacharacters even if they slip past validation. "--" separates
        // options from positional arguments so a value cannot be mistaken for an option.
        File mountDir = new File(mountPoint);
        if (!mountDir.exists() && !mountDir.mkdirs()) {
            throw new CloudRuntimeException("Failed to create mount point " + mountPoint);
        }

        // NFS can only mount an exported directory, never an individual file, so mount the
        // parent directory of srcPath and copy the filename relative to the mount point.
        int lastSlash = srcPath.lastIndexOf('/');
        String parentPath = lastSlash > 0 ? srcPath.substring(0, lastSlash) : "/";
        String fileName = srcPath.substring(lastSlash + 1);

        Script mount = new Script("mount", logger);
        mount.add("-t", "nfs");
        mount.add("--");
        mount.add(srcHost + ":" + parentPath);
        mount.add(mountPoint);
        String result = mount.execute();
        if (result != null) {
            throw new CloudRuntimeException(String.format("Failed to mount NFS source %s:%s : %s", srcHost, parentPath, result));
        }

        try {
            String downloadDir = getDestPoolPath() + File.separator + getDirectDownloadTempPath(getTemplateId());
            setDownloadedFilePath(downloadDir + File.separator + getTemporaryFileName());

            Script copy = new Script("cp", logger);
            copy.add("--");
            copy.add(mountPoint + "/" + fileName);
            copy.add(getDownloadedFilePath());
            String copyResult = copy.execute();
            if (copyResult != null) {
                throw new CloudRuntimeException(String.format("Failed to copy template from NFS source %s:%s : %s", srcHost, srcPath, copyResult));
            }
        } finally {
            Script umount = new Script("umount", logger);
            umount.add("--");
            umount.add(mountPoint);
            String umountResult = umount.execute();
            if (umountResult != null) {
                logger.warn(String.format("Failed to unmount %s : %s", mountPoint, umountResult));
            }
        }

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
        return null;
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
