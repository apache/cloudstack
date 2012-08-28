package org.apache.cloudstack.storage.epselector;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.Inject;

public class DefaultPrimaryEndpointSelector implements
		DataStoreEndPointSelector {
	protected DataStore _ds;
	
    @Inject
    protected ResourceManager _resourceMgr;
	
    public DefaultPrimaryEndpointSelector(DataStore ds) {
    	_ds = ds;
    }
    
	public List<DataStoreEndPoint> getEndPoints() {
		List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, _ds.getCluterId(), _ds.getPodId(), _ds.getZoneId());
		List<DataStoreEndPoint> dseps = new ArrayList<DataStoreEndPoint>();
		for (HostVO host : allHosts) {
			dseps.add(new DataStoreEndPoint(host.getId(), host.getPrivateIpAddress()));
		}
		return dseps;
	}
}
