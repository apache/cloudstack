/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.baremetal;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.baremetal.PreparePxeServerAnswer;
import com.cloud.agent.api.baremetal.PreparePxeServerCommand;
import com.cloud.agent.api.baremetal.prepareCreateTemplateCommand;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=PxeServerService.class)
public class BareMetalPingServiceImpl extends BareMetalPxeServiceBase implements PxeServerService {
	private static final Logger s_logger = Logger.getLogger(BareMetalPingServiceImpl.class);
	@Inject ResourceManager _resourceMgr;
	
	@Override
	public Host addPxeServer(PxeServerProfile profile) {
		Long zoneId = profile.getZoneId();
		Long podId = profile.getPodId();

		DataCenterVO zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
		} 
		
		List<HostVO> pxeServers = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.PxeServer, null, podId, zoneId);
		if (pxeServers.size() != 0) {
			InvalidParameterValueException ex = new InvalidParameterValueException("Already had a PXE server in Pod with specified podId and zone with specified zoneId");
			ex.addProxyObject("pod", podId, "podId");
			ex.addProxyObject(zone, zoneId, "zoneId");
		}
		
		
		String ipAddress = profile.getUrl();
		String username = profile.getUsername();
		String password = profile.getPassword();
		
		ServerResource resource = null;
		Map params = new HashMap<String, String>();
		params.put("type", PxeServerType.PING.getName());
		params.put("zone", Long.toString(zoneId));
		params.put("pod", podId.toString());
		params.put("ip", ipAddress);
		params.put("username", username);
		params.put("password", password);
		if (profile.getType().equalsIgnoreCase(PxeServerType.PING.getName())) {
			String storageServerIp = profile.getPingStorageServerIp();
			if (storageServerIp == null) {
				throw new InvalidParameterValueException("No IP for storage server specified");
			}
			String pingDir = profile.getPingDir();
			if (pingDir == null) {
				throw new InvalidParameterValueException("No direcotry for storage server specified");
			}
			String tftpDir = profile.getTftpDir();
			if (tftpDir == null) {
				throw new InvalidParameterValueException("No TFTP directory specified");
			}
			String cifsUsername = profile.getPingCifsUserName();
			if (cifsUsername == null || cifsUsername.equalsIgnoreCase("")) {
				cifsUsername = "xxx";
			}
			String cifsPassword = profile.getPingCifspassword();
			if (cifsPassword == null || cifsPassword.equalsIgnoreCase("")) {
				cifsPassword = "xxx";
			}
			String guid = getPxeServerGuid(Long.toString(zoneId)  + "-" + Long.toString(podId), PxeServerType.PING.getName(), ipAddress);
			
			params.put("storageServer", storageServerIp);
			params.put("pingDir", pingDir);
			params.put("tftpDir", tftpDir);
			params.put("cifsUserName", cifsUsername);
			params.put("cifsPassword", cifsPassword);
			params.put("guid", guid);
			
			resource = new PingPxeServerResource();
			try {
				resource.configure("PING PXE resource", params);
			} catch (Exception e) {
				s_logger.debug(e);
				throw new CloudRuntimeException(e.getMessage());
			}
			
		} else {
			throw new CloudRuntimeException("Unsupport PXE server type:" + profile.getType());
		}
		
		Host pxeServer = _resourceMgr.addHost(zoneId, resource, Host.Type.PxeServer, params);
		if (pxeServer == null) {
			throw new CloudRuntimeException("Cannot add PXE server as a host");
		}
		
		return pxeServer;
	}
	
	
	@Override
	public boolean prepare(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId) {
		List<NicProfile> nics = profile.getNics();
	    if (nics.size() == 0) {
	    	throw new CloudRuntimeException("Cannot do PXE start without nic");
	    }
	    
		NicProfile pxeNic = nics.get(0);
	    String mac = pxeNic.getMacAddress();
	    String ip = pxeNic.getIp4Address();
	    String gateway = pxeNic.getGateway();
	    String mask = pxeNic.getNetmask();
	    String dns = pxeNic.getDns1();
	    if (dns == null) {
	    	dns = pxeNic.getDns2();
	    }

		try {
			String tpl = profile.getTemplate().getUrl();
			assert tpl != null : "How can a null template get here!!!";
			PreparePxeServerCommand cmd = new PreparePxeServerCommand(ip, mac, mask, gateway, dns, tpl,
					profile.getVirtualMachine().getInstanceName(), dest.getHost().getName());
			PreparePxeServerAnswer ans = (PreparePxeServerAnswer) _agentMgr.send(pxeServerId, cmd);
			return ans.getResult();
		} catch (Exception e) {
			s_logger.warn("Cannot prepare PXE server", e);
			return false;
		}
	}


    @Override
    public boolean prepareCreateTemplate(Long pxeServerId, UserVm vm, String templateUrl) {        
        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        if (nics.size() != 1) {
            throw new CloudRuntimeException("Wrong nic number " + nics.size() + " of vm " + vm.getId());
        }
        
        /* use last host id when VM stopped */
        Long hostId = (vm.getHostId() == null ? vm.getLastHostId() : vm.getHostId());
        HostVO host = _hostDao.findById(hostId);
        DataCenterVO dc = _dcDao.findById(host.getDataCenterId());
        NicVO nic = nics.get(0);
        String mask = nic.getNetmask();
        String mac = nic.getMacAddress();
        String ip = nic.getIp4Address();
        String gateway = nic.getGateway();
        String dns = dc.getDns1();
        if (dns == null) {
            dns = dc.getDns2();
        }
        
        try {
            prepareCreateTemplateCommand cmd = new prepareCreateTemplateCommand(ip, mac, mask, gateway, dns, templateUrl);
            Answer ans = _agentMgr.send(pxeServerId, cmd);
            return ans.getResult();
        } catch (Exception e) {
            s_logger.debug("Prepare for creating baremetal template failed", e);
            return false;
        }
    }
}
