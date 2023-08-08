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

import java.util.Map;

import com.cloud.storage.template.Processor;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;
import org.apache.cloudstack.storage.resource.SecondaryStorageResource;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.utils.net.Proxy;
import com.cloud.agent.api.to.S3TO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.template.TemplateDownloader;
import com.cloud.storage.template.TemplateProp;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.component.Manager;

public interface DownloadManager extends Manager {

    /**
     * Initiate download of a public template
     * @param id unique id.
     * @param url  the url from where to download from
     * @param hvm  whether the template is a hardware virtual machine
     * @param accountId the accountId of the iso owner (null if public iso)
     * @param descr    description of the template
     * @param user username used for authentication to the server
     * @param password password used for authentication to the server
     * @param maxDownloadSizeInBytes (optional) max download size for the template, in bytes.
     * @param resourceType signifying the type of resource like template, volume etc.
     * @return job-id that can be used to interrogate the status of the download.
     */
    public String downloadPublicTemplate(long id, String url, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum,
        String installPathPrefix, String templatePath, String userName, String passwd, long maxDownloadSizeInBytes, Proxy proxy, ResourceType resourceType);

    public String downloadS3Template(S3TO s3, long id, String url, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum,
        String installPathPrefix, String user, String password, long maxTemplateSizeInBytes, Proxy proxy, ResourceType resourceType);

    Map<String, Processor> getProcessors();

    /**
     * Get the status of a download job
     * @param jobId job Id
     * @return status of the download job
     */
    public TemplateDownloader.Status getDownloadStatus(String jobId);

    /**
     * Get the status of a download job
     * @param jobId job Id
     * @return status of the download job
     */
    public Status getDownloadStatus2(String jobId);

    /**
     * Get the download percent of a download job
     * @param jobId job Id
     * @return
     */
    public int getDownloadPct(String jobId);

    /**
     * Get the download error if any
     * @param jobId job Id
     * @return
     */
    public String getDownloadError(String jobId);

    /**
     * Get the local path for the download
     * @param jobId job Id
     * @return
    public String getDownloadLocalPath(String jobId);
     */

    /** Handle download commands from the management server
     * @param cmd cmd from server
     * @return answer representing status of download.
     */
    public DownloadAnswer handleDownloadCommand(SecondaryStorageResource resource, DownloadCommand cmd);

    /**
    /**
     * @return list of template info for installed templates
     */
    public Map<String, TemplateProp> gatherTemplateInfo(String templateDir);

    /**
    /**
     * @return list of volume info for installed volumes
     */
    public Map<Long, TemplateProp> gatherVolumeInfo(String volumeDir);

}
