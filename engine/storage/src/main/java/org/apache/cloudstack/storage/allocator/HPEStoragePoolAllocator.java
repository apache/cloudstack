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
package org.apache.cloudstack.storage.allocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cloud.storage.VolumeApiServiceImpl;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

/**
 * HPE Storage Pool Allocator
 *
 * This allocator is specifically designed for HPE storage systems and filters
 * storage pools based on the storage provider name being "Primera". It extends the
 * AbstractStoragePoolAllocator to provide specialized allocation logic for
 * HPE storage infrastructure.
 *
 * Key features:
 * - Filters pools by HPE storage provider name
 * - Supports cluster-scoped storage allocation
 * - Handles storage tags for pool selection
 * - Implements proper fallback mechanism when no suitable pools are found
 *
 * @author CloudStack Development Team
 */
@Component
public class HPEStoragePoolAllocator extends AbstractStoragePoolAllocator {

    /**
     * Selects suitable storage pools for volume allocation based on HPE storage criteria.
     *
     * This method implements the core allocation logic for HPE storage systems by:
     * 1. Validating storage type requirements (local vs shared)
     * 2. Finding pools that match deployment plan criteria (datacenter, pod, cluster)
     * 3. Filtering pools by storage tags if specified
     * 4. Selecting only pools with "HPE" as storage provider name
     * 5. Applying additional filtering based on capacity, compatibility, etc.
     *
     * @param dskCh Disk profile containing volume requirements and specifications
     * @param vmProfile Virtual machine profile for which storage is being allocated
     * @param plan Deployment plan specifying datacenter, pod, and cluster constraints
     * @param avoid List of storage pools to exclude from allocation
     * @param returnUpTo Maximum number of storage pools to return
     * @param bypassStorageTypeCheck Flag to bypass local/shared storage type validation
     * @param keyword Additional keyword filter for pool selection
     * @return List of suitable HPE storage pools, or null if no suitable pools found
     */
    @Override
    protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck, String keyword) {
        // Log entry into HPE storage allocator with detailed parameters for debugging
        logger.debug("Starting pool selection process.");

        // Log the start of search using parent class logging method
        logStartOfSearch(dskCh, vmProfile, plan, returnUpTo, bypassStorageTypeCheck);

        // Early return if local storage is required but storage type check is not bypassed
        // HPE allocator is designed for shared storage, so we skip local storage requests
        if (!bypassStorageTypeCheck && dskCh.useLocalStorage()) {
            logger.debug("Skipping allocation as local storage is required but HPE allocator handles shared storage only.");
            return null;
        }

        // Initialize list to collect suitable storage pools
        List<StoragePool> suitablePools = new ArrayList<>();

        // Extract deployment constraints from the plan
        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();

        // Zone-wide storage is not supported by this allocator (requires pod-level scope)
        if (podId == null) {
            logger.debug("Returning null as pod ID is null. This may indicate zone-wide storage which is not supported by HPE allocator.");
            return null;
        }

        // Log search criteria based on whether storage tags are specified
        if (dskCh.getTags() != null && dskCh.getTags().length != 0) {
            logger.debug("Searching for HPE pools in datacenter [{}], pod [{}], cluster [{}] with storage tags [{}]. Disabled pools will be excluded.",
                         dcId, podId, clusterId, Arrays.toString(dskCh.getTags()));
        } else {
            logger.debug("Searching for HPE pools in datacenter [{}], pod [{}], cluster [{}] without specific storage tags. Disabled pools will be excluded.",
                         dcId, podId, clusterId);
        }

        // Log disabled pools if trace logging is enabled
        if (logger.isTraceEnabled()) {
            logDisabledStoragePools(dcId, podId, clusterId, ScopeType.CLUSTER);
        }

        // Find storage pools that match the deployment criteria and tags
        List<StoragePoolVO> pools = storagePoolDao.findPoolsByTags(dcId, podId, clusterId, ScopeType.CLUSTER, dskCh.getTags(), true, VolumeApiServiceImpl.storageTagRuleExecutionTimeout.value());
        pools.addAll(storagePoolJoinDao.findStoragePoolByScopeAndRuleTags(dcId, podId, clusterId, ScopeType.CLUSTER, List.of(dskCh.getTags())));

        logger.debug("Found {} candidate pools matching deployment criteria and tags [{}]: {}",
                     pools.size(), Arrays.toString(dskCh.getTags()), pools);

        // Add remaining pools in cluster that didn't match tags to the avoid set
        // This ensures they won't be considered by subsequent allocators either
        List<StoragePoolVO> allPools = storagePoolDao.findPoolsByTags(dcId, podId, clusterId, ScopeType.CLUSTER, null, false, 0);
        allPools.removeAll(pools);
        for (StoragePoolVO pool : allPools) {
            logger.trace("Adding pool [{}] to avoid set as it did not match required storage tags.", pool);
            avoid.addPool(pool.getId());
        }

        // Early return if no candidate pools were found
        if (pools.isEmpty()) {
            logger.debug("No storage pools available for HPE storage allocation in the specified scope.");
            return null; // Return null to allow other allocators to attempt allocation
        }

        // Iterate through candidate pools and filter by HPE storage provider name
        for (StoragePoolVO pool : pools) {
            // Stop if we've reached the maximum number of pools to return
            if (suitablePools.size() == returnUpTo) {
                logger.debug("Reached maximum number of pools to return ({}), stopping search.", returnUpTo);
                break;
            }

            // Filter by Primera storage provider name (exact case match)
            if ("Primera".equals(pool.getStorageProviderName())) {
                StoragePool storagePool = (StoragePool)dataStoreMgr.getPrimaryDataStore(pool.getId());

                // Apply comprehensive filtering (capacity, compatibility, maintenance status, etc.)
                if (filter(avoid, storagePool, dskCh, plan)) {
                    logger.debug("Found suitable HPE storage pool [{}] with provider [{}] for disk allocation [{}]. Adding to candidate list.",
                                 pool.getName(), pool.getStorageProviderName(), dskCh);
                    suitablePools.add(storagePool);
                } else {
                    logger.debug("HPE storage pool [{}] did not pass filtering checks for disk allocation [{}]. Adding to avoid set.",
                                 pool.getName(), dskCh);
                    avoid.addPool(pool.getId());
                }
            } else {
                // Log pools that don't match HPE provider name for debugging purposes
                logger.debug("Storage pool [{}] with provider [{}] is not an HPE storage pool. Skipping.",
                             pool.getName(), pool.getStorageProviderName());
            }
        }

        // Final check: if no HPE pools were found after filtering, return null
        // This allows other allocators in the chain to attempt allocation
        if (suitablePools.isEmpty()) {
            logger.debug("No HPE storage pools found after filtering. All candidate pools had different storage providers. Returning null to allow other allocators to attempt allocation.");
            return null;
        }

        // Log successful completion with final pool count
        logEndOfSearch(suitablePools);
        return suitablePools;
    }
}
