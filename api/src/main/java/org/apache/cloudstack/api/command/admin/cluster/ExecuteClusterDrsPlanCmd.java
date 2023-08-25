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

package org.apache.cloudstack.api.command.admin.cluster;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.user.Account;
import com.cloud.utils.UuidUtils;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.cluster.ClusterDrsService;
import org.apache.commons.collections.MapUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@APICommand(name = "executeClusterDrsPlan",
            description = "Execute DRS for a cluster. If there is another plan in progress for the same cluster, " +
                    "this command will fail.",
            responseObject = ClusterDrsPlanResponse.class, since = "4.19.0", requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ExecuteClusterDrsPlanCmd extends BaseAsyncCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ClusterResponse.class, required = true,
               description = "ID of cluster")
    private Long id;

    @Parameter(
            name = ApiConstants.MIGRATE_TO,
            type = CommandType.MAP,
            description = "Virtual Machine to destination host mapping. This parameter specifies the mapping between " +
                    "a vm and a host to migrate that VM. clusterid is required if this parameter is set." +
                    "Format of this parameter: migrateto[vm-index].vm=<uuid>&migrateto[vm-index].host=<uuid> " +
                    "Where, [vm-index] indicates the index to identify the vm that you want to migrate, " +
                    "vm=<uuid> indicates the UUID of the vm that you want to migrate, and " +
                    "host=<uuid> indicates the UUID of the host where you want to migrate the vm. " +
                    "Example: migrateto[0].vm=<71f43cd6-69b0-4d3b-9fbc-67f50963d60b>" +
                    "&migrateto[0].host=<a382f181-3d2b-4413-b92d-b8931befa7e1>" +
                    "&migrateto[1].vm=<88de0173-55c0-4c1c-a269-83d0279eeedf>" +
                    "&migrateto[1].host=<95d6e97c-6766-4d67-9a30-c449c15011d1>" +
                    "&migrateto[2].vm=<1b331390-59f2-4796-9993-bf11c6e76225>" +
                    "&migrateto[2].host=<41fdb564-9d3b-447d-88ed-7628f7640cbc>")
    private Map<String, String> migrateVmTo;

    @Inject
    private ClusterDrsService clusterDrsService;

    public Map<VirtualMachine, Host> getVmToHostMap() {
        Map<VirtualMachine, Host> vmToHostMap = new HashMap<>();
        if (MapUtils.isNotEmpty(migrateVmTo)) {
            Collection<?> allValues = migrateVmTo.values();
            Iterator<?> iter = allValues.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> vmToHost = (HashMap<String, String>) iter.next();

                String vmId = vmToHost.get("vm");
                String hostId = vmToHost.get("host");

                VirtualMachine vm;
                Host host;
                if (UuidUtils.isUuid(vmId)) {
                    vm = _entityMgr.findByUuid(VirtualMachine.class, vmId);
                } else {
                    vm = _entityMgr.findById(VirtualMachine.class, Long.parseLong(vmId));
                }

                if (UuidUtils.isUuid(hostId)) {
                    host = _entityMgr.findByUuid(Host.class, hostId);
                } else {
                    host = _entityMgr.findById(Host.class, Long.parseLong(hostId));
                }

                if (vm == null || host == null) {
                    throw new InvalidParameterValueException(
                            String.format("Unable to find the vm/host for vmId=%s, destHostId=%s", vmId, hostId));
                }

                vmToHostMap.put(vm, host);
            }
        }
        return vmToHostMap;
    }

    @Override
    public void execute() {
        ClusterDrsPlanResponse response = clusterDrsService.executeDrsPlan(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    public Long getId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Cluster;
    }


    @Override
    public String getEventType() {
        return EventTypes.EVENT_CLUSTER_DRS;
    }

    @Override
    public String getEventDescription() {
        return String.format("Executing DRS plan for cluster: %d", getId());
    }
}
