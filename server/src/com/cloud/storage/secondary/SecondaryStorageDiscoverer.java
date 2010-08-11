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
package com.cloud.storage.secondary;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.resource.DummySecondaryStorageResource;
import com.cloud.storage.resource.NfsSecondaryStorageResource;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NfsUtils;
import com.cloud.utils.script.Script;

/**
 * SecondaryStorageDiscoverer is used to discover secondary
 * storage servers and make sure everything it can do is
 * correct.
 */
@Local(value=Discoverer.class)
public class SecondaryStorageDiscoverer extends DiscovererBase implements Discoverer {
    private static final Logger s_logger = Logger.getLogger(SecondaryStorageDiscoverer.class);
    
    long _timeout = 2 * 60 * 1000; // 2 minutes
    String _mountParent;
    boolean _useServiceVM = false;
    Random _random = new Random(System.currentTimeMillis());
    @Inject
    protected HostDao _hostDao = null;
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
    
    protected SecondaryStorageDiscoverer() {
    }
    
    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI uri, String username, String password) {
        if (!uri.getScheme().equalsIgnoreCase("nfs") && !uri.getScheme().equalsIgnoreCase("file")
                && !uri.getScheme().equalsIgnoreCase("iso") && !uri.getScheme().equalsIgnoreCase("dummy")) {
            s_logger.debug("It's not NFS or file or ISO, so not a secondary storage server: " + uri.toString());
            return null;
        }

        if (uri.getScheme().equalsIgnoreCase("nfs") || uri.getScheme().equalsIgnoreCase("iso")) {
            return createNfsSecondaryStorageResource(dcId, podId, uri);
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            return createLocalSecondaryStorageResource(dcId, podId, uri);
        } else if (uri.getScheme().equalsIgnoreCase("dummy")) {
            return createDummySecondaryStorageResource(dcId, podId, uri);
        } else {
            return null;
        }
    }
    
    protected Map<? extends ServerResource, Map<String, String>> createNfsSecondaryStorageResource(long dcId, Long podId, URI uri) {
        
    	if (_useServiceVM) {
    	    return createDummySecondaryStorageResource(dcId, podId, uri);
    	}
        String mountStr = NfsUtils.uri2Mount(uri);
        
        Script script = new Script(true, "mount", _timeout, s_logger);
        String mntPoint = null;
        File file = null;
        do {
            mntPoint = _mountParent + File.separator + Integer.toHexString(_random.nextInt(Integer.MAX_VALUE));
            file = new File(mntPoint);
        } while (file.exists());
                
        if (!file.mkdirs()) {
            s_logger.warn("Unable to make directory: " + mntPoint);
            return null;
        }
        
        script.add(mountStr, mntPoint);
        String result = script.execute();
        if (result != null && !result.contains("already mounted")) {
            s_logger.warn("Unable to mount " + uri.toString() + " due to " + result);
            file.delete();
            return null;
        }
        
        script = new Script(true, "umount", 0, s_logger);
        script.add(mntPoint);
        script.execute();
        
        file.delete();
        
        Map<NfsSecondaryStorageResource, Map<String, String>> srs = new HashMap<NfsSecondaryStorageResource, Map<String, String>>();
        
        NfsSecondaryStorageResource storage = new NfsSecondaryStorageResource();
        Map<String, String> details = new HashMap<String, String>();
        details.put("mount.path", mountStr);
        details.put("orig.url", uri.toString());
        details.put("mount.parent", _mountParent);
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(details);
        params.put("zone", Long.toString(dcId));
        if (podId != null) {
            params.put("pod", podId.toString());
        }
        params.put("guid", uri.toString());
        params.put("secondary.storage.vm", "false");
        params.put("max.template.iso.size", _configDao.getValue("max.template.iso.size"));
        
        try {
            storage.configure("Storage", params);
        } catch (ConfigurationException e) {
            s_logger.warn("Unable to configure the storage ", e);
            return null;
        }
        srs.put(storage, details);
        
        return srs;
    }
    
    protected Map<? extends ServerResource, Map<String, String>> createLocalSecondaryStorageResource(long dcId, Long podId, URI uri) {
        Map<LocalSecondaryStorageResource, Map<String, String>> srs = new HashMap<LocalSecondaryStorageResource, Map<String, String>>();
        
        LocalSecondaryStorageResource storage = new LocalSecondaryStorageResource();
        Map<String, String> details = new HashMap<String, String>();
        
        File file = new File(uri);
        details.put("mount.path", file.getAbsolutePath());
        details.put("orig.url", uri.toString());
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(details);
        params.put("zone", Long.toString(dcId));
        if (podId != null) {
            params.put("pod", podId.toString());
        }
        params.put("guid", uri.toString());
        
        try {
            storage.configure("Storage", params);
        } catch (ConfigurationException e) {
            s_logger.warn("Unable to configure the storage ", e);
            return null;
        }
        srs.put(storage, details);
        
        return srs;
    }
    
    protected Map<ServerResource, Map<String, String>> createDummySecondaryStorageResource(long dcId, Long podId, URI uri) {
        Map<ServerResource, Map<String, String>> srs = new HashMap<ServerResource, Map<String, String>>();
        
        DummySecondaryStorageResource storage = new DummySecondaryStorageResource(_useServiceVM);
        Map<String, String> details = new HashMap<String, String>();
        
        details.put("mount.path", uri.toString());
        details.put("orig.url", uri.toString());
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(details);
        params.put("zone", Long.toString(dcId));
        if (podId != null) {
            params.put("pod", podId.toString());
        }
        params.put("guid", uri.toString());
        
        try {
            storage.configure("Storage", params);
        } catch (ConfigurationException e) {
            s_logger.warn("Unable to configure the storage ", e);
            return null;
        }
        srs.put(storage, details);
        
        return srs;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        _mountParent = _params.get("mount.parent");
        if (_mountParent == null) {
            _mountParent = "/mnt";
        }
        
        String useServiceVM = _params.get("secondary.storage.vm");
        if ("true".equalsIgnoreCase(useServiceVM)){
        	_useServiceVM = true;
        }
        return true;
    }

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId) {
		if (_useServiceVM) {
			for (HostVO h: hosts) {
				_hostDao.disconnect(h, Event.AgentDisconnected, msId);
				associateSystemVmTemplate(h.getId(), h.getDataCenterId());
			}
		}
		for (HostVO h: hosts) {
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
