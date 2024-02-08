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

package org.apache.cloudstack.api.command.user.kubernetes.version;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.kubernetes.version.KubernetesVersionService;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = "listKubernetesSupportedVersions",
        description = "Lists supported Kubernetes version",
        responseObject = KubernetesSupportedVersionResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListKubernetesSupportedVersionsCmd extends BaseListCmd {

    @Inject
    private KubernetesVersionService kubernetesVersionService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = KubernetesSupportedVersionResponse.class,
            description = "the ID of the Kubernetes supported version")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "the ID of the zone in which Kubernetes supported version will be available")
    private Long zoneId;

    @Parameter(name = ApiConstants.MIN_SEMANTIC_VERSION, type = CommandType.STRING,
            description = "the minimum semantic version for the Kubernetes supported version to be listed")
    private String minimumSemanticVersion;

    @Parameter(name = ApiConstants.MIN_KUBERNETES_VERSION_ID, type = CommandType.UUID,
            entityType = KubernetesSupportedVersionResponse.class,
            description = "the ID of the minimum Kubernetes supported version")
    private Long minimumKubernetesVersionId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getMinimumSemanticVersion() {
        if(StringUtils.isNotEmpty(minimumSemanticVersion) &&
                !minimumSemanticVersion.matches("[0-9]+(\\.[0-9]+)*")) {
            throw new IllegalArgumentException("Invalid version format");
        }
        return minimumSemanticVersion;
    }

    public Long getMinimumKubernetesVersionId() {
        return minimumKubernetesVersionId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        ListResponse<KubernetesSupportedVersionResponse> response = kubernetesVersionService.listKubernetesSupportedVersions(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
