package com.cloud.hypervisor.vmware.discoverer;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;

@Local(value=Discoverer.class)
public class VmwareServerDiscoverer extends DiscovererBase implements Discoverer {
	
    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI url, 
    	String username, String password) throws DiscoveryException {

    	// ???
    	return null;
    }
    
    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
        //do nothing
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
    
        // TODO
        return true;
    }
}

