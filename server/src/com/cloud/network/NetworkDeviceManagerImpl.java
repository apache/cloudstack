package com.cloud.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AddNetworkDeviceCmd;
import com.cloud.api.commands.DeleteNetworkDeviceCmd;
import com.cloud.api.commands.ListNetworkDeviceCmd;
import com.cloud.baremetal.ExternalDhcpManager;
import com.cloud.baremetal.PxeServerManager;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.baremetal.PxeServerProfile;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.HostDao;
import com.cloud.server.api.response.NetworkDeviceResponse;
import com.cloud.server.api.response.NwDeviceDhcpResponse;
import com.cloud.server.api.response.PxePingResponse;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={NetworkDeviceManager.class})
public class NetworkDeviceManagerImpl implements NetworkDeviceManager {
	public static final Logger s_logger = Logger.getLogger(NetworkDeviceManagerImpl.class);
	String _name;
	@Inject ExternalDhcpManager _dhcpMgr;
	@Inject PxeServerManager _pxeMgr;
	@Inject HostDao _hostDao;
	@Inject ExternalNetworkDeviceManager _externalNetworkDeviceMgr;
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public Host addNetworkDevice(AddNetworkDeviceCmd cmd) {
		Map paramList = cmd.getParamList();
		if (paramList == null) {
			throw new CloudRuntimeException("Parameter list is null");
		}
	
		Collection paramsCollection = paramList.values();
		HashMap params = (HashMap) (paramsCollection.toArray())[0];
		if (cmd.getType().equalsIgnoreCase(NetworkDeviceType.ExternalDhcp.getName())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
			String type = (String) params.get(ApiConstants.DHCP_SERVER_TYPE);
			String url = (String) params.get(ApiConstants.URL);
			String username = (String) params.get(ApiConstants.USERNAME);
			String password = (String) params.get(ApiConstants.PASSWORD);

			return _dhcpMgr.addDhcpServer(zoneId, podId, type, url, username, password);
		} else if (cmd.getType().equalsIgnoreCase(NetworkDeviceType.PxeServer.getName())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
			String type = (String) params.get(ApiConstants.PXE_SERVER_TYPE);
			String url = (String) params.get(ApiConstants.URL);
			String username = (String) params.get(ApiConstants.USERNAME);
			String password = (String) params.get(ApiConstants.PASSWORD);
			String pingStorageServerIp = (String) params.get(ApiConstants.PING_STORAGE_SERVER_IP);
			String pingDir = (String) params.get(ApiConstants.PING_DIR);
			String tftpDir = (String) params.get(ApiConstants.TFTP_DIR);
			String pingCifsUsername = (String) params.get(ApiConstants.PING_CIFS_USERNAME);
			String pingCifsPassword = (String) params.get(ApiConstants.PING_CIFS_PASSWORD);
			PxeServerProfile profile = new PxeServerProfile(zoneId, podId, url, username, password, type, pingStorageServerIp, pingDir, tftpDir,
					pingCifsUsername, pingCifsPassword);
			return _pxeMgr.addPxeServer(profile);
		} else if (cmd.getType().equalsIgnoreCase(NetworkDeviceType.JuniperSRXFirewall.getName())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long networkId = (params.get(ApiConstants.NETWORK_ID)==null)?Long.parseLong((String)params.get(ApiConstants.NETWORK_ID)):null;
			return _externalNetworkDeviceMgr.addExternalFirewall(zoneId, networkId, NetworkDeviceType.JuniperSRXFirewall.getName(), cmd.getParamList());
		} else if (cmd.getType().equalsIgnoreCase(NetworkDeviceType.NetscalerLoadBalancer.getName())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			return _externalNetworkDeviceMgr.addExternalLoadBalancer(zoneId, null, NetworkDeviceType.NetscalerLoadBalancer.getName(), cmd.getParamList());
		} else if (cmd.getType().equalsIgnoreCase(NetworkDeviceType.F5BigIpLoadBalancer.getName())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			return _externalNetworkDeviceMgr.addExternalLoadBalancer(zoneId, null, NetworkDeviceType.F5BigIpLoadBalancer.getName(), cmd.getParamList());
		} else {
			throw new CloudRuntimeException("Unsupported network device type:" + cmd.getType());
		}
	}

	@Override
	public NetworkDeviceResponse getApiResponse(Host device) {
		NetworkDeviceResponse response;
		HostVO host = (HostVO)device;
		_hostDao.loadDetails(host);
		if (host.getType() == Host.Type.ExternalDhcp) {
			NwDeviceDhcpResponse r = new NwDeviceDhcpResponse();
			r.setZoneId(host.getDataCenterId());
			r.setPodId(host.getPodId());
			r.setUrl(host.getPrivateIpAddress());
			r.setType(host.getDetail("type"));
			response = r;
		} else if (host.getType() == Host.Type.PxeServer) {
			String pxeType = host.getDetail("type");
			if (pxeType.equalsIgnoreCase(PxeServerType.PING.getName())) {
				PxePingResponse r = new PxePingResponse();
				r.setZoneId(host.getDataCenterId());
				r.setPodId(host.getPodId());
				r.setUrl(host.getPrivateIpAddress());
				r.setType(pxeType);
				r.setStorageServerIp(host.getDetail("storageServer"));
				r.setPingDir(host.getDetail("pingDir"));
				r.setTftpDir(host.getDetail("tftpDir"));
				response = r;
			} else {
				throw new CloudRuntimeException("Unsupported PXE server type:" + pxeType);
			}
		} else {
			throw new CloudRuntimeException("Unsupported network device type:" + host.getType());
		}
		
		response.setId(device.getId());
		return response;
	}

	private List<Host> listNetworkDevice(Long zoneId, Long podId, Host.Type type) {
		List<Host> res = new ArrayList<Host>();
		if (podId != null) {
			List<HostVO> devs = _hostDao.listBy(type, null, podId, zoneId);
			if (devs.size() == 1) {
				res.add(devs.get(0));
			} else {
				s_logger.debug("List " + type + ": " + devs.size() + " found");
			}
		} else {
			List<HostVO> devs = _hostDao.listBy(type, zoneId);
			res.addAll(devs);
		}
		
		return res;
	}
	
	@Override
	public List<Host> listNetworkDevice(ListNetworkDeviceCmd cmd) {
		Map paramList = cmd.getParamList();
		if (paramList == null) {
			throw new CloudRuntimeException("Parameter list is null");
		}
		
		List<Host> res;
		Collection paramsCollection = paramList.values();
		HashMap params = (HashMap) (paramsCollection.toArray())[0];
		if (NetworkDeviceType.ExternalDhcp.getName().equalsIgnoreCase(cmd.getType())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
			res = listNetworkDevice(zoneId, podId, Host.Type.ExternalDhcp);
		} else if (NetworkDeviceType.PxeServer.getName().equalsIgnoreCase(cmd.getType())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
			res = listNetworkDevice(zoneId, podId, Host.Type.PxeServer);
		} else if (NetworkDeviceType.F5BigIpLoadBalancer.getName().equalsIgnoreCase(cmd.getType())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long networkId = Long.parseLong((String) params.get(ApiConstants.NETWORK_ID));
			return _externalNetworkDeviceMgr.listExternalLoadBalancers(zoneId, networkId, NetworkDeviceType.F5BigIpLoadBalancer.getName());
		} else if (NetworkDeviceType.NetscalerLoadBalancer.getName().equalsIgnoreCase(cmd.getType())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long networkId = Long.parseLong((String) params.get(ApiConstants.NETWORK_ID));
			return _externalNetworkDeviceMgr.listExternalLoadBalancers(zoneId, networkId, NetworkDeviceType.NetscalerLoadBalancer.getName());			
		} else if (NetworkDeviceType.JuniperSRXFirewall.getName().equalsIgnoreCase(cmd.getType())) {
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long networkId = Long.parseLong((String) params.get(ApiConstants.NETWORK_ID));
			return _externalNetworkDeviceMgr.listExternalFirewalls(zoneId, networkId, NetworkDeviceType.JuniperSRXFirewall.getName());
		} else if (cmd.getType() == null){
			Long zoneId = Long.parseLong((String) params.get(ApiConstants.ZONE_ID));
			Long podId = Long.parseLong((String)params.get(ApiConstants.POD_ID));
			List<Host> res1 = listNetworkDevice(zoneId, podId, Host.Type.PxeServer);
			List<Host> res2 = listNetworkDevice(zoneId, podId, Host.Type.ExternalDhcp);
			List<Host> res3 = listNetworkDevice(zoneId, podId, Host.Type.ExternalLoadBalancer);
			List<Host> res4 = listNetworkDevice(zoneId, podId, Host.Type.ExternalFirewall);
			List<Host> deviceAll = new ArrayList<Host>();
			deviceAll.addAll(res1);
			deviceAll.addAll(res2);
			deviceAll.addAll(res3);
			deviceAll.addAll(res4);
			res = deviceAll;
		} else {
			throw new CloudRuntimeException("Unknown network device type:" + cmd.getType());
		}
		
		return res;
	}

	@Override
	public boolean deleteNetworkDevice(DeleteNetworkDeviceCmd cmd) {
	   HostVO device = _hostDao.findById(cmd.getId());
	   if (device.getType() == Type.ExternalLoadBalancer) {
	       return _externalNetworkDeviceMgr.deleteExternalLoadBalancer(cmd.getId());	    	   
	   } else if (device.getType() == Type.ExternalLoadBalancer) {
	       return _externalNetworkDeviceMgr.deleteExternalFirewall(cmd.getId());	    	   
	   }
	   return true;
	}
}