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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.gpu.GPU;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
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
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;


/**
 * An allocator that tries to find a fit on a computing host.  This allocator does not care whether or not the host supports routing.
 */
@Component
public class FirstFitAllocator extends AdapterBase implements HostAllocator {
    @Inject
    protected HostDao _hostDao = null;
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
    ClusterDao _clusterDao;
    @Inject
    ClusterDetailsDao _clusterDetailsDao;
    @Inject
    ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    CapacityManager _capacityMgr;
    @Inject
    CapacityDao _capacityDao;
    @Inject
    UserVmDetailsDao _userVmDetailsDao;

    boolean _checkHvm = true;
    protected String _allocationAlgorithm = "random";


    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, returnUpTo, true);
    }

    @Override
    public List<Host> allocateTo(VirtualMachineProfile vmProfile, DeploymentPlan plan, Type type, ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity) {
        if (type == Host.Type.Storage) {
            return null;
        }

        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();
        ServiceOffering offering = vmProfile.getServiceOffering();
        VMTemplateVO template = (VMTemplateVO)vmProfile.getTemplate();
        Account account = vmProfile.getOwner();

        String hostTagOnOffering = offering.getHostTag();
        String hostTagOnTemplate = template.getTemplateTag();
        String paramAsStringToLog = String.format("zone [%s], pod [%s], cluster [%s]", dcId, podId, clusterId);

        List<HostVO> suitableHosts = retrieveHosts(vmProfile, type, clusterId, podId, dcId, hostTagOnOffering, hostTagOnTemplate);

        if (isSuitableHostsEmpty(vmProfile, suitableHosts, paramAsStringToLog)) {
            return null;
        }

        addHostsToAvoidSet(type, avoid, clusterId, podId, dcId, suitableHosts);

        return allocateTo(plan, offering, template, avoid, suitableHosts, returnUpTo, considerReservedCapacity, account);
    }

    private boolean isSuitableHostsEmpty(VirtualMachineProfile vmProfile, List<HostVO> suitableHosts, String paramAsStringToLog) {
        if (suitableHosts.isEmpty()) {
            logger.info("No suitable host found for VM [{}] in {}.", vmProfile, paramAsStringToLog);
            return true;
        }
        return false;
    }

    private List<HostVO> retrieveHosts(VirtualMachineProfile vmProfile, Type type, Long clusterId, Long podId, long dcId, String hostTagOnOffering, String hostTagOnTemplate) {
        String haVmTag = (String) vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);
        List<HostVO> clusterHosts;

        if (haVmTag != null) {
            clusterHosts = _hostDao.listByHostTag(type, clusterId, podId, dcId, haVmTag);
        } else if (ObjectUtils.allNull(hostTagOnOffering, hostTagOnTemplate)) {
            clusterHosts = _resourceMgr.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId);
        } else {
            clusterHosts = retrieveHostsMatchingServiceOfferingAndTemplateTags(hostTagOnTemplate, hostTagOnOffering, type, clusterId, podId, dcId);
        }

        filterHostsWithUefiEnabled(type, vmProfile, clusterId, podId, dcId, clusterHosts);

        addHostsBasedOnTagRules(hostTagOnOffering, clusterHosts);

        return clusterHosts;
    }

    private void addHostsBasedOnTagRules(String hostTagOnOffering, List<HostVO> clusterHosts) {
        List<HostVO> hostsWithTagRules = _hostDao.findHostsWithTagRuleThatMatchComputeOfferingTags(hostTagOnOffering);

        if (CollectionUtils.isEmpty(hostsWithTagRules)) {
            logger.info("No hosts found with tag rules matching the compute offering tag {}.", hostTagOnOffering);
        }

        logger.info("Found hosts {} with tag rules matching the compute offering tag [{}].", hostsWithTagRules, hostTagOnOffering);
        clusterHosts.addAll(hostsWithTagRules);
    }

    private List<HostVO> retrieveHostsMatchingServiceOfferingAndTemplateTags(String hostTagOnTemplate, String hostTagOnOffering, Type type, Long clusterId, Long podId, long dcId) {
        boolean hasSvcOfferingTag = hostTagOnOffering != null;
        boolean hasTemplateTag = hostTagOnTemplate != null;
        List<HostVO> clusterHosts;
        List<HostVO> hostsMatchingOfferingTag = new ArrayList<>();
        List<HostVO> hostsMatchingTemplateTag = new ArrayList<>();

        if (hasSvcOfferingTag) {
            logger.debug("Looking for hosts having the tag [{}] specified in the Service Offering.", hostTagOnOffering);
            hostsMatchingOfferingTag = _hostDao.listByHostTag(type, clusterId, podId, dcId, hostTagOnOffering);
            logger.debug("Hosts with Service Offering tag [{}] are {}.", hostTagOnOffering, hostsMatchingOfferingTag);
        }

        if (hasTemplateTag) {
            logger.debug("Looking for hosts having the tag [{}] specified in the Template.", hostTagOnTemplate);
            hostsMatchingTemplateTag = _hostDao.listByHostTag(type, clusterId, podId, dcId, hostTagOnTemplate);
            logger.debug("Hosts with Template tag [{}] are {}.", hostTagOnTemplate, hostsMatchingTemplateTag);
        }

        if (hasSvcOfferingTag && hasTemplateTag) {
            hostsMatchingOfferingTag.retainAll(hostsMatchingTemplateTag);
            logger.debug("Found {} Hosts satisfying both tags; host IDs are {}.", hostsMatchingOfferingTag.size(), hostsMatchingOfferingTag);
            clusterHosts = hostsMatchingOfferingTag;
        } else if (hasSvcOfferingTag) {
            clusterHosts = hostsMatchingOfferingTag;
        } else {
            clusterHosts = hostsMatchingTemplateTag;
        }

        return clusterHosts;
    }

    /**
     * Add all hosts to the avoid set that were not considered during the allocation
     */
    private void addHostsToAvoidSet(Type type, ExcludeList avoid, Long clusterId, Long podId, long dcId, List<HostVO> suitableHosts) {
        List<HostVO> allHostsInCluster = _hostDao.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId, null);

        allHostsInCluster.removeAll(suitableHosts);

        logger.debug("Adding hosts [{}] to the avoid set because these hosts were not considered for allocation.",
                () -> ReflectionToStringBuilderUtils.reflectOnlySelectedFields(allHostsInCluster, "uuid", "name"));

        for (HostVO host : allHostsInCluster) {
            avoid.addHost(host.getId());
        }
    }

    private void filterHostsWithUefiEnabled(Type type, VirtualMachineProfile vmProfile, Long clusterId, Long podId, long dcId, List<HostVO> clusterHosts) {
        UserVmDetailVO userVmDetailVO = _userVmDetailsDao.findDetail(vmProfile.getId(), "UEFI");

        if (userVmDetailVO == null) {
            return;
        }

        if (!StringUtils.equalsAnyIgnoreCase(userVmDetailVO.getValue(), ApiConstants.BootMode.SECURE.toString(), ApiConstants.BootMode.LEGACY.toString())) {
            return;
        }

        logger.info("Guest VM is requested with Custom[UEFI] Boot Type enabled.");

        List<HostVO> hostsMatchingUefiTag = _hostDao.listByHostCapability(type, clusterId, podId, dcId, Host.HOST_UEFI_ENABLE);

        logger.debug("Hosts with UEFI enabled are {}.", hostsMatchingUefiTag);
        clusterHosts.retainAll(hostsMatchingUefiTag);
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
        List<HostVO> suitableHosts = (List<HostVO>) new ArrayList<>(hosts);

        String paramAsStringToLog = String.format("zone [%s], pod [%s], cluster [%s]", dcId, podId, clusterId);
        logger.debug("Looking for hosts in {}.", paramAsStringToLog);

        retainHostsMatchingCriteria(vmProfile, type, suitableHosts, clusterId, podId, dcId, hostTagOnOffering, hostTagOnTemplate);

        addHostsBasedOnTagRules(hostTagOnOffering, suitableHosts);

        if (isSuitableHostsEmpty(vmProfile, suitableHosts, paramAsStringToLog)) {
            return null;
        }

        return allocateTo(plan, offering, template, avoid, suitableHosts, returnUpTo, considerReservedCapacity, account);
    }

    private void retainHostsMatchingCriteria(VirtualMachineProfile vmProfile, Type type, List<HostVO> suitableHosts, Long clusterId, Long podId, long dcId,
                                             String hostTagOnOffering, String hostTagOnTemplate) {
        String haVmTag = (String) vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);
        boolean hasSvcOfferingTag = hostTagOnOffering != null;
        boolean hasTemplateTag = hostTagOnTemplate != null;

        if (haVmTag != null) {
            suitableHosts.retainAll(_hostDao.listByHostTag(type, clusterId, podId, dcId, haVmTag));
        } else if (ObjectUtils.allNull(hostTagOnOffering, hostTagOnTemplate)) {
            suitableHosts.retainAll(_resourceMgr.listAllUpAndEnabledNonHAHosts(type, clusterId, podId, dcId));
        } else {
            if (hasSvcOfferingTag) {
                logger.debug("Looking for hosts having the tag [{}] specified in the Service Offering.", hostTagOnOffering);
                suitableHosts.retainAll(_hostDao.listByHostTag(type, clusterId, podId, dcId, hostTagOnOffering));
                logger.debug("Hosts with Service Offering tag [{}] are {}.", hostTagOnOffering, suitableHosts);
            }

            if (hasTemplateTag) {
                logger.debug("Looking for hosts having the tag [{}] specified in the Template.", hostTagOnTemplate);
                suitableHosts.retainAll(_hostDao.listByHostTag(type, clusterId, podId, dcId, hostTagOnTemplate));
                logger.debug("Hosts with Template tag [{}] are {}.", hostTagOnTemplate, suitableHosts);
            }
        }
    }

    protected List<Host> allocateTo(DeploymentPlan plan, ServiceOffering offering, VMTemplateVO template, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo,
                                    boolean considerReservedCapacity, Account account) {
        switch (_allocationAlgorithm) {
            case "random":
            case "userconcentratedpod_random":
                // Shuffle this so that we don't check the hosts in the same order.
                Collections.shuffle(hosts);
                break;
            case "userdispersing":
                hosts = reorderHostsByNumberOfVms(plan, hosts, account);
                break;
            case "firstfitleastconsumed":
                hosts = reorderHostsByCapacity(plan, hosts);
                break;
        }

        logger.debug("FirstFitAllocator has {} hosts to check for allocation: {}.", hosts.size(), hosts);

        hosts = prioritizeHosts(template, offering, hosts);

        logger.debug("Found {} hosts for allocation after prioritization: {}.", hosts.size(), hosts);
        logger.debug("Looking for frequency {} MHz and RAM {} MB.", () -> offering.getCpu() * offering.getSpeed(), offering::getRamSize);

        List<Host> suitableHosts = checkHostsCompatibilities(offering, avoid, hosts, returnUpTo, considerReservedCapacity);

        logger.debug("Host Allocator returning {} suitable hosts",  suitableHosts.size());

        return suitableHosts;
    }

    private List<Host> checkHostsCompatibilities(ServiceOffering offering, ExcludeList avoid, List<? extends Host> hosts, int returnUpTo, boolean considerReservedCapacity) {
        List<Host> suitableHosts = new ArrayList<>();

        for (Host host : hosts) {
            if (suitableHosts.size() == returnUpTo) {
                break;
            }

            if (avoid.shouldAvoid(host)) {
                logger.debug("Host [{}] is in avoid set, skipping this and trying other available hosts", () -> host);
                continue;
            }

            if (_capacityMgr.checkIfHostReachMaxGuestLimit(host)) {
                logger.debug("Adding host [{}] to the avoid set because this host already has the max number of running (user and/or system) VMs.", () -> host);
                avoid.addHost(host.getId());
                continue;
            }

            if (offeringRequestedVGpuAndHostDoesNotHaveIt(offering, avoid, host)) {
                continue;
            }

            Pair<Boolean, Boolean> cpuCapabilityAndCapacity = _capacityMgr.checkIfHostHasCpuCapabilityAndCapacity(host, offering, considerReservedCapacity);
            if (cpuCapabilityAndCapacity.first() && cpuCapabilityAndCapacity.second()) {
                logger.debug("Found a suitable host, adding to list host [{}].", () -> host);
                suitableHosts.add(host);
            } else {
                logger.debug("Not using host {}; host has cpu capability? {}, host has capacity? {}.", () -> host, cpuCapabilityAndCapacity::first, cpuCapabilityAndCapacity::second);
                avoid.addHost(host.getId());
            }
        }
        return suitableHosts;
    }


    private boolean offeringRequestedVGpuAndHostDoesNotHaveIt(ServiceOffering offering, ExcludeList avoid, Host host) {
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

        List<Long> hostIdsByFreeCapacity = _capacityDao.orderHostsByFreeCapacity(zoneId, clusterId, capacityType);
        logger.debug("List of hosts in descending order of free capacity in the cluster: {}.", hostIdsByFreeCapacity);

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

    @Override
    public boolean isVirtualMachineUpgradable(VirtualMachine vm, ServiceOffering offering) {
        // currently we do no special checks to rule out a VM being upgradable to an offering, so
        // return true
        return true;
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
        prioritizeHostsByHvmCapability(template, hostsToCheck, prioritizedHosts, highPriorityHosts, lowPriorityHosts);
        prioritizeHostsByGpuEnabled(offering, prioritizedHosts);

        return prioritizedHosts;
    }

    /**
     * If a template requires HVM and a host doesn't support HVM, remove it from consideration
     */
    private List<Host> filterHostWithNoHvmIfTemplateRequested(VMTemplateVO template, List<? extends Host> hosts) {
        List<Host> noHvmHosts = new ArrayList<>();
        List<Host> hostsToCheck = new ArrayList<>();

        if (!template.isRequiresHvm()) {
            hostsToCheck.addAll(hosts);
        } else {
            for (Host host : hosts) {
                if (hostSupportsHVM(host)) {
                    hostsToCheck.add(host);
                } else {
                    noHvmHosts.add(host);
                }
            }
        }

        if (!noHvmHosts.isEmpty()) {
            logger.debug("Not considering hosts: " + noHvmHosts + "  to deploy template: " + template + " as they are not HVM enabled");
        }

        return hostsToCheck;
    }

    /**
     * If service offering does not request for vGPU, then append all host with GPU to the end of the host priority list.
     */
    private void prioritizeHostsByGpuEnabled(ServiceOffering offering, List<Host> prioritizedHosts) {
        boolean serviceOfferingRequestedVGpu = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString()) == null;

        if (serviceOfferingRequestedVGpu) {
            List<Host> gpuEnabledHosts = new ArrayList<>();

            for (Host host : prioritizedHosts) {
                if (_resourceMgr.isHostGpuEnabled(host.getId())) {
                    gpuEnabledHosts.add(host);
                }
            }

            if(!gpuEnabledHosts.isEmpty()) {
                prioritizedHosts.removeAll(gpuEnabledHosts);
                prioritizedHosts.addAll(gpuEnabledHosts);
            }
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
    private void prioritizeHostsByHvmCapability(VMTemplateVO template, List<Host> hostsToCheck, List<Host> prioritizedHosts, List<Host> highPriorityHosts, List<Host> lowPriorityHosts) {

        for (Host host : hostsToCheck) {
            if (!template.isRequiresHvm() && !hostSupportsHVM(host)) {
                prioritizedHosts.add(0, host);
            } else {
                prioritizedHosts.add(host);
            }
        }

        prioritizedHosts.addAll(0, highPriorityHosts);
        prioritizedHosts.addAll(lowPriorityHosts);
    }

    /**
     * <ul>
     *     <li>If a host is tagged with the same guest OS category as the template, move it to a high priority list.</li>
     *     <li>If a host is tagged with a different guest OS category than the template, move it to a low priority list.</li>
     * </ul>
     */
    private void prioritizeHostsWithMatchingGuestOs(VMTemplateVO template, List<Host> hostsToCheck, List<Host> highPriorityHosts, List<Host> lowPriorityHosts) {
        String templateGuestOSCategory = getTemplateGuestOSCategory(template);

        for (Host host : hostsToCheck) {
            String hostGuestOSCategory = getHostGuestOSCategory(host);

            if (StringUtils.equals(templateGuestOSCategory, hostGuestOSCategory)) {
                highPriorityHosts.add(host);
            } else if (hostGuestOSCategory != null) {
                lowPriorityHosts.add(host);

            }
        }

        hostsToCheck.removeAll(highPriorityHosts);
        hostsToCheck.removeAll(lowPriorityHosts);
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
        DetailVO hostDetail = _hostDetailsDao.findDetail(host.getId(), "guest.os.category.id");
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

            String allocationAlgorithm = configs.get("vm.allocation.algorithm");
            if (allocationAlgorithm != null) {
                _allocationAlgorithm = allocationAlgorithm;
            }
            String value = configs.get("xenserver.check.hvm");
            _checkHvm = value == null || Boolean.parseBoolean(value);
        }
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

}
