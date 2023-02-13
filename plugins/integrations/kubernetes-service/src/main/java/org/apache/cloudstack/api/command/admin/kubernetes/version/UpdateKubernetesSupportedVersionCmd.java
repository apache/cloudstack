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
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "updateKubernetesSupportedVersion",
        description = "Update a supported Kubernetes version",
        responseObject = KubernetesSupportedVersionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        entityType = {KubernetesSupportedVersion.class},
        authorized = {RoleType.Admin})
public class UpdateKubernetesSupportedVersionCmd extends BaseCmd implements AdminCmd {
    public static final Logger LOGGER = Logger.getLogger(UpdateKubernetesSupportedVersionCmd.class.getName());

    @Inject
    private KubernetesVersionService kubernetesVersionService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID,
            entityType = KubernetesSupportedVersionResponse.class,
            description = "the ID of the Kubernetes supported version",
            required = true)
    private Long id;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING,
            description = "the enabled or disabled state of the Kubernetes supported version",
            required = true)
    private String state;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        try {
            KubernetesSupportedVersionResponse response = kubernetesVersionService.updateKubernetesSupportedVersion(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update Kubernetes supported version");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
