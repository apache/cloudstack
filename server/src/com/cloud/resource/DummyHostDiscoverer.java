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

package com.cloud.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;

@Local(value=Discoverer.class)
public class DummyHostDiscoverer implements Discoverer {
    private static final Logger s_logger = Logger.getLogger(DummyHostDiscoverer.class);
    
    private String _name;

    @Override
    public Map<ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI url, String username, String password, List<String> hostTags) {
        if (!url.getScheme().equals("dummy")) {
            return null;
        }
        
        Map<ServerResource, Map<String, String>> resources = new HashMap<ServerResource, Map<String, String>>();
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, String> details = new HashMap<String, String>();
        
        details.put("url", url.toString());
        details.put("username", username);
        details.put("password", password);
        
        params.put("url", url.toString());
        params.put("username", username);
        params.put("password", password);
        params.put("zone", Long.toString(dcId));
        params.put("guid", UUID.randomUUID().toString());
        params.put("pod", Long.toString(podId));

        DummyHostServerResource resource = new DummyHostServerResource();
        try {
            resource.configure("Dummy Host Server", params);
        } catch (ConfigurationException e) {
            s_logger.warn("Unable to instantiate dummy host server resource");
        }
        resource.start();
        resources.put(resource, details);
        return resources;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	return true;
    }
    
    @Override
    public String getName() {
        return _name;
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
	public boolean matchHypervisor(String hypervisor) {
    	return false;
    }
    
    @Override
	public Hypervisor.HypervisorType getHypervisorType() {
    	return Hypervisor.HypervisorType.None;
    }
    
	@Override
	public void postDiscovery(List<HostVO> hosts, long msId) {
		//do nothing
	}

	@Override
	public void putParam(Map<String, String> params) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public ServerResource reloadResource(HostVO host) {
        // TODO Auto-generated method stub
        return null;
    }
}
