package com.cloud.resource;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.agent.manager.MockStorageManager;
import com.cloud.host.HostVO;
import com.cloud.storage.secondary.SecondaryStorageDiscoverer;
import com.cloud.utils.component.Inject;
@Local(value=Discoverer.class)
public class SimulatorSecondaryDiscoverer extends SecondaryStorageDiscoverer {
    @Inject
    MockStorageManager _mockStorageMgr = null;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return super.configure(name, params);
    }
    
    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
        super.postDiscovery(hosts, msId);
        for (HostVO host: hosts) {
            _mockStorageMgr.preinstallTemplates(host.getStorageUrl(), host.getDataCenterId());
        }
    }
}
