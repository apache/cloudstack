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
import com.cloud.vm.ConsoleProxyVO;

public class ConsoleProxyExecutorHelper {
	
	public static ConsoleProxyOperationResultObject composeResultObject(ManagementServer managementServer, ConsoleProxyVO proxy) {
		ConsoleProxyOperationResultObject result = new ConsoleProxyOperationResultObject();
		result.setId(proxy.getId());
		result.setName(proxy.getName());
		result.setZoneId(proxy.getDataCenterId());
		result.setZoneName(managementServer.findDataCenterById(proxy.getDataCenterId()).getName());
		result.setDns1(proxy.getDns1());
		result.setDns2(proxy.getDns2());
		result.setNetworkDomain(proxy.getDomain());
		result.setGateway(proxy.getGateway());
		result.setPodId(proxy.getPodId());
		result.setHostId(proxy.getHostId());
		if(proxy.getHostId() != null) 
			result.setHostName(managementServer.getHostBy(proxy.getHostId()).getName());
		
		result.setPrivateIp(proxy.getPrivateIpAddress());
		result.setPrivateMac(proxy.getPrivateMacAddress());
		result.setPrivateNetmask(proxy.getPrivateNetmask());
		result.setPublicIp(proxy.getPublicIpAddress());
		result.setPublicMac(proxy.getPublicMacAddress());
		result.setPublicNetmask(proxy.getPublicNetmask());
		result.setTemplateId(proxy.getTemplateId());
		result.setCreated(proxy.getCreated());
		result.setActionSessions(proxy.getActiveSession());
		result.setState(proxy.getState().toString());
		
		return result;
	}
}
