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

import com.cloud.storage.StorageLayer;
import com.cloud.utils.UriUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MetalinkTemplateDownloader extends TemplateDownloaderBase implements TemplateDownloader {

    private TemplateDownloader.Status status = TemplateDownloader.Status.NOT_STARTED;
    protected HttpClient client;
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();
    protected HttpMethodRetryHandler myretryhandler;
    protected GetMethod request;
    private boolean toFileSet = false;


    public MetalinkTemplateDownloader(StorageLayer storageLayer, String downloadUrl, String toDir, DownloadCompleteCallback callback, long maxTemplateSize) {
        super(storageLayer, downloadUrl, toDir, maxTemplateSize, callback);
        s_httpClientManager.getParams().setConnectionTimeout(5000);
        client = new HttpClient(s_httpClientManager);
        myretryhandler = createRetryTwiceHandler();
        request = createRequest(downloadUrl);
    }

    protected GetMethod createRequest(String downloadUrl) {
        GetMethod request = new GetMethod(downloadUrl);
        request.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
        request.setFollowRedirects(true);
        if (!toFileSet) {
            String[] parts = downloadUrl.split("/");
            String filename = parts[parts.length - 1];
            _toFile = _toDir + File.separator + filename;
            toFileSet = true;
        }
        return request;
    }

    protected HttpMethodRetryHandler createRetryTwiceHandler() {
        return new HttpMethodRetryHandler() {
            @Override
            public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                if (executionCount >= 2) {
                    // Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // Retry if the server dropped connection on us
                    return true;
                }
                if (!method.isRequestSent()) {
                    // Retry if the request has not been sent fully or
                    // if it's OK to retry methods that have been sent
                    return true;
                }
                // otherwise do not retry
                return false;
            }
        };
    }

    private boolean downloadTemplate() {
        try {
            client.executeMethod(request);
        } catch (IOException e) {
            logger.error("Error on HTTP request: " + e.getMessage());
            return false;
        }
        return performDownload();
    }

    private boolean performDownload() {
        try (
                InputStream in = request.getResponseBodyAsStream();
                OutputStream out = new FileOutputStream(_toFile);
        ) {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            logger.error("Error downloading template from: " + _downloadUrl + " due to: " + e.getMessage());
            return false;
        }
        return true;
    }
    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (_status == Status.ABORTED || _status == Status.UNRECOVERABLE_ERROR || _status == Status.DOWNLOAD_FINISHED) {
            return 0;
        }

        logger.info("Starting metalink download from: " + _downloadUrl);
        _start = System.currentTimeMillis();

        status = Status.IN_PROGRESS;
        List<String> metalinkUrls = UriUtils.getMetalinkUrls(_downloadUrl);
        if (CollectionUtils.isEmpty(metalinkUrls)) {
            logger.error("No URLs found for metalink: " + _downloadUrl);
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }
        boolean downloaded = false;
        int i = 0;
        while (!downloaded && i < metalinkUrls.size()) {
            String url = metalinkUrls.get(i);
            request = createRequest(url);
            downloaded = downloadTemplate();
            i++;
        }
        if (!downloaded) {
            logger.error("Template couldn't be downloaded");
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }
        logger.info("Template downloaded successfully on: " + _toFile);
        status = Status.DOWNLOAD_FINISHED;
        _downloadTime = System.currentTimeMillis() - _start;
        if (_callback != null) {
            _callback.downloadComplete(status);
        }
        return _totalBytes;
    }

    @Override
    public int getDownloadPercent() {
        if (status == Status.DOWNLOAD_FINISHED) {
            return 100;
        } else if (status == Status.IN_PROGRESS) {
            return 50;
        } else {
            return 0;
        }
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }
}
