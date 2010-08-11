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

package com.cloud.async.executor;

import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouter;

public class RouterExecutorHelper {
	public static RouterOperationResultObject composeResultObject(ManagementServer managementServer, DomainRouter router) {
		RouterOperationResultObject resultObject = new RouterOperationResultObject();
		resultObject.setId(router.getId());
		resultObject.setZoneId(router.getDataCenterId());
		resultObject.setZoneName(managementServer.findDataCenterById(router.getDataCenterId()).getName());
		resultObject.setDns1(router.getDns1());
		resultObject.setDns2(router.getDns2());
		resultObject.setNetworkDomain(router.getDomain());
		resultObject.setGateway(router.getGateway());
		resultObject.setName(router.getName());
		resultObject.setPodId(router.getPodId());
		resultObject.setPrivateIp(router.getPrivateIpAddress());
		resultObject.setPrivateMacAddress(router.getPrivateMacAddress());
		resultObject.setPrivateNetMask(router.getPrivateNetmask());
		resultObject.setPublicIp(router.getPublicIpAddress());
		resultObject.setPublicMacAddress(router.getPublicMacAddress());
		resultObject.setPublicNetMask(router.getPrivateNetmask());
		resultObject.setGuestIp(router.getGuestIpAddress());
		resultObject.setGuestMacAddress(router.getGuestMacAddress());
		resultObject.setTemplateId(router.getTemplateId());
		resultObject.setCreated(router.getCreated());
		
		if (router.getHostId() != null) {
        	resultObject.setHostname(managementServer.getHostBy(router.getHostId()).getName());
        	resultObject.setHostId(router.getHostId());
        }
		
		Account acct = managementServer.findAccountById(Long.valueOf(router.getAccountId()));
        if (acct != null) {
        	resultObject.setAccount(acct.getAccountName());
        	resultObject.setDomainId(acct.getDomainId());
        	resultObject.setDomain(managementServer.findDomainIdById(acct.getDomainId()).getName());
        }

        if (router.getState() != null) 
        	resultObject.setState(router.getState().toString());
		return resultObject;
	}
}
