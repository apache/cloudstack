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

import static com.cloud.utils.StringUtils.join;
import static java.util.Arrays.asList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;

import com.cloud.agent.api.storage.Proxy;
import com.cloud.agent.api.to.S3TO;
import com.cloud.utils.Pair;
import com.cloud.utils.S3Utils;
import com.cloud.utils.UriUtils;

/**
 * Download a template file using HTTP
 *
 */
public class S3TemplateDownloader extends ManagedContextRunnable implements TemplateDownloader {
    public static final Logger s_logger = Logger.getLogger(S3TemplateDownloader.class.getName());
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    private String downloadUrl;
    private String installPath;
    private String s3Key;
    private String fileName;
    public TemplateDownloader.Status status = TemplateDownloader.Status.NOT_STARTED;
    public String errorString = " ";
    private long remoteSize = 0;
    public long downloadTime = 0;
    public long totalBytes;
    private final HttpClient client;
    private GetMethod request;
    private boolean resume = false;
    private DownloadCompleteCallback completionCallback;
    private S3TO s3;
    private boolean inited = true;

    private long maxTemplateSizeInByte;
    private ResourceType resourceType = ResourceType.TEMPLATE;
    private final HttpMethodRetryHandler myretryhandler;

    public S3TemplateDownloader(S3TO storageLayer, String downloadUrl, String installPath,
            DownloadCompleteCallback callback, long maxTemplateSizeInBytes, String user, String password, Proxy proxy,
            ResourceType resourceType) {
        s3 = storageLayer;
        this.downloadUrl = downloadUrl;
        this.installPath = installPath;
        status = TemplateDownloader.Status.NOT_STARTED;
        this.resourceType = resourceType;
        maxTemplateSizeInByte = maxTemplateSizeInBytes;

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

            Pair<String, Integer> hostAndPort = UriUtils.validateUrl(downloadUrl);
            fileName = StringUtils.substringAfterLast(downloadUrl, "/");

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
                client.getState().setCredentials(
                        new AuthScope(hostAndPort.first(), hostAndPort.second(), AuthScope.ANY_REALM), defaultcreds);
                s_logger.info("Added username=" + user + ", password=" + password + "for host " + hostAndPort.first()
                        + ":" + hostAndPort.second());
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

        try {
            // execute get method
            int responseCode = HttpStatus.SC_OK;
            if ((responseCode = client.executeMethod(request)) != HttpStatus.SC_OK) {
                status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                errorString = " HTTP Server returned " + responseCode + " (expected 200 OK) ";
                return 0; // FIXME: retry?
            }
            // get the total size of file
            Header contentLengthHeader = request.getResponseHeader("Content-Length");
            boolean chunked = false;
            long remoteSize2 = 0;
            if (contentLengthHeader == null) {
                Header chunkedHeader = request.getResponseHeader("Transfer-Encoding");
                if (chunkedHeader == null || !"chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                    status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                    errorString = " Failed to receive length of download ";
                    return 0; // FIXME: what status do we put here? Do we retry?
                } else if ("chunked".equalsIgnoreCase(chunkedHeader.getValue())) {
                    chunked = true;
                }
            } else {
                remoteSize2 = Long.parseLong(contentLengthHeader.getValue());
            }

            if (remoteSize == 0) {
                remoteSize = remoteSize2;
            }

            if (remoteSize > maxTemplateSizeInByte) {
                s_logger.info("Remote size is too large: " + remoteSize + " , max=" + maxTemplateSizeInByte);
                status = Status.UNRECOVERABLE_ERROR;
                errorString = "Download file size is too large";
                return 0;
            }

            if (remoteSize == 0) {
                remoteSize = maxTemplateSizeInByte;
            }

            // get content type
            String contentType = null;
            Header contentTypeHeader = request.getResponseHeader("Content-Type");
            if ( contentTypeHeader != null ){
                contentType = contentTypeHeader.getValue();
            }

            InputStream in = !chunked ? new BufferedInputStream(request.getResponseBodyAsStream())
                    : new ChunkedInputStream(request.getResponseBodyAsStream());

            s_logger.info("Starting download from " + getDownloadUrl() + " to s3 bucket " + s3.getBucketName()
                    + " remoteSize=" + remoteSize + " , max size=" + maxTemplateSizeInByte);

            Date start = new Date();
            // compute s3 key
            s3Key = join(asList(installPath, fileName), S3Utils.SEPARATOR);

            // download using S3 API
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(remoteSize);
            if ( contentType != null ){
                metadata.setContentType(contentType);
            }
            PutObjectRequest putObjectRequest = new PutObjectRequest(s3.getBucketName(), s3Key, in, metadata);
            // check if RRS is enabled
            if (s3.getEnableRRS()){
                putObjectRequest = putObjectRequest.withStorageClass(StorageClass.ReducedRedundancy);
            }
            // register progress listenser
            putObjectRequest.setProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(ProgressEvent progressEvent) {
                    // s_logger.debug(progressEvent.getBytesTransfered()
                    // + " number of byte transferd "
                    // + new Date());
                    totalBytes += progressEvent.getBytesTransfered();
                    if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                        s_logger.info("download completed");
                        status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
                    } else if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                        status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
                    } else if (progressEvent.getEventCode() == ProgressEvent.CANCELED_EVENT_CODE) {
                        status = TemplateDownloader.Status.ABORTED;
                    } else {
                        status = TemplateDownloader.Status.IN_PROGRESS;
                    }
                }

            });
            

            if ( !s3.getSingleUpload(remoteSize) ){
                // use TransferManager to do multipart upload
                S3Utils.mputObject(s3, putObjectRequest);
            } else{
                // single part upload, with 5GB limit in Amazon
                S3Utils.putObject(s3, putObjectRequest);
                while (status != TemplateDownloader.Status.DOWNLOAD_FINISHED &&
                        status != TemplateDownloader.Status.UNRECOVERABLE_ERROR &&
                        status != TemplateDownloader.Status.ABORTED) {
                    // wait for completion
                }
            }

            // finished or aborted
            Date finish = new Date();
            String downloaded = "(incomplete download)";
            if (totalBytes >= remoteSize) {
                status = TemplateDownloader.Status.DOWNLOAD_FINISHED;
                downloaded = "(download complete remote=" + remoteSize + "bytes)";
            } else {
                errorString = "Downloaded " + totalBytes + " bytes " + downloaded;
            }
            downloadTime += finish.getTime() - start.getTime();
            return totalBytes;
        } catch (HttpException hte) {
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = hte.getMessage();
        } catch (IOException ioe) {
            // probably a file write error
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = ioe.getMessage();
        } catch (AmazonClientException ex) {
            // S3 api exception
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = ex.getMessage();
        } catch (InterruptedException e) {
            // S3 upload api exception
            status = TemplateDownloader.Status.UNRECOVERABLE_ERROR;
            errorString = e.getMessage();
        } finally {
            // close input stream
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
            try {
                S3Utils.deleteObject(s3, s3.getBucketName(), s3Key);
            } catch (Exception ex) {
                // ignore delete exception if it is not there
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

        return (int) (100.0 * totalBytes / remoteSize);
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
        return s3Key;
    }

    @Override
    public void setResume(boolean resume) {
        this.resume = resume;
    }

    @Override
    public long getMaxTemplateSizeInBytes() {
        return maxTemplateSizeInByte;
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
