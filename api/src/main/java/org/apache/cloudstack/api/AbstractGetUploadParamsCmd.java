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
package org.apache.cloudstack.api;

import java.net.URL;
import java.util.UUID;

import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

public abstract class AbstractGetUploadParamsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(AbstractGetUploadParamsCmd.class.getName());

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the volume/template/iso")
    private String name;

    @Parameter(name = ApiConstants.FORMAT, type = CommandType.STRING, required = true, description = "the format for the volume/template/iso. Possible values include QCOW2, OVA, "
            + "and VHD.")
    private String format;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the zone the volume/template/iso is "
            + "to be hosted on")
    private Long zoneId;

    @Parameter(name = ApiConstants.CHECKSUM, type = CommandType.STRING, description = "the checksum value of this volume/template/iso " + ApiConstants.CHECKSUM_PARAMETER_PREFIX_DESCRIPTION)
    private String checksum;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional accountName. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "an optional domainId. If the account parameter is used, "
            + "domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Upload volume/template/iso for the project")
    private Long projectId;

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public GetUploadParamsResponse createGetUploadParamsResponse(UUID id, URL postURL, String metadata, String timeout, String signature) {
        return new GetUploadParamsResponse(id, postURL, metadata, timeout, signature);
    }
}
