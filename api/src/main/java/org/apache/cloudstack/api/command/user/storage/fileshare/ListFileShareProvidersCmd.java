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

package org.apache.cloudstack.api.command.user.storage.fileshare;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.FileShareProviderResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.storage.fileshare.FileShareProvider;
import org.apache.cloudstack.storage.fileshare.FileShareService;

@APICommand(name = "listFileShareProviders",
        responseObject = FileShareProviderResponse.class,
        description = "Lists all available file share providers.",
        requestHasSensitiveInfo = false,
        since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListFileShareProvidersCmd extends BaseListCmd {

    @Inject
    public FileShareService fileShareService;

    @Override
    public void execute() {
        List<FileShareProvider> fileShareProviders = fileShareService.getFileShareProviders();
        final ListResponse<FileShareProviderResponse> response = new ListResponse<>();
        final List<FileShareProviderResponse> responses = new ArrayList<>();

        for (FileShareProvider fileShareProvider : fileShareProviders) {
            FileShareProviderResponse fileShareProviderResponse = new FileShareProviderResponse();
            fileShareProviderResponse.setName(fileShareProvider.getName());
            fileShareProviderResponse.setObjectName("fileshareprovider");
            responses.add(fileShareProviderResponse);
        }
        response.setResponses(responses, responses.size());
        response.setResponseName(this.getCommandName());
        setResponseObject(response);
    }
}
