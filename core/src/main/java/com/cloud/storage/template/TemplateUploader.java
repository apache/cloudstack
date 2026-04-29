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

public interface TemplateUploader extends Runnable {

    /**
     * Callback used to notify completion of upload
     *
     */
    public interface UploadCompleteCallback {
        void uploadComplete(Status status);

    }

    public static enum Status {
        UNKNOWN, NOT_STARTED, IN_PROGRESS, ABORTED, UNRECOVERABLE_ERROR, RECOVERABLE_ERROR, UPLOAD_FINISHED, POST_UPLOAD_FINISHED
    }

    /**
     * Initiate upload
     * @param callback completion callback to be called after upload is complete
     * @return bytes uploaded
     */
    public long upload(UploadCompleteCallback callback);

    /**
     * @return
     */
    public boolean stopUpload();

    /**
     * @return percent of file uploaded
     */
    public int getUploadPercent();

    /**
     * Get the status of the upload
     * @return status of upload
     */
    public TemplateUploader.Status getStatus();

    /**
     * Get time taken to upload so far
     * @return time in seconds taken to upload
     */
    public long getUploadTime();

    /**
     * Get bytes uploaded
     * @return bytes uploaded so far
     */
    public long getUploadedBytes();

    /**
     * Get the error if any
     * @return error string if any
     */
    public String getUploadError();

    /** Get local path of the uploaded file
     * @return local path of the file uploaded
     */
    public String getUploadLocalPath();

    public void setStatus(TemplateUploader.Status status);

    public void setUploadError(String string);

    public void setResume(boolean resume);

}
