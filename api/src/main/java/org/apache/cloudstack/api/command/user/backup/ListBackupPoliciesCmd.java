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
package org.apache.cloudstack.api.command.user.backup;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseBackupListCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupPolicyResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupPolicy;
import org.apache.commons.lang.BooleanUtils;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = ListBackupPoliciesCmd.APINAME,
        description = "Lists backup policies",
        responseObject = BackupPolicyResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListBackupPoliciesCmd extends BaseBackupListCmd {
    public static final String APINAME = "listBackupPolicies";

    @Inject
    private BackupManager backupManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, entityType = BackupPolicyResponse.class,
            description = "The backup policy ID")
    private Long policyId;

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "The zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.EXTERNAL, type = CommandType.BOOLEAN,
            description = "True if list external backup policies (provider policies)", authorized = {RoleType.Admin})
    private Boolean external;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public boolean isExternal() {
        return BooleanUtils.isTrue(external);
    }

    public Long getPolicyId() {
        return policyId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, ServerApiException, ConcurrentOperationException {
        validateParameters();
        try {
            final List<BackupPolicy> backupPolicies = backupManager.listBackupPolicies(getZoneId(), isExternal(), getPolicyId());
            setupResponseBackupPolicyList(backupPolicies);
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private void validateParameters() {
        if (isExternal() && getZoneId() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please provide the zone id when external option is specified");
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }
}
