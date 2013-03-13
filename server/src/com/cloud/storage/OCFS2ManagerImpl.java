// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.storage;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareOCFS2NodesCommand;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceListener;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value ={OCFS2Manager.class})
public class OCFS2ManagerImpl extends ManagerBase implements OCFS2Manager, ResourceListener {
    private static final Logger s_logger = Logger.getLogger(OCFS2ManagerImpl.class);
    
    @Inject ClusterDetailsDao _clusterDetailsDao;
    @Inject AgentManager _agentMgr;
    @Inject HostDao _hostDao;
    @Inject ClusterDao _clusterDao;
    @Inject ResourceManager _resourceMgr;
    @Inject StoragePoolHostDao _poolHostDao;
    @Inject PrimaryDataStoreDao _poolDao;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        _resourceMgr.registerResourceEvent(ResourceListener.EVENT_DELETE_HOST_AFTER, this);
        return true;
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceEvent(this);
        return true;
    }

    private List<Ternary<Integer, String, String>> marshalNodes(List<HostVO> hosts) {
        Integer i = 0;
        List<Ternary<Integer, String, String>> lst = new ArrayList<Ternary<Integer, String, String>>();
        for (HostVO h : hosts) {
            /**
             * Don't show "node" in node name otherwise OVM's utils/config_o2cb.sh will be going crazy
             */
            String nodeName = "ovm_" + h.getPrivateIpAddress().replace(".", "_");
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
        
		String clusterName = "OvmCluster" + cluster.getId();      
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
        
        SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
        sc.addAnd(sc.getEntity().getClusterId(), Op.EQ, clusterId);
        sc.addAnd(sc.getEntity().getPodId(), Op.EQ, cluster.getPodId());
        sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, cluster.getDataCenterId());
        sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.Routing);
        List<HostVO> hosts = sc.list();
        if (hosts.isEmpty()) {
            s_logger.debug("There is no host in cluster " + clusterId + ", no need to prepare OCFS2 nodes");
            return true;
        }
        
        return prepareNodes(getClusterName(clusterId), hosts);
    }

    @Override
    public void processDiscoverEventBefore(Long dcid, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processDiscoverEventAfter(Map<? extends ServerResource, Map<String, String>> resources) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processDeleteHostEventBefore(HostVO host) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processDeletHostEventAfter(HostVO host) {
        String errMsg = String.format("Prepare OCFS2 nodes failed after delete host %1$s (zone:%2$s, pod:%3$s, cluster:%4$s", host.getId(), host.getDataCenterId(), host.getPodId(), host.getClusterId());
        
        if (host.getHypervisorType() != HypervisorType.Ovm) {
            return;
        }
        
        boolean hasOcfs2 = false;
        List<StoragePoolHostVO> poolRefs = _poolHostDao.listByHostId(host.getId());
        for (StoragePoolHostVO poolRef : poolRefs) {
            StoragePoolVO pool = _poolDao.findById(poolRef.getPoolId());
            if (pool.getPoolType() == StoragePoolType.OCFS2) {
                hasOcfs2 = true;
                break;
            }
        }

        if (hasOcfs2) {
            try {
                if (!prepareNodes(host.getClusterId())) {
                    s_logger.warn(errMsg);
                }
            } catch (Exception e) {
                s_logger.error(errMsg, e);
            }
        }
    }

    @Override
    public void processCancelMaintenaceEventBefore(Long hostId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processCancelMaintenaceEventAfter(Long hostId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processPrepareMaintenaceEventBefore(Long hostId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processPrepareMaintenaceEventAfter(Long hostId) {
        // TODO Auto-generated method stub
        
    }
}
