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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class RandomAllocator extends AdapterBase implements HostAllocator {
    private static final Logger s_logger = Logger.getLogger(RandomAllocator.class);
    @Inject
    private HostDao _hostDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private CapacityManager capacityManager;

    private List<Host> findSuitableHosts(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type,
                                         ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                         boolean considerReservedCapacity) {
        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();
        ServiceOffering offering = vmProfile.getServiceOffering();
        List<? extends Host> hostsCopy = null;
        List<Host> suitableHosts = new ArrayList<Host>();

        if (type == Host.Type.Storage) {
            return suitableHosts;
        }
        String hostTag = offering.getHostTag();
        if (hostTag != null) {
            s_logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId + " having host tag:" + hostTag);
        } else {
            s_logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId);
        }
        if (hosts != null) {
            // retain all computing hosts, regardless of whether they support routing...it's random after all
            hostsCopy = new ArrayList<Host>(hosts);
            if (hostTag != null) {
                hostsCopy.retainAll(_hostDao.listByHostTag(type, clusterId, podId, dcId, hostTag));
            } else {
                hostsCopy.retainAll(_resourceMgr.listAllUpAndEnabledHosts(type, clusterId, podId, dcId));
            }
        } else {
            // list all computing hosts, regardless of whether they support routing...it's random after all
            hostsCopy = new ArrayList<HostVO>();
            if (hostTag != null) {
                hostsCopy = _hostDao.listByHostTag(type, clusterId, podId, dcId, hostTag);
            } else {
                hostsCopy = _resourceMgr.listAllUpAndEnabledHosts(type, clusterId, podId, dcId);
            }
        }
        s_logger.debug("Random Allocator found " + hostsCopy.size() + "  hosts");
        if (hostsCopy.size() == 0) {
            return suitableHosts;
        }
        Collections.shuffle(hostsCopy);
        for (Host host : hostsCopy) {
            if (suitableHosts.size() == returnUpTo) {
                break;
            }
            if (avoid.shouldAvoid(host)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host name: " + host.getName() + ", hostId: " + host.getId() + " is in avoid set, skipping this and trying other available hosts");
                }
                continue;
            }
            Pair<Boolean, Boolean> cpuCapabilityAndCapacity = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, considerReservedCapacity);
            if (!cpuCapabilityAndCapacity.first() || !cpuCapabilityAndCapacity.second()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Not using host " + host.getId() + "; host has cpu capability? " + cpuCapabilityAndCapacity.first() + ", host has capacity?" + cpuCapabilityAndCapacity.second());
                }
                continue;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a suitable host, adding to list: " + host.getId());
            }
            suitableHosts.add(host);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Random Host Allocator returning " + suitableHosts.size() + " suitable hosts");
        }
        return suitableHosts;
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, returnUpTo, true);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type,
                                 ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                 boolean considerReservedCapacity) {
        if (CollectionUtils.isEmpty(hosts)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Random Allocator found 0 hosts as given host list is empty");
            }
            return new ArrayList<Host>();
        }
        return findSuitableHosts(vmProfile, plan, type, avoid, hosts, returnUpTo, considerReservedCapacity);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan,
                                 Type type, ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity) {
        return findSuitableHosts(vmProfile, plan, type, avoid, null, returnUpTo, considerReservedCapacity);
    }

    @Override
    public boolean isVirtualMachineUpgradable(VirtualMachine vm, ServiceOffering offering) {
        // currently we do no special checks to rule out a VM being upgradable to an offering, so
        // return true
        return true;
    }
}
