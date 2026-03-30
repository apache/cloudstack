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
package com.cloud.agent.api.to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualMachineMetadataTO {
    // VM details
    private final String name;
    private final String internalName;
    private final String displayName;
    private final String instanceUuid;
    private final Integer cpuCores;
    private final Integer memory;
    private final Long created;
    private final Long started;

    // Owner details
    private final String ownerDomainUuid;
    private final String ownerDomainName;
    private final String ownerAccountUuid;
    private final String ownerAccountName;
    private final String ownerProjectUuid;
    private final String ownerProjectName;

    // Host and service offering
    private final String serviceOfferingName;
    private final List<String> serviceOfferingHostTags;

    // zone, pod, and cluster details
    private final String zoneName;
    private final String zoneUuid;
    private final String podName;
    private final String podUuid;
    private final String clusterName;
    private final String clusterUuid;

    // resource tags
    private final Map<String, String> resourceTags;

    public VirtualMachineMetadataTO(
            String name, String internalName, String displayName, String instanceUuid, Integer cpuCores, Integer memory, Long created, Long started,
            String ownerDomainUuid, String ownerDomainName, String ownerAccountUuid, String ownerAccountName, String ownerProjectUuid, String ownerProjectName,
            String serviceOfferingName, List<String> serviceOfferingHostTags,
            String zoneName, String zoneUuid, String podName, String podUuid, String clusterName, String clusterUuid, Map<String, String> resourceTags) {
        /*
        * Something failed in the metadata shall not be a fatal error, the VM can still be started
        * Thus, the unknown fields just get an explicit "unknown" value so it can be fixed in case
        * there are bugs on some execution paths.
        * */

        this.name = (name != null) ? name : "unknown";
        this.internalName = (internalName != null) ? internalName : "unknown";
        this.displayName = (displayName != null) ? displayName : "unknown";
        this.instanceUuid = (instanceUuid != null) ? instanceUuid : "unknown";
        this.cpuCores = (cpuCores != null) ? cpuCores : -1;
        this.memory = (memory != null) ? memory : -1;
        this.created = (created != null) ? created : 0;
        this.started = (started != null) ? started : 0;
        this.ownerDomainUuid = (ownerDomainUuid != null) ? ownerDomainUuid : "unknown";
        this.ownerDomainName = (ownerDomainName != null) ? ownerDomainName : "unknown";
        this.ownerAccountUuid = (ownerAccountUuid != null) ? ownerAccountUuid : "unknown";
        this.ownerAccountName = (ownerAccountName != null) ? ownerAccountName : "unknown";
        this.ownerProjectUuid = (ownerProjectUuid != null) ? ownerProjectUuid : "unknown";
        this.ownerProjectName = (ownerProjectName != null) ? ownerProjectName : "unknown";
        this.serviceOfferingName = (serviceOfferingName != null) ? serviceOfferingName : "unknown";
        this.serviceOfferingHostTags = (serviceOfferingHostTags != null) ? serviceOfferingHostTags : new ArrayList<>();
        this.zoneName = (zoneName != null) ? zoneName : "unknown";
        this.zoneUuid = (zoneUuid != null) ? zoneUuid : "unknown";
        this.podName = (podName != null) ? podName : "unknown";
        this.podUuid = (podUuid != null) ? podUuid : "unknown";
        this.clusterName = (clusterName != null) ? clusterName : "unknown";
        this.clusterUuid = (clusterUuid != null) ? clusterUuid : "unknown";

        this.resourceTags = (resourceTags != null) ? resourceTags : new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public Integer getMemory() {
        return memory;
    }

    public Long getCreated() { return created; }

    public Long getStarted() {
        return started;
    }

    public String getOwnerDomainUuid() {
        return ownerDomainUuid;
    }

    public String getOwnerDomainName() {
        return ownerDomainName;
    }

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public String getOwnerAccountName() {
        return ownerAccountName;
    }

    public String getOwnerProjectUuid() {
        return ownerProjectUuid;
    }

    public String getOwnerProjectName() {
        return ownerProjectName;
    }

    public String getserviceOfferingName() {
        return serviceOfferingName;
    }

    public List<String> getserviceOfferingHostTags() {
        return serviceOfferingHostTags;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public String getPodName() {
        return podName;
    }

    public String getPodUuid() {
        return podUuid;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public Map<String, String> getResourceTags() { return resourceTags; }
}
