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
package com.cloud.agent.manager.allocator.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodCluster;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmCharacteristics;

@Local(value=HostAllocator.class)
public class RecreateHostAllocator extends FirstFitRoutingAllocator {
    private final static Logger s_logger = Logger.getLogger(RecreateHostAllocator.class);
    
    @Inject HostPodDao _podDao;
    @Inject StoragePoolDao _poolDao;
    @Inject ClusterDao _clusterDao;
    @Inject AgentManager _agentMgr;
    @Inject VolumeDao _volsDao;
    boolean _isDirect;
    
    @Override
    public Host allocateTo(VmCharacteristics vm, ServiceOffering offering, Host.Type type, DataCenterVO dc, HostPodVO pod,
            StoragePoolVO sp, VMTemplateVO template, Set<Host> avoid) {
        Host host = super.allocateTo(vm, offering, type, dc, pod, sp, template, avoid);
        if (host != null) {
            return host;
        }
    
        s_logger.debug("First fit was unable to find a host");
        VirtualMachine.Type vmType = vm.getType();
        if (vmType == VirtualMachine.Type.User) {
            s_logger.debug("vm is not a system vm so let's just return null");
            return null;
        }
        
        List<PodCluster> pcs = _agentMgr.listByDataCenter(dc.getId());
        if (_isDirect) {
            s_logger.debug("Direct Networking mode so we can only allow the host to be allocated in the same pod due to public ip address cannot change");
            long podId = sp.getPodId();
            s_logger.debug("Pod id determined is " + podId);
            Iterator<PodCluster> it = pcs.iterator();
            while (it.hasNext()) {
                PodCluster pc = it.next();
                if (pc.getPod().getId() != podId) {
                    it.remove();
                }
            }
        }
        Set<Pair<Long, Long>> avoidPcs = new HashSet<Pair<Long, Long>>();
        for (Host h : avoid) {
            avoidPcs.add(new Pair<Long, Long>(h.getPodId(), h.getClusterId()));
        }
        
        for (Pair<Long, Long> pcId : avoidPcs) {
            s_logger.debug("Removing " + pcId + " from the list of available pods");
            pcs.remove(new PodCluster(new HostPodVO(pcId.first()), pcId.second() != null ? new ClusterVO(pcId.second()) : null));
        }
        
        for (PodCluster p : pcs) {
            List<StoragePoolVO> pools = _poolDao.listBy(dc.getId(), p.getPod().getId(), p.getCluster() == null ? null : p.getCluster().getId());
            for (StoragePoolVO pool : pools) {
                host = super.allocateTo(vm, offering, type, dc, p.getPod(), pool, template, avoid);
                if (host != null) {
                    return host;
                }
            }
        }

        s_logger.debug("Unable to find any available pods at all!");
        return null;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> dbParams = configDao.getConfiguration(params);
        
        _isDirect = Boolean.parseBoolean(dbParams.get(Config.DirectAttachUntaggedVlanEnabled.key()));
        return true;
    }
    
    protected RecreateHostAllocator() {
        super();
    }
}
