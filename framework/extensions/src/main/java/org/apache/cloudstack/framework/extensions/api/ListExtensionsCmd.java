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

package org.apache.cloudstack.framework.extensions.api;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listExtensions",
        description = "Lists extensions",
        responseObject = ExtensionResponse.class,
        responseHasSensitiveInfo = false,
        entityType = {Extension.class},
        authorized = {RoleType.Admin},
        since = "4.21.0")
public class ListExtensionsCmd extends BaseListCmd {

    @Inject
    ExtensionsManager extensionsManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the extension")
    private String name;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ExtensionResponse.class, description = "uuid of the extension")
    private Long extensionId;

    @Parameter(name = ApiConstants.DETAILS,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "comma separated list of extension details requested, "
                    + "value can be a list of [all, resources, external, min]."
                    + " When no parameters are passed, all the details are returned.")
    private List<String> details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public Long getExtensionId() {
        return extensionId;
    }

    public EnumSet<ApiConstants.ExtensionDetails> getDetails() throws InvalidParameterValueException {
        if (CollectionUtils.isEmpty(details)) {
            return EnumSet.of(ApiConstants.ExtensionDetails.all);
        }
        try {
            Set<ApiConstants.ExtensionDetails> detailsSet = new HashSet<>();
            for (String detail : details) {
                detailsSet.add(ApiConstants.ExtensionDetails.valueOf(detail));
            }
            return EnumSet.copyOf(detailsSet);
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException("The details parameter contains a non permitted value." +
                    "The allowed values are " + EnumSet.allOf(ApiConstants.ExtensionDetails.class));
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        List<ExtensionResponse> responses = extensionsManager.listExtensions(this);

        ListResponse<ExtensionResponse> response = new ListResponse<>();
        response.setResponses(responses, responses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
