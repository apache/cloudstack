package com.cloud.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareOCFS2NodesCommand;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value ={OCFS2Manager.class})
public class OCFS2ManagerImpl implements OCFS2Manager {
    String _name;
    private static final Logger s_logger = Logger.getLogger(OCFS2ManagerImpl.class);
    
    @Inject ClusterDetailsDao _clusterDetailsDao;
    @Inject AgentManager _agentMgr;
    @Inject HostDao _hostDao;
    @Inject ClusterDao _clusterDao;
    
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

    private List<Ternary<Integer, String, String>> marshalNodes(List<HostVO> hosts) {
        Integer i = 0;
        List<Ternary<Integer, String, String>> lst = new ArrayList<Ternary<Integer, String, String>>();
        for (HostVO h : hosts) {
            String nodeName = "node_" + h.getPrivateIpAddress().replace(".", "_");
            Ternary<Integer, String, String> node = new Ternary<Integer, String, String>(i, h.getPrivateIpAddress(), nodeName);
            lst.add(node);
            i ++;
        }
        return lst;
    }
    
    
    private boolean prepareNodes(String clusterName, List<HostVO> hosts) {
        PrepareOCFS2NodesCommand cmd = new PrepareOCFS2NodesCommand(clusterName, marshalNodes(hosts));
        for (HostVO h : hosts) {
            Answer ans = _agentMgr.easySend(h.getId(), cmd);
            if (ans == null) {
                s_logger.debug("Host " + h.getId() + " is not in UP state, skip preparing OCFS2 node on it");
                continue;
            }
            if (!ans.getResult()) {
                s_logger.warn("PrepareOCFS2NodesCommand failed on host " + h.getId() + " " + ans.getDetails());
                return false;
            }
        }
        
        return true;
    }
    
    private String getClusterName(Long clusterId) {
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Cannot get cluster for id " + clusterId);
        }
        
        String clusterName = cluster.getName(); 
        if (clusterName == null) {
            clusterName = "cluster" + cluster.getId();
        }
        
        return clusterName;
    }
    
    @Override
    public boolean prepareNodes(List<HostVO> hosts, StoragePool pool) {
        if (pool.getPoolType() != StoragePoolType.OCFS2) {
            throw new CloudRuntimeException("None OCFS2 storage pool is getting into OCFS2 manager!");
        }
        
        return prepareNodes(getClusterName(pool.getClusterId()), hosts);
    }

    @Override
    public boolean prepareNodes(Long clusterId) {    
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Cannot find cluster for ID " + clusterId);
        }
        
        List<HostVO> hosts = _hostDao.listByInAllStatus(Host.Type.Routing, clusterId, cluster.getPodId(), cluster.getDataCenterId());
        if (hosts.isEmpty()) {
            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster " + clusterId);
        }
        
        return prepareNodes(getClusterName(clusterId), hosts);
    }
}
