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

package org.apache.cloudstack.api.command.admin.kubernetes.version;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;

@APICommand(name = AddKubernetesSupportedVersionCmd.APINAME,
        description = "Add a supported Kubernetes version",
        responseObject = KubernetesSupportedVersionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        entityType = {KubernetesSupportedVersion.class},
        authorized = {RoleType.Admin})
public class AddKubernetesSupportedVersionCmd extends BaseCmd implements AdminCmd {
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
            description = "the semantic version of the Kubernetes version. It needs to be specified in MAJOR.MINOR.PATCH format")
    private String semanticVersion;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "the ID of the zone in which Kubernetes supported version will be available")
    private Long zoneId;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING,
            description = "the URL of the binaries ISO for Kubernetes supported version")
    private String url;

    @Parameter(name = ApiConstants.CHECKSUM, type = CommandType.STRING,
            description = "the checksum value of the binaries ISO. " + ApiConstants.CHECKSUM_PARAMETER_PREFIX_DESCRIPTION)
    private String checksum;

    @Parameter(name = ApiConstants.MIN_CPU_NUMBER, type = CommandType.INTEGER, required = true,
            description = "the minimum number of CPUs to be set with the Kubernetes version")
    private Integer minimumCpu;

    @Parameter(name = ApiConstants.MIN_MEMORY, type = CommandType.INTEGER, required = true,
            description = "the minimum RAM size in MB to be set with the Kubernetes version")
    private Integer minimumRamSize;

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

    public String getUrl() {
        return url;
    }

    public String getChecksum() {
        return checksum;
    }

    public Integer getMinimumCpu() {
        return minimumCpu;
    }

    public Integer getMinimumRamSize() {
        return minimumRamSize;
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
    public void execute() throws ServerApiException, ConcurrentOperationException {
        try {
            KubernetesSupportedVersionResponse response = kubernetesVersionService.addKubernetesSupportedVersion(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Kubernetes supported version");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
