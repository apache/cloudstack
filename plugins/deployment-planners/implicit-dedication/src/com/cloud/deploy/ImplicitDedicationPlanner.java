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

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=DeploymentPlanner.class)
public class ImplicitDedicationPlanner extends FirstFitPlanner implements DeploymentClusterPlanner {

    private static final Logger s_logger = Logger.getLogger(ImplicitDedicationPlanner.class);

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
        capacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);
        return true;
    }

    @Override
    public List<Long> orderClusters(VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
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
            List<UserVmVO> userVms = getVmsOnHost(host);
            if (userVms == null || userVms.isEmpty()) {
                emptyHosts.add(host);
            } else if (checkHostSuitabilityForImplicitDedication(account.getAccountId(), userVms)) {
                hostRunningVmsOfAccount.add(host);
            } else if (checkIfAllVmsCreatedInStrictMode(account.getAccountId(), userVms)) {
                hostRunningStrictImplicitVmsOfOtherAccounts.add(host);
            } else {
                allOtherHosts.add(host);
            }
        }

        // Hosts running vms of other accounts created by ab implicit planner in strict mode should always be avoided.
        avoid.addHostList(hostRunningStrictImplicitVmsOfOtherAccounts);

        if (!hostRunningVmsOfAccount.isEmpty() && (hostsToAvoid == null ||
                !hostsToAvoid.containsAll(hostRunningVmsOfAccount))) {
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

    private List<UserVmVO> getVmsOnHost(long hostId) {
        List<UserVmVO> vms = _vmDao.listUpByHostId(hostId);
        List<UserVmVO> vmsByLastHostId = _vmDao.listByLastHostId(hostId);
        if (vmsByLastHostId.size() > 0) {
            // check if any VMs are within skip.counting.hours, if yes we have to consider the host.
            for (UserVmVO stoppedVM : vmsByLastHostId) {
                long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - stoppedVM.getUpdateTime()
                        .getTime()) / 1000;
                if (secondsSinceLastUpdate < capacityReleaseInterval) {
                    vms.add(stoppedVM);
                }
            }
        }

        return vms;
    }

    private boolean checkHostSuitabilityForImplicitDedication(Long accountId, List<UserVmVO> allVmsOnHost) {
        boolean suitable = true;
        for (UserVmVO vm : allVmsOnHost) {
            if (vm.getAccountId() != accountId) {
                s_logger.info("Host " + vm.getHostId() + " found to be unsuitable for implicit dedication as it is " +
                        "running instances of another account");
                suitable = false;
                break;
            } else {
                if (!isImplicitPlannerUsedByOffering(vm.getServiceOfferingId())) {
                    s_logger.info("Host " + vm.getHostId() + " found to be unsuitable for implicit dedication as it " +
                            "is running instances of this account which haven't been created using implicit dedication.");
                    suitable = false;
                    break;
                }
            }
        }
        return suitable;
    }

    private boolean checkIfAllVmsCreatedInStrictMode(Long accountId, List<UserVmVO> allVmsOnHost) {
        boolean createdByImplicitStrict = true;
        for (UserVmVO vm : allVmsOnHost) {
            if (!isImplicitPlannerUsedByOffering(vm.getServiceOfferingId())) {
                s_logger.info("Host " + vm.getHostId() + " found to be running a vm created by a planner other" +
                        " than implicit.");
                createdByImplicitStrict = false;
                break;
            } else if (isServiceOfferingUsingPlannerInPreferredMode(vm.getServiceOfferingId())) {
                s_logger.info("Host " + vm.getHostId() + " found to be running a vm created by an implicit planner" +
                        " in preferred mode.");
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
            s_logger.error("Couldn't retrieve the offering by the given id : " + offeringId);
        } else {
            String plannerName = offering.getDeploymentPlanner();
            if (plannerName == null) {
                plannerName = _globalDeploymentPlanner;
            }

            if (plannerName != null && this.getName().equals(plannerName)) {
                implicitPlannerUsed = true;
            }
        }

        return implicitPlannerUsed;
    }

    private boolean isServiceOfferingUsingPlannerInPreferredMode(long serviceOfferingId) {
        boolean preferred = false;
        Map<String, String> details = serviceOfferingDetailsDao.findDetails(serviceOfferingId);
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
    public PlannerResourceUsage getResourceUsage() {
        return PlannerResourceUsage.Dedicated;
    }
}