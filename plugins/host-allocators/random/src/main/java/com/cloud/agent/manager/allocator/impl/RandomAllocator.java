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
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class RandomAllocator extends AdapterBase implements HostAllocator {
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

    protected List<HostVO> listHostsByTags(Host.Type type, long dcId, Long podId, Long clusterId, String offeringHostTag, String templateTag) {
        List<HostVO> taggedHosts = new ArrayList<>();
        if (offeringHostTag != null) {
            taggedHosts.addAll(_hostDao.listByHostTag(type, clusterId, podId, dcId, offeringHostTag));
        }
        if (templateTag != null) {
            List<HostVO> templateTaggedHosts = _hostDao.listByHostTag(type, clusterId, podId, dcId, templateTag);
            if (taggedHosts.isEmpty()) {
                taggedHosts = templateTaggedHosts;
            } else {
                taggedHosts.retainAll(templateTaggedHosts);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Found %d hosts %s with type: %s, zone ID: %d, pod ID: %d, cluster ID: %s, offering host tag(s): %s, template tag: %s",
                    taggedHosts.size(),
                    (taggedHosts.isEmpty() ? "" : String.format("(%s)", StringUtils.join(taggedHosts.stream().map(HostVO::getId).toArray(), ","))),
                    type.name(), dcId, podId, clusterId, offeringHostTag, templateTag));
        }
        return taggedHosts;
    }
    private List<Host> findSuitableHosts(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type,
                                         ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                         boolean considerReservedCapacity) {
        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();
        ServiceOffering offering = vmProfile.getServiceOffering();
        List<? extends Host> hostsCopy = null;
        List<Host> suitableHosts = new ArrayList<>();

        if (type == Host.Type.Storage) {
            return suitableHosts;
        }
        String offeringHostTag = offering.getHostTag();
        VMTemplateVO template = (VMTemplateVO)vmProfile.getTemplate();
        String templateTag = template.getTemplateTag();
        String hostTag = null;
        if (ObjectUtils.anyNull(offeringHostTag, templateTag)) {
            hostTag = offeringHostTag;
            hostTag = hostTag == null ? templateTag : String.format("%s, %s", hostTag, templateTag);
            logger.debug(String.format("Looking for hosts in dc [%s], pod [%s], cluster [%s] and complying with host tag(s): [%s]", dcId, podId, clusterId, hostTag));
        } else {
            logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId);
        }
        if (hosts != null) {
            // retain all computing hosts, regardless of whether they support routing...it's random after all
            hostsCopy = new ArrayList<>(hosts);
            if (ObjectUtils.anyNotNull(offeringHostTag, templateTag)) {
                hostsCopy.retainAll(listHostsByTags(type, dcId, podId, clusterId, offeringHostTag, templateTag));
            } else {
                hostsCopy.retainAll(_hostDao.listAllHostsThatHaveNoRuleTag(type, clusterId, podId, dcId));
            }
        } else {
            // list all computing hosts, regardless of whether they support routing...it's random after all
            if (offeringHostTag != null) {
                hostsCopy = listHostsByTags(type, dcId, podId, clusterId, offeringHostTag, templateTag);
            } else {
                hostsCopy = _hostDao.listAllHostsThatHaveNoRuleTag(type, clusterId, podId, dcId);
            }
        }
        hostsCopy = ListUtils.union(hostsCopy, _hostDao.findHostsWithTagRuleThatMatchComputeOferringTags(offeringHostTag));

        if (hostsCopy.isEmpty()) {
            logger.info("No suitable host found for VM [{}] in {}.", vmProfile, hostTag);
            return null;
        }

        logger.debug("Random Allocator found {} hosts", hostsCopy.size());
        if (hostsCopy.isEmpty()) {
            return suitableHosts;
        }

        Collections.shuffle(hostsCopy);
        for (Host host : hostsCopy) {
            if (suitableHosts.size() == returnUpTo) {
                break;
            }
            if (avoid.shouldAvoid(host)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Host name: " + host.getName() + ", hostId: " + host.getId() + " is in avoid set, skipping this and trying other available hosts");
                }
                continue;
            }
            Pair<Boolean, Boolean> cpuCapabilityAndCapacity = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, considerReservedCapacity);
            if (!cpuCapabilityAndCapacity.first() || !cpuCapabilityAndCapacity.second()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not using host " + host.getId() + "; host has cpu capability? " + cpuCapabilityAndCapacity.first() + ", host has capacity?" + cpuCapabilityAndCapacity.second());
                }
                continue;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Found a suitable host, adding to list: " + host.getId());
            }
            suitableHosts.add(host);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Random Host Allocator returning " + suitableHosts.size() + " suitable hosts");
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
            if (logger.isDebugEnabled()) {
                logger.debug("Random Allocator found 0 hosts as given host list is empty");
            }
            return new ArrayList<>();
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
