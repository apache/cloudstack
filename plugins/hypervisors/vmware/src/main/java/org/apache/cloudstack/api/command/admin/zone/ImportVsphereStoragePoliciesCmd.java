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

import com.cloud.dc.DataCenter;
import com.cloud.dc.VsphereStoragePolicy;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VsphereStoragePoliciesResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = ImportVsphereStoragePoliciesCmd.APINAME, description = "Import vSphere storage policies",
        responseObject = VsphereStoragePoliciesResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ImportVsphereStoragePoliciesCmd extends BaseCmd {

    public static final Logger LOGGER = Logger.getLogger(ImportVsphereStoragePoliciesCmd.class.getName());

    public static final String APINAME = "importVsphereStoragePolicies";

    @Inject
    public VmwareDatacenterService _vmwareDatacenterService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            description = "ID of the zone")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final DataCenter dataCenter = _resourceService.getZone(getZoneId());
        if (dataCenter == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find zone by ID: " + getZoneId());
        }

        List<? extends VsphereStoragePolicy> storagePolicies = _vmwareDatacenterService.importVsphereStoragePolicies(this);
        final ListResponse<VsphereStoragePoliciesResponse> responseList = new ListResponse<>();
        final List<VsphereStoragePoliciesResponse> storagePoliciesResponseList = new ArrayList<>();
        for (VsphereStoragePolicy storagePolicy : storagePolicies) {
            final VsphereStoragePoliciesResponse storagePoliciesResponse = new VsphereStoragePoliciesResponse();
            storagePoliciesResponse.setZoneId(dataCenter.getUuid());
            storagePoliciesResponse.setId(storagePolicy.getUuid());
            storagePoliciesResponse.setName(storagePolicy.getName());
            storagePoliciesResponse.setPolicyId(storagePolicy.getPolicyId());
            storagePoliciesResponse.setDescription(storagePolicy.getDescription());
            storagePoliciesResponse.setObjectName("StoragePolicy");

            storagePoliciesResponseList.add(storagePoliciesResponse);
        }
        responseList.setResponses(storagePoliciesResponseList);
        responseList.setResponseName(getCommandName());
        setResponseObject(responseList);
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

}
