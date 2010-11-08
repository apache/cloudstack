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

package com.cloud.consoleproxy;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.utils.component.Inject;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value={ConsoleProxyManager.class})
public class AgentBasedStandaloneConsoleProxyManager extends
AgentBasedConsoleProxyManager {
	@Inject private VMInstanceDao _instanceDao;
	private static final Logger s_logger = Logger.getLogger(AgentBasedStandaloneConsoleProxyManager.class);

	@Override
	public ConsoleProxyVO assignProxy(long dataCenterId, long vmId) {
		VMInstanceVO vm = _instanceDao.findById(vmId);
		if (vm == null) {
			s_logger.warn("VM " + vmId
					+ " no longer exists, return a null proxy for vm:"
					+ vmId);
			return null;
		}

		HostVO host = findHost(vm);
		if(host != null) {
			HostVO allocatedHost = null;
			/*Is there a consoleproxy agent running on the same machine?*/
			List<HostVO> hosts = _hostDao.listAll();
			for (HostVO hv : hosts) {
				if ((hv.getType() == Host.Type.ConsoleProxy) && (hv.getPublicIpAddress() != null ) && (hv.getPublicIpAddress().equalsIgnoreCase(host.getPublicIpAddress()))) {
					allocatedHost = hv;
					break;
				}
			}
			if (allocatedHost == null) {
				/*Is there a consoleproxy agent running in the same pod?*/
				for (HostVO hv : hosts) {
					if (hv.getType() == Host.Type.ConsoleProxy && hv.getPodId() == host.getPodId()) {
						allocatedHost = hv;
						break;
					}
				}
			}
			if (allocatedHost == null) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Failed to find a console proxy at host: " + host.getName() + " and in the pod: " + host.getPodId() + " to vm " + vmId);
				return null;
			}
			if(s_logger.isDebugEnabled())
				s_logger.debug("Assign standalone console proxy running at " + allocatedHost.getName() + " to user vm " + vmId + " with public IP " + allocatedHost.getPublicIpAddress());

			// only private IP, public IP, host id have meaningful values, rest of all are place-holder values
			String publicIp = allocatedHost.getPublicIpAddress();
			if(publicIp == null) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Host " + allocatedHost.getName() + "/" + allocatedHost.getPrivateIpAddress() +
					" does not have public interface, we will return its private IP for cosole proxy.");
				publicIp = allocatedHost.getPrivateIpAddress();
			}

			ConsoleProxyVO proxy = allocateProxy(allocatedHost, dataCenterId);

			if(allocatedHost.getProxyPort() != null && allocatedHost.getProxyPort().intValue() > 0)
				proxy.setPort(allocatedHost.getProxyPort().intValue());
			else
				proxy.setPort(_consoleProxyUrlPort);

			proxy.setSslEnabled(_sslEnabled);
			return proxy;
		} else {
			s_logger.warn("Host that VM is running is no longer available, console access to VM " + vmId + " will be temporarily unavailable.");
		}
		return null;
	}
}
