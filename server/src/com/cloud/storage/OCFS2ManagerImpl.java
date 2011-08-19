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
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.StoragePoolDetailsDao;
import com.cloud.utils.Pair;
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
    
    @Override
    public boolean prepareNodes(List<HostVO> hosts, StoragePool pool, Map<String, String> params) {
        if (pool.getPoolType() != StoragePoolType.OCFS2) {
            throw new CloudRuntimeException("None OCFS2 storage pool is getting into OCFS2 manager!");
        }
        
        /*
        String clusterName = params.get(OCFS2Manager.CLUSTER_NAME);
        if (clusterName == null) {
            throw new CloudRuntimeException("Cannot get OCFS2 cluster name");
        }
        */
        String clusterName = "ofcs2";
        
        Map<String, String> details = _clusterDetailsDao.findDetails(pool.getClusterId());
        String currentClusterName = details.get(OCFS2Manager.CLUSTER_NAME);
        if (currentClusterName == null) {
            details.put(OCFS2Manager.CLUSTER_NAME, clusterName);
            /* This is actual _clusterDetailsDao.update() */
            _clusterDetailsDao.persist(pool.getClusterId(), details);
        } else {
            if (!currentClusterName.equals(clusterName)) {
                throw new CloudRuntimeException("Cluster already has name " + currentClusterName + " while name you giving is " + clusterName);
            }
        }
        
        return prepareNodes(clusterName, hosts);
    }

    @Override
    public boolean prepareNodes(Long clusterId) {
        Map<String, String> details = _clusterDetailsDao.findDetails(clusterId);
        String clusterName = details.get(OCFS2Manager.CLUSTER_NAME);
        if (clusterName == null) {
            throw new CloudRuntimeException("Cannot find OCFS2 cluster name for cluster " + clusterId);
        }
        
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new CloudRuntimeException("Cannot find cluster for ID " + clusterId);
        }
        
        List<HostVO> hosts = _hostDao.listByInAllStatus(Host.Type.Routing, clusterId, cluster.getPodId(), cluster.getDataCenterId());
        if (hosts.isEmpty()) {
            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster " + clusterId);
        }
        
        return prepareNodes(clusterName, hosts);
    }
}
