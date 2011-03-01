package com.cloud.baremetal;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerAnswer;
import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerCommand;
import com.cloud.api.AddPxeServerCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = {LinMinPxeServerManager.class})
public class LinMinPxeServerManagerImpl extends PxeServerManagerImpl implements LinMinPxeServerManager {
	private static final org.apache.log4j.Logger s_logger = Logger.getLogger(LinMinPxeServerManagerImpl.class);
	@Inject DataCenterDao _dcDao;
	@Inject HostDao _hostDao;
	@Inject AgentManager _agentMgr;
	
	@Override
	public Host addPxeServer(AddPxeServerCmd cmd) throws InvalidParameterValueException, CloudRuntimeException {
		long zoneId = cmd.getZoneId();
		Long podId = cmd.getPod();
		String apiUsername;
		String apiPassword;
		String apid;

		DataCenterVO zone = _dcDao.findById(zoneId);
		if (zone == null) {
			throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
		} 
		
		List<HostVO> pxeServers = _hostDao.listBy(Host.Type.PxeServer, null, podId, zoneId);
		if (pxeServers.size() != 0) {
			throw new InvalidParameterValueException("Already had a PXE server in Pod: " + podId + " zone: " + zoneId);
		}
		
		URI uri;
		try {
			uri = new URI(cmd.getUrl());
		} catch (Exception e) {
			s_logger.debug(e);
			throw new InvalidParameterValueException(e.getMessage());
		}
		
		apiUsername = cmd.getLinMinUsername();
		apiPassword = cmd.getLinMinPassword();
		apid = cmd.getLinMinApid();
		if (apiUsername == null) {
			throw new InvalidParameterValueException("No LinMin username specified, without it I can user LinMin API");
		}
		
		if (apiPassword == null) {
			throw new InvalidParameterValueException("No LinMin password specified, without it I can user LinMin API");
		}
		
		if (apid == null) {
			throw new InvalidParameterValueException("No LinMin apid specified, without it I can user LinMin API");
		}
		
		String ipAddress = uri.getHost();
		String username = cmd.getUsername();
		String password = cmd.getPassword();
		String guid = getPxeServerGuid(Long.toString(zoneId), PxeServerType.LinMin.getName(), ipAddress);
		Map params = new HashMap<String, String>();
		params.put("zone", Long.toString(zoneId));
		params.put("pod", podId.toString());
		params.put("ip", ipAddress);
		params.put("username", username);
		params.put("password", password);
		params.put("guid", guid);
		params.put("pod", Long.toString(cmd.getPod()));
		params.put("apiUsername", apiUsername);
		params.put("apiPassword", apiPassword);
		params.put("apid", apid);
		
		ServerResource resource = null;
		try {
			if (cmd.getType().equalsIgnoreCase(PxeServerType.LinMin.getName())) {
				resource = new LinMinPxeServerResource();
				resource.configure("LinMin PXE resource", params);
			}
		} catch (Exception e) {
			s_logger.debug(e);
			throw new CloudRuntimeException(e.getMessage());
		}
		
		Host pxeServer = _agentMgr.addHost(zoneId, resource, Host.Type.PxeServer, params);
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
			String linMinTpl = profile.getTemplate().getUrl();
			assert linMinTpl != null : "How can a null template get here!!!";
			PrepareLinMinPxeServerCommand cmd = new PrepareLinMinPxeServerCommand(ip, mac, mask, gateway, dns, linMinTpl,
					profile.getVirtualMachine().getName(), dest.getHost().getName());
			PrepareLinMinPxeServerAnswer ans = (PrepareLinMinPxeServerAnswer) _agentMgr.send(pxeServerId, cmd);
			return ans.getResult();
		} catch (Exception e) {
			s_logger.warn("Cannot prepare PXE server", e);
			return false;
		}
	}
}
