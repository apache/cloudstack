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
package org.apache.cloudstack.storage.template;

import com.cloud.agent.api.Answer;
import org.apache.cloudstack.storage.resource.SecondaryStorageResource;

import com.cloud.agent.api.storage.CreateEntityDownloadURLAnswer;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.agent.api.storage.UploadAnswer;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Upload.Status;
import com.cloud.storage.template.TemplateUploader;
import com.cloud.utils.component.Manager;

public interface UploadManager extends Manager {

    /**
     * @param jobId job Id
     * @return status of the upload job
     */
    public TemplateUploader.Status getUploadStatus(String jobId);

    /**
     * @param jobId job Id
     * @return status of the upload job
     */
    public Status getUploadStatus2(String jobId);

    /**
     * Get the upload percent of a upload job
     * @param jobId job Id
     * @return
     */
    public int getUploadPct(String jobId);

    /**
     * Get the upload error if any
     * @param jobId job Id
     * @return
     */
    public String getUploadError(String jobId);

    /**
     * Get the local path for the upload
     * @param jobId job Id
     * @return
    public String getUploadLocalPath(String jobId);
     */

    /** Handle upload commands from the management server
     * @param cmd cmd from server
     * @return answer representing status of upload.
     */
    public UploadAnswer handleUploadCommand(SecondaryStorageResource resource, UploadCommand cmd);

    public String getPublicTemplateRepo();

    String uploadPublicTemplate(long id, String url, String name, ImageFormat format, Long accountId, String descr, String cksum, String installPathPrefix, String user,
        String password, long maxTemplateSizeInBytes);

    CreateEntityDownloadURLAnswer handleCreateEntityURLCommand(CreateEntityDownloadURLCommand cmd);

    Answer handleDeleteEntityDownloadURLCommand(DeleteEntityDownloadURLCommand cmd);

}
