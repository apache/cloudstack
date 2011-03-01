/**
 * 
 */
package com.cloud.resource;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentResourceBase;
import com.cloud.agent.SimulatorManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * @author prasanna
 * 
 */
@Local(value = Discoverer.class)
public class SimulatorDiscoverer extends DiscovererBase implements Discoverer {
	private static final Logger s_logger = Logger
			.getLogger(SimulatorDiscoverer.class);
	
	@Inject
	protected HostDao _hostDao;
	@Inject
	protected VMTemplateDao _tmpltDao = null;
	@Inject
	protected VMTemplateHostDao _vmTemplateHostDao = null;
	@Inject
	protected VMTemplateZoneDao _vmTemplateZoneDao = null;
	@Inject
	protected VMTemplateDao _vmTemplateDao = null;
	@Inject
	protected ConfigurationDao _configDao = null;
	
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
			SimulatorManager simMgr = SimulatorManager.getInstance();
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
			_hostDao.disconnect(h, Event.AgentDisconnected, msId);
			associateSystemVmTemplate(h.getId(), h.getDataCenterId());
			associateTemplatesToZone(h.getId(), h.getDataCenterId());
		}
	}
	
    protected void associateSystemVmTemplate(long hostId, long dcId) {
    	VMTemplateVO tmplt = _tmpltDao.findById(TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID);
    	if (tmplt == null) {
    		throw new CloudRuntimeException("Cannot find routing template in vm_template table. Check your configuration");
    	}
    	VMTemplateHostVO tmpltHost = _vmTemplateHostDao.findByHostTemplate(hostId, TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID);
    	if (tmpltHost == null) {
    		VMTemplateHostVO vmTemplateHost = new VMTemplateHostVO(hostId, TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID, new Date(), 100, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, null, null, null, TemplateConstants.DEFAULT_SYSTEM_VM_TEMPLATE_PATH, null);
    		_vmTemplateHostDao.persist(vmTemplateHost);
    	}    	
    }
    
    private void associateTemplatesToZone(long hostId, long dcId){
    	VMTemplateZoneVO tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID);
    	if (tmpltZone == null) {
    		VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, TemplateConstants.DEFAULT_SYSTEM_VM_DB_ID, new Date());
    		_vmTemplateZoneDao.persist(vmTemplateZone);
    	}

    	List<VMTemplateVO> allTemplates = _vmTemplateDao.listAllActive();
    	for (VMTemplateVO vt: allTemplates){
    		if (vt.isCrossZones()){
    			tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, vt.getId());
    			if (tmpltZone == null) {
    				VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, vt.getId(), new Date());
    				_vmTemplateZoneDao.persist(vmTemplateZone);
    			}
    		}
    	}
    }
}
