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

package org.apache.cloudstack.api.command.admin.br.provider;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.br.BRManager;
import org.apache.cloudstack.api.response.BRProviderResponse;
import org.apache.cloudstack.framework.br.BRProvider;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListBRProvidersCmd.APINAME,
        description = "Lists Backup and Recovery providers",
        responseObject = BRProviderResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin})
public class ListBRProvidersCmd extends BaseListCmd {

    public static final String APINAME = "listBRProviders";

    @Inject
    BRManager brManager;

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        try {
            List<BRProvider> providers = brManager.listBRProviders();
            ListResponse<BRProviderResponse> response = new ListResponse<BRProviderResponse>();
            List<BRProviderResponse> providersResponse = new ArrayList<BRProviderResponse>();

            if (CollectionUtils.isNotEmpty(providers)) {
                for (BRProvider providerVO : providers) {
                    BRProviderResponse providerResponse = brManager.createBRProviderResponse(providerVO);
                    providersResponse.add(providerResponse);
                }
            }

            response.setResponses(providersResponse, providersResponse.size());
            response.setObjectName("brproviders");
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }
}
