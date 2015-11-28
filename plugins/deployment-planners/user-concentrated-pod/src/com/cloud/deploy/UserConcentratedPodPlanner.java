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
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachineProfile;

public class UserConcentratedPodPlanner extends FirstFitPlanner implements DeploymentClusterPlanner {

    private static final Logger s_logger = Logger.getLogger(UserConcentratedPodPlanner.class);

    /**
     * This method should reorder the given list of Cluster Ids by applying any necessary heuristic
     * for this planner
     * For UserConcentratedPodPlanner we need to order the clusters in a zone across pods, by considering those pods first which have more number of VMs for this account
     * This reordering is not done incase the clusters within single pod are passed when the allocation is applied at pod-level.
     * @return List<Long> ordered list of Cluster Ids
     */
    @Override
    protected List<Long> reorderClusters(long id, boolean isZone, Pair<List<Long>, Map<Long, Double>> clusterCapacityInfo, VirtualMachineProfile vmProfile,
        DeploymentPlan plan) {
        List<Long> clusterIdsByCapacity = clusterCapacityInfo.first();
        if (vmProfile.getOwner() == null || !isZone) {
            return clusterIdsByCapacity;
        }
        return applyUserConcentrationPodHeuristicToClusters(id, clusterIdsByCapacity, vmProfile.getOwner().getAccountId());
    }

    private List<Long> applyUserConcentrationPodHeuristicToClusters(long zoneId, List<Long> prioritizedClusterIds, long accountId) {
        //user has VMs in certain pods. - prioritize those pods first
        //UserConcentratedPod strategy
        List<Long> clusterList = new ArrayList<Long>();
        List<Long> podIds = listPodsByUserConcentration(zoneId, accountId);
        if (!podIds.isEmpty()) {
            clusterList = reorderClustersByPods(prioritizedClusterIds, podIds);
        } else {
            clusterList = prioritizedClusterIds;
        }
        return clusterList;
    }

    private List<Long> reorderClustersByPods(List<Long> clusterIds, List<Long> podIds) {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Reordering cluster list as per pods ordered by user concentration");
        }

        Map<Long, List<Long>> podClusterMap = clusterDao.getPodClusterIdMap(clusterIds);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Pod To cluster Map is: " + podClusterMap);
        }

        List<Long> reorderedClusters = new ArrayList<Long>();
        for (Long pod : podIds) {
            if (podClusterMap.containsKey(pod)) {
                List<Long> clustersOfThisPod = podClusterMap.get(pod);
                if (clustersOfThisPod != null) {
                    for (Long clusterId : clusterIds) {
                        if (clustersOfThisPod.contains(clusterId)) {
                            reorderedClusters.add(clusterId);
                        }
                    }
                    clusterIds.removeAll(clustersOfThisPod);
                }
            }
        }
        reorderedClusters.addAll(clusterIds);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Reordered cluster list: " + reorderedClusters);
        }
        return reorderedClusters;
    }

    protected List<Long> listPodsByUserConcentration(long zoneId, long accountId) {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Applying UserConcentratedPod heuristic for account: " + accountId);
        }

        List<Long> prioritizedPods = vmDao.listPodIdsHavingVmsforAccount(zoneId, accountId);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("List of pods to be considered, after applying UserConcentratedPod heuristic: " + prioritizedPods);
        }

        return prioritizedPods;
    }

    /**
     * This method should reorder the given list of Pod Ids by applying any necessary heuristic
     * for this planner
     * For UserConcentratedPodPlanner we need to order the pods by considering those pods first which have more number of VMs for this account
     * @return List<Long> ordered list of Pod Ids
     */
    @Override
    protected List<Long> reorderPods(Pair<List<Long>, Map<Long, Double>> podCapacityInfo, VirtualMachineProfile vmProfile, DeploymentPlan plan) {
        List<Long> podIdsByCapacity = podCapacityInfo.first();
        if (vmProfile.getOwner() == null) {
            return podIdsByCapacity;
        }
        long accountId = vmProfile.getOwner().getAccountId();

        //user has VMs in certain pods. - prioritize those pods first
        //UserConcentratedPod strategy
        List<Long> podIds = listPodsByUserConcentration(plan.getDataCenterId(), accountId);
        if (!podIds.isEmpty()) {
            //remove pods that dont have capacity for this vm
            podIds.retainAll(podIdsByCapacity);
            podIdsByCapacity.removeAll(podIds);
            podIds.addAll(podIdsByCapacity);
            return podIds;
        } else {
            return podIdsByCapacity;
        }

    }

}
