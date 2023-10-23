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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.storage.StorageLayer;

public class SimpleHttpMultiFileDownloader extends ManagedContextRunnable implements TemplateDownloader {
    public static final Logger s_logger = Logger.getLogger(SimpleHttpMultiFileDownloader.class.getName());
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    private static final int CHUNK_SIZE = 1024 * 1024; //1M
    private String[] downloadUrls;
    private String currentToFile;
    public TemplateDownloader.Status currentStatus;
    public TemplateDownloader.Status status;
    private String errorString = null;
    private long totalRemoteSize = 0;
    private long currentRemoteSize = 0;
    public long downloadTime = 0;
    public long currentTotalBytes;
    public long totalBytes = 0;
    private final HttpClient client;
    private GetMethod request;
    private boolean resume = false;
    private DownloadCompleteCallback completionCallback;
    StorageLayer _storage;
    boolean inited = true;

    private String toDir;
    private long maxTemplateSizeInBytes;
    private DownloadCommand.ResourceType resourceType = DownloadCommand.ResourceType.TEMPLATE;
    private final HttpMethodRetryHandler retryHandler;

    private HashMap<String, String> urlFileMap;

    public SimpleHttpMultiFileDownloader(StorageLayer storageLayer, String[] downloadUrls, String toDir,
                                         DownloadCompleteCallback callback, long maxTemplateSizeInBytes,
                                         DownloadCommand.ResourceType resourceType) {
        _storage = storageLayer;
        this.downloadUrls = downloadUrls;
        this.toDir = toDir;
        this.resourceType = resourceType;
        this.maxTemplateSizeInBytes = maxTemplateSizeInBytes;
        completionCallback = callback;
        status = TemplateDownloader.Status.NOT_STARTED;
        currentStatus = TemplateDownloader.Status.NOT_STARTED;
        currentTotalBytes = 0;
        client = new HttpClient(s_httpClientManager);
        retryHandler = createRetryTwiceHandler();
        urlFileMap = new HashMap<>();
    }

    private GetMethod createRequest(String downloadUrl) {
        GetMethod request = new GetMethod(downloadUrl);
        request.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
        request.setFollowRedirects(true);
        return request;
    }

    private void checkTemporaryDestination(String toDir) {
        try {
            File f = File.createTempFile("dnld", "tmp_", new File(toDir));
            if (_storage != null) {
                _storage.setWorldReadableAndWriteable(f);
            }
            currentToFile = f.getAbsolutePath();
        } catch (IOException ex) {
            errorString = "Unable to start download -- check url? ";
            currentStatus = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            s_logger.warn("Exception in constructor -- " + ex.toString());
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

    private void tryAndGetTotalRemoteSize() {
        for (String downloadUrl : downloadUrls) {
            if (StringUtils.isBlank(downloadUrl)) {
                continue;
            }
            HeadMethod headMethod = new HeadMethod(downloadUrl);
            try {
                if (client.executeMethod(headMethod) != HttpStatus.SC_OK) {
                    continue;
                }
                Header contentLengthHeader = headMethod.getResponseHeader("content-length");
                if (contentLengthHeader == null) {
                    continue;
                }
                totalRemoteSize += Long.parseLong(contentLengthHeader.getValue());
            } catch (IOException e) {
                s_logger.warn(String.format("Cannot reach URL: %s while trying to get remote sizes due to: %s", downloadUrl, e.getMessage()), e);
            } finally {
                headMethod.releaseConnection();
            }
        }
    }

    private long downloadFile(String downloadUrl) {
        s_logger.debug("Starting download for " + downloadUrl);
        currentTotalBytes = 0;
        currentRemoteSize = 0;
        File file = null;
        request = null;
        try {
            request = createRequest(downloadUrl);
            checkTemporaryDestination(toDir);
            urlFileMap.put(downloadUrl, currentToFile);
            file = new File(currentToFile);
            long localFileSize = checkLocalFileSizeForResume(resume, file);
            if (checkServerResponse(localFileSize)) return 0;
            if (!tryAndGetRemoteSize()) return 0;
            if (!canHandleDownloadSize()) return 0;
            checkAndSetDownloadSize();
            try (InputStream in = request.getResponseBodyAsStream();
                 RandomAccessFile out = new RandomAccessFile(file, "rw");
            ) {
                out.seek(localFileSize);
                s_logger.info("Starting download from " + downloadUrl + " to " + currentToFile + " remoteSize=" + toHumanReadableSize(currentRemoteSize) + " , max size=" + toHumanReadableSize(maxTemplateSizeInBytes));
                if (copyBytes(file, in, out)) return 0;
                checkDownloadCompletion();
            }
            return currentTotalBytes;
        } catch (HttpException hte) {
            currentStatus = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = hte.getMessage();
        } catch (IOException ioe) {
            currentStatus = TemplateDownloader.Status.UNRECOVERABLE_ERROR; //probably a file write error?
            // Let's not overwrite the original error message.
            if (errorString == null) {
                errorString = ioe.getMessage();
            }
        } finally {
            if (currentStatus == Status.UNRECOVERABLE_ERROR && file != null && file.exists() && !file.isDirectory()) {
                file.delete();
            }
            if (request != null) {
                request.releaseConnection();
            }
        }
        return 0;
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (skipDownloadOnStatus()) return 0;
        if (resume) {
            s_logger.error("Resume not allowed for this downloader");
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }
        s_logger.debug("Starting downloads");
        status = Status.IN_PROGRESS;
        Date start = new Date();
        tryAndGetTotalRemoteSize();
        for (String downloadUrl : downloadUrls) {
            if (StringUtils.isBlank(downloadUrl)) {
                continue;
            }
            long bytes = downloadFile(downloadUrl);
            if (currentStatus != Status.DOWNLOAD_FINISHED) {
                break;
            }
            totalBytes += bytes;
        }
        status = currentStatus;
        Date finish = new Date();
        downloadTime += finish.getTime() - start.getTime();
        if (callback != null) {
            callback.downloadComplete(status);
        }
        return 0;
    }

    private boolean copyBytes(File file, InputStream in, RandomAccessFile out) throws IOException {
        int bytes;
        byte[] block = new byte[CHUNK_SIZE];
        long offset = 0;
        boolean done = false;
        currentStatus = Status.IN_PROGRESS;
        while (!done && currentStatus != Status.ABORTED && offset <= currentRemoteSize) {
            if ((bytes = in.read(block, 0, CHUNK_SIZE)) > -1) {
                offset = writeBlock(bytes, out, block, offset);
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
        currentTotalBytes += bytes;
        return offset;
    }

    private void checkDownloadCompletion() {
        String downloaded = "(incomplete download)";
        if (currentTotalBytes >= currentRemoteSize) {
            currentStatus = Status.DOWNLOAD_FINISHED;
            downloaded = "(download complete remote=" + toHumanReadableSize(currentRemoteSize) + " bytes)";
        }
        errorString = "Downloaded " + toHumanReadableSize(currentTotalBytes) + " bytes " + downloaded;
    }

    private boolean canHandleDownloadSize() {
        if (currentRemoteSize > maxTemplateSizeInBytes) {
            s_logger.info("Remote size is too large: " + toHumanReadableSize(currentRemoteSize) + " , max=" + toHumanReadableSize(maxTemplateSizeInBytes));
            currentStatus = Status.UNRECOVERABLE_ERROR;
            errorString = "Download file size is too large";
            return false;
        }
        return true;
    }

    private void checkAndSetDownloadSize() {
        if (currentRemoteSize == 0) {
            currentRemoteSize = maxTemplateSizeInBytes;
        }
        if (totalRemoteSize == 0) {
            totalRemoteSize = currentRemoteSize;
        }
    }

    private boolean tryAndGetRemoteSize() {
        Header contentLengthHeader = request.getResponseHeader("content-length");
        boolean chunked = false;
        long reportedRemoteSize = 0;
        if (contentLengthHeader == null) {
            Header chunkedHeader = request.getResponseHeader("Transfer-Encoding");
            if (chunkedHeader == null || !"chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                currentStatus = Status.UNRECOVERABLE_ERROR;
                errorString = " Failed to receive length of download ";
                return false;
            } else if ("chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                chunked = true;
            }
        } else {
            reportedRemoteSize = Long.parseLong(contentLengthHeader.getValue());
            if (reportedRemoteSize == 0) {
                currentStatus = Status.DOWNLOAD_FINISHED;
                String downloaded = "(download complete remote=" + currentRemoteSize + "bytes)";
                errorString = "Downloaded " + currentTotalBytes + " bytes " + downloaded;
                downloadTime = 0;
                return false;
            }
        }

        if (currentRemoteSize == 0) {
            currentRemoteSize = reportedRemoteSize;
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
                currentStatus = Status.UNRECOVERABLE_ERROR;
                return true;
            }
        } else if ((responseCode = client.executeMethod(request)) != HttpStatus.SC_OK) {
            currentStatus = Status.UNRECOVERABLE_ERROR;
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
        switch (currentStatus) {
            case ABORTED:
            case UNRECOVERABLE_ERROR:
            case DOWNLOAD_FINISHED:
                return true;
            default:

        }
        return false;
    }

    public String[] getDownloadUrls() {
        return downloadUrls;
    }

    public String getCurrentToFile() {
        File file = new File(currentToFile);

        return file.getAbsolutePath();
    }

    @Override
    public TemplateDownloader.Status getStatus() {
        return currentStatus;
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
                currentStatus = TemplateDownloader.Status.ABORTED;
                return true;
            case UNKNOWN:
            case NOT_STARTED:
            case RECOVERABLE_ERROR:
            case UNRECOVERABLE_ERROR:
            case ABORTED:
                currentStatus = TemplateDownloader.Status.ABORTED;
            case DOWNLOAD_FINISHED:
                File f = new File(currentToFile);
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
        if (totalRemoteSize == 0) {
            return 0;
        }

        return (int)(100.0 * totalBytes / totalRemoteSize);
    }

    @Override
    protected void runInContext() {
        try {
            download(resume, completionCallback);
        } catch (Throwable t) {
            s_logger.warn("Caught exception during download " + t.getMessage(), t);
            errorString = "Failed to install: " + t.getMessage();
            currentStatus = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
        }

    }

    @Override
    public void setStatus(TemplateDownloader.Status status) {
        this.currentStatus = status;
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
        return toDir;
    }

    @Override
    public void setResume(boolean resume) {
        this.resume = resume;
    }

    @Override
    public long getMaxTemplateSizeInBytes() {
        return maxTemplateSizeInBytes;
    }

    @Override
    public void setDownloadError(String error) {
        errorString = error;
    }

    @Override
    public boolean isInited() {
        return inited;
    }

    public DownloadCommand.ResourceType getResourceType() {
        return resourceType;
    }

    public Map<String, String> getDownloadedFilesMap() {
        return urlFileMap;
    }
}
