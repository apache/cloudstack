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

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareService;

import javax.inject.Inject;

@APICommand(name = "listFileShares", responseObject= FileShareResponse.class, description = "Lists File Shares by.. ",
        responseView = ResponseObject.ResponseView.Restricted, entityType = FileShare.class, requestHasSensitiveInfo = false, since = "4.20.0")
public class ListFileSharesCmd extends BaseListCmd {

    @Inject
    FileShareService fileShareService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FileShareResponse.class, description = "the ID of the file share")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "the ID of the network")
    private Long networkId;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "the ID of the account")
    private Long accountId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getAccountId() {
        return accountId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public long getEntityOwnerId() {
        return 0;
    }

    @Override
    public void execute() {
        Pair<List<? extends FileShare>, Integer> fileShares = fileShareService.searchForFileShares(this);

        ListResponse<FileShareResponse> response = new ListResponse<FileShareResponse>();
        List<FileShareResponse> fileShareResponses = new ArrayList<FileShareResponse>();
        for (FileShare fileShare : fileShares.first()) {
            FileShareResponse fileShareResponse = _responseGenerator.createFileShareResponse(fileShare);
            response.setObjectName(FileShare.class.getSimpleName().toLowerCase());
            fileShareResponses.add(fileShareResponse);
        }

        response.setResponses(fileShareResponses, fileShares.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
