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
package org.apache.cloudstack.api.command.admin.systemvm;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "listSystemVms", description = "List system virtual machines.", responseObject = SystemVmResponse.class, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSystemVMsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSystemVMsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "the host ID of the system VM")
    private Long hostId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = SystemVmResponse.class, description = "the ID of the system VM")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the system VM")
    private String systemVmName;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "the Pod ID of the system VM")
    private Long podId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "the state of the system VM")
    private String state;

    @Parameter(name = ApiConstants.SYSTEM_VM_TYPE,
               type = CommandType.STRING,
               description = "the system VM type. Possible types are \"consoleproxy\" and \"secondarystoragevm\".")
    private String systemVmType;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID of the system VM")
    private Long zoneId;

    @Parameter(name = ApiConstants.STORAGE_ID,
               type = CommandType.UUID,
               entityType = StoragePoolResponse.class,
               description = "the storage ID where vm's volumes belong to",
               since = "3.0.1")
    private Long storageId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getSystemVmName() {
        return systemVmName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getStorageId() {
        return storageId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.SystemVm;
    }

    @Override
    public void execute() {
        Pair<List<? extends VirtualMachine>, Integer> systemVMs = _mgr.searchForSystemVm(this);
        ListResponse<SystemVmResponse> response = new ListResponse<SystemVmResponse>();
        List<SystemVmResponse> vmResponses = new ArrayList<SystemVmResponse>();
        for (VirtualMachine systemVM : systemVMs.first()) {
            SystemVmResponse vmResponse = _responseGenerator.createSystemVmResponse(systemVM);
            vmResponse.setObjectName("systemvm");
            vmResponses.add(vmResponse);
        }

        response.setResponses(vmResponses, systemVMs.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
