/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@Local(value=Discoverer.class)
public class BareMetalDiscoverer extends DiscovererBase implements Discoverer {
	private static final Logger s_logger = Logger.getLogger(BareMetalDiscoverer.class);
	@Inject ClusterDao _clusterDao;
	@Inject protected HostDao _hostDao;
	@Inject DataCenterDao _dcDao;
	
	@Override
	public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI url, String username, String password, List<String> hostTags)
			throws DiscoveryException {
		Map<BareMetalResourceBase, Map<String, String>> resources = new HashMap<BareMetalResourceBase, Map<String, String>>();
		Map<String, String> details = new HashMap<String, String>();
		        
		if (!url.getScheme().equals("http")) {
			String msg = "urlString is not http so we're not taking care of the discovery for this: " + url;
			s_logger.debug(msg);
			return null;
		}
		if (clusterId == null) {
			String msg = "must specify cluster Id when add host";
			s_logger.debug(msg);
			throw new RuntimeException(msg);
		}

		if (podId == null) {
			String msg = "must specify pod Id when add host";
			s_logger.debug(msg);
			throw new RuntimeException(msg);
		}
		
		ClusterVO cluster = _clusterDao.findById(clusterId);
		if (cluster == null || (cluster.getHypervisorType() != HypervisorType.BareMetal)) {
			if (s_logger.isInfoEnabled())
				s_logger.info("invalid cluster id or cluster is not for Bare Metal hosts");
			return null;
		}
		
		DataCenterVO zone = _dcDao.findById(dcId);
		if (zone == null) {
			throw new RuntimeException("Cannot find zone " + dcId);
		}

		try {
			String hostname = url.getHost();
			InetAddress ia = InetAddress.getByName(hostname);
			String ipmiIp = ia.getHostAddress();
			String guid = UUID.nameUUIDFromBytes(ipmiIp.getBytes()).toString();
			
			String injectScript = "scripts/util/ipmi.py";
			String scriptPath = Script.findScript("", injectScript);
			if (scriptPath == null) {
				throw new CloudRuntimeException("Unable to find key ipmi script "
						+ injectScript);
			}

			final Script command = new Script(scriptPath, s_logger);
			command.add("ping");
			command.add("hostname="+ipmiIp);
			command.add("usrname="+username);
			command.add("password="+password);
			final String result = command.execute();
			if (result != null) {
				s_logger.warn(String.format("Can not set up ipmi connection(ip=%1$s, username=%2$s, password=%3$s, args) because %4$s", ipmiIp, username, password, result));
				return null;
			}
			
			ClusterVO clu = _clusterDao.findById(clusterId);
			if (clu.getGuid() == null) {
				clu.setGuid("/root");
				_clusterDao.update(clusterId, clu);
			}
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.putAll(_params);
			params.put("zone", Long.toString(dcId));
			params.put("pod", Long.toString(podId));
			params.put("cluster",  Long.toString(clusterId));
			params.put("guid", guid); 
			params.put(ApiConstants.PRIVATE_IP, ipmiIp);
			params.put(ApiConstants.USERNAME, username);
			params.put(ApiConstants.PASSWORD, password);
			BareMetalResourceBase resource = new BareMetalResourceBase();
			resource.configure("Bare Metal Agent", params);
			
			String memCapacity = (String)params.get(ApiConstants.MEMORY);
			String cpuCapacity = (String)params.get(ApiConstants.CPU_SPEED);
			String cpuNum = (String)params.get(ApiConstants.CPU_NUMBER);
			String mac = (String)params.get(ApiConstants.HOST_MAC);
			if (hostTags != null && hostTags.size() != 0) {
			    details.put("hostTag", hostTags.get(0));
			}
			details.put(ApiConstants.MEMORY, memCapacity);
			details.put(ApiConstants.CPU_SPEED, cpuCapacity);
			details.put(ApiConstants.CPU_NUMBER, cpuNum);
			details.put(ApiConstants.HOST_MAC, mac);
			details.put(ApiConstants.USERNAME, username);
			details.put(ApiConstants.PASSWORD, password);
			details.put(ApiConstants.PRIVATE_IP, ipmiIp);

			resources.put(resource, details);
			resource.start();
			
			zone.setGatewayProvider(Network.Provider.ExternalGateWay.getName());
			zone.setDnsProvider(Network.Provider.ExternalDhcpServer.getName());
			zone.setDhcpProvider(Network.Provider.ExternalDhcpServer.getName());	
			_dcDao.update(zone.getId(), zone);
			
			s_logger.debug(String.format("Discover Bare Metal host successfully(ip=%1$s, username=%2$s, password=%3%s," +
					"cpuNum=%4$s, cpuCapacity-%5$s, memCapacity=%6$s)", ipmiIp, username, password, cpuNum, cpuCapacity, memCapacity));
			return resources;
		} catch (Exception e) {
			s_logger.warn("Can not set up bare metal agent", e);
		}

		return null;
	}

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId)
			throws DiscoveryException {
	}

	@Override
	public boolean matchHypervisor(String hypervisor) {
		return hypervisor.equalsIgnoreCase(Hypervisor.HypervisorType.BareMetal.toString());
	}

	@Override
	public HypervisorType getHypervisorType() {
		return Hypervisor.HypervisorType.BareMetal;
	}

}
