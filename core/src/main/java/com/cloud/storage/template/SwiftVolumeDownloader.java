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

import com.cloud.agent.api.to.SwiftTO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Download a volume file using HTTP(S)
 *
 * This class, once instantiated, has the purpose to download a Volume to staging nfs or cache when using as Swift Image Store.
 *
 * Execution of the instance is started when runInContext() is called.
 */
public class SwiftVolumeDownloader extends ManagedContextRunnable implements TemplateDownloader {
    private static final Logger LOGGER = Logger.getLogger(SwiftVolumeDownloader.class.getName());
    private static final int DOWNLOAD_BUFFER_SIZE_BYTES = 1024* 1024;

    private final String downloadUrl;
    private final String fileName;
    private final String fileExtension;
    private final long volumeId;
    private final CloseableHttpClient httpClient;
    private final HttpGet httpGet;
    private final DownloadCompleteCallback downloadCompleteCallback;
    private final SwiftTO swiftTO;
    private String errorString = "";
    private Status status = Status.NOT_STARTED;
    private final ResourceType resourceType = ResourceType.VOLUME;
    private long remoteSize;
    private String md5sum;
    private long downloadTime;
    private long totalBytes;
    private final long maxVolumeSizeInBytes;
    private final String installPathPrefix;
    private final String installPath;
    private File volumeFile;
    private boolean resume = false;

    public SwiftVolumeDownloader(DownloadCommand cmd, DownloadCompleteCallback downloadCompleteCallback, long maxVolumeSizeInBytes, String installPathPrefix) {
        this.downloadUrl = cmd.getUrl();
        this.swiftTO = (SwiftTO) cmd.getDataStore();
        this.maxVolumeSizeInBytes = maxVolumeSizeInBytes;
        this.httpClient = initializeHttpClient();
        this.downloadCompleteCallback = downloadCompleteCallback;
        this.fileName = cmd.getName();
        this.fileExtension = cmd.getFormat().getFileExtension();
        this.volumeId = cmd.getId();
        this.installPathPrefix = installPathPrefix;
        this.installPath = cmd.getInstallPath();
        this.httpGet = new HttpGet(downloadUrl);
    }

    private CloseableHttpClient initializeHttpClient(){

        CloseableHttpClient client = null;
        try {
            //trust all certs
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true)
                    .build();
            client = HttpClients.custom().setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .setRetryHandler(buildRetryHandler(5))
                    .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        return client;
    }

    private HttpRequestRetryHandler buildRetryHandler(int retryCount){

        HttpRequestRetryHandler customRetryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(
                    IOException exception,
                    int executionCount,
                    HttpContext context) {
                if (executionCount >= retryCount) {
                    // Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    // Timeout
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    // Unknown host
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {
                    // Connection refused
                    return false;
                }
                if (exception instanceof SSLException) {
                    // SSL handshake exception
                    return false;
                }
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // Retry if the request is considered idempotent
                    return true;
                }
                return false;
            }

        };
        return customRetryHandler;
    }

    @Override
    public long download(boolean resume, DownloadCompleteCallback callback) {
        if (!status.equals(Status.NOT_STARTED)) {
            // Only start downloading if we haven't started yet.
            LOGGER.info("Volume download is already started, not starting again. Volume: " + downloadUrl);
            return 0;
        }

        HttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
            errorString = "Exception while executing HttpMethod " + httpGet.getMethod() + " on URL " + downloadUrl + " "
                    + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
            LOGGER.error(errorString);
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        // Headers
        long contentLength = response.getEntity().getContentLength();
        Header contentType = response.getEntity().getContentType();

        // Check the contentLengthHeader and transferEncodingHeader.
        if (contentLength <= 0) {
            errorString = "The Content Length of " + downloadUrl + " is <= 0 and content Type is "+contentType.toString();
            LOGGER.error(errorString);
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        } else {
            // The ContentLengthHeader is supplied, parse it's value.
            remoteSize = contentLength;
        }

        if (remoteSize > maxVolumeSizeInBytes) {
            errorString = "Remote size is too large for volume " + downloadUrl + " remote size is " + remoteSize + " max allowed is " + maxVolumeSizeInBytes;
            LOGGER.error(errorString);
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        InputStream inputStream;
        try {
            inputStream = new BufferedInputStream(response.getEntity().getContent());
        } catch (IOException e) {
            errorString = "Exception occurred while opening InputStream for volume from " + downloadUrl;
            LOGGER.error(errorString);
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        String filePath = installPathPrefix + File.separator + installPath;
        File directory = new File(filePath);
        File srcFile = new File(filePath + File.separator + fileName);
        try {
            if (!directory.exists()) {
                LOGGER.info("Creating directories "+filePath);
                directory.mkdirs();
            }
            if (!srcFile.createNewFile()) {
                LOGGER.info("Reusing existing file " + srcFile.getPath());
            }
        } catch (IOException e) {
            errorString = "Exception occurred while creating temp file " + srcFile.getPath();
            LOGGER.error(errorString);
            status = Status.UNRECOVERABLE_ERROR;
            return 0;
        }

        LOGGER.info("Starting download from " + downloadUrl + " to staging with size " + remoteSize + " bytes to " + srcFile.getPath());
        final Date downloadStart = new Date();

        try (FileOutputStream fileOutputStream = new FileOutputStream(srcFile);) {
            BufferedOutputStream outputStream = new BufferedOutputStream(fileOutputStream,DOWNLOAD_BUFFER_SIZE_BYTES);
            byte[] data = new byte[DOWNLOAD_BUFFER_SIZE_BYTES];
            int bufferLength = 0;
            while((bufferLength = inputStream.read(data,0,DOWNLOAD_BUFFER_SIZE_BYTES)) >= 0){
                totalBytes += bufferLength;
                outputStream.write(data,0,bufferLength);
                status = Status.IN_PROGRESS;
                LOGGER.trace("Download in progress: " + getDownloadPercent() + "%");
                if(totalBytes >= remoteSize){
                    volumeFile = srcFile;
                    status = Status.DOWNLOAD_FINISHED;
                }
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            LOGGER.error("Exception when downloading from url " + downloadUrl + " with remote size " + remoteSize
                    + " to staging nfs path " + srcFile.getPath() + " : " + e.getMessage(), e);
            status = Status.RECOVERABLE_ERROR;
            return 0;
        }

        downloadTime = new Date().getTime() - downloadStart.getTime();

        try (FileInputStream fs = new FileInputStream(srcFile)) {
            md5sum = DigestUtils.md5Hex(fs);
        } catch (IOException e) {
            LOGGER.error("Failed to get md5sum: " + srcFile.getAbsoluteFile());
        }

        if (status == Status.DOWNLOAD_FINISHED) {
            LOGGER.info("Template download from " + downloadUrl + " to staging nfs, transferred  " + totalBytes + " in " + (downloadTime / 1000) + " seconds, completed successfully!");
        } else {
            LOGGER.error("Template download from " + downloadUrl + " to staging nfs, transferred  " + totalBytes + " in " + (downloadTime / 1000) + " seconds, completed with status " + status.toString());
        }

        // Close http connection
        httpGet.releaseConnection();

        // Call the callback!
        if (callback != null) {
            callback.downloadComplete(status);
        }

        return totalBytes;
    }

    public String getDownloadUrl() {
        return httpGet.getURI().toString();
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

    @Override
    public boolean stopDownload() {
        switch (status) {
            case IN_PROGRESS:
                if (httpGet != null) {
                    httpGet.abort();
                }
                break;
            case UNKNOWN:
            case NOT_STARTED:
            case RECOVERABLE_ERROR:
            case UNRECOVERABLE_ERROR:
            case ABORTED:
            case DOWNLOAD_FINISHED:
                // Remove the object if it already has been uploaded.
                // SwiftUtil.deleteObject(swiftTO, swiftPath);
                break;
            default:
                break;
        }

        status = Status.ABORTED;
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
        LOGGER.info("Starting download in managed context resume = " + resume + " callback = " + downloadCompleteCallback.toString());
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
        return installPath;
    }

    @Override
    public void setResume(boolean resume) {
        this.resume = resume;
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

    public String getFileExtension() {
        return fileExtension;
    }

    public String getMd5sum() { return md5sum; }

    public File getVolumeFile() { return volumeFile; }

    public long getMaxTemplateSizeInBytes() {return maxVolumeSizeInBytes;}
}