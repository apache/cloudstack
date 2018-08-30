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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.log4j.Logger;

public class FtpTemplateUploader implements TemplateUploader {

    public static final Logger s_logger = Logger.getLogger(FtpTemplateUploader.class.getName());
    public TemplateUploader.Status status = TemplateUploader.Status.NOT_STARTED;
    public String errorString = "";
    public long totalBytes = 0;
    public long entitySizeinBytes;
    private String sourcePath;
    private String ftpUrl;
    private UploadCompleteCallback completionCallback;
    private BufferedInputStream inputStream = null;
    private BufferedOutputStream outputStream = null;
    private static final int CHUNK_SIZE = 1024 * 1024; //1M

    public FtpTemplateUploader(String sourcePath, String url, UploadCompleteCallback callback, long entitySizeinBytes) {

        this.sourcePath = sourcePath;
        ftpUrl = url;
        completionCallback = callback;
        this.entitySizeinBytes = entitySizeinBytes;

    }

    @Override
    public long upload(UploadCompleteCallback callback) {

        switch (status) {
            case ABORTED:
            case UNRECOVERABLE_ERROR:
            case UPLOAD_FINISHED:
                return 0;
            default:

        }

        new Date();

        StringBuffer sb = new StringBuffer(ftpUrl);
        // check for authentication else assume its anonymous access.
        /* if (user != null && password != null)
                 {
                    sb.append( user );
                    sb.append( ':' );
                    sb.append( password );
                    sb.append( '@' );
                 }*/
        /*
         * type ==> a=ASCII mode, i=image (binary) mode, d= file directory
         * listing
         */
        sb.append(";type=i");

        try {
            URL url = new URL(sb.toString());
            URLConnection urlc = url.openConnection();
            File sourceFile = new File(sourcePath);
            entitySizeinBytes = sourceFile.length();

            outputStream = new BufferedOutputStream(urlc.getOutputStream());
            inputStream = new BufferedInputStream(new FileInputStream(sourceFile));

            status = TemplateUploader.Status.IN_PROGRESS;

            int bytes = 0;
            byte[] block = new byte[CHUNK_SIZE];
            boolean done = false;
            while (!done && status != Status.ABORTED) {
                if ((bytes = inputStream.read(block, 0, CHUNK_SIZE)) > -1) {
                    outputStream.write(block, 0, bytes);
                    totalBytes += bytes;
                } else {
                    done = true;
                }
            }
            status = TemplateUploader.Status.UPLOAD_FINISHED;
            return totalBytes;
        } catch (MalformedURLException e) {
            status = TemplateUploader.Status.UNRECOVERABLE_ERROR;
            errorString = e.getMessage();
            s_logger.error(errorString);
        } catch (IOException e) {
            status = TemplateUploader.Status.UNRECOVERABLE_ERROR;
            errorString = e.getMessage();
            s_logger.error(errorString);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ioe) {
                s_logger.error(" Caught exception while closing the resources");
            }
            if (callback != null) {
                callback.uploadComplete(status);
            }
        }

        return 0;
    }

    @Override
    public void run() {
        try {
            upload(completionCallback);
        } catch (Throwable t) {
            s_logger.warn("Caught exception during upload " + t.getMessage(), t);
            errorString = "Failed to install: " + t.getMessage();
            status = TemplateUploader.Status.UNRECOVERABLE_ERROR;
        }

    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getUploadError() {
        return errorString;
    }

    @Override
    public String getUploadLocalPath() {
        return sourcePath;
    }

    @Override
    public int getUploadPercent() {
        if (entitySizeinBytes == 0) {
            return 0;
        }
        return (int)(100.0 * totalBytes / entitySizeinBytes);
    }

    @Override
    public long getUploadTime() {
        // TODO
        return 0;
    }

    @Override
    public long getUploadedBytes() {
        return totalBytes;
    }

    @Override
    public void setResume(boolean resume) {

    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public void setUploadError(String string) {
        errorString = string;
    }

    @Override
    public boolean stopUpload() {
        switch (getStatus()) {
            case IN_PROGRESS:
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    s_logger.error(" Caught exception while closing the resources");
                }
                status = TemplateUploader.Status.ABORTED;
                return true;
            case UNKNOWN:
            case NOT_STARTED:
            case RECOVERABLE_ERROR:
            case UNRECOVERABLE_ERROR:
            case ABORTED:
                status = TemplateUploader.Status.ABORTED;
            case UPLOAD_FINISHED:
                return true;

            default:
                return true;
        }
    }

}
