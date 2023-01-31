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
package org.apache.cloudstack.api.command.admin.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.host.Host;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;

@APICommand(name = "findHostsForMigration", description = "Find hosts suitable for migrating a virtual machine.", responseObject = HostForMigrationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class FindHostsForMigrationCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(FindHostsForMigrationCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               required = true,
               description = "find hosts to which this VM can be migrated and flag the hosts with enough " + "CPU/RAM to host the VM")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<HostForMigrationResponse> response = null;
        Pair<List<? extends Host>, Integer> result;
        Map<Host, Boolean> hostsRequiringStorageMotion;

        Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigration =
            _mgr.listHostsForMigrationOfVM(getVirtualMachineId(), this.getStartIndex(), this.getPageSizeVal(), this.getKeyword());
        result = hostsForMigration.first();
        List<? extends Host> hostsWithCapacity = hostsForMigration.second();
        hostsRequiringStorageMotion = hostsForMigration.third();

        response = new ListResponse<HostForMigrationResponse>();
        List<HostForMigrationResponse> hostResponses = new ArrayList<HostForMigrationResponse>();
        for (Host host : result.first()) {
            HostForMigrationResponse hostResponse = _responseGenerator.createHostForMigrationResponse(host);
            Boolean suitableForMigration = false;
            if (hostsWithCapacity.contains(host)) {
                suitableForMigration = true;
            }
            hostResponse.setSuitableForMigration(suitableForMigration);

            Boolean requiresStorageMotion = hostsRequiringStorageMotion.get(host);
            if (requiresStorageMotion != null && requiresStorageMotion) {
                hostResponse.setRequiresStorageMotion(true);
            } else {
                hostResponse.setRequiresStorageMotion(false);
            }

            hostResponse.setObjectName("host");
            hostResponses.add(hostResponse);
        }

        response.setResponses(hostResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
