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
package com.cloud.vm;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

/**
 * Works out the overcommit ratio a VM runs at - its service offering's if it sets one, otherwise its
 * cluster's - and records it on the VM, which is what capacity accounting and the hypervisor then read.
 */
public class VirtualMachineManagerOverCommit {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    public void updateOverCommitRatioForVmProfile(VirtualMachineProfile vmProfile, long clusterId) {
        final ClusterDetailsVO clusterDetailCpu = clusterDetailsDao.findDetail(clusterId, VmDetailConstants.CPU_OVER_COMMIT_RATIO);
        final ClusterDetailsVO clusterDetailRam = clusterDetailsDao.findDetail(clusterId, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO);
        final float clusterCpuRatio = Float.parseFloat(clusterDetailCpu.getValue());
        final float clusterRamRatio = Float.parseFloat(clusterDetailRam.getValue());

        final Float offeringCpuRatio = offeringOverCommitRatio(vmProfile, VmDetailConstants.CPU_OVER_COMMIT_RATIO, clusterCpuRatio);
        final Float offeringRamRatio = offeringOverCommitRatio(vmProfile, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, clusterRamRatio);

        final float cpuRatio = offeringCpuRatio != null ? offeringCpuRatio : clusterCpuRatio;
        final float ramRatio = offeringRamRatio != null ? offeringRamRatio : clusterRamRatio;

        persistOverCommitRatio(vmProfile.getId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO, cpuRatio, clusterCpuRatio);
        persistOverCommitRatio(vmProfile.getId(), VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, ramRatio, clusterRamRatio);
        persistMemoryReclaimFlag(vmProfile.getId(), offeringRamRatio);

        vmProfile.setCpuOvercommitRatio(cpuRatio);
        vmProfile.setMemoryOvercommitRatio(ramRatio);
    }

    /**
     * The overcommit ratio a VM's service offering asks for, or null when it does not ask for one
     * and should inherit its cluster's.
     *
     * An offering setting a ratio of 1 on an overcommitted cluster is how infrastructure VMs are
     * kept off the overcommit: they are charged their full request and hold all of it.
     */
    protected Float offeringOverCommitRatio(VirtualMachineProfile vmProfile, String key, float clusterRatio) {
        String offeringRatio = serviceOfferingDetailsDao.getDetail(vmProfile.getServiceOfferingId(), key);
        if (offeringRatio == null) {
            return null;
        }
        float ratio;
        try {
            ratio = Float.parseFloat(offeringRatio);
        } catch (NumberFormatException e) {
            logger.warn("Ignoring {} of [{}] on service offering {}: it is not a number.",
                    key, offeringRatio, vmProfile.getServiceOfferingId());
            return null;
        }
        if (ratio < 1) {
            // rejected when the offering is created; only reachable by writing the detail directly
            logger.warn("Ignoring {} of {} on service offering {}: it must be at least 1.",
                    key, offeringRatio, vmProfile.getServiceOfferingId());
            return null;
        }
        if (ratio > clusterRatio) {
            logger.info("Service offering {} declares {} of {}, more than its cluster's {}, so its VMs are "
                            + "oversubscribed further than the cluster default.",
                    vmProfile.getServiceOfferingId(), key, ratio, clusterRatio);
        }
        return ratio;
    }

    /**
     * Turns off returning the guest's unused pages to the host, for a VM whose offering has
     * explicitly opted out of overcommit. Otherwise the exemption would hold in the books only.
     *
     * Only an explicit choice counts. An offering that says nothing inherits its cluster, and a
     * cluster with no overcommit configured is the default everywhere - reading that as "pin the
     * memory of every VM" would change behaviour for installations that opted into nothing.
     *
     * @param offeringRatio
     *         the ratio the offering asked for, or null if it asked for nothing
     */
    protected void persistMemoryReclaimFlag(long vmId, Float offeringRatio) {
        boolean disable = offeringRatio != null && offeringRatio <= 1f;
        VMInstanceDetailVO existing = vmInstanceDetailsDao.findDetail(vmId, VmDetailConstants.MEMORY_RECLAIM_DISABLED);
        if (disable && existing == null) {
            vmInstanceDetailsDao.addDetail(vmId, VmDetailConstants.MEMORY_RECLAIM_DISABLED, "true", true);
        } else if (!disable && existing != null) {
            vmInstanceDetailsDao.removeDetail(vmId, VmDetailConstants.MEMORY_RECLAIM_DISABLED);
        }
    }

    /**
     * Records the ratio the VM is running at.
     *
     * Capacity is counted in cluster-overcommitted units, and the charge scales a VM by this detail,
     * so it has to be present whenever the VM's ratio differs from its cluster's - otherwise the VM
     * is charged as though it shared the cluster's ratio. It is also kept while the cluster is
     * overcommitted at all, so that capacity accounting survives the cluster ratio being changed
     * beneath a running VM.
     */
    protected void persistOverCommitRatio(long vmId, String key, float ratio, float clusterRatio) {
        boolean needed = ratio != clusterRatio || clusterRatio > 1f;
        VMInstanceDetailVO existing = vmInstanceDetailsDao.findDetail(vmId, key);
        if (!needed) {
            if (existing != null) {
                vmInstanceDetailsDao.removeDetail(vmId, key);
            }
            return;
        }
        if (existing == null || parseRatioOrDefault(existing.getValue(), Float.NaN) != ratio) {
            vmInstanceDetailsDao.addDetail(vmId, key, String.valueOf(ratio), true);
        }
    }

    private float parseRatioOrDefault(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }
}
