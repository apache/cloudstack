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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.gpu.GPU;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import static com.cloud.deploy.DeploymentPlanner.AllocationAlgorithm.firstfitleastconsumed;
import static com.cloud.deploy.DeploymentPlanner.AllocationAlgorithm.random;
import static com.cloud.deploy.DeploymentPlanner.AllocationAlgorithm.userconcentratedpod_random;
import static com.cloud.deploy.DeploymentPlanner.AllocationAlgorithm.userdispersing;

/**
 * An allocator that tries to find a fit on a computing host.  This allocator does not care whether or not the host supports routing.
 */
@Component
public class FirstFitAllocator extends BaseAllocator {
    @Inject
    HostDetailsDao _hostDetailsDao = null;
    @Inject
    ConfigurationDao _configDao = null;
    @Inject
    GuestOSDao _guestOSDao = null;
    @Inject
    GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject
    VMInstanceDao _vmInstanceDao = null;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    CapacityDao _capacityDao;
    @Inject
    UserVmDetailsDao _userVmDetailsDao;

    boolean _checkHvm = true;

    static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, null, returnUpTo, true);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                 boolean considerReservedCapacity) {
        if (type == Host.Type.Storage) {
            return null;
        }

        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();
        ServiceOffering offering = vmProfile.getServiceOffering();
        VMTemplateVO template = (VMTemplateVO) vmProfile.getTemplate();
        Account account = vmProfile.getOwner();

        String hostTagOnOffering = offering.getHostTag();
        String hostTagOnTemplate = template.getTemplateTag();
        String paramAsStringToLog = String.format("zone [%s], pod [%s], cluster [%s]", dcId, podId, clusterId);

        List<HostVO> suitableHosts = retrieveHosts(vmProfile, type, (List<HostVO>) hosts, template, clusterId, podId, dcId, hostTagOnOffering, hostTagOnTemplate);

        if (suitableHosts.isEmpty()) {
            logger.info("No suitable host found for VM [{}] in {}.", vmProfile, paramAsStringToLog);
            return null;
        }

        if (CollectionUtils.isEmpty(hosts)) {
            addHostsToAvoidSet(type, avoid, clusterId, podId, dcId, suitableHosts);
        }

        return allocateTo(plan, offering, template, avoid, suitableHosts, returnUpTo, considerReservedCapacity, account);
    }

    protected List<HostVO> retrieveHosts(VirtualMachineProfile vmProfile, Type type, List<HostVO> hostsToFilter, VMTemplateVO template, Long clusterId, Long podId, long dcId,
                                         String hostTagOnOffering, String hostTagOnTemplate) {
        String haVmTag = (String) vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);
        List<HostVO> clusterHosts;

        if (CollectionUtils.isNotEmpty(hostsToFilter)) {
            clusterHosts = new ArrayList<>(hostsToFilter);
        } else {
            clusterHosts = _resourceMgr.listAllUpAndEnabledHosts(type, clusterId, podId, dcId);
        }

        if (haVmTag != null) {
            clusterHosts.retainAll(hostDao.listByHostTag(type, clusterId, podId, dcId, haVmTag));
        } else if (ObjectUtils.allNull(hostTagOnOffering, hostTagOnTemplate)) {
            clusterHosts.retainAll(_resourceMgr.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId));
        } else {
            retainHostsMatchingServiceOfferingAndTemplateTags(clusterHosts, type, dcId, podId, clusterId, hostTagOnOffering, hostTagOnTemplate);
        }

        filterHostsWithUefiEnabled(type, vmProfile, clusterId, podId, dcId, clusterHosts);

        addHostsBasedOnTagRules(hostTagOnOffering, clusterHosts);
        filterHostsBasedOnGuestOsRules(template, clusterHosts);

        return clusterHosts;

    }

    /**
     * Add all hosts to the avoid set that were not considered during the allocation
     */
    protected void addHostsToAvoidSet(Type type, ExcludeList avoid, Long clusterId, Long podId, long dcId, List<HostVO> suitableHosts) {
        List<HostVO> allHostsInCluster = hostDao.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId, null);

        allHostsInCluster.removeAll(suitableHosts);

        logger.debug("Adding hosts [{}] to the avoid set because these hosts were not considered for allocation.",
                () -> ReflectionToStringBuilderUtils.reflectOnlySelectedFields(allHostsInCluster, "uuid", "name"));

        for (HostVO host : allHostsInCluster) {
            avoid.addHost(host.getId());
        }
    }

    protected void filterHostsWithUefiEnabled(Type type, VirtualMachineProfile vmProfile, Long clusterId, Long podId, long dcId, List<HostVO> clusterHosts) {
        UserVmDetailVO userVmDetailVO = _userVmDetailsDao.findDetail(vmProfile.getId(), "UEFI");

        if (userVmDetailVO == null) {
            return;
        }

        if (!StringUtils.equalsAnyIgnoreCase(userVmDetailVO.getValue(), ApiConstants.BootMode.SECURE.toString(), ApiConstants.BootMode.LEGACY.toString())) {
            return;
        }

        logger.info("Guest VM is requested with Custom[UEFI] Boot Type enabled.");

        List<HostVO> hostsMatchingUefiTag = hostDao.listByHostCapability(type, clusterId, podId, dcId, Host.HOST_UEFI_ENABLE);

        logger.debug("Hosts with UEFI enabled are {}.", hostsMatchingUefiTag);
        clusterHosts.retainAll(hostsMatchingUefiTag);
    }

    protected List<Host> allocateTo(DeploymentPlan plan, ServiceOffering offering, VMTemplateVO template, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                    boolean considerReservedCapacity, Account account) {

        String vmAllocationAlgorithm = DeploymentClusterPlanner.VmAllocationAlgorithm.value();
        if (List.of(random.toString(), userconcentratedpod_random.toString()).contains(vmAllocationAlgorithm)) {
            Collections.shuffle(hosts);
        } else if (userdispersing.toString().equals(vmAllocationAlgorithm)) {
            hosts = reorderHostsByNumberOfVms(plan, hosts, account);
        } else if (firstfitleastconsumed.toString().equals(vmAllocationAlgorithm)) {
            hosts = reorderHostsByCapacity(plan, hosts);
        }

        logger.debug("FirstFitAllocator has {} hosts to check for allocation {}.", hosts.size(), hosts);
        hosts = prioritizeHosts(template, offering, hosts);
        logger.debug("Found {} hosts for allocation after prioritization: {}.", hosts.size(), hosts);

        List<Host> suitableHosts = checkHostsCompatibilities(offering, avoid, hosts, returnUpTo, considerReservedCapacity);
        logger.debug("Host Allocator returning {} suitable hosts.", suitableHosts.size());

        return suitableHosts;
    }

    protected List<Host> checkHostsCompatibilities(ServiceOffering offering, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo, boolean considerReservedCapacity) {
        List<Host> suitableHosts = new ArrayList<>();
        logger.debug("Checking compatibility for the following hosts {}.", suitableHosts);

        for (Host host : hosts) {
            if (suitableHosts.size() == returnUpTo) {
                break;
            }

            if (avoid.shouldAvoid(host)) {
                logger.debug("Host [{}] is in avoid set, skipping this and trying other available hosts", () -> host);
                continue;
            }

            if (capacityManager.checkIfHostReachMaxGuestLimit(host)) {
                logger.debug("Adding host [{}] to the avoid set because this host already has the max number of running (user and/or system) VMs.", () ->  host);
                avoid.addHost(host.getId());
                continue;
            }

            if (offeringRequestedVGpuAndHostDoesNotHaveIt(offering, avoid, host)) {
                continue;
            }

            if (hostHasCpuCapabilityAndCapacity(considerReservedCapacity, offering, host)) {
                suitableHosts.add(host);
                continue;
            }
            avoid.addHost(host.getId());
        }
        return suitableHosts;
    }

    protected boolean offeringRequestedVGpuAndHostDoesNotHaveIt(ServiceOffering offering, ExcludeList avoid, Host host) {
        long serviceOfferingId = offering.getId();
        ServiceOfferingDetailsVO requestedVGpuType = _serviceOfferingDetailsDao.findDetail(serviceOfferingId, GPU.Keys.vgpuType.toString());

        if (requestedVGpuType == null) {
            return false;
        }

        ServiceOfferingDetailsVO groupName = _serviceOfferingDetailsDao.findDetail(serviceOfferingId, GPU.Keys.pciDevice.toString());
        if (!_resourceMgr.isGPUDeviceAvailable(host, groupName.getValue(), requestedVGpuType.getValue())) {
            logger.debug("Adding host [{}] to avoid set, because this host does not have required GPU devices available.", () -> host);
            avoid.addHost(host.getId());
            return true;
        }
        return false;
    }

    /**
     * Reorder hosts in the decreasing order of free capacity.
     */
    private List<? extends Host> reorderHostsByCapacity(DeploymentPlan plan, List<? extends Host> hosts) {
        Long zoneId = plan.getDataCenterId();
        Long clusterId = plan.getClusterId();
        String capacityTypeToOrder = _configDao.getValue(Config.HostCapacityTypeToOrderClusters.key());
        short capacityType = "RAM".equalsIgnoreCase(capacityTypeToOrder) ? CapacityVO.CAPACITY_TYPE_MEMORY : CapacityVO.CAPACITY_TYPE_CPU;

        Pair<List<Long>, Map<Long, Double>> result = _capacityDao.orderHostsByFreeCapacity(zoneId, clusterId, capacityType);
        List<Long> hostIdsByFreeCapacity = result.first();
        Map<Long, String> sortedHostByCapacity = result.second().entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> decimalFormat.format(entry.getValue() * 100) + "%",
                        (e1, e2) -> e1, LinkedHashMap::new));
        logger.debug("List of hosts: [{}] in descending order of free capacity (percentage) in the cluster: {}.",
                hostIdsByFreeCapacity, sortedHostByCapacity);

        return filterHosts(hosts, hostIdsByFreeCapacity);
    }

    private List<? extends Host> reorderHostsByNumberOfVms(DeploymentPlan plan, List<? extends Host> hosts, Account account) {
        if (account == null) {
            return hosts;
        }
        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();

        List<Long> hostIdsByVmCount = _vmInstanceDao.listHostIdsByVmCount(dcId, podId, clusterId, account.getAccountId());
        logger.debug("List of hosts in ascending order of number of VMs: {}.", hostIdsByVmCount);

        return filterHosts(hosts, hostIdsByVmCount);
    }

    /**
     * Filter the given list of Hosts considering the ordered list
     */
    private List<? extends Host> filterHosts(List<? extends Host> hosts, List<Long> orderedHostIdsList) {
        Map<Long, Host> hostMap = new HashMap<>();

        for (Host host : hosts) {
            hostMap.put(host.getId(), host);
        }
        List<Long> matchingHostIds = new ArrayList<>(hostMap.keySet());
        orderedHostIdsList.retainAll(matchingHostIds);

        List<Host> reorderedHosts = new ArrayList<>();
        for(Long id: orderedHostIdsList){
            reorderedHosts.add(hostMap.get(id));
        }

        return reorderedHosts;
    }

    /**
     * Reorder the host list giving priority to hosts that have the minimum to support the VM's requirements.
     */
    protected List<? extends Host> prioritizeHosts(VMTemplateVO template, ServiceOffering offering, List<? extends Host> hosts) {
        if (template == null) {
            return hosts;
        }

        List<Host> hostsToCheck = filterHostWithNoHvmIfTemplateRequested(template, hosts);

        List<Host> prioritizedHosts = new ArrayList<>();
        List<Host> highPriorityHosts = new ArrayList<>();
        List<Host> lowPriorityHosts = new ArrayList<>();

        prioritizeHostsWithMatchingGuestOs(template, hostsToCheck, highPriorityHosts, lowPriorityHosts);
        hostsToCheck.removeAll(highPriorityHosts);
        hostsToCheck.removeAll(lowPriorityHosts);

        prioritizeHostsByHvmCapability(template, hostsToCheck, prioritizedHosts);
        prioritizedHosts.addAll(0, highPriorityHosts);
        prioritizedHosts.addAll(lowPriorityHosts);

        prioritizeHostsByGpuEnabled(offering, prioritizedHosts);

        return prioritizedHosts;
    }


    /**
     * If a template requires HVM and a host doesn't support HVM, remove it from consideration.
     */
    protected List<Host> filterHostWithNoHvmIfTemplateRequested(VMTemplateVO template, List<? extends Host> hosts) {
        List<Host> hostsToCheck = new ArrayList<>();

        if (!template.isRequiresHvm()) {
            logger.debug("Template [{}] does not require HVM, therefore, the hosts {} will not be checked for HVM compatibility.", template, hostsToCheck);
            hostsToCheck.addAll(hosts);
            return hostsToCheck;
        }

        List<Host> noHvmHosts = new ArrayList<>();
        logger.debug("Template [{}] requires HVM, therefore, the hosts %s will be checked for HVM compatibility.", template, hostsToCheck);

        for (Host host : hosts) {
            if (hostSupportsHVM(host)) {
                hostsToCheck.add(host);
            } else {
                noHvmHosts.add(host);
            }
        }

        if (!noHvmHosts.isEmpty()) {
            logger.debug("Not considering hosts {} to deploy VM using template {} as they are not HVM enabled.", noHvmHosts, template);
        }

        return hostsToCheck;
    }


    /**
     * If service offering did not request for vGPU, then move all host with GPU to the end of the host priority list.
     */
    protected void prioritizeHostsByGpuEnabled(ServiceOffering offering, List<Host> prioritizedHosts) {
        boolean serviceOfferingRequestedVGpu = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString()) != null;

        if (serviceOfferingRequestedVGpu) {
            return;
        }

        List<Host> gpuEnabledHosts = new ArrayList<>();

        for (Host host : prioritizedHosts) {
            if (_resourceMgr.isHostGpuEnabled(host.getId())) {
                gpuEnabledHosts.add(host);
            }
        }

        if (!gpuEnabledHosts.isEmpty()) {
            prioritizedHosts.removeAll(gpuEnabledHosts);
            prioritizedHosts.addAll(gpuEnabledHosts);
        }
    }

    /**
     * Prioritize remaining host by HVM capability.
     *
     * <ul>
     *     <li>If host and template both do not support HVM, put it at the start of the list.</li>
     *     <li>If the template doesn't require HVM, but the machine supports it, append it to the list.</li>
     * </ul>
     */
    protected void prioritizeHostsByHvmCapability(VMTemplateVO template, List<Host> hostsToCheck, List<Host> prioritizedHosts) {
        for (Host host : hostsToCheck) {
            if (!template.isRequiresHvm() && !hostSupportsHVM(host)) {
                prioritizedHosts.add(0, host);
            } else {
                prioritizedHosts.add(host);
            }
        }
    }


    /**
     * <ul>
     *     <li>If a host is tagged with the same guest OS category as the template, move it to a high priority list.</li>
     *     <li>If a host is tagged with a different guest OS category than the template, move it to a low priority list.</li>
     * </ul>
     */
    protected void prioritizeHostsWithMatchingGuestOs(VMTemplateVO template, List<Host> hostsToCheck, List<Host> highPriorityHosts, List<Host> lowPriorityHosts) {
        String templateGuestOSCategory = getTemplateGuestOSCategory(template);

        for (Host host : hostsToCheck) {
            String hostGuestOSCategory = getHostGuestOSCategory(host);

            if (StringUtils.equals(templateGuestOSCategory, hostGuestOSCategory)) {
                highPriorityHosts.add(host);
            } else if (hostGuestOSCategory != null) {
                lowPriorityHosts.add(host);
            }
        }
    }


    protected boolean hostSupportsHVM(Host host) {
        if (!_checkHvm) {
            return true;
        }
        // Determine host capabilities
        String caps = host.getCapabilities();

        if (caps != null) {
            String[] tokens = caps.split(",");
            for (String token : tokens) {
                if (token.contains("hvm")) {
                    return true;
                }
            }
        }

        return false;
    }

    protected String getHostGuestOSCategory(Host host) {
        DetailVO hostDetail = _hostDetailsDao.findDetail(host.getId(), Host.GUEST_OS_CATEGORY_ID);
        if (hostDetail != null) {
            String guestOSCategoryIdString = hostDetail.getValue();
            long guestOSCategoryId;

            try {
                guestOSCategoryId = Long.parseLong(guestOSCategoryIdString);
            } catch (Exception e) {
                return null;
            }

            GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);

            if (guestOSCategory != null) {
                return guestOSCategory.getName();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected String getTemplateGuestOSCategory(VMTemplateVO template) {
        long guestOSId = template.getGuestOSId();
        GuestOSVO guestOS = _guestOSDao.findById(guestOSId);

        if (guestOS == null) {
            return null;
        }

        long guestOSCategoryId = guestOS.getCategoryId();
        GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
        return guestOSCategory.getName();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (_configDao != null) {
            Map<String, String> configs = _configDao.getConfiguration(params);
            String value = configs.get("xenserver.check.hvm");
            _checkHvm = value == null || Boolean.parseBoolean(value);
        }
        return true;
    }
}
