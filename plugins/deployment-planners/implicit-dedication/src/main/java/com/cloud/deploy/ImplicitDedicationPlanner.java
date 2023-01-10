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
package com.cloud.deploy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import com.cloud.configuration.Config;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.HostVO;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.user.Account;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import org.springframework.util.CollectionUtils;

public class ImplicitDedicationPlanner extends FirstFitPlanner implements DeploymentClusterPlanner {


    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private ResourceManager resourceMgr;

    private int capacityReleaseInterval;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        capacityReleaseInterval = NumbersUtil.parseInt(configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);
        return true;
    }

    @Override
    public List<Long> orderClusters(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        List<Long> clusterList = super.orderClusters(vmProfile, plan, avoid);
        Set<Long> hostsToAvoid = avoid.getHostsToAvoid();
        Account account = vmProfile.getOwner();

        if (clusterList == null || clusterList.isEmpty()) {
            return clusterList;
        }

        // Check if strict or preferred mode should be used.
        boolean preferred = isServiceOfferingUsingPlannerInPreferredMode(vmProfile.getServiceOfferingId());

        // Get the list of all the hosts in the given clusters
        List<Long> allHosts = new ArrayList<Long>();
        for (Long cluster : clusterList) {
            List<HostVO> hostsInCluster = resourceMgr.listAllHostsInCluster(cluster);
            for (HostVO hostVO : hostsInCluster) {
                allHosts.add(hostVO.getId());
            }
        }

        // Go over all the hosts in the cluster and get a list of
        // 1. All empty hosts, not running any vms.
        // 2. Hosts running vms for this account and created by a service offering which uses an
        //    implicit dedication planner.
        // 3. Hosts running vms created by implicit planner and in strict mode of other accounts.
        // 4. Hosts running vms from other account or from this account but created by a service offering which uses
        //    any planner besides implicit.
        Set<Long> emptyHosts = new HashSet<Long>();
        Set<Long> hostRunningVmsOfAccount = new HashSet<Long>();
        Set<Long> hostRunningStrictImplicitVmsOfOtherAccounts = new HashSet<Long>();
        Set<Long> allOtherHosts = new HashSet<Long>();
        for (Long host : allHosts) {
            List<VMInstanceVO> vms = getVmsOnHost(host);
            if (vms == null || vms.isEmpty()) {
                emptyHosts.add(host);
            } else if (checkHostSuitabilityForImplicitDedication(account.getAccountId(), vms)) {
                hostRunningVmsOfAccount.add(host);
            } else if (checkIfAllVmsCreatedInStrictMode(account.getAccountId(), vms)) {
                hostRunningStrictImplicitVmsOfOtherAccounts.add(host);
            } else {
                allOtherHosts.add(host);
            }
        }

        // Hosts running vms of other accounts created by an implicit planner in strict mode should always be avoided.
        avoid.addHostList(hostRunningStrictImplicitVmsOfOtherAccounts);

        if (!hostRunningVmsOfAccount.isEmpty() && (hostsToAvoid == null || !hostsToAvoid.containsAll(hostRunningVmsOfAccount))) {
            // Check if any of hosts that are running implicit dedicated vms are available (not in avoid list).
            // If so, we'll try and use these hosts.
            avoid.addHostList(emptyHosts);
            avoid.addHostList(allOtherHosts);
            clusterList = getUpdatedClusterList(clusterList, avoid.getHostsToAvoid());
        } else if (!emptyHosts.isEmpty() && (hostsToAvoid == null || !hostsToAvoid.containsAll(emptyHosts))) {
            // If there aren't implicit resources try on empty hosts
            avoid.addHostList(allOtherHosts);
            clusterList = getUpdatedClusterList(clusterList, avoid.getHostsToAvoid());
        } else if (!preferred) {
            // If in strict mode, there is nothing else to try.
            clusterList = null;
        } else {
            // If in preferred mode, check if hosts are available to try, otherwise return an empty cluster list.
            if (!allOtherHosts.isEmpty() && (hostsToAvoid == null || !hostsToAvoid.containsAll(allOtherHosts))) {
                clusterList = getUpdatedClusterList(clusterList, avoid.getHostsToAvoid());
            } else {
                clusterList = null;
            }
        }

        return clusterList;
    }

    private List<VMInstanceVO> getVmsOnHost(long hostId) {
        List<VMInstanceVO> vms = vmInstanceDao.listUpByHostId(hostId);
        List<VMInstanceVO> vmsByLastHostId = vmInstanceDao.listByLastHostId(hostId);
        if (vmsByLastHostId.size() > 0) {
            // check if any VMs are within skip.counting.hours, if yes we have to consider the host.
            for (VMInstanceVO stoppedVM : vmsByLastHostId) {
                long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - stoppedVM.getUpdateTime().getTime()) / 1000;
                if (secondsSinceLastUpdate < capacityReleaseInterval) {
                    vms.add(stoppedVM);
                }
            }
        }

        return vms;
    }

    private boolean checkHostSuitabilityForImplicitDedication(Long accountId, List<VMInstanceVO> allVmsOnHost) {
        boolean suitable = true;
        if (allVmsOnHost.isEmpty())
            return false;

        for (VMInstanceVO vm : allVmsOnHost) {
            if (vm.getAccountId() != accountId) {
                logger.info("Host " + vm.getHostId() + " found to be unsuitable for implicit dedication as it is " + "running instances of another account");
                suitable = false;
                break;
            } else {
                if (!isImplicitPlannerUsedByOffering(vm.getServiceOfferingId())) {
                    logger.info("Host " + vm.getHostId() + " found to be unsuitable for implicit dedication as it " +
                        "is running instances of this account which haven't been created using implicit dedication.");
                    suitable = false;
                    break;
                }
            }
        }
        return suitable;
    }

    private boolean checkIfAllVmsCreatedInStrictMode(Long accountId, List<VMInstanceVO> allVmsOnHost) {
        boolean createdByImplicitStrict = true;
        if (allVmsOnHost.isEmpty())
            return false;
        for (VMInstanceVO vm : allVmsOnHost) {
            if (!isImplicitPlannerUsedByOffering(vm.getServiceOfferingId())) {
                logger.info("Host " + vm.getHostId() + " found to be running a vm created by a planner other" + " than implicit.");
                createdByImplicitStrict = false;
                break;
            } else if (isServiceOfferingUsingPlannerInPreferredMode(vm.getServiceOfferingId())) {
                logger.info("Host " + vm.getHostId() + " found to be running a vm created by an implicit planner" + " in preferred mode.");
                createdByImplicitStrict = false;
                break;
            }
        }
        return createdByImplicitStrict;
    }

    private boolean isImplicitPlannerUsedByOffering(long offeringId) {
        boolean implicitPlannerUsed = false;
        ServiceOfferingVO offering = serviceOfferingDao.findByIdIncludingRemoved(offeringId);
        if (offering == null) {
            logger.error("Couldn't retrieve the offering by the given id : " + offeringId);
        } else {
            String plannerName = offering.getDeploymentPlanner();
            if (plannerName == null) {
                plannerName = globalDeploymentPlanner;
            }

            if (plannerName != null && this.getName().equals(plannerName)) {
                implicitPlannerUsed = true;
            }
        }

        return implicitPlannerUsed;
    }

    private boolean isServiceOfferingUsingPlannerInPreferredMode(long serviceOfferingId) {
        boolean preferred = false;
        Map<String, String> details = serviceOfferingDetailsDao.listDetailsKeyPairs(serviceOfferingId);
        if (details != null && !details.isEmpty()) {
            String preferredAttribute = details.get("ImplicitDedicationMode");
            if (preferredAttribute != null && preferredAttribute.equals("Preferred")) {
                preferred = true;
            }
        }
        return preferred;
    }

    private List<Long> getUpdatedClusterList(List<Long> clusterList, Set<Long> hostsSet) {
        List<Long> updatedClusterList = new ArrayList<Long>();
        for (Long cluster : clusterList) {
            List<HostVO> hosts = resourceMgr.listAllHostsInCluster(cluster);
            Set<Long> hostsInClusterSet = new HashSet<Long>();
            for (HostVO host : hosts) {
                hostsInClusterSet.add(host.getId());
            }

            if (!hostsSet.containsAll(hostsInClusterSet)) {
                updatedClusterList.add(cluster);
            }
        }

        return updatedClusterList;
    }

    @Override
    public PlannerResourceUsage getResourceUsage(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        // Check if strict or preferred mode should be used.
        boolean preferred = isServiceOfferingUsingPlannerInPreferredMode(vmProfile.getServiceOfferingId());

        // If service offering in strict mode return resource usage as Dedicated
        if (!preferred) {
            return PlannerResourceUsage.Dedicated;
        } else {
            // service offering is in implicit mode.
            // find is it possible to deploy in dedicated mode,
            // if its possible return dedicated else return shared.
            List<Long> clusterList = super.orderClusters(vmProfile, plan, avoid);
            Set<Long> hostsToAvoid = avoid.getHostsToAvoid();
            Account account = vmProfile.getOwner();

            // Get the list of all the hosts in the given clusters
            List<Long> allHosts = new ArrayList<Long>();
            if (!CollectionUtils.isEmpty(clusterList)) {
                for (Long cluster : clusterList) {
                    List<HostVO> hostsInCluster = resourceMgr.listAllHostsInCluster(cluster);
                    for (HostVO hostVO : hostsInCluster) {

                        allHosts.add(hostVO.getId());
                    }
                }
            }
            // Go over all the hosts in the cluster and get a list of
            // 1. All empty hosts, not running any vms.
            // 2. Hosts running vms for this account and created by a service
            // offering which uses an
            // implicit dedication planner.
            // 3. Hosts running vms created by implicit planner and in strict
            // mode of other accounts.
            // 4. Hosts running vms from other account or from this account but
            // created by a service offering which uses
            // any planner besides implicit.
            Set<Long> emptyHosts = new HashSet<Long>();
            Set<Long> hostRunningVmsOfAccount = new HashSet<Long>();
            Set<Long> hostRunningStrictImplicitVmsOfOtherAccounts = new HashSet<Long>();
            Set<Long> allOtherHosts = new HashSet<Long>();
            for (Long host : allHosts) {
                List<VMInstanceVO> vms = getVmsOnHost(host);
                // emptyHost should contain only Hosts which are not having any VM's (user/system) on it.
                if (vms == null || vms.isEmpty()) {
                    emptyHosts.add(host);
                } else if (checkHostSuitabilityForImplicitDedication(account.getAccountId(), vms)) {
                    hostRunningVmsOfAccount.add(host);
                } else if (checkIfAllVmsCreatedInStrictMode(account.getAccountId(), vms)) {
                    hostRunningStrictImplicitVmsOfOtherAccounts.add(host);
                } else {
                    allOtherHosts.add(host);
                }
            }

            // Hosts running vms of other accounts created by ab implicit
            // planner in strict mode should always be avoided.
            avoid.addHostList(hostRunningStrictImplicitVmsOfOtherAccounts);

            if (!hostRunningVmsOfAccount.isEmpty() && (hostsToAvoid == null || !hostsToAvoid.containsAll(hostRunningVmsOfAccount))) {
                // Check if any of hosts that are running implicit dedicated vms are available (not in avoid list).
                // If so, we'll try and use these hosts. We can deploy in Dedicated mode
                return PlannerResourceUsage.Dedicated;
            } else if (!emptyHosts.isEmpty() && (hostsToAvoid == null || !hostsToAvoid.containsAll(emptyHosts))) {
                // If there aren't implicit resources try on empty hosts, As empty hosts are available we can deploy in Dedicated mode.
                // Empty hosts can contain hosts which are not having user vms but system vms are running.
                // But the host where system vms are running is marked as shared and still be part of empty Hosts.
                // The scenario will fail where actual Empty hosts and uservms not running host.
                return PlannerResourceUsage.Dedicated;
            } else {
                if (!allOtherHosts.isEmpty() && (hostsToAvoid == null || !hostsToAvoid.containsAll(allOtherHosts))) {
                    return PlannerResourceUsage.Shared;
                }
            }
            return PlannerResourceUsage.Shared;
        }
    }
}
