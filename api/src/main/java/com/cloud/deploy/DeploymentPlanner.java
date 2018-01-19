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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.org.Cluster;
import com.cloud.storage.StoragePool;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachineProfile;

/**
 */
public interface DeploymentPlanner extends Adapter {

    /**
     * plan is called to determine where a virtual machine should be running.
     *
     * @param vm
     *            virtual machine.
     * @param plan
     *            deployment plan that tells you where it's being deployed to.
     * @param avoid
     *            avoid these data centers, pods, clusters, or hosts.
     * @return DeployDestination for that virtual machine.
     */
    @Deprecated
    DeployDestination plan(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException;

    /**
     * canHandle is called before plan to determine if the plan can do the allocation. Planers should be exclusive so
     * planner writer must
     * make sure only one planer->canHandle return true in the planner list
     *
     * @param vm
     *            virtual machine.
     * @param plan
     *            deployment plan that tells you where it's being deployed to.
     * @param avoid
     *            avoid these data centers, pods, clusters, or hosts.
     * @return true if it's okay to allocate; false or not
     */
    boolean canHandle(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid);

    public enum AllocationAlgorithm {
        random, firstfit, userdispersing, userconcentratedpod_random, userconcentratedpod_firstfit;
    }

    public enum PlannerResourceUsage {
        Shared, Dedicated;
    }

    public static class ExcludeList implements Serializable {
        private static final long serialVersionUID = -482175549460148301L;

        private Set<Long> _dcIds;
        private Set<Long> _podIds;
        private Set<Long> _clusterIds;
        private Set<Long> _hostIds;
        private Set<Long> _poolIds;

        public ExcludeList() {
        }

        public ExcludeList(Set<Long> dcIds, Set<Long> podIds, Set<Long> clusterIds, Set<Long> hostIds, Set<Long> poolIds) {
            if (dcIds != null) {
                this._dcIds = new HashSet<Long>(dcIds);
            }
            if (podIds != null) {
                this._podIds = new HashSet<Long>(podIds);
            }
            if (clusterIds != null) {
                this._clusterIds = new HashSet<Long>(clusterIds);
            }

            if (hostIds != null) {
                this._hostIds = new HashSet<Long>(hostIds);
            }
            if (poolIds != null) {
                this._poolIds = new HashSet<Long>(poolIds);
            }
        }

        public boolean add(InsufficientCapacityException e) {
            Class<?> scope = e.getScope();

            if (scope == null) {
                return false;
            }

            if (Host.class.isAssignableFrom(scope)) {
                addHost(e.getId());
            } else if (Pod.class.isAssignableFrom(scope)) {
                addPod(e.getId());
            } else if (DataCenter.class.isAssignableFrom(scope)) {
                addDataCenter(e.getId());
            } else if (Cluster.class.isAssignableFrom(scope)) {
                addCluster(e.getId());
            } else if (StoragePool.class.isAssignableFrom(scope)) {
                addPool(e.getId());
            } else {
                return false;
            }

            return true;
        }

        public boolean add(ResourceUnavailableException e) {
            Class<?> scope = e.getScope();

            if (scope == null) {
                return false;
            }

            if (Host.class.isAssignableFrom(scope)) {
                addHost(e.getResourceId());
            } else if (Pod.class.isAssignableFrom(scope)) {
                addPod(e.getResourceId());
            } else if (DataCenter.class.isAssignableFrom(scope)) {
                addDataCenter(e.getResourceId());
            } else if (Cluster.class.isAssignableFrom(scope)) {
                addCluster(e.getResourceId());
            } else if (StoragePool.class.isAssignableFrom(scope)) {
                addPool(e.getResourceId());
            } else {
                return false;
            }

            return true;
        }

        public void addPool(long poolId) {
            if (_poolIds == null) {
                _poolIds = new HashSet<Long>();
            }
            _poolIds.add(poolId);
        }

        public void addDataCenter(long dataCenterId) {
            if (_dcIds == null) {
                _dcIds = new HashSet<Long>();
            }
            _dcIds.add(dataCenterId);
        }

        public void addPod(long podId) {
            if (_podIds == null) {
                _podIds = new HashSet<Long>();
            }
            _podIds.add(podId);
        }

        public void addPodList(Collection<Long> podList) {
            if (_podIds == null) {
                _podIds = new HashSet<Long>();
            }
            _podIds.addAll(podList);
        }

        public void addCluster(long clusterId) {
            if (_clusterIds == null) {
                _clusterIds = new HashSet<Long>();
            }
            _clusterIds.add(clusterId);
        }

        public void addClusterList(Collection<Long> clusterList) {
            if (_clusterIds == null) {
                _clusterIds = new HashSet<Long>();
            }
            _clusterIds.addAll(clusterList);
        }

        public void addHost(long hostId) {
            if (_hostIds == null) {
                _hostIds = new HashSet<Long>();
            }
            _hostIds.add(hostId);
        }

        public void addHostList(Collection<Long> hostList) {
            if (_hostIds == null) {
                _hostIds = new HashSet<Long>();
            }
            _hostIds.addAll(hostList);
        }

        public boolean shouldAvoid(Host host) {
            if (_dcIds != null && _dcIds.contains(host.getDataCenterId())) {
                return true;
            }

            if (_podIds != null && _podIds.contains(host.getPodId())) {
                return true;
            }

            if (_clusterIds != null && _clusterIds.contains(host.getClusterId())) {
                return true;
            }

            if (_hostIds != null && _hostIds.contains(host.getId())) {
                return true;
            }

            return false;
        }

        public boolean shouldAvoid(Cluster cluster) {
            if (_dcIds != null && _dcIds.contains(cluster.getDataCenterId())) {
                return true;
            }

            if (_podIds != null && _podIds.contains(cluster.getPodId())) {
                return true;
            }

            if (_clusterIds != null && _clusterIds.contains(cluster.getId())) {
                return true;
            }
            return false;
        }

        public boolean shouldAvoid(Pod pod) {
            if (_dcIds != null && _dcIds.contains(pod.getDataCenterId())) {
                return true;
            }

            if (_podIds != null && _podIds.contains(pod.getId())) {
                return true;
            }

            return false;
        }

        public boolean shouldAvoid(StoragePool pool) {
            if (_dcIds != null && _dcIds.contains(pool.getDataCenterId())) {
                return true;
            }

            if (_podIds != null && _podIds.contains(pool.getPodId())) {
                return true;
            }

            if (_clusterIds != null && _clusterIds.contains(pool.getClusterId())) {
                return true;
            }

            if (_poolIds != null && _poolIds.contains(pool.getId())) {
                return true;
            }

            return false;
        }

        public boolean shouldAvoid(DataCenter dc) {
            if (_dcIds != null && _dcIds.contains(dc.getId())) {
                return true;
            }
            return false;
        }

        public Set<Long> getDataCentersToAvoid() {
            return _dcIds;
        }

        public Set<Long> getPodsToAvoid() {
            return _podIds;
        }

        public Set<Long> getClustersToAvoid() {
            return _clusterIds;
        }

        public Set<Long> getHostsToAvoid() {
            return _hostIds;
        }

        public Set<Long> getPoolsToAvoid() {
            return _poolIds;
        }
    }
}
