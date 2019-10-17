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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.trilead.ssh2.SCPClient;

import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;

public class ScpTemplateDownloader extends TemplateDownloaderBase implements TemplateDownloader {
    private static final Logger s_logger = Logger.getLogger(ScpTemplateDownloader.class);

    public ScpTemplateDownloader(StorageLayer storageLayer, String downloadUrl, String toDir, long maxTemplateSizeInBytes, DownloadCompleteCallback callback) {
        super(storageLayer, downloadUrl, toDir, maxTemplateSizeInBytes, callback);

        URI uri;
        try {
            uri = new URI(_downloadUrl);
        } catch (URISyntaxException e) {
            s_logger.warn("URI syntax error: " + _downloadUrl);
            _status = Status.UNRECOVERABLE_ERROR;
            return;
        }

        String path = uri.getPath();
        String filename = path.substring(path.lastIndexOf("/") + 1);
        _toFile = toDir + File.separator + filename;
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (_status == Status.ABORTED || _status == Status.UNRECOVERABLE_ERROR || _status == Status.DOWNLOAD_FINISHED) {
            return 0;
        }

        _resume = resume;

        _start = System.currentTimeMillis();

        URI uri;
        try {
            uri = new URI(_downloadUrl);
        } catch (URISyntaxException e1) {
            _status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        String username = uri.getUserInfo();
        String queries = uri.getQuery();
        String password = null;
        if (queries != null) {
            String[] qs = queries.split("&");
            for (String q : qs) {
                String[] tokens = q.split("=");
                if (tokens[0].equalsIgnoreCase("password")) {
                    password = tokens[1];
                    break;
                }
            }
        }
        int port = uri.getPort();
        if (port == -1) {
            port = 22;
        }
        File file = new File(_toFile);

        com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(uri.getHost(), port);
        try {
            if (_storage != null) {
                file.createNewFile();
                _storage.setWorldReadableAndWriteable(file);
            }

            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(username, password)) {
                throw new CloudRuntimeException("Unable to authenticate");
            }

            SCPClient scp = new SCPClient(sshConnection);

            String src = uri.getPath();

            _status = Status.IN_PROGRESS;
            scp.get(src, _toDir);

            if (!file.exists()) {
                _status = Status.UNRECOVERABLE_ERROR;
                s_logger.debug("unable to scp the file " + _downloadUrl);
                return 0;
            }

            _status = Status.DOWNLOAD_FINISHED;

            _totalBytes = file.length();

            String downloaded = "(download complete)";

            _errorString = "Downloaded " + _remoteSize + " bytes " + downloaded;
            _downloadTime += System.currentTimeMillis() - _start;
            return _totalBytes;

        } catch (Exception e) {
            s_logger.warn("Unable to download " + _downloadUrl, e);
            _status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            _errorString = e.getMessage();
            return 0;
        } finally {
            sshConnection.close();
            if (_status == Status.UNRECOVERABLE_ERROR && file.exists()) {
                file.delete();
            }
            if (callback != null) {
                callback.downloadComplete(_status);
            }
        }
    }

    @Override
    public int getDownloadPercent() {
        if (_status == Status.DOWNLOAD_FINISHED) {
            return 100;
        } else if (_status == Status.IN_PROGRESS) {
            return 50;
        } else {
            return 0;
        }
    }

    public static void main(String[] args) {
        String url = "scp://root@sol10-2/root/alex/agent.zip?password=password";
        TemplateDownloader td = new ScpTemplateDownloader(null, url, "/tmp/mysql", TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES, null);
        long bytes = td.download(true, null);
        if (bytes > 0) {
            System.out.println("Downloaded  (" + bytes + " bytes)" + " in " + td.getDownloadTime() / 1000 + " secs");
        } else {
            System.out.println("Failed download");
        }

    }
}
