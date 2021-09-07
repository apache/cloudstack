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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.utils.imagestore.ImageStoreUtil;
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

import com.cloud.storage.StorageLayer;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.net.Proxy;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

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
    public TemplateDownloader.Status status;
    private String errorString = null;
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
    private long maxTemplateSizeInBytes;
    private ResourceType resourceType = ResourceType.TEMPLATE;
    private final HttpMethodRetryHandler myretryhandler;

    public HttpTemplateDownloader(StorageLayer storageLayer, String downloadUrl, String toDir, DownloadCompleteCallback callback, long maxTemplateSizeInBytes,
            String user, String password, Proxy proxy, ResourceType resourceType) {
        _storage = storageLayer;
        this.downloadUrl = downloadUrl;
        this.toDir = toDir;
        this.resourceType = resourceType;
        this.maxTemplateSizeInBytes = maxTemplateSizeInBytes;
        completionCallback = callback;

        status = TemplateDownloader.Status.NOT_STARTED;
        totalBytes = 0;
        client = new HttpClient(s_httpClientManager);
        myretryhandler = createRetryTwiceHandler();
        try {
            request = createRequest(downloadUrl);
            checkTemporaryDestination(toDir);
            checkProxy(proxy);
            checkCredentials(user, password);
        } catch (Exception ex) {
            errorString = "Unable to start download -- check url? ";
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            s_logger.warn("Exception in constructor -- " + ex.toString());
        } catch (Throwable th) {
            s_logger.warn("throwable caught ", th);
        }
    }

    private GetMethod createRequest(String downloadUrl) {
        GetMethod request = new GetMethod(downloadUrl);
        request.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
        request.setFollowRedirects(true);
        return request;
    }

    private void checkTemporaryDestination(String toDir) {
        try {
            File f = File.createTempFile("dnld", "tmp_", new File(toDir));

            if (_storage != null) {
                _storage.setWorldReadableAndWriteable(f);
            }

            toFile = f.getAbsolutePath();
        } catch (IOException ex) {
            errorString = "Unable to start download -- check url? ";
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            s_logger.warn("Exception in constructor -- " + ex.toString());
        }
    }

    private void checkCredentials(String user, String password) {
        try {
            Pair<String, Integer> hostAndPort = UriUtils.validateUrl(downloadUrl);
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
        }
    }

    private void checkProxy(Proxy proxy) {
        if (proxy != null) {
            client.getHostConfiguration().setProxy(proxy.getHost(), proxy.getPort());
            if (proxy.getUserName() != null) {
                Credentials proxyCreds = new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword());
                client.getState().setProxyCredentials(AuthScope.ANY, proxyCreds);
            }
        }
    }

    private HttpMethodRetryHandler createRetryTwiceHandler() {
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

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (skipDownloadOnStatus()) return 0;
        int bytes = 0;
        File file = new File(toFile);
        try {

            long localFileSize = checkLocalFileSizeForResume(resume, file);

            Date start = new Date();

            if (checkServerResponse(localFileSize)) return 0;

            if (!tryAndGetRemoteSize()) return 0;

            if (!canHandleDownloadSize()) return 0;

            checkAndSetDownloadSize();

            try (InputStream in = request.getResponseBodyAsStream();
                 RandomAccessFile out = new RandomAccessFile(file, "rw");
            ) {
                out.seek(localFileSize);

                s_logger.info("Starting download from " + downloadUrl + " to " + toFile + " remoteSize=" + toHumanReadableSize(remoteSize) + " , max size=" + toHumanReadableSize(maxTemplateSizeInBytes));

                if (copyBytes(file, in, out)) return 0;

                Date finish = new Date();
                checkDowloadCompletion();
                downloadTime += finish.getTime() - start.getTime();
            } finally { /* in.close() and out.close() */ }
            return totalBytes;
        } catch (HttpException hte) {
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = hte.getMessage();
        } catch (IOException ioe) {
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR; //probably a file write error?
            // Let's not overwrite the original error message.
            if (errorString == null) {
                errorString = ioe.getMessage();
            }
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

    private boolean copyBytes(File file, InputStream in, RandomAccessFile out) throws IOException {
        int bytes;
        byte[] block = new byte[CHUNK_SIZE];
        long offset = 0;
        boolean done = false;
        VerifyFormat verifyFormat = new VerifyFormat(file);
        status = Status.IN_PROGRESS;
        while (!done && status != Status.ABORTED && offset <= remoteSize) {
            if ((bytes = in.read(block, 0, CHUNK_SIZE)) > -1) {
                offset = writeBlock(bytes, out, block, offset);
                if (!verifyFormat.isVerifiedFormat() && (offset >= 1048576 || offset >= remoteSize)) { //let's check format after we get 1MB or full file
                    verifyFormat.invoke();
                }
            } else {
                done = true;
            }
        }
        out.getFD().sync();
        return false;
    }

    private long writeBlock(int bytes, RandomAccessFile out, byte[] block, long offset) throws IOException {
        out.write(block, 0, bytes);
        offset += bytes;
        out.seek(offset);
        totalBytes += bytes;
        return offset;
    }

    private void checkDowloadCompletion() {
        String downloaded = "(incomplete download)";
        if (totalBytes >= remoteSize) {
            status = Status.DOWNLOAD_FINISHED;
            downloaded = "(download complete remote=" + toHumanReadableSize(remoteSize) + " bytes)";
        }
        errorString = "Downloaded " + toHumanReadableSize(totalBytes) + " bytes " + downloaded;
    }

    private boolean canHandleDownloadSize() {
        if (remoteSize > maxTemplateSizeInBytes) {
            s_logger.info("Remote size is too large: " + toHumanReadableSize(remoteSize) + " , max=" + toHumanReadableSize(maxTemplateSizeInBytes));
            status = Status.UNRECOVERABLE_ERROR;
            errorString = "Download file size is too large";
            return false;
        }

        return true;
    }

    private void checkAndSetDownloadSize() {
        if (remoteSize == 0) {
            remoteSize = maxTemplateSizeInBytes;
        }
    }

    private boolean tryAndGetRemoteSize() {
        Header contentLengthHeader = request.getResponseHeader("Content-Length");
        boolean chunked = false;
        long reportedRemoteSize = 0;
        if (contentLengthHeader == null) {
            Header chunkedHeader = request.getResponseHeader("Transfer-Encoding");
            if (chunkedHeader == null || !"chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                status = Status.UNRECOVERABLE_ERROR;
                errorString = " Failed to receive length of download ";
                return false;
            } else if ("chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                chunked = true;
            }
        } else {
            reportedRemoteSize = Long.parseLong(contentLengthHeader.getValue());
            if (reportedRemoteSize == 0) {
                status = Status.DOWNLOAD_FINISHED;
                String downloaded = "(download complete remote=" + remoteSize + "bytes)";
                errorString = "Downloaded " + totalBytes + " bytes " + downloaded;
                downloadTime = 0;
                return false;
            }
        }

        if (remoteSize == 0) {
            remoteSize = reportedRemoteSize;
        }
        return true;
    }

    private boolean checkServerResponse(long localFileSize) throws IOException {
        int responseCode = 0;

        if (localFileSize > 0) {
            // require partial content support for resume
            request.addRequestHeader("Range", "bytes=" + localFileSize + "-");
            if (client.executeMethod(request) != HttpStatus.SC_PARTIAL_CONTENT) {
                errorString = "HTTP Server does not support partial get";
                status = Status.UNRECOVERABLE_ERROR;
                return true;
            }
        } else if ((responseCode = client.executeMethod(request)) != HttpStatus.SC_OK) {
            status = Status.UNRECOVERABLE_ERROR;
            errorString = " HTTP Server returned " + responseCode + " (expected 200 OK) ";
            return true; //FIXME: retry?
        }
        return false;
    }

    private long checkLocalFileSizeForResume(boolean resume, File file) {
        // TODO check the status of this downloader as well?
        long localFileSize = 0;
        if (file.exists() && resume) {
            localFileSize = file.length();
            s_logger.info("Resuming download to file (current size)=" + toHumanReadableSize(localFileSize));
        }
        return localFileSize;
    }

    private boolean skipDownloadOnStatus() {
        switch (status) {
            case ABORTED:
            case UNRECOVERABLE_ERROR:
            case DOWNLOAD_FINISHED:
                return true;
            default:

        }
        return false;
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
        return errorString == null ? " " : errorString;
    }

    @Override
    public String getDownloadLocalPath() {
        return getToFile();
    }

    @Override
    public void setResume(boolean resume) {
        this.resume = resume;
    }

    @Override
    public long getMaxTemplateSizeInBytes() {
        return maxTemplateSizeInBytes;
    }

    // TODO move this test code to unit tests or integration tests
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

    private class VerifyFormat {
        private File file;
        private boolean verifiedFormat;

        public VerifyFormat(File file) {
            this.file = file;
            this.verifiedFormat = false;
        }

        public boolean isVerifiedFormat() {
            return verifiedFormat;
        }

        public VerifyFormat invoke() {
            String uripath = null;
            try {
                URI str = new URI(downloadUrl);
                uripath = str.getPath();
            } catch (URISyntaxException e) {
                s_logger.warn("Invalid download url: " + downloadUrl + ", This should not happen since we have validated the url before!!");
            }
            String unsupportedFormat = ImageStoreUtil.checkTemplateFormat(file.getAbsolutePath(), uripath);
            if (unsupportedFormat == null || !unsupportedFormat.isEmpty()) {
                try {
                    request.abort();
                } catch (Exception ex) {
                    s_logger.debug("Error on http connection : " + ex.getMessage());
                }
                status = Status.UNRECOVERABLE_ERROR;
                errorString = "Template content is unsupported, or mismatch between selected format and template content. Found  : " + unsupportedFormat;
                throw new CloudRuntimeException(errorString);
            } else {
                s_logger.debug("Verified format of downloading file " + file.getAbsolutePath() + " is supported");
                verifiedFormat = true;
            }
            return this;
        }
    }
}
