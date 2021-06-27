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
package org.apache.cloudstack.api.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.projects.Project;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Project.class)
public class ProjectResponse extends BaseResponse implements ResourceLimitAndCountResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the project")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the project")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT)
    @Param(description = "the displaytext of the project")
    private String displaytext;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain id the project belongs to")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name where the project belongs to")
    private String domain;

    @SerializedName(ApiConstants.OWNER)
    @Param(description = "the account name of the project's owners")
    private List<Map<String, String>> owners;

    @SerializedName("projectaccountname")
    @Param(description="the project account name of the project")
    private String projectAccountName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the project")
    private String state;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with vm", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags = new ArrayList<ResourceTagResponse>();

    @SerializedName("networklimit")
    @Param(description = "the total number of networks the project can own", since = "4.2.0")
    private String networkLimit;

    @SerializedName("networktotal")
    @Param(description = "the total number of networks owned by project", since = "4.2.0")
    private Long networkTotal;

    @SerializedName("networkavailable")
    @Param(description = "the total number of networks available to be created for this project", since = "4.2.0")
    private String networkAvailable;

    @SerializedName("vpclimit")
    @Param(description = "the total number of vpcs the project can own", since = "4.2.0")
    private String vpcLimit;

    @SerializedName("vpctotal")
    @Param(description = "the total number of vpcs owned by project", since = "4.2.0")
    private Long vpcTotal;

    @SerializedName("vpcavailable")
    @Param(description = "the total number of vpcs available to be created for this project", since = "4.2.0")
    private String vpcAvailable;

    @SerializedName("cpulimit")
    @Param(description = "the total number of cpu cores the project can own", since = "4.2.0")
    private String cpuLimit;

    @SerializedName("cputotal")
    @Param(description = "the total number of cpu cores owned by project", since = "4.2.0")
    private Long cpuTotal;

    @SerializedName("cpuavailable")
    @Param(description = "the total number of cpu cores available to be created for this project", since = "4.2.0")
    private String cpuAvailable;

    @SerializedName("memorylimit")
    @Param(description = "the total memory (in MB) the project can own", since = "4.2.0")
    private String memoryLimit;

    @SerializedName("memorytotal")
    @Param(description = "the total memory (in MB) owned by project", since = "4.2.0")
    private Long memoryTotal;

    @SerializedName("memoryavailable")
    @Param(description = "the total memory (in MB) available to be created for this project", since = "4.2.0")
    private String memoryAvailable;

    @SerializedName("primarystoragelimit")
    @Param(description = "the total primary storage space (in GiB) the project can own", since = "4.2.0")
    private String primaryStorageLimit;

    @SerializedName("primarystoragetotal")
    @Param(description = "the total primary storage space (in GiB) owned by project", since = "4.2.0")
    private Long primaryStorageTotal;

    @SerializedName("primarystorageavailable")
    @Param(description = "the total primary storage space (in GiB) available to be used for this project", since = "4.2.0")
    private String primaryStorageAvailable;

    @SerializedName("secondarystoragelimit")
    @Param(description = "the total secondary storage space (in GiB) the project can own", since = "4.2.0")
    private String secondaryStorageLimit;

    @SerializedName("secondarystoragetotal")
    @Param(description = "the total secondary storage space (in GiB) owned by project", since = "4.2.0")
    private float secondaryStorageTotal;

    @SerializedName("secondarystorageavailable")
    @Param(description = "the total secondary storage space (in GiB) available to be used for this project", since = "4.2.0")
    private String secondaryStorageAvailable;

    @SerializedName(ApiConstants.VM_LIMIT)
    @Param(description = "the total number of virtual machines that can be deployed by this project", since = "4.2.0")
    private String vmLimit;

    @SerializedName(ApiConstants.VM_TOTAL)
    @Param(description = "the total number of virtual machines deployed by this project", since = "4.2.0")
    private Long vmTotal;

    @SerializedName(ApiConstants.VM_AVAILABLE)
    @Param(description = "the total number of virtual machines available for this project to acquire", since = "4.2.0")
    private String vmAvailable;

    @SerializedName(ApiConstants.IP_LIMIT)
    @Param(description = "the total number of public ip addresses this project can acquire", since = "4.2.0")
    private String ipLimit;

    @SerializedName(ApiConstants.IP_TOTAL)
    @Param(description = "the total number of public ip addresses allocated for this project", since = "4.2.0")
    private Long ipTotal;

    @SerializedName(ApiConstants.IP_AVAILABLE)
    @Param(description = "the total number of public ip addresses available for this project to acquire", since = "4.2.0")
    private String ipAvailable;

    @SerializedName("volumelimit")
    @Param(description = "the total volume which can be used by this project", since = "4.2.0")
    private String volumeLimit;

    @SerializedName("volumetotal")
    @Param(description = "the total volume being used by this project", since = "4.2.0")
    private Long volumeTotal;

    @SerializedName("volumeavailable")
    @Param(description = "the total volume available for this project", since = "4.2.0")
    private String volumeAvailable;

    @SerializedName("snapshotlimit")
    @Param(description = "the total number of snapshots which can be stored by this project", since = "4.2.0")
    private String snapshotLimit;

    @SerializedName("snapshottotal")
    @Param(description = "the total number of snapshots stored by this project", since = "4.2.0")
    private Long snapshotTotal;

    @SerializedName("snapshotavailable")
    @Param(description = "the total number of snapshots available for this project", since = "4.2.0")
    private String snapshotAvailable;

    @SerializedName("templatelimit")
    @Param(description = "the total number of templates which can be created by this project", since = "4.2.0")
    private String templateLimit;

    @SerializedName("templatetotal")
    @Param(description = "the total number of templates which have been created by this project", since = "4.2.0")
    private Long templateTotal;

    @SerializedName("templateavailable")
    @Param(description = "the total number of templates available to be created by this project", since = "4.2.0")
    private String templateAvailable;

    @SerializedName("vmstopped")
    @Param(description = "the total number of virtual machines stopped for this project", since = "4.2.0")
    private Integer vmStopped;

    @SerializedName("vmrunning")
    @Param(description = "the total number of virtual machines running for this project", since = "4.2.0")
    private Integer vmRunning;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplaytext(String displaytext) {
        this.displaytext = displaytext;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setProjectAccountName(String projectAccountName) {
        this.projectAccountName = projectAccountName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void addTag(ResourceTagResponse tag) {
        tags.add(tag);
    }

    @Override
    public void setVmLimit(String vmLimit) {
        this.vmLimit = vmLimit;
    }

    @Override
    public void setVmTotal(Long vmTotal) {
        this.vmTotal = vmTotal;
    }

    @Override
    public void setVmAvailable(String vmAvailable) {
        this.vmAvailable = vmAvailable;
    }

    @Override
    public void setIpLimit(String ipLimit) {
        this.ipLimit = ipLimit;
    }

    @Override
    public void setIpTotal(Long ipTotal) {
        this.ipTotal = ipTotal;
    }

    @Override
    public void setIpAvailable(String ipAvailable) {
        this.ipAvailable = ipAvailable;
    }

    @Override
    public void setVolumeLimit(String volumeLimit) {
        this.volumeLimit = volumeLimit;
    }

    @Override
    public void setVolumeTotal(Long volumeTotal) {
        this.volumeTotal = volumeTotal;
    }

    @Override
    public void setVolumeAvailable(String volumeAvailable) {
        this.volumeAvailable = volumeAvailable;
    }

    @Override
    public void setSnapshotLimit(String snapshotLimit) {
        this.snapshotLimit = snapshotLimit;
    }

    @Override
    public void setSnapshotTotal(Long snapshotTotal) {
        this.snapshotTotal = snapshotTotal;
    }

    @Override
    public void setSnapshotAvailable(String snapshotAvailable) {
        this.snapshotAvailable = snapshotAvailable;
    }

    @Override
    public void setTemplateLimit(String templateLimit) {
        this.templateLimit = templateLimit;
    }

    @Override
    public void setTemplateTotal(Long templateTotal) {
        this.templateTotal = templateTotal;
    }

    @Override
    public void setTemplateAvailable(String templateAvailable) {
        this.templateAvailable = templateAvailable;
    }

    @Override
    public void setVmStopped(Integer vmStopped) {
        this.vmStopped = vmStopped;
    }

    @Override
    public void setVmRunning(Integer vmRunning) {
        this.vmRunning = vmRunning;
    }

    @Override
    public void setNetworkLimit(String networkLimit) {
        this.networkLimit = networkLimit;
    }

    @Override
    public void setNetworkTotal(Long networkTotal) {
        this.networkTotal = networkTotal;
    }

    @Override
    public void setNetworkAvailable(String networkAvailable) {
        this.networkAvailable = networkAvailable;
    }

    @Override
    public void setVpcLimit(String vpcLimit) {
        this.vpcLimit = networkLimit;
    }

    @Override
    public void setVpcTotal(Long vpcTotal) {
        this.vpcTotal = vpcTotal;
    }

    @Override
    public void setVpcAvailable(String vpcAvailable) {
        this.vpcAvailable = vpcAvailable;
    }

    @Override
    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    @Override
    public void setCpuTotal(Long cpuTotal) {
        this.cpuTotal = cpuTotal;
    }

    @Override
    public void setCpuAvailable(String cpuAvailable) {
        this.cpuAvailable = cpuAvailable;
    }

    @Override
    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    @Override
    public void setMemoryTotal(Long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    @Override
    public void setMemoryAvailable(String memoryAvailable) {
        this.memoryAvailable = memoryAvailable;
    }

    @Override
    public void setPrimaryStorageLimit(String primaryStorageLimit) {
        this.primaryStorageLimit = primaryStorageLimit;
    }

    @Override
    public void setPrimaryStorageTotal(Long primaryStorageTotal) {
        this.primaryStorageTotal = primaryStorageTotal;
    }

    @Override
    public void setPrimaryStorageAvailable(String primaryStorageAvailable) {
        this.primaryStorageAvailable = primaryStorageAvailable;
    }

    @Override
    public void setSecondaryStorageLimit(String secondaryStorageLimit) {
        this.secondaryStorageLimit = secondaryStorageLimit;
    }

    @Override
    public void setSecondaryStorageTotal(float secondaryStorageTotal) {
        this.secondaryStorageTotal = secondaryStorageTotal;
    }

    @Override
    public void setSecondaryStorageAvailable(String secondaryStorageAvailable) {
        this.secondaryStorageAvailable = secondaryStorageAvailable;
    }

    public void setOwners(List<Map<String, String>> owners) {
        this.owners = owners;
    }
}
