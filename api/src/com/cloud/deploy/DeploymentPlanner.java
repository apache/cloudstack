/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

/**
 * 
 */
package com.cloud.deploy;

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
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * Returns a deployment destination for the VM.
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
    DeployDestination plan(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException;

    /**
     * check() is called right before the virtual machine starts to make sure
     * the host has enough capacity.
     * 
     * @param vm
     *            virtual machine in question.
     * @param plan
     *            deployment plan used to determined the deploy destination.
     * @param dest
     *            destination returned by plan.
     * @param avoid
     *            what to avoid.
     * @return true if it's okay to start; false if not. If false, the exclude list will include what should be
     *         excluded.
     */
    boolean check(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, DeployDestination dest, ExcludeList exclude);

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
    boolean canHandle(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid);

    public enum AllocationAlgorithm {
        random,
        firstfit,
        userdispersing,
        userconcentratedpod_random,
        userconcentratedpod_firstfit;
    }

    public static class ExcludeList {
        private Set<Long> _dcIds;
        private Set<Long> _podIds;
        private Set<Long> _clusterIds;
        private Set<Long> _hostIds;
        private Set<Long> _poolIds;

        public ExcludeList() {
        }

        public ExcludeList(Set<Long> _dcIds, Set<Long> _podIds, Set<Long> _clusterIds, Set<Long> _hostIds, Set<Long> _poolIds) {
            this._dcIds = _dcIds;
            this._podIds = _podIds;
            this._clusterIds = _clusterIds;
            this._poolIds = _poolIds;
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

        public void addCluster(long clusterId) {
            if (_clusterIds == null) {
                _clusterIds = new HashSet<Long>();
            }
            _clusterIds.add(clusterId);
        }

        public void addHost(long hostId) {
            if (_hostIds == null) {
                _hostIds = new HashSet<Long>();
            }
            _hostIds.add(hostId);
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
