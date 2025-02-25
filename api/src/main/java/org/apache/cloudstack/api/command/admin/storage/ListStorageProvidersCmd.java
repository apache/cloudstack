/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.admin.storage;

import java.util.List;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StorageProviderResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "listStorageProviders", description = "Lists storage providers.", responseObject = StorageProviderResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListStorageProvidersCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "the type of storage provider: either primary or image", required = true)
    private String type;

    public String getType() {
        return this.type;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        if (getType() == null) {
            throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "need to specify type: either primary or image");
        }

        List<StorageProviderResponse> providers = this.dataStoreProviderApiService.getDataStoreProviders(getType());
        ListResponse<StorageProviderResponse> responses = new ListResponse<StorageProviderResponse>();
        for (StorageProviderResponse provider : providers) {
            provider.setObjectName("dataStoreProvider");
        }
        responses.setResponses(providers);
        responses.setResponseName(this.getCommandName());
        this.setResponseObject(responses);
    }
}
