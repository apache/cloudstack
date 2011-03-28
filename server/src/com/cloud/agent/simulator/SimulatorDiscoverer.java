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
package com.cloud.agent.simulator;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentResourceBase;
import com.cloud.agent.SimulatorManager;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.utils.component.Inject;

/**
 * @author prasanna
 * 
 */
@Local(value = Discoverer.class)
public class SimulatorDiscoverer extends DiscovererBase implements Discoverer {
	private static final Logger s_logger = Logger
			.getLogger(SimulatorDiscoverer.class);
	
	@Inject protected HostDao _hostDao;
	@Inject protected VMTemplateDao _vmTemplateDao;
    @Inject protected VMTemplateHostDao _vmTemplateHostDao;
    @Inject protected VMTemplateZoneDao _vmTemplateZoneDao;
	
	/**
	 * Finds ServerResources of an in-process simulator
	 * 
	 * @see com.cloud.resource.Discoverer#find(long, java.lang.Long,
	 *      java.lang.Long, java.net.URI, java.lang.String, java.lang.String)
	 */
	@Override
	public Map<? extends ServerResource, Map<String, String>> find(long dcId,
			Long podId, Long clusterId, URI uri, String username,
			String password) throws DiscoveryException {
		Map<AgentResourceBase, Map<String, String>> resources;

		try {
			if (uri.getScheme().equals("http")) {
				if (!uri.getAuthority().contains("sim")) {
					String msg = "uri is not of simulator type so we're not taking care of the discovery for this: "
							+ uri;
					s_logger.debug(msg);
					return null;
				}
			} else {
				String msg = "uriString is not http so we're not taking care of the discovery for this: "
						+ uri;
				s_logger.debug(msg);
				return null;
			}

			String cluster = null;
			if (clusterId == null) {
				String msg = "must specify cluster Id when adding host";
				s_logger.debug(msg);
				throw new RuntimeException(msg);
			} else {
				cluster = Long.toString(clusterId);
			}

			String pod;
			if (podId == null) {
				String msg = "must specify pod Id when adding host";
				s_logger.debug(msg);
				throw new RuntimeException(msg);
			} else {
				pod = Long.toString(podId);
			}

			Map<String, String> details = new HashMap<String, String>();
			Map<String, Object> params = new HashMap<String, Object>();
			details.put("username", username);
			params.put("username", username);
			details.put("password", password);
			params.put("password", password);
			params.put("zone", Long.toString(dcId));
			params.put("pod", pod);
			params.put("cluster", cluster);

			resources = createAgentResources(params);
			return resources;
		} catch (Exception ex) {
			s_logger.error("Exception when discovering simulator hosts: "
					+ ex.getMessage());
		}
		return null;
	}

	private Map<AgentResourceBase, Map<String, String>> createAgentResources(
			Map<String, Object> params) {
		try {
			s_logger.error("Creating Resources ...");
			SimulatorManager simMgr = SimulatorManagerImpl.getInstance();
			simMgr.start();
			return simMgr.createServerResources(params);
		} catch (Exception ex) {
			s_logger.error("Caught exception at agent resource creation: "
					+ ex.getMessage());
		}
		return null;
	}

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId) {

		for (HostVO h : hosts) {
			associateTemplatesToZone(h.getId(), h.getDataCenterId());
		}
	}    

    private void associateTemplatesToZone(long hostId, long dcId){
    	VMTemplateZoneVO tmpltZone;

    	List<VMTemplateVO> allTemplates = _vmTemplateDao.listAll();
    	for (VMTemplateVO vt: allTemplates){
    		if (vt.isCrossZones()) {
    			tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, vt.getId());
    			if (tmpltZone == null) {
    				VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, vt.getId(), new Date());
    				_vmTemplateZoneDao.persist(vmTemplateZone);
    			}
    		}
    	}
    }
    
	@Override
	public HypervisorType getHypervisorType() {
		return HypervisorType.Simulator;
	}

	@Override
	public boolean matchHypervisor(String hypervisor) {
		if(hypervisor == HypervisorType.Simulator.name())
			return true;
		return false;
	}
}