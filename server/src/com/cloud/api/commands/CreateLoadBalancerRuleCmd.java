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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

public class CreateLoadBalancerRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "createloadbalancerruleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    
    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DESCRIPTION, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_IP, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PRIVATE_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ALGORITHM, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        String description = (String)params.get(BaseCmd.Properties.DESCRIPTION.getName());
        String publicIP = (String)params.get(BaseCmd.Properties.PUBLIC_IP.getName());
        String publicPort = (String)params.get(BaseCmd.Properties.PUBLIC_PORT.getName());
        String privatePort = (String)params.get(BaseCmd.Properties.PRIVATE_PORT.getName());
        String algorithm = (String)params.get(BaseCmd.Properties.ALGORITHM.getName());

        UserVmDao _userVmDao;
        ComponentLocator locator = ComponentLocator.getLocator("management-server");
        _userVmDao = locator.getDao(UserVmDao.class);
        
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        IPAddressVO ipAddr = getManagementServer().findIPAddressById(publicIP);
        if (ipAddr == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create load balancer rule, invalid IP address " + publicIP);
        }

        VlanVO vlan = getManagementServer().findVlanById(ipAddr.getVlanDbId());
        if (vlan != null) {
            if (!VlanType.VirtualNetwork.equals(vlan.getVlanType())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create load balancer rule for IP address " + publicIP + ", only VirtualNetwork type IP addresses can be used for load balancers.");
            }
        } // else ERROR?

        // Verify input parameters
        Account accountByIp = getManagementServer().findAccountByIpAddress(publicIP);
        if(accountByIp == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create load balancer rule, cannot find account owner for ip " + publicIP);
        }

        Long accountId = accountByIp.getId();
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId().longValue() != accountId.longValue()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create load balancer rule, account " + account.getAccountName() + " doesn't own ip address " + publicIP);
                }
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), accountByIp.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create load balancer rule on IP address " + publicIP + ", permission denied.");
            }
        }

        List<UserVmVO> userVmVO = _userVmDao.listByAccountId(accountId);
        
        if(userVmVO.size()==0)
        {
        	//this means there are no associated vm's to the user account, and hence, the load balancer cannot be created
        	throw new ServerApiException(BaseCmd.UNSUPPORTED_ACTION_ERROR, "Unable to create load balancer rule, no vm for the user exists.");
        }
        	
        LoadBalancerVO existingLB = getManagementServer().findLoadBalancer(accountId, name);

        if (existingLB != null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create load balancer rule, an existing load balancer rule with name " + name + " already exisits.");
        }

        try {
            LoadBalancerVO loadBalancer = getManagementServer().createLoadBalancer(userId, accountId, name, description, publicIP, publicPort, privatePort, algorithm);
            List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), loadBalancer.getId().toString()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), loadBalancer.getName()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), loadBalancer.getDescription()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_IP.getName(), loadBalancer.getIpAddress()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_PORT.getName(), loadBalancer.getPublicPort()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_PORT.getName(), loadBalancer.getPrivatePort()));
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ALGORITHM.getName(), loadBalancer.getAlgorithm()));
            
            Account accountTemp = getManagementServer().findAccountById(loadBalancer.getAccountId());
            if (accountTemp != null) {
            	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
            	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
            	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
            }
            embeddedObject.add(new Pair<String, Object>("loadbalancerrule", new Object[] { returnValues } ));
            return embeddedObject;
        } catch (InvalidParameterValueException paramError) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, paramError.getMessage());
        } catch (PermissionDeniedException permissionError) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, permissionError.getMessage());
        } catch (Exception ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
