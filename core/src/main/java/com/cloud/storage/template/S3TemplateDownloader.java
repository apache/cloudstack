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

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.transfer.Upload;
import com.cloud.agent.api.to.S3TO;
import com.cloud.utils.net.HTTPUtils;
import com.cloud.utils.net.Proxy;
import com.cloud.utils.storage.S3.S3Utils;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;
import static java.util.Arrays.asList;

/**
 * Download a template file using HTTP(S)
 *
 * This class, once instantiated, has the purpose to download a single Template to an S3 Image Store.
 *
 * Execution of the instance is started when runInContext() is called.
 */
public class S3TemplateDownloader extends ManagedContextRunnable implements TemplateDownloader {
    private static final Logger LOGGER = Logger.getLogger(S3TemplateDownloader.class.getName());

    private final String downloadUrl;
    private final String s3Key;
    private final String fileExtension;
    private final HttpClient httpClient;
    private final GetMethod getMethod;
    private final DownloadCompleteCallback downloadCompleteCallback;
    private final S3TO s3TO;
    private String errorString = "";
    private TemplateDownloader.Status status = TemplateDownloader.Status.NOT_STARTED;
    private ResourceType resourceType = ResourceType.TEMPLATE;
    private long remoteSize;
    private long downloadTime;
    private long totalBytes;
    private long maxTemplateSizeInByte;

    private boolean resume = false;

    public S3TemplateDownloader(S3TO s3TO, String downloadUrl, String installPath, DownloadCompleteCallback downloadCompleteCallback,
            long maxTemplateSizeInBytes, String username, String password, Proxy proxy, ResourceType resourceType) {
        this.downloadUrl = downloadUrl;
        this.s3TO = s3TO;
        this.resourceType = resourceType;
        this.maxTemplateSizeInByte = maxTemplateSizeInBytes;
        this.httpClient = HTTPUtils.getHTTPClient();
        this.downloadCompleteCallback = downloadCompleteCallback;

        // Create a GET method for the download url.
        this.getMethod = new GetMethod(downloadUrl);

        // Set the retry handler, default to retry 5 times.
        this.getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, HTTPUtils.getHttpMethodRetryHandler(5));

        // Follow redirects
        this.getMethod.setFollowRedirects(true);

        // Set file extension.
        this.fileExtension = StringUtils.substringAfterLast(StringUtils.substringAfterLast(downloadUrl, "/"), ".");

        // Calculate and set S3 Key.
        this.s3Key = StringUtils.join(asList(installPath, StringUtils.substringAfterLast(downloadUrl, "/")), S3Utils.SEPARATOR);

        // Set proxy if available.
        HTTPUtils.setProxy(proxy, this.httpClient);

        // Set credentials if available.
        HTTPUtils.setCredentials(username, password, this.httpClient);
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (!status.equals(Status.NOT_STARTED)) {
            // Only start downloading if we haven't started yet.
            LOGGER.debug("Template download is already started, not starting again. Template: " + downloadUrl);

            return 0;
        }

        int responseCode;
        if ((responseCode = HTTPUtils.executeMethod(httpClient, getMethod)) == -1) {
            errorString = "Exception while executing HttpMethod " + getMethod.getName() + " on URL " + downloadUrl;
            LOGGER.warn(errorString);

            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        if (!HTTPUtils.verifyResponseCode(responseCode)) {
            errorString = "Response code for GetMethod of " + downloadUrl + " is incorrect, responseCode: " + responseCode;
            LOGGER.warn(errorString);

            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        // Headers
        Header contentLengthHeader = getMethod.getResponseHeader("content-length");
        Header contentTypeHeader = getMethod.getResponseHeader("content-type");

        // Check the contentLengthHeader and transferEncodingHeader.
        if (contentLengthHeader == null) {
            errorString = "The ContentLengthHeader of " + downloadUrl + " isn't supplied";
            LOGGER.warn(errorString);

            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        } else {
            // The ContentLengthHeader is supplied, parse it's value.
            remoteSize = Long.parseLong(contentLengthHeader.getValue());
        }

        if (remoteSize > maxTemplateSizeInByte) {
            errorString = "Remote size is too large for template " + downloadUrl + " remote size is " + remoteSize + " max allowed is " + maxTemplateSizeInByte;
            LOGGER.warn(errorString);

            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        InputStream inputStream;

        try {
            inputStream = new BufferedInputStream(getMethod.getResponseBodyAsStream());
        } catch (IOException e) {
            errorString = "Exception occurred while opening InputStream for template " + downloadUrl;
            LOGGER.warn(errorString);

            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        LOGGER.info("Starting download from " + downloadUrl + " to S3 bucket " + s3TO.getBucketName() + " and size " + toHumanReadableSize(remoteSize) + " bytes");

        // Time the upload starts.
        final Date start = new Date();

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(remoteSize);

        if (contentTypeHeader.getValue() != null) {
            objectMetadata.setContentType(contentTypeHeader.getValue());
        }

        // Create the PutObjectRequest.
        PutObjectRequest putObjectRequest = new PutObjectRequest(s3TO.getBucketName(), s3Key, inputStream, objectMetadata);

        // If reduced redundancy is enabled, set it.
        if (s3TO.isEnableRRS()) {
            putObjectRequest.withStorageClass(StorageClass.ReducedRedundancy);
        }

        Upload upload = S3Utils.putObject(s3TO, putObjectRequest);

        upload.addProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {

                // Record the amount of bytes transferred.
                totalBytes += progressEvent.getBytesTransferred();

                LOGGER.trace("Template download from " + downloadUrl + " to S3 bucket " + s3TO.getBucketName() + " transferred  " + toHumanReadableSize(totalBytes) + " in " + ((new Date().getTime() - start.getTime()) / 1000) + " seconds");

                if (progressEvent.getEventType() == ProgressEventType.TRANSFER_STARTED_EVENT) {
                    status = Status.IN_PROGRESS;
                } else if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                    status = Status.DOWNLOAD_FINISHED;
                } else if (progressEvent.getEventType() == ProgressEventType.TRANSFER_CANCELED_EVENT) {
                    status = Status.ABORTED;
                } else if (progressEvent.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT) {
                    status = Status.UNRECOVERABLE_ERROR;
                }
            }
        });

        try {
            // Wait for the upload to complete.
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            // Interruption while waiting for the upload to complete.
            LOGGER.warn("Interruption occurred while waiting for upload of " + downloadUrl + " to complete");
        }

        downloadTime = new Date().getTime() - start.getTime();

        if (status == Status.DOWNLOAD_FINISHED) {
             LOGGER.info("Template download from " + downloadUrl + " to S3 bucket " + s3TO.getBucketName() + " transferred  " + toHumanReadableSize(totalBytes) + " in " + (downloadTime / 1000) + " seconds, completed successfully!");
        } else {
             LOGGER.warn("Template download from " + downloadUrl + " to S3 bucket " + s3TO.getBucketName() + " transferred  " + toHumanReadableSize(totalBytes) + " in " + (downloadTime / 1000) + " seconds, completed with status " + status.toString());
        }

        // Close input stream
        getMethod.releaseConnection();

        // Call the callback!
        if (callback != null) {
            callback.downloadComplete(status);
        }

        return totalBytes;
    }

    public String getDownloadUrl() {
        try {
            return getMethod.getURI().toString();
        } catch (URIException e) {
            return null;
        }
    }

    @Override
    public Status getStatus() {
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

    /**
     * Returns an InputStream only when the status is DOWNLOAD_FINISHED.
     *
     * The caller of this method must close the InputStream to prevent resource leaks!
     *
     * @return S3ObjectInputStream of the object.
     */
    public InputStream getS3ObjectInputStream() {
        // Check if the download is finished
        if (status != Status.DOWNLOAD_FINISHED) {
            return null;
        }

        return S3Utils.getObjectStream(s3TO, s3TO.getBucketName(), s3Key);
    }

    public void cleanupAfterError() {
        LOGGER.warn("Cleanup after error, trying to remove object: " + s3Key);

        S3Utils.deleteObject(s3TO, s3TO.getBucketName(), s3Key);
    }

    @Override
    public boolean stopDownload() {
        switch (status) {
            case IN_PROGRESS:
                if (getMethod != null) {
                    getMethod.abort();
                }
                break;
            case UNKNOWN:
            case NOT_STARTED:
            case RECOVERABLE_ERROR:
            case UNRECOVERABLE_ERROR:
            case ABORTED:
            case DOWNLOAD_FINISHED:
                // Remove the object if it already has been uploaded.
                S3Utils.deleteObject(s3TO, s3TO.getBucketName(), s3Key);
                break;
            default:
                break;
        }

        status = TemplateDownloader.Status.ABORTED;
        return true;
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
        // Start the download!
        download(resume, downloadCompleteCallback);
    }

    @Override
    public void setStatus(Status status) {
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
        return true;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
