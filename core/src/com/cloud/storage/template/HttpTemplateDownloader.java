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
package com.cloud.storage.template;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;

import com.cloud.agent.api.storage.Proxy;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;

/**
 * Download a template file using HTTP
 *
 */
public class HttpTemplateDownloader extends ManagedContextRunnable implements TemplateDownloader {
    public static final Logger s_logger = Logger.getLogger(HttpTemplateDownloader.class.getName());
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    private static final int CHUNK_SIZE = 1024 * 1024; //1M
    private String downloadUrl;
    private String toFile;
    public TemplateDownloader.Status status = TemplateDownloader.Status.NOT_STARTED;
    public String errorString = " ";
    private long remoteSize = 0;
    public long downloadTime = 0;
    public long totalBytes;
    private final HttpClient client;
    private GetMethod request;
    private boolean resume = false;
    private DownloadCompleteCallback completionCallback;
    StorageLayer _storage;
    boolean inited = true;

    private String toDir;
    private long MAX_TEMPLATE_SIZE_IN_BYTES;
    private ResourceType resourceType = ResourceType.TEMPLATE;
    private final HttpMethodRetryHandler myretryhandler;

    public HttpTemplateDownloader(StorageLayer storageLayer, String downloadUrl, String toDir, DownloadCompleteCallback callback, long maxTemplateSizeInBytes,
            String user, String password, Proxy proxy, ResourceType resourceType) {
        _storage = storageLayer;
        this.downloadUrl = downloadUrl;
        setToDir(toDir);
        status = TemplateDownloader.Status.NOT_STARTED;
        this.resourceType = resourceType;
        MAX_TEMPLATE_SIZE_IN_BYTES = maxTemplateSizeInBytes;

        totalBytes = 0;
        client = new HttpClient(s_httpClientManager);

        myretryhandler = new HttpMethodRetryHandler() {
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

        try {
            request = new GetMethod(downloadUrl);
            request.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
            completionCallback = callback;
            //this.request.setFollowRedirects(false);

            File f = File.createTempFile("dnld", "tmp_", new File(toDir));

            if (_storage != null) {
                _storage.setWorldReadableAndWriteable(f);
            }

            toFile = f.getAbsolutePath();
            Pair<String, Integer> hostAndPort = UriUtils.validateUrl(downloadUrl);

            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.getHost(), proxy.getPort());
                if (proxy.getUserName() != null) {
                    Credentials proxyCreds = new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword());
                    client.getState().setProxyCredentials(AuthScope.ANY, proxyCreds);
                }
            }
            if ((user != null) && (password != null)) {
                client.getParams().setAuthenticationPreemptive(true);
                Credentials defaultcreds = new UsernamePasswordCredentials(user, password);
                client.getState().setCredentials(new AuthScope(hostAndPort.first(), hostAndPort.second(), AuthScope.ANY_REALM), defaultcreds);
                s_logger.info("Added username=" + user + ", password=" + password + "for host " + hostAndPort.first() + ":" + hostAndPort.second());
            } else {
                s_logger.info("No credentials configured for host=" + hostAndPort.first() + ":" + hostAndPort.second());
            }
        } catch (IllegalArgumentException iae) {
            errorString = iae.getMessage();
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            inited = false;
        } catch (Exception ex) {
            errorString = "Unable to start download -- check url? ";
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            s_logger.warn("Exception in constructor -- " + ex.toString());
        } catch (Throwable th) {
            s_logger.warn("throwable caught ", th);
        }
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        switch (status) {
            case ABORTED:
            case UNRECOVERABLE_ERROR:
            case DOWNLOAD_FINISHED:
                return 0;
            default:

        }
        int bytes = 0;
        File file = new File(toFile);
        try {

            long localFileSize = 0;
            if (file.exists() && resume) {
                localFileSize = file.length();
                s_logger.info("Resuming download to file (current size)=" + localFileSize);
            }

            Date start = new Date();

            int responseCode = 0;

            if (localFileSize > 0) {
                // require partial content support for resume
                request.addRequestHeader("Range", "bytes=" + localFileSize + "-");
                if (client.executeMethod(request) != HttpStatus.SC_PARTIAL_CONTENT) {
                    errorString = "HTTP Server does not support partial get";
                    status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                    return 0;
                }
            } else if ((responseCode = client.executeMethod(request)) != HttpStatus.SC_OK) {
                status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                errorString = " HTTP Server returned " + responseCode + " (expected 200 OK) ";
                return 0; //FIXME: retry?
            }

            Header contentLengthHeader = request.getResponseHeader("Content-Length");
            boolean chunked = false;
            long remoteSize2 = 0;
            if (contentLengthHeader == null) {
                Header chunkedHeader = request.getResponseHeader("Transfer-Encoding");
                if (chunkedHeader == null || !"chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                    status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                    errorString = " Failed to receive length of download ";
                    return 0; //FIXME: what status do we put here? Do we retry?
                } else if ("chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                    chunked = true;
                }
            } else {
                remoteSize2 = Long.parseLong(contentLengthHeader.getValue());
                if (remoteSize2 == 0) {
                    status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
                    String downloaded = "(download complete remote=" + remoteSize + "bytes)";
                    errorString = "Downloaded " + totalBytes + " bytes " + downloaded;
                    downloadTime = 0;
                    return 0;
                }
            }

            if (remoteSize == 0) {
                remoteSize = remoteSize2;
            }

            if (remoteSize > MAX_TEMPLATE_SIZE_IN_BYTES) {
                s_logger.info("Remote size is too large: " + remoteSize + " , max=" + MAX_TEMPLATE_SIZE_IN_BYTES);
                status = Status.UNRECOVERABLE_ERROR;
                errorString = "Download file size is too large";
                return 0;
            }

            if (remoteSize == 0) {
                remoteSize = MAX_TEMPLATE_SIZE_IN_BYTES;
            }

            InputStream in = !chunked ? new BufferedInputStream(request.getResponseBodyAsStream()) : new ChunkedInputStream(request.getResponseBodyAsStream());

            RandomAccessFile out = new RandomAccessFile(file, "rwd");
            out.seek(localFileSize);

            s_logger.info("Starting download from " + getDownloadUrl() + " to " + toFile + " remoteSize=" + remoteSize + " , max size=" + MAX_TEMPLATE_SIZE_IN_BYTES);

            byte[] block = new byte[CHUNK_SIZE];
            long offset = 0;
            boolean done = false;
            status = TemplateDownloader.Status.IN_PROGRESS;
            while (!done && status != Status.ABORTED && offset <= remoteSize) {
                if ((bytes = in.read(block, 0, CHUNK_SIZE)) > -1) {
                    out.write(block, 0, bytes);
                    offset += bytes;
                    out.seek(offset);
                    totalBytes += bytes;
                } else {
                    done = true;
                }
            }
            Date finish = new Date();
            String downloaded = "(incomplete download)";
            if (totalBytes >= remoteSize) {
                status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
                downloaded = "(download complete remote=" + remoteSize + "bytes)";
            }
            errorString = "Downloaded " + totalBytes + " bytes " + downloaded;
            downloadTime += finish.getTime() - start.getTime();
            in.close();
            out.close();

            return totalBytes;
        } catch (HttpException hte) {
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = hte.getMessage();
        } catch (IOException ioe) {
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR; //probably a file write error?
            errorString = ioe.getMessage();
        } finally {
            if (status == Status.UNRECOVERABLE_ERROR && file.exists() && !file.isDirectory()) {
                file.delete();
            }
            request.releaseConnection();
            if (callback != null) {
                callback.downloadComplete(status);
            }
        }
        return 0;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getToFile() {
        File file = new File(toFile);

        return file.getAbsolutePath();
    }

    @Override
    public TemplateDownloader.Status getStatus() {
        return status;
    }

    @Override
    public long getDownloadTime() {
        return downloadTime;
    }

    @Override
    public long getDownloadedBytes() {
        return totalBytes;
    }

    @Override
    @SuppressWarnings("fallthrough")
    public boolean stopDownload() {
        switch (getStatus()) {
            case IN_PROGRESS:
                if (request != null) {
                    request.abort();
                }
                status = TemplateDownloader.Status.ABORTED;
                return true;
            case UNKNOWN:
            case NOT_STARTED:
            case RECOVERABLE_ERROR:
            case UNRECOVERABLE_ERROR:
            case ABORTED:
                status = TemplateDownloader.Status.ABORTED;
            case DOWNLOAD_FINISHED:
                File f = new File(toFile);
                if (f.exists()) {
                    f.delete();
                }
                return true;

            default:
                return true;
        }
    }

    @Override
    public int getDownloadPercent() {
        if (remoteSize == 0) {
            return 0;
        }

        return (int)(100.0 * totalBytes / remoteSize);
    }

    @Override
    protected void runInContext() {
        try {
            download(resume, completionCallback);
        } catch (Throwable t) {
            s_logger.warn("Caught exception during download " + t.getMessage(), t);
            errorString = "Failed to install: " + t.getMessage();
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
        }

    }

    @Override
    public void setStatus(TemplateDownloader.Status status) {
        this.status = status;
    }

    public boolean isResume() {
        return resume;
    }

    @Override
    public String getDownloadError() {
        return errorString;
    }

    @Override
    public String getDownloadLocalPath() {
        return getToFile();
    }

    @Override
    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public void setToDir(String toDir) {
        this.toDir = toDir;
    }

    public String getToDir() {
        return toDir;
    }

    @Override
    public long getMaxTemplateSizeInBytes() {
        return MAX_TEMPLATE_SIZE_IN_BYTES;
    }

    public static void main(String[] args) {
        String url = "http:// dev.mysql.com/get/Downloads/MySQL-5.0/mysql-noinstall-5.0.77-win32.zip/from/http://mirror.services.wisc.edu/mysql/";
        try {
            new java.net.URI(url);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        TemplateDownloader td = new HttpTemplateDownloader(null, url, "/tmp/mysql", null, TemplateDownloader.DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES, null, null, null, null);
        long bytes = td.download(true, null);
        if (bytes > 0) {
            System.out.println("Downloaded  (" + bytes + " bytes)" + " in " + td.getDownloadTime() / 1000 + " secs");
        } else {
            System.out.println("Failed download");
        }

    }

    @Override
    public void setDownloadError(String error) {
        errorString = error;
    }

    @Override
    public boolean isInited() {
        return inited;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

}
