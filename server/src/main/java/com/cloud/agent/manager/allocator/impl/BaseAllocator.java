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

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.CapacityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.List;

public abstract class BaseAllocator extends AdapterBase implements HostAllocator {

    @Inject
    protected HostDao hostDao;

    @Inject
    protected CapacityManager capacityManager;

    protected void retainHostsMatchingServiceOfferingAndTemplateTags(List<HostVO> availableHosts, Host.Type type, long dcId, Long podId, Long clusterId, String offeringHostTag, String templateTag) {
        logger.debug("Hosts {} will be checked for template and host tags compatibility.", availableHosts);

        if (offeringHostTag != null) {
            logger.debug("Looking for hosts having the tag [{}] specified in the Service Offering.", offeringHostTag);
            List<HostVO> hostsWithHostTag = hostDao.listByHostTag(type, clusterId, podId, dcId, offeringHostTag);
            logger.debug("Retaining hosts {} because they match the offering host tag {}.", hostsWithHostTag, offeringHostTag);
            availableHosts.retainAll(hostsWithHostTag);
        }

        if (templateTag != null) {
            logger.debug("Looking for hosts having the tag [{}] specified in the Template.", templateTag);
            List<HostVO> hostsWithTemplateTag = hostDao.listByHostTag(type, clusterId, podId, dcId, templateTag);
            logger.debug("Retaining hosts {} because they match the template tag {}.", hostsWithTemplateTag, templateTag);
            availableHosts.retainAll(hostsWithTemplateTag);
        }

        logger.debug("Remaining hosts after template tag and host tags validations are {}.", availableHosts);
    }

    protected void addHostsBasedOnTagRules(String hostTagOnOffering, List<HostVO> clusterHosts) {
        List<HostVO> hostsWithTagRules = hostDao.findHostsWithTagRuleThatMatchComputeOfferingTags(hostTagOnOffering);

        if (CollectionUtils.isEmpty(hostsWithTagRules)) {
            logger.info("No hosts found with tag rules matching the compute offering tag [{}].", hostTagOnOffering);
            return;
        }

        logger.info("Found hosts %s with tag rules matching the compute offering tag [{}].", hostsWithTagRules, hostTagOnOffering);
        clusterHosts.addAll(hostsWithTagRules);
    }

    protected void filterHostsBasedOnGuestOsRules(VMTemplateVO vmTemplate, List<? extends Host> clusterHosts) {
        if (clusterHosts.isEmpty()) {
            logger.info("Will not filter hosts based on guest OS as there is no available hosts left to verify.");
            return;
        }

        String templateGuestOSName = ApiDBUtils.getTemplateGuestOSName(vmTemplate);
        List<HostVO> incompatibleHosts = hostDao.findHostsWithGuestOsRulesThatDidNotMatchOsOfGuestVm(templateGuestOSName);

        if (incompatibleHosts.isEmpty()) {
            logger.info("No incompatible hosts found with guest OS rules matching the VM guest OS [{}].", templateGuestOSName);
            return;
        }

        logger.info("Found incompatible hosts {} with guest OS rules that did not match the VM guest OS [{}]. They will be removed from the suitable hosts list.",
                incompatibleHosts, templateGuestOSName);
        clusterHosts.removeAll(incompatibleHosts);

        if (clusterHosts.isEmpty()) {
            logger.info("After filtering by guest OS rules, no compatible hosts were found for VM with OS [{}].", templateGuestOSName);
        }
    }

    /**
     * Adds hosts with enough CPU capability and enough CPU capacity to the suitable hosts list.
     */
    protected boolean hostHasCpuCapabilityAndCapacity(boolean considerReservedCapacity, ServiceOffering offering, Host host) {
        logger.debug("Looking for CPU frequency {} MHz and RAM {} MB.", () -> offering.getCpu() * offering.getSpeed(), offering::getRamSize);
        Pair<Boolean, Boolean> cpuCapabilityAndCapacity = capacityManager.checkIfHostHasCpuCapabilityAndCapacity(host, offering, considerReservedCapacity);
        Boolean hasCpuCapability = cpuCapabilityAndCapacity.first();
        Boolean hasCpuCapacity = cpuCapabilityAndCapacity.second();

        if (hasCpuCapability && hasCpuCapacity) {
            logger.debug("Host {} is a suitable host as it has enough CPU capability and CPU capacity.", () -> host);
            return true;
        }

        logger.debug("Cannot use host {}. Does the host have CPU capability? {}. Does the host have CPU capacity? {}..", () -> host, () -> hasCpuCapability, () -> hasCpuCapacity);
        return false;
    }

}
