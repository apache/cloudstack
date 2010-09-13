/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="removeFromLoadBalancer", manager=Manager.NetworkManager)
public class RemoveFromLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveFromLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "removefromloadbalancerruleresponse";
 
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
    private Long virtualMachineId;

    @Parameter(name="virtualmachineids", type=CommandType.LIST, collectionType=CommandType.LONG)
    private List<Long> virtualMachineIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public List<Long> getVirtualMachineIds() {
        return virtualMachineIds;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        Long loadBalancerId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        Long instanceId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
//        String instanceIds = (String)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_IDS.getName());
//
//        if ((instanceId == null) && (instanceIds == null)) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No virtual machine id specified.");
//        }
//
//        List<Long> instanceIdList = new ArrayList<Long>();
//        if (instanceIds != null) {
//            StringTokenizer st = new StringTokenizer(instanceIds, ",");
//            while (st.hasMoreTokens()) {
//                String token = st.nextToken();
//                try {
//                    Long nextInstanceId = Long.parseLong(token);
//                    instanceIdList.add(nextInstanceId);
//                } catch (NumberFormatException nfe) {
//                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "The virtual machine id " + token + " is not a valid parameter.");
//                }
//            }
//        } else {
//            instanceIdList.add(instanceId);
//        }
//
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        LoadBalancerVO loadBalancer = getManagementServer().findLoadBalancerById(loadBalancerId.longValue());
//
//        if (loadBalancer == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find load balancer rule with id " + loadBalancerId);
//        } else if (account != null) {
//            if (!isAdmin(account.getType()) && (loadBalancer.getAccountId() != account.getId().longValue())) {
//                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() +
//                        " (id:" + loadBalancer.getId() + ")");
//            } else if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
//                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid load balancer rule id (" + loadBalancer.getId() + ") given, unable to remove virtual machine instances.");
//            }
//        }
//
//        long jobId = getManagementServer().removeFromLoadBalancerAsync(userId.longValue(), loadBalancerId.longValue(), instanceIdList);
//        if(jobId == 0) {
//        	s_logger.warn("Unable to schedule async-job for RemoveFromLoadBalancerRule comamnd");
//        } else {
//	        if(s_logger.isDebugEnabled())
//	        	s_logger.debug("RemoveFromLoadBalancerRule command has been accepted, job id: " + jobId);
//        }
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
//        return returnValues;
//    }

	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
}
