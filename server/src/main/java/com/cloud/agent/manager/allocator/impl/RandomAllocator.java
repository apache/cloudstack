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
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class RandomAllocator extends BaseAllocator {
    @Inject
    private ResourceManager _resourceMgr;

    protected List<Host> findSuitableHosts(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                         boolean considerReservedCapacity) {
        if (type == Host.Type.Storage) {
            return null;
        }

        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();
        ServiceOffering offering = vmProfile.getServiceOffering();

        String offeringHostTag = offering.getHostTag();
        VMTemplateVO template = (VMTemplateVO) vmProfile.getTemplate();
        logger.debug("Looking for hosts in zone [{}], pod [{}], cluster [{}].", dcId, podId, clusterId);

        List<? extends Host> availableHosts = retrieveHosts(type, (List<HostVO>) hosts, template, offeringHostTag, clusterId, podId, dcId);

        if (availableHosts.isEmpty()) {
            logger.info("No suitable host found for VM [{}] in zone [{}], pod [{}], cluster [{}].", vmProfile, dcId, podId, clusterId);
            return null;
        }

        return filterAvailableHosts(avoid, returnUpTo, considerReservedCapacity, availableHosts, offering);
    }

    protected List<Host> filterAvailableHosts(ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity, List<? extends Host> availableHosts, ServiceOffering offering) {
        logger.debug("Random Allocator found [{}] available hosts. They will be checked if they are in the avoid set and for CPU capability and capacity.", availableHosts::size);
        List<Host> suitableHosts = new ArrayList<>();

        Collections.shuffle(availableHosts);
        for (Host host : availableHosts) {
            if (suitableHosts.size() == returnUpTo) {
                break;
            }

            if (avoid.shouldAvoid(host)) {
                logger.debug("Host [{}] is in the avoid set, skipping it and trying other available hosts.", () -> host);
                continue;
            }

            if (!hostHasCpuCapabilityAndCapacity(considerReservedCapacity, offering, host)) {
                continue;
            }

            logger.debug("Found the suitable host [{}], adding to list.", () -> host);
            suitableHosts.add(host);
        }

        logger.debug("Random Host Allocator returning {} suitable hosts.", suitableHosts::size);
        return suitableHosts;
    }

    /**
     * @return all computing hosts, regardless of whether they support routing.
     */
    protected List<HostVO> retrieveHosts(Type type, List<HostVO> hosts, VMTemplateVO template, String offeringHostTag, Long clusterId, Long podId, long dcId) {
        List<HostVO> availableHosts;
        String templateTag = template.getTemplateTag();

        if (CollectionUtils.isNotEmpty(hosts)) {
            availableHosts = new ArrayList<>(hosts);
        } else {
            availableHosts = _resourceMgr.listAllUpAndEnabledHosts(type, clusterId, podId, dcId);
        }

        if (ObjectUtils.anyNotNull(offeringHostTag, templateTag)) {
            retainHostsMatchingServiceOfferingAndTemplateTags(availableHosts, type, clusterId, podId, dcId, offeringHostTag, templateTag);
        } else {
            List<HostVO> hostsWithNoRuleTag = hostDao.listAllHostsThatHaveNoRuleTag(type, clusterId, podId, dcId);
            logger.debug("Retaining hosts {} because they do not have rule tags.", hostsWithNoRuleTag);
            availableHosts.retainAll(hostsWithNoRuleTag);
        }

        addHostsBasedOnTagRules(offeringHostTag, availableHosts);

        return availableHosts;
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, null, returnUpTo, true);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                 boolean considerReservedCapacity) {
        if (CollectionUtils.isEmpty(hosts)) {
            logger.debug("Random Allocator found 0 hosts as given host list is empty");
            return new ArrayList<>();
        }
        return findSuitableHosts(vmProfile, plan, type, avoid, hosts, returnUpTo, considerReservedCapacity);
    }
}
