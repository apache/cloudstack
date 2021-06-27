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
package com.cloud.storage;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class StorageUtil {
    @Inject private ClusterDao clusterDao;
    @Inject private HostDao hostDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private VMInstanceDao vmInstanceDao;
    @Inject private VolumeDao volumeDao;

    private Long getClusterId(Long hostId) {
        if (hostId == null) {
            return null;
        }

        HostVO hostVO = hostDao.findById(hostId);

        if (hostVO == null) {
            return null;
        }

        return hostVO.getClusterId();
    }

    /**
     * This method relates to managed storage only. CloudStack currently supports managed storage with XenServer, vSphere, and KVM.
     * With managed storage on XenServer and vSphere, CloudStack needs to use an iSCSI SR (XenServer) or datastore (vSphere) per CloudStack
     * volume. Since XenServer and vSphere are limited to the hundreds with regards to how many SRs or datastores can be leveraged per
     * compute cluster, this method is used to check a Global Setting (that specifies the maximum number of SRs or datastores per compute cluster)
     * against what is being requested. KVM does not apply here here because it does not suffer from the same scalability limits as XenServer and
     * vSphere do. With XenServer and vSphere, each host is configured to see all the SRs/datastores of the cluster. With KVM, each host typically
     * is only configured to see the managed volumes of the VMs that are currently running on that host.
     *
     * If the clusterId is passed in, we use it. Otherwise, we use the hostId. If neither leads to a cluster, we just return true.
     */
    public boolean managedStoragePoolCanScale(StoragePool storagePool, Long clusterId, Long hostId) {
        if (clusterId == null) {
            clusterId = getClusterId(hostId);

            if (clusterId == null) {
                return true;
            }
        }

        ClusterVO clusterVO = clusterDao.findById(clusterId);

        if (clusterVO == null) {
            return true;
        }

        Hypervisor.HypervisorType hypervisorType = clusterVO.getHypervisorType();

        if (hypervisorType == null) {
            return true;
        }

        if (Hypervisor.HypervisorType.XenServer.equals(hypervisorType) || Hypervisor.HypervisorType.VMware.equals(hypervisorType)) {
            int maxValue = StorageManager.MaxNumberOfManagedClusteredFileSystems.valueIn(clusterId);

            return getNumberOfManagedClusteredFileSystemsInComputeCluster(storagePool.getDataCenterId(), clusterId) < maxValue;
        }

        return true;
    }

    private int getNumberOfManagedClusteredFileSystemsInComputeCluster(long zoneId, long clusterId) {
        int numberOfManagedClusteredFileSystemsInComputeCluster = 0;

        List<VolumeVO> volumes = volumeDao.findByDc(zoneId);

        if (CollectionUtils.isEmpty(volumes)) {
            return numberOfManagedClusteredFileSystemsInComputeCluster;
        }

        for (VolumeVO volume : volumes) {
            Long instanceId = volume.getInstanceId();

            if (instanceId == null) {
                continue;
            }

            VMInstanceVO vmInstanceVO = vmInstanceDao.findById(instanceId);

            if (vmInstanceVO == null) {
                continue;
            }

            Long vmHostId = vmInstanceVO.getHostId();

            if (vmHostId == null) {
                vmHostId = vmInstanceVO.getLastHostId();
            }

            if (vmHostId == null) {
                continue;
            }

            HostVO vmHostVO = hostDao.findById(vmHostId);

            if (vmHostVO == null) {
                continue;
            }

            Long vmHostClusterId = vmHostVO.getClusterId();

            if (vmHostClusterId != null && vmHostClusterId == clusterId) {
                StoragePoolVO storagePoolVO = storagePoolDao.findById(volume.getPoolId());

                if (storagePoolVO != null && storagePoolVO.isManaged()) {
                    numberOfManagedClusteredFileSystemsInComputeCluster++;
                }
            }
        }

        return numberOfManagedClusteredFileSystemsInComputeCluster;
    }
}
