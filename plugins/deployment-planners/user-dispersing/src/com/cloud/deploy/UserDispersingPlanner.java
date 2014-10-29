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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = DeploymentPlanner.class)
public class UserDispersingPlanner extends FirstFitPlanner implements DeploymentClusterPlanner {

    private static final Logger s_logger = Logger.getLogger(UserDispersingPlanner.class);

    /**
     * This method should reorder the given list of Cluster Ids by applying any necessary heuristic
     * for this planner
     * For UserDispersingPlanner we need to order the clusters by considering the number of VMs for this account
     * @return List<Long> ordered list of Cluster Ids
     */
    @Override
    protected List<Long> reorderClusters(long id, boolean isZone, Pair<List<Long>, Map<Long, Double>> clusterCapacityInfo, VirtualMachineProfile vmProfile,
        DeploymentPlan plan) {
        List<Long> clusterIdsByCapacity = clusterCapacityInfo.first();
        if (vmProfile.getOwner() == null) {
            return clusterIdsByCapacity;
        }
        long accountId = vmProfile.getOwner().getAccountId();
        Pair<List<Long>, Map<Long, Double>> clusterIdsVmCountInfo = listClustersByUserDispersion(id, isZone, accountId);

        //now we have 2 cluster lists - one ordered by capacity and the other by number of VMs for this account
        //need to apply weights to these to find the correct ordering to follow

        if (_userDispersionWeight == 1.0f) {
            List<Long> clusterIds = clusterIdsVmCountInfo.first();
            clusterIds.retainAll(clusterIdsByCapacity);
            return clusterIds;
        } else {
            //apply weights to the two lists
            return orderByApplyingWeights(clusterCapacityInfo, clusterIdsVmCountInfo, accountId);
        }

    }

    /**
     * This method should reorder the given list of Pod Ids by applying any necessary heuristic
     * for this planner
     * For UserDispersingPlanner we need to order the pods by considering the number of VMs for this account
     * @return List<Long> ordered list of Pod Ids
     */
    @Override
    protected List<Long> reorderPods(Pair<List<Long>, Map<Long, Double>> podCapacityInfo, VirtualMachineProfile vmProfile, DeploymentPlan plan) {
        List<Long> podIdsByCapacity = podCapacityInfo.first();
        if (vmProfile.getOwner() == null) {
            return podIdsByCapacity;
        }
        long accountId = vmProfile.getOwner().getAccountId();

        Pair<List<Long>, Map<Long, Double>> podIdsVmCountInfo = listPodsByUserDispersion(plan.getDataCenterId(), accountId);

        //now we have 2 pod lists - one ordered by capacity and the other by number of VMs for this account
        //need to apply weights to these to find the correct ordering to follow

        if (_userDispersionWeight == 1.0f) {
            List<Long> podIds = podIdsVmCountInfo.first();
            podIds.retainAll(podIdsByCapacity);
            return podIds;
        } else {
            //apply weights to the two lists
            return orderByApplyingWeights(podCapacityInfo, podIdsVmCountInfo, accountId);
        }

    }

    protected Pair<List<Long>, Map<Long, Double>> listClustersByUserDispersion(long id, boolean isZone, long accountId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Applying Userdispersion heuristic to clusters for account: " + accountId);
        }
        Pair<List<Long>, Map<Long, Double>> clusterIdsVmCountInfo;
        if (isZone) {
            clusterIdsVmCountInfo = _vmInstanceDao.listClusterIdsInZoneByVmCount(id, accountId);
        } else {
            clusterIdsVmCountInfo = _vmInstanceDao.listClusterIdsInPodByVmCount(id, accountId);
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("List of clusters in ascending order of number of VMs: " + clusterIdsVmCountInfo.first());
        }
        return clusterIdsVmCountInfo;
    }

    protected Pair<List<Long>, Map<Long, Double>> listPodsByUserDispersion(long dataCenterId, long accountId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Applying Userdispersion heuristic to pods for account: " + accountId);
        }
        Pair<List<Long>, Map<Long, Double>> podIdsVmCountInfo = _vmInstanceDao.listPodIdsInZoneByVmCount(dataCenterId, accountId);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("List of pods in ascending order of number of VMs: " + podIdsVmCountInfo.first());
        }

        return podIdsVmCountInfo;
    }

    private List<Long> orderByApplyingWeights(Pair<List<Long>, Map<Long, Double>> capacityInfo, Pair<List<Long>, Map<Long, Double>> vmCountInfo, long accountId) {
        List<Long> capacityOrderedIds = capacityInfo.first();
        List<Long> vmCountOrderedIds = vmCountInfo.first();
        Map<Long, Double> capacityMap = capacityInfo.second();
        Map<Long, Double> vmCountMap = vmCountInfo.second();

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Capacity Id list: " + capacityOrderedIds + " , capacityMap:" + capacityMap);
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Vm Count Id list: " + vmCountOrderedIds + " , vmCountMap:" + vmCountMap);
        }

        List<Long> idsReorderedByWeights = new ArrayList<Long>();
        float capacityWeight = (1.0f - _userDispersionWeight);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Applying userDispersionWeight: " + _userDispersionWeight);
        }
        //normalize the vmCountMap
        LinkedHashMap<Long, Double> normalisedVmCountIdMap = new LinkedHashMap<Long, Double>();

        Long totalVmsOfAccount = _vmInstanceDao.countRunningByAccount(accountId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Total VMs for account: " + totalVmsOfAccount);
        }
        for (Long id : vmCountOrderedIds) {
            Double normalisedCount = vmCountMap.get(id) / totalVmsOfAccount;
            normalisedVmCountIdMap.put(id, normalisedCount);
        }

        //consider only those ids that are in capacity map.

        SortedMap<Double, List<Long>> sortedMap = new TreeMap<Double, List<Long>>();
        for (Long id : capacityOrderedIds) {
            Double weightedCapacityValue = capacityMap.get(id) * capacityWeight;
            Double weightedVmCountValue = normalisedVmCountIdMap.get(id) * _userDispersionWeight;
            Double totalWeight = weightedCapacityValue + weightedVmCountValue;
            if (sortedMap.containsKey(totalWeight)) {
                List<Long> idList = sortedMap.get(totalWeight);
                idList.add(id);
                sortedMap.put(totalWeight, idList);
            } else {
                List<Long> idList = new ArrayList<Long>();
                idList.add(id);
                sortedMap.put(totalWeight, idList);
            }
        }

        for (List<Long> idList : sortedMap.values()) {
            idsReorderedByWeights.addAll(idList);
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Reordered Id list: " + idsReorderedByWeights);
        }

        return idsReorderedByWeights;
    }

    float _userDispersionWeight;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        String weight = _configDao.getValue(Config.VmUserDispersionWeight.key());
        _userDispersionWeight = NumbersUtil.parseFloat(weight, 1.0f);

        return true;
    }

}
