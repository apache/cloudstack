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

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.HttpDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.HttpsDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.MetalinkDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.NfsDirectDownloadCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirectDownloadHelper {

    protected static Logger LOGGER = LogManager.getLogger(DirectDownloadHelper.class);

    /**
     * Get direct template downloader from direct download command and destination pool
     */
    public static DirectTemplateDownloader getDirectTemplateDownloaderFromCommand(DirectDownloadCommand cmd,
                                                                                  String destPoolLocalPath,
                                                                                  String temporaryDownloadPath) {
        if (cmd instanceof HttpDirectDownloadCommand) {
            return new HttpDirectTemplateDownloader(cmd.getUrl(), cmd.getTemplateId(), destPoolLocalPath, cmd.getChecksum(), cmd.getHeaders(),
                    cmd.getConnectTimeout(), cmd.getSoTimeout(), temporaryDownloadPath);
        } else if (cmd instanceof HttpsDirectDownloadCommand) {
            return new HttpsDirectTemplateDownloader(cmd.getUrl(), cmd.getTemplateId(), destPoolLocalPath, cmd.getChecksum(), cmd.getHeaders(),
                    cmd.getConnectTimeout(), cmd.getSoTimeout(), cmd.getConnectionRequestTimeout(), temporaryDownloadPath);
        } else if (cmd instanceof NfsDirectDownloadCommand) {
            return new NfsDirectTemplateDownloader(cmd.getUrl(), destPoolLocalPath, cmd.getTemplateId(), cmd.getChecksum(), temporaryDownloadPath);
        } else if (cmd instanceof MetalinkDirectDownloadCommand) {
            return new MetalinkDirectTemplateDownloader(cmd.getUrl(), destPoolLocalPath, cmd.getTemplateId(), cmd.getChecksum(), cmd.getHeaders(),
                    cmd.getConnectTimeout(), cmd.getSoTimeout(), temporaryDownloadPath);
        } else {
            throw new IllegalArgumentException("Unsupported protocol, please provide HTTP(S), NFS or a metalink");
        }
    }

    public static boolean checkUrlExistence(String url) {
        try {
            DirectTemplateDownloader checker = getCheckerDownloader(url, null, null, null);
            return checker.checkUrl(url);
        } catch (CloudRuntimeException e) {
            LOGGER.error(String.format("Cannot check URL %s is reachable due to: %s", url, e.getMessage()), e);
            return false;
        }
    }

    public static boolean checkUrlExistence(String url, Integer connectTimeout, Integer connectionRequestTimeout, Integer socketTimeout) {
        try {
            DirectTemplateDownloader checker = getCheckerDownloader(url, connectTimeout, connectionRequestTimeout, socketTimeout);
            return checker.checkUrl(url);
        } catch (CloudRuntimeException e) {
            LOGGER.error(String.format("Cannot check URL %s is reachable due to: %s", url, e.getMessage()), e);
            return false;
        }
    }

    private static DirectTemplateDownloader getCheckerDownloader(String url, Integer connectTimeout, Integer connectionRequestTimeout, Integer socketTimeout) {
        if (url.toLowerCase().startsWith("https:")) {
            return new HttpsDirectTemplateDownloader(url, connectTimeout, connectionRequestTimeout, socketTimeout);
        } else if (url.toLowerCase().startsWith("http:")) {
            return new HttpDirectTemplateDownloader(url, connectTimeout, socketTimeout);
        } else if (url.toLowerCase().startsWith("nfs:")) {
            return new NfsDirectTemplateDownloader(url);
        } else if (url.toLowerCase().endsWith(".metalink")) {
            return new MetalinkDirectTemplateDownloader(url, connectTimeout, socketTimeout);
        } else {
            throw new CloudRuntimeException(String.format("Cannot find a download checker for url: %s", url));
        }
    }

    public static Long getFileSize(String url, String format) {
        DirectTemplateDownloader checker = getCheckerDownloader(url, null, null, null);
        return checker.getRemoteFileSize(url, format);
    }

    public static Long getFileSize(String url, String format, Integer connectTimeout, Integer connectionRequestTimeout, Integer socketTimeout) {
        DirectTemplateDownloader checker = getCheckerDownloader(url, connectTimeout, connectionRequestTimeout, socketTimeout);
        return checker.getRemoteFileSize(url, format);
    }
}
