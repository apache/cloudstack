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
package org.apache.cloudstack.api.command.admin.zone;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VsphereStoragePoliciesResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ListVsphereStoragePolicyCompatiblePoolsCmd.APINAME, description = "List storage pools compatible with a vSphere storage policy",
        responseObject = StoragePoolResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ListVsphereStoragePolicyCompatiblePoolsCmd extends BaseListCmd {
    public static final String APINAME = "listVsphereStoragePolicyCompatiblePools";

    @Inject
    public VmwareDatacenterService vmwareDatacenterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            description = "ID of the zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.POLICY_ID, type = BaseCmd.CommandType.UUID, entityType = VsphereStoragePoliciesResponse.class,
            description = "ID of the storage policy")
    private Long policyId;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        List<StoragePool> pools = vmwareDatacenterService.listVsphereStoragePolicyCompatibleStoragePools(this);
        ListResponse<StoragePoolResponse> response = new ListResponse<>();
        List<StoragePoolResponse> poolResponses = new ArrayList<>();
        for (StoragePool pool : pools) {
            StoragePoolResponse poolResponse = _responseGenerator.createStoragePoolForMigrationResponse(pool);
            poolResponse.setObjectName("storagepool");
            poolResponses.add(poolResponse);
        }
        response.setResponses(poolResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getPolicyId() {
        return policyId;
    }
}
