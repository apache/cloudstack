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

package org.apache.cloudstack.api.command.admin.kubernetesversion;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.kubernetesversion.KubernetesSupportedVersion;
import com.cloud.kubernetesversion.KubernetesVersionService;
import com.google.common.base.Strings;

@APICommand(name = AddKubernetesSupportedVersionCmd.APINAME,
        description = "Add a supported Kubernetes version",
        responseObject = KubernetesSupportedVersionResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {KubernetesSupportedVersion.class},
        authorized = {RoleType.Admin})
public class AddKubernetesSupportedVersionCmd extends BaseCmd implements UserCmd {
    public static final Logger LOGGER = Logger.getLogger(AddKubernetesSupportedVersionCmd.class.getName());
    public static final String APINAME = "addKubernetesSupportedVersion";

    @Inject
    private KubernetesVersionService kubernetesVersionService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING,
            description = "the name of the Kubernetes supported version")
    private String name;

    @Parameter(name = ApiConstants.SEMANTIC_VERSION, type = CommandType.STRING, required = true,
            description = "the semantic version of the Kubernetes version")
    private String semanticVersion;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "the ID of the zone in which Kubernetes supported version will be available")
    private Long zoneId;

    @Parameter(name = ApiConstants.ISO_ID, type = CommandType.UUID,
            entityType = TemplateResponse.class,
            description = "the ID of the binaries ISO for Kubernetes supported version")
    private Long isoId;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING,
            description = "the URL of the binaries ISO for Kubernetes supported version")
    private String url;

    @Parameter(name = ApiConstants.CHECKSUM, type = CommandType.STRING,
            description = "the checksum value of the binaries ISO. " + ApiConstants.CHECKSUM_PARAMETER_PREFIX_DESCRIPTION)
    private String checksum;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getName() {
        return name;
    }

    public String getSemanticVersion() {
        if(Strings.isNullOrEmpty(semanticVersion)) {
            throw new InvalidParameterValueException("Version can not be null");
        }
        if(!semanticVersion.matches("[0-9]+(\\.[0-9]+)*")) {
            throw new IllegalArgumentException("Invalid version format. Semantic version needed");
        }
        return semanticVersion;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getIsoId() {
        return isoId;
    }

    public String getUrl() {
        return url;
    }

    public String getChecksum() {
        return checksum;
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + "response";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        KubernetesSupportedVersionResponse response = kubernetesVersionService.addKubernetesSupportedVersion(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
