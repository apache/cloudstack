//
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
//
package org.apache.cloudstack.api.command.admin.direct.download;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DirectDownloadCertificateHostStatusResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.direct.download.DirectDownloadManager;

import javax.inject.Inject;

@APICommand(name = "provisionTemplateDirectDownloadCertificate",
        description = "Provisions a host with a direct download certificate",
        responseObject = DirectDownloadCertificateHostStatusResponse.class,
        since = "4.17.0",
        authorized = {RoleType.Admin})
public class ProvisionTemplateDirectDownloadCertificateCmd extends BaseCmd {


    @Inject
    DirectDownloadManager directDownloadManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DirectDownloadCertificateResponse.class,
            description = "the id of the direct download certificate to provision", required = true)
    private Long id;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "the host to provision the certificate", required = true)
    private Long hostId;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        Pair<Boolean, String> result = directDownloadManager.provisionCertificate(id, hostId);
        DirectDownloadCertificateHostStatusResponse response = _responseGenerator.createDirectDownloadCertificateProvisionResponse(id, hostId, result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
