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
package org.apache.cloudstack.api.command.user.vm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.IsoVmResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.vm.VirtualMachine;


@APICommand(name = "listVirtualMachines", description = "List the virtual machines owned by the account.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ListVMsCmd extends BaseListTaggedResourcesCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ListVMsCmd.class.getName());

    private static final String s_name = "listvirtualmachinesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.GROUP_ID, type = CommandType.UUID, entityType = InstanceGroupResponse.class, description = "the group ID")
    private Long groupId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "the host ID")
    private Long hostId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "the ID of the virtual machine")
    private Long id;

    @Parameter(name=ApiConstants.IDS, type=CommandType.LIST, collectionType=CommandType.UUID, entityType=UserVmResponse.class, description="the IDs of the virtual machines, mutually exclusive with id", since = "4.4")
    private List<Long> ids;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "name of the virtual machine (a substring match is made against the parameter value, data for all matching VMs will be returned)")
    private String name;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "the pod ID")
    private Long podId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "state of the virtual machine. Possible values are: Running, Stopped, Present, Destroyed, Expunged. Present is used for the state equal not destroyed.")
    private String state;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the availability zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.FOR_VIRTUAL_NETWORK,
               type = CommandType.BOOLEAN,
               description = "list by network type; true if need to list vms using Virtual Network, false otherwise")
    private Boolean forVirtualNetwork;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list by network id")
    private Long networkId;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "the target hypervisor for the template")
    private String hypervisor;

    @Parameter(name = ApiConstants.STORAGE_ID,
               type = CommandType.UUID,
               entityType = StoragePoolResponse.class,
               description = "the storage ID where vm's volumes belong to")
    private Long storageId;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "comma separated list of host details requested, "
                   + "value can be a list of [all, group, nics, stats, secgrp, tmpl, servoff, diskoff, iso, volume, min, affgrp]."
                   + " If no parameter is passed in, the details will be defaulted to all")
    private List<String> viewDetails;

    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class, description = "list vms by template")
    private Long templateId;

    @Parameter(name = ApiConstants.ISO_ID, type = CommandType.UUID, entityType = IsoVmResponse.class, description = "list vms by iso")
    private Long isoId;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "list vms by vpc")
    private Long vpcId;

    @Parameter(name = ApiConstants.AFFINITY_GROUP_ID, type = CommandType.UUID, entityType = AffinityGroupResponse.class, description = "list vms by affinity group")
    private Long affinityGroupId;

    @Parameter(name = ApiConstants.SSH_KEYPAIR, type = CommandType.STRING, description = "list vms by ssh keypair name")
    private String keypair;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, description = "list by the service offering", since = "4.4")
    private Long serviceOffId;

    @Parameter(name = ApiConstants.DISPLAY_VM, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, required = false, description = "the user ID that created the VM and is under the account that owns the VM")
    private Long userId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getGroupId() {
        return groupId;
    }

    public Long getId() {
        return id;
    }

    public List<Long> getIds() {
        return ids;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public Long getServiceOfferingId() {
        return serviceOffId;
    }

    public Long getZoneId() {
        return zoneId;
    }


    public Long getNetworkId() {
        return networkId;
    }

    public String getHypervisor() {
        return hypervisor;
    }


    public Long getTemplateId() {
        return templateId;
    }

    public Long getIsoId() {
        return isoId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getAffinityGroupId() {
        return affinityGroupId;
    }

    public String getKeyPairName() {
        return keypair;
    }

    public EnumSet<VMDetails> getDetails() throws InvalidParameterValueException {
        EnumSet<VMDetails> dv;
        if (viewDetails == null || viewDetails.size() <= 0) {
            dv = EnumSet.of(VMDetails.all);
        } else {
            try {
                ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
                for (String detail : viewDetails) {
                    dc.add(VMDetails.valueOf(detail));
                }
                dv = EnumSet.copyOf(dc);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("The details parameter contains a non permitted value. The allowed values are " + EnumSet.allOf(VMDetails.class));
            }
        }
        return dv;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    @Override
    public void execute() {
        ListResponse<UserVmResponse> response = _queryService.searchForUserVMs(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
