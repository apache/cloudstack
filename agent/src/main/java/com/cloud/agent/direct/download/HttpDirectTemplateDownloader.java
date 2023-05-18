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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class HttpDirectTemplateDownloader extends DirectTemplateDownloaderImpl {

    protected HttpClient client;
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();
    public static final Logger s_logger = Logger.getLogger(HttpDirectTemplateDownloader.class.getName());
    protected GetMethod request;
    protected Map<String, String> reqHeaders = new HashMap<>();

    public HttpDirectTemplateDownloader(String url, Long templateId, String destPoolPath, String checksum,
                                        Map<String, String> headers, Integer connectTimeout, Integer soTimeout, String downloadPath) {
        super(url, destPoolPath, templateId, checksum, downloadPath);
        s_httpClientManager.getParams().setConnectionTimeout(connectTimeout == null ? 5000 : connectTimeout);
        s_httpClientManager.getParams().setSoTimeout(soTimeout == null ? 5000 : soTimeout);
        client = new HttpClient(s_httpClientManager);
        request = createRequest(url, headers);
        String downloadDir = getDirectDownloadTempPath(templateId);
        File tempFile = createTemporaryDirectoryAndFile(downloadDir);
        setDownloadedFilePath(tempFile.getAbsolutePath());
    }

    /**
     * Create download directory (if it does not exist) and set the download file
     * @return
     */
    protected File createTemporaryDirectoryAndFile(String downloadDir) {
        createFolder(downloadDir);
        return new File(downloadDir + File.separator + getFileNameFromUrl());
    }

    protected GetMethod createRequest(String downloadUrl, Map<String, String> headers) {
        GetMethod request = new GetMethod(downloadUrl);
        request.setFollowRedirects(true);
        if (MapUtils.isNotEmpty(headers)) {
            for (String key : headers.keySet()) {
                request.setRequestHeader(key, headers.get(key));
                reqHeaders.put(key, headers.get(key));
            }
        }
        return request;
    }

    @Override
    public Pair<Boolean, String> downloadTemplate() {
        try {
            int status = client.executeMethod(request);
            if (status != HttpStatus.SC_OK) {
                s_logger.warn("Not able to download template, status code: " + status);
                return new Pair<>(false, null);
            }
            return performDownload();
        } catch (IOException e) {
            throw new CloudRuntimeException("Error on HTTP request: " + e.getMessage());
        } finally {
            request.releaseConnection();
        }
    }

    protected Pair<Boolean, String> performDownload() {
        s_logger.info("Downloading template " + getTemplateId() + " from " + getUrl() + " to: " + getDownloadedFilePath());
        try (
                InputStream in = request.getResponseBodyAsStream();
                OutputStream out = new FileOutputStream(getDownloadedFilePath());
        ) {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            s_logger.error("Error downloading template " + getTemplateId() + " due to: " + e.getMessage());
            return new Pair<>(false, null);
        }
        return new Pair<>(true, getDownloadedFilePath());
    }
}
