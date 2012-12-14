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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Implementation;
import org.apache.cloudstack.api.Parameter;

import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.IsoVmResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.async.AsyncJob;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;


@Implementation(description="List the virtual machines owned by the account.", responseObject=UserVmResponse.class)
public class ListVMsCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListVMsCmd.class.getName());

    private static final String s_name = "listvirtualmachinesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    //@IdentityMapper(entityTableName="instance_group")
    @Parameter(name=ApiConstants.GROUP_ID, type=CommandType.LONG, description="the group ID", entityType=InstanceGroupResponse.class)
    private Long groupId;

    //@IdentityMapper(entityTableName="host")
    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, description="the host ID", entityType=HostResponse.class)
    private Long hostId;

    //@IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the virtual machine", entityType=UserVmResponse.class)
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="name of the virtual machine")
    private String instanceName;

    //@IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the pod ID", entityType=PodResponse.class)
    private Long podId;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="state of the virtual machine")
    private String state;

    //@IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the availability zone ID", entityType=ZoneResponse.class)
    private Long zoneId;

    @Parameter(name=ApiConstants.FOR_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="list by network type; true if need to list vms using Virtual Network, false otherwise")
    private Boolean forVirtualNetwork;

    //@IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="list by network id", entityType=NetworkResponse.class)
    private Long networkId;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the target hypervisor for the template")
    private String hypervisor;

    //@IdentityMapper(entityTableName="storage_pool")
    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.LONG, description="the storage ID where vm's volumes belong to", entityType=StoragePoolResponse.class)
    private Long storageId;

    @Parameter(name=ApiConstants.DETAILS, type=CommandType.LIST, collectionType=CommandType.STRING, description="comma separated list of host details requested, " +
            "value can be a list of [all, group, nics, stats, secgrp, tmpl, servoff, iso, volume, min]. If no parameter is passed in, the details will be defaulted to all" )
    private List<String> viewDetails;

    //@IdentityMapper(entityTableName="vm_template")
    @Parameter(name=ApiConstants.TEMPLATE_ID, type=CommandType.LONG, description="list vms by template", entityType=TemplateResponse.class)
    private Long templateId;

    //@IdentityMapper(entityTableName="vm_template")
    @Parameter(name=ApiConstants.ISO_ID, type=CommandType.LONG, description="list vms by iso", entityType=IsoVmResponse.class)
    private Long isoId;

    //@IdentityMapper(entityTableName="vpc")
    @Parameter(name=ApiConstants.VPC_ID, type=CommandType.LONG, description="list vms by vpc", entityType=VpcResponse.class)
    private Long vpcId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getGroupId() {
        return groupId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public Long getStorageId() {
        return storageId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getIsoId() {
        return isoId;
    }

    public Long getVpcId(){
        return vpcId;
    }

    public EnumSet<VMDetails> getDetails() throws InvalidParameterValueException {
        EnumSet<VMDetails> dv;
        if (viewDetails==null || viewDetails.size() <=0){
            dv = EnumSet.of(VMDetails.all);
        }
        else {
            try {
                ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
                for (String detail: viewDetails){
                    dc.add(VMDetails.valueOf(detail));
                }
                dv = EnumSet.copyOf(dc);
            }
            catch (IllegalArgumentException e){
                throw new InvalidParameterValueException("The details parameter contains a non permitted value. The allowed values are " + EnumSet.allOf(VMDetails.class));
            }
        }
        return dv;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.VirtualMachine;
    }

    @Override
    public void execute(){
        ListResponse<UserVmResponse> response = _queryService.searchForUserVMs(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}
