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
package org.apache.cloudstack.api.command.admin.direct.download;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DirectDownloadCertificateHostStatusResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.DirectDownloadCertificateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.direct.download.DirectDownloadCertificate;
import org.apache.cloudstack.direct.download.DirectDownloadCertificateHostMap;
import org.apache.cloudstack.direct.download.DirectDownloadManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListTemplateDirectDownloadCertificatesCmd.APINAME,
        description = "List the uploaded certificates for direct download templates",
        responseObject = DirectDownloadCertificateResponse.class,
        since = "4.17.0",
        authorized = {RoleType.Admin})
public class ListTemplateDirectDownloadCertificatesCmd extends BaseListCmd {

    @Inject
    DirectDownloadManager directDownloadManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DirectDownloadCertificateResponse.class,
            description = "list direct download certificate by ID")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            description = "the zone where certificates are uploaded")
    private Long zoneId;

    @Parameter(name = ApiConstants.LIST_HOSTS, type = CommandType.BOOLEAN,
            description = "if set to true: include the hosts where the certificate is uploaded to")
    private Boolean listHosts;

    private static final Logger LOG = Logger.getLogger(ListTemplateDirectDownloadCertificatesCmd.class);
    public static final String APINAME = "listTemplateDirectDownloadCertificates";

    public boolean isListHosts() {
        return listHosts != null && listHosts;
    }

    private void createResponse(final List<DirectDownloadCertificate> certificates) {
        final ListResponse<DirectDownloadCertificateResponse> response = new ListResponse<>();
        final List<DirectDownloadCertificateResponse> responses = new ArrayList<>();
        for (final DirectDownloadCertificate certificate : certificates) {
            if (certificate == null) {
                continue;
            }
            DirectDownloadCertificateResponse certificateResponse = _responseGenerator.createDirectDownloadCertificateResponse(certificate);
            if (isListHosts()) {
                List<DirectDownloadCertificateHostMap> hostMappings = directDownloadManager.getCertificateHostsMapping(certificate.getId());
                List<DirectDownloadCertificateHostStatusResponse> hostMapResponses = _responseGenerator.createDirectDownloadCertificateHostMapResponse(hostMappings);
                certificateResponse.setHostsMap(hostMapResponses);
            }
            responses.add(certificateResponse);
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        List<DirectDownloadCertificate> certificates = directDownloadManager.listDirectDownloadCertificates(id, zoneId);
        createResponse(certificates);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
