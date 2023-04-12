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
package org.apache.cloudstack.api.command.user.loadbalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "assignToLoadBalancerRule",
            description = "Assigns virtual machine or a list of virtual machines to a load balancer rule.",
            responseObject = SuccessResponse.class,
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class AssignToLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AssignToLoadBalancerRuleCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               required = true,
               description = "the ID of the load balancer rule")
    private Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "the list of IDs of the virtual machine that are being assigned to the load balancer rule(i.e. virtualMachineIds=1,2,3)")
    private List<Long> virtualMachineIds;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID_IP,
            type = CommandType.MAP,
            description = "VM ID and IP map, vmidipmap[0].vmid=1 vmidipmap[0].ip=10.1.1.75",
            since = "4.4")
    private Map vmIdIpMap;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getLoadBalancerId() {
        return id;
    }

    public List<Long> getVirtualMachineIds() {
        return virtualMachineIds;
    }

    public Map<Long, String> getVmIdIpMap() {
        return vmIdIpMap;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        LoadBalancer lb = _entityMgr.findById(LoadBalancer.class, getLoadBalancerId());
        if (lb == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return lb.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ASSIGN_TO_LOAD_BALANCER_RULE;
    }

    @Override
    public String getEventDescription() {
        return "applying instances for load balancer: " + getLoadBalancerId() + " (ids: " + StringUtils.join(getVirtualMachineIds(), ",") + ")";
    }


    public Map<Long, List<String>> getVmIdIpListMap() {
        Map<Long, List<String>> vmIdIpsMap = new HashMap<Long, List<String>>();
        if (vmIdIpMap != null && !vmIdIpMap.isEmpty()) {
            Collection idIpsCollection = vmIdIpMap.values();
            Iterator iter = idIpsCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> idIpsMap = (HashMap<String, String>)iter.next();
                String vmId = idIpsMap.get("vmid");
                String vmIp = idIpsMap.get("vmip");

                VirtualMachine lbvm = _entityMgr.findByUuid(VirtualMachine.class, vmId);
                if (lbvm == null) {
                    throw new InvalidParameterValueException("Unable to find virtual machine ID: " + vmId);
                }

                //check wether the given ip is valid ip or not
                if (vmIp == null || !NetUtils.isValidIp4(vmIp)) {
                    throw new InvalidParameterValueException("Invalid ip address "+ vmIp +" passed in vmidipmap for " +
                            "vmid " + vmId);
                }
                Long longVmId = lbvm.getId();

                List<String> ipsList = null;
                if (vmIdIpsMap.containsKey(longVmId)) {
                    ipsList = vmIdIpsMap.get(longVmId);
                } else {
                    ipsList = new ArrayList<String>();
                }
                ipsList.add(vmIp);
                vmIdIpsMap.put(longVmId, ipsList);

            }
        }

        return vmIdIpsMap;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Load balancer Id: " + getLoadBalancerId() + " VmIds: " + StringUtils.join(getVirtualMachineIds(), ","));

        Map<Long, List<String>> vmIdIpsMap = getVmIdIpListMap();
        boolean result = false;

        try {
            result = _lbService.assignToLoadBalancer(getLoadBalancerId(), virtualMachineIds, vmIdIpsMap, false);
        }catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign load balancer rule");
        }

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign load balancer rule");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        LoadBalancer lb = _lbService.findById(id);
        if (lb == null) {
            throw new InvalidParameterValueException("Unable to find load balancer rule: " + id);
        }
        return lb.getNetworkId();
    }
}
