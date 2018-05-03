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
package org.apache.cloudstack.framework.backuprecovery.api;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.framework.backuprecovery.BackupRecoveryManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderResponse;
import org.apache.cloudstack.framework.backuprecovery.impl.BackupRecoveryProviderVO;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;

@APICommand(name = AddBackupRecoveryProviderCmd.APINAME,
        description = "Adds a Backup and Recovery provider on a zone",
        responseObject = BackupRecoveryProviderResponse.class, since = "4.12.0",
        authorized = {RoleType.Admin})
public class AddBackupRecoveryProviderCmd extends BaseCmd {

    public static final String APINAME = "addBackupRecoveryProvider";

    private static final Logger s_logger = Logger.getLogger(AddBackupRecoveryProviderCmd.class);

    @Inject
    BackupRecoveryManager backupRecoveryManager;

    @Parameter(name=ApiConstants.ZONE_ID, type=BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            required=true, description="the ID of the zone you wish to register the Backup and Recovery provider to.")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the Backup and Recovery provider")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, length = 2048, description = "the URL of the Backup and Recovery provider portal")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "the username for the Backup and Recovery provider")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "the password for the Backup and Recovery provider")
    private String password;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, required = true, description = "the Backup and Recovery provider type")
    private String provider;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            BackupRecoveryProviderVO providerVO = backupRecoveryManager.addBackupRecoveryProvider(this);
            if (providerVO != null) {
                BackupRecoveryProviderResponse response = backupRecoveryManager.createBackupRecoveryProviderResponse(providerVO);
                response.setObjectName("backuprecoveryprovider");
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add a Backup and Recovery Provider");
            }
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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

    public Long getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getProvider() {
        return provider;
    }
}
