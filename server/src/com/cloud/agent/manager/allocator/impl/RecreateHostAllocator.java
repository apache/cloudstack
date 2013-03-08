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
package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodCluster;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Component
@Local(value=HostAllocator.class)
public class RecreateHostAllocator extends FirstFitRoutingAllocator {
    private final static Logger s_logger = Logger.getLogger(RecreateHostAllocator.class);
    
    @Inject HostPodDao _podDao;
    @Inject PrimaryDataStoreDao _poolDao;
    @Inject ClusterDao _clusterDao;
    @Inject VolumeDao _volsDao;
    @Inject DataCenterDao _dcDao;
    @Inject HostDao _hostDao;
    @Inject ResourceManager _resourceMgr;
    
    @Override
    public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vm,DeploymentPlan plan, Type type,
			ExcludeList avoid, int returnUpTo) {
    	
    	List<Host> hosts = super.allocateTo(vm, plan, type, avoid, returnUpTo);
        if (hosts != null && !hosts.isEmpty()) {
            return hosts;
        }
    
        s_logger.debug("First fit was unable to find a host");
        VirtualMachine.Type vmType = vm.getType();
        if (vmType == VirtualMachine.Type.User) {
            s_logger.debug("vm is not a system vm so let's just return empty list");
            return new ArrayList<Host>();
        }
        
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        List<PodCluster> pcs = _resourceMgr.listByDataCenter(dc.getId());
        //getting rid of direct.attached.untagged.vlan.enabled config param: Bug 7204
        //basic network type for zone maps to direct untagged case
        if (dc.getNetworkType().equals(NetworkType.Basic)) { 
            s_logger.debug("Direct Networking mode so we can only allow the host to be allocated in the same pod due to public ip address cannot change");
            List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
            VolumeVO vol = vols.get(0);
            long podId = vol.getPodId();
            s_logger.debug("Pod id determined from volume " + vol.getId() + " is " + podId);
            Iterator<PodCluster> it = pcs.iterator();
            while (it.hasNext()) {
                PodCluster pc = it.next();
                if (pc.getPod().getId() != podId) {
                    it.remove();
                }
            }
        }
        Set<Pair<Long, Long>> avoidPcs = new HashSet<Pair<Long, Long>>();
        Set<Long> hostIdsToAvoid = avoid.getHostsToAvoid();
        if(hostIdsToAvoid != null){
	        for (Long hostId : hostIdsToAvoid) {
	        	Host h = _hostDao.findById(hostId);
	        	if(h != null){
	        		avoidPcs.add(new Pair<Long, Long>(h.getPodId(), h.getClusterId()));
	        	}
	        }
        }
        
        for (Pair<Long, Long> pcId : avoidPcs) {
            s_logger.debug("Removing " + pcId + " from the list of available pods");
            pcs.remove(new PodCluster(new HostPodVO(pcId.first()), pcId.second() != null ? new ClusterVO(pcId.second()) : null));
        }

		for (PodCluster p : pcs) {
            if(p.getPod().getAllocationState() != Grouping.AllocationState.Enabled){
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Pod name: " + p.getPod().getName() + ", podId: "+ p.getPod().getId() +" is in " + p.getPod().getAllocationState().name() + " state, skipping this and trying other pods");
                }
                continue;
            }
			Long clusterId = p.getCluster() == null ? null : p.getCluster().getId();
            if(p.getCluster() != null && p.getCluster().getAllocationState() != Grouping.AllocationState.Enabled){
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cluster name: " + p.getCluster().getName() + ", clusterId: "+ clusterId +" is in " + p.getCluster().getAllocationState().name() + " state, skipping this and trying other pod-clusters");
                }
                continue;
            }
			DataCenterDeployment newPlan = new DataCenterDeployment(plan.getDataCenterId(), p.getPod().getId(), clusterId, null, null, null);
			hosts = super.allocateTo(vm, newPlan, type, avoid, returnUpTo);
			if (hosts != null && !hosts.isEmpty()) {
				return hosts;
			}

		}

        s_logger.debug("Unable to find any available pods at all!");
        return new ArrayList<Host>();
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        return true;
    }
    
    protected RecreateHostAllocator() {
        super();
    }
}
