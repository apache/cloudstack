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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;

@APICommand(name = "listHosts", description = "Lists hosts.", responseObject = HostResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListHostsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListHostsCmd.class.getName());

    private static final String s_name = "listhostsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "lists hosts existing in particular cluster")
    private Long clusterId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostResponse.class, description = "the id of the host")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the host")
    private String hostName;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "the Pod ID for the host")
    private Long podId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "the state of the host")
    private String state;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "the host type")
    private String type;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID for the host")
    private Long zoneId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               required = false,
               description = "lists hosts in the same cluster as this VM and flag hosts with enough CPU/RAm to host this VM")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.OUTOFBANDMANAGEMENT_ENABLED,
            type = CommandType.BOOLEAN,
            description = "list hosts for which out-of-band management is enabled")
    private Boolean outOfBandManagementEnabled;

    @Parameter(name = ApiConstants.OUTOFBANDMANAGEMENT_POWERSTATE,
            type = CommandType.STRING,
            description = "list hosts by its out-of-band management interface's power state. Its value can be one of [On, Off, Unknown]")
    private String outOfBandManagementPowerState;

    @Parameter(name = ApiConstants.RESOURCE_STATE,
               type = CommandType.STRING,
               description = "list hosts by resource state. Resource state represents current state determined by admin of host, value can be one of [Enabled, Disabled, Unmanaged, PrepareForMaintenance, ErrorInMaintenance, Maintenance, Error]")
    private String resourceState;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "comma separated list of host details requested, value can be a list of [ min, all, capacity, events, stats]")
    private List<String> viewDetails;

    @Parameter(name = ApiConstants.HA_HOST, type = CommandType.BOOLEAN, description = "if true, list only hosts dedicated to HA")
    private Boolean haHost;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "hypervisor type of host: XenServer,KVM,VMware,Hyperv,BareMetal,Simulator")
    private String hypervisor;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public Long getId() {
        return id;
    }

    public String getHostName() {
        return hostName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Boolean getHaHost() {
        return haHost;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public HypervisorType getHypervisor() {
        return HypervisorType.getType(hypervisor);
    }

    public EnumSet<HostDetails> getDetails() throws InvalidParameterValueException {
        EnumSet<HostDetails> dv;
        if (viewDetails == null || viewDetails.size() <= 0) {
            dv = EnumSet.of(HostDetails.all);
        } else {
            try {
                ArrayList<HostDetails> dc = new ArrayList<HostDetails>();
                for (String detail : viewDetails) {
                    dc.add(HostDetails.valueOf(detail));
                }
                dv = EnumSet.copyOf(dc);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("The details parameter contains a non permitted value. The allowed values are " +
                    EnumSet.allOf(HostDetails.class));
            }
        }
        return dv;
    }

    public String getResourceState() {
        return resourceState;
    }


    public Boolean isOutOfBandManagementEnabled() {
        return outOfBandManagementEnabled;
    }

    public String getHostOutOfBandManagementPowerState() {
        return outOfBandManagementPowerState;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }

    protected ListResponse<HostResponse> getHostResponses() {
        ListResponse<HostResponse> response = new ListResponse<>();
        if (getVirtualMachineId() == null) {
            response = _queryService.searchForServers(this);
        } else {
            Pair<List<? extends Host>, Integer> result;
            Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigration =
                _mgr.listHostsForMigrationOfVM(getVirtualMachineId(), this.getStartIndex(), this.getPageSizeVal(), null);
            result = hostsForMigration.first();
            List<? extends Host> hostsWithCapacity = hostsForMigration.second();
            List<HostResponse> hostResponses = new ArrayList<HostResponse>();
            for (Host host : result.first()) {
                HostResponse hostResponse = _responseGenerator.createHostResponse(host, getDetails());
                Boolean suitableForMigration = false;
                if (hostsWithCapacity.contains(host)) {
                    suitableForMigration = true;
                }
                hostResponse.setSuitableForMigration(suitableForMigration);
                hostResponse.setObjectName("host");
                hostResponses.add(hostResponse);
            }
            response.setResponses(hostResponses, result.second());
        }
        return response;
    }

    @Override
    public void execute() {
        ListResponse<HostResponse> response = getHostResponses();
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
