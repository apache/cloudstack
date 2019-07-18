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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.direct.download.DirectDownloadManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = UploadTemplateDirectDownloadCertificateCmd.APINAME,
        description = "Upload a certificate for HTTPS direct template download on KVM hosts",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = true,
        since = "4.11.0",
        authorized = {RoleType.Admin})
public class UploadTemplateDirectDownloadCertificateCmd extends BaseCmd {

    @Inject
    DirectDownloadManager directDownloadManager;

    private static final Logger LOG = Logger.getLogger(UploadTemplateDirectDownloadCertificateCmd.class);
    public static final String APINAME = "uploadTemplateDirectDownloadCertificate";

    @Parameter(name = ApiConstants.CERTIFICATE, type = BaseCmd.CommandType.STRING, required = true, length = 65535,
            description = "SSL certificate")
    private String certificate;

    @Parameter(name = ApiConstants.NAME , type = BaseCmd.CommandType.STRING, required = true,
            description = "Name for the uploaded certificate")
    private String name;

    @Parameter(name = ApiConstants.HYPERVISOR, type = BaseCmd.CommandType.STRING, required = true, description = "Hypervisor type")
    private String hypervisor;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            description = "Zone to upload certificate", required = true)
    private Long zoneId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "(optional) the host ID to revoke certificate")
    private Long hostId;

    @Override
    public void execute() {
        if (!hypervisor.equalsIgnoreCase("kvm")) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Currently supporting KVM hosts only");
        }

        try {
            LOG.debug("Uploading certificate " + name + " to agents for Direct Download");
            boolean result = directDownloadManager.uploadCertificateToHosts(certificate, name, hypervisor, zoneId, hostId);
            SuccessResponse response = new SuccessResponse(getCommandName());
            response.setSuccess(result);
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
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


