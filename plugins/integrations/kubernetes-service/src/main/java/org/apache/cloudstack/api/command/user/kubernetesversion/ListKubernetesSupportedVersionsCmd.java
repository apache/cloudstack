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

package org.apache.cloudstack.api.command.user.kubernetesversion;

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
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.kubernetesversion.KubernetesVersionService;
import com.google.common.base.Strings;

@APICommand(name = ListKubernetesSupportedVersionsCmd.APINAME,
        description = "Lists container clusters",
        responseObject = KubernetesSupportedVersionResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListKubernetesSupportedVersionsCmd extends BaseListCmd {
    public static final Logger LOGGER = Logger.getLogger(ListKubernetesSupportedVersionsCmd.class.getName());
    public static final String APINAME = "listKubernetesSupportedVersions";

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

    @Parameter(name = ApiConstants.MIN_KUBERNETES_VERSION, type = CommandType.STRING,
            description = "the minimum Kubernetes version")
    private String minimumKubernetesVersion;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getMinimumKubernetesVersion() {
        if(!Strings.isNullOrEmpty(minimumKubernetesVersion) &&
                !minimumKubernetesVersion.matches("[0-9]+(\\.[0-9]+)*")) {
            throw new IllegalArgumentException("Invalid version format");
        }
        return minimumKubernetesVersion;
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + "response";
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ListResponse<KubernetesSupportedVersionResponse> response = kubernetesVersionService.listKubernetesSupportedVersions(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
