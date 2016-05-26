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
package com.cloud.baremetal.manager;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;

public class BareMetalPlanner extends AdapterBase implements DeploymentPlanner {
    private static final Logger s_logger = Logger.getLogger(BareMetalPlanner.class);

    //If true, the bare metal planner will ignore tagged hosts
    //when using untagged service offerings.
    //Tagged hosts will still be used for tagged offerings.
    private boolean _exclusiveMode;

    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected HostPodDao _podDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    protected CapacityManager _capacityMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected ClusterDetailsDao _clusterDetailsDao;

    @Override
    public DeployDestination plan(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();
        String hostTag = null;

        String haVmTag = (String)vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);

        //If we have a last host ID, use that.
        if (vm.getLastHostId() != null && haVmTag == null) {
            HostVO h = _hostDao.findById(vm.getLastHostId());
            DataCenter dc = _dcDao.findById(h.getDataCenterId());
            Pod pod = _podDao.findById(h.getPodId());
            Cluster c = _clusterDao.findById(h.getClusterId());
            s_logger.debug("Start baremetal vm " + vm.getId() + " on last stayed host " + h.getId());
            return new DeployDestination(dc, pod, c, h);
        }

        //If there is an HA tag, set the host tag to that. Otherwise load any normal host tags if they exist.
        //Only the first tag is used.
        if (haVmTag != null) {
            hostTag = haVmTag;
        } else if (offering.getHostTag() != null) {
            String[] tags = offering.getHostTag().split(",");
            if (tags.length > 0) {
                hostTag = tags[0];
            }
        }

        //First loop through the clusters and check if there is a tagged host that fits our tag.
        //Used to determine requested CPU and RAM.
        List<ClusterVO> clusters = _clusterDao.listByDcHyType(vm.getDataCenterId(), HypervisorType.BareMetal.toString());
        int cpu_requested;
        long ram_requested;
        HostVO target = null;
        List<HostVO> hosts;
        for (ClusterVO cluster : clusters) {
            hosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());
            if (hostTag != null) {
                outer:
                for (HostVO h : hosts) {
                    _hostDao.loadHostTags(h);

                    //If only there was a .containsIgnoreCase...
                    if (h.getHostTags() != null) {
                        boolean foundTag = false;
                        for (String tag : h.getHostTags()) {
                            if (tag.equalsIgnoreCase(hostTag)) {
                                foundTag = true;
                                break;
                            }
                        }

                        if (foundTag) {
                            target = h;
                            break outer;
                        }
                    }
                }
            }
        }

        //Fall back to getting the requested CPU/RAM from offering if we did not find a tagged host.
        if (target == null) {
            s_logger.warn("Cannot find host with tag " + hostTag + " use capacity from service offering");
            cpu_requested = offering.getCpu() * offering.getSpeed();
            ram_requested = offering.getRamSize() * 1024L * 1024L;
        } else {
            cpu_requested = target.getCpus() * target.getSpeed().intValue();
            ram_requested = target.getTotalMemory();
        }

        //Now really pick a target host.
        for (ClusterVO cluster : clusters) {
            if (haVmTag == null) {
                hosts = _resourceMgr.listAllUpAndEnabledNonHAHosts(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());
            } else {
                s_logger.warn("Cannot find HA host with tag " + haVmTag + " in cluster id=" + cluster.getId() + ", pod id=" + cluster.getPodId() + ", data center id=" + cluster.getDataCenterId());
                return null;
            }

            //Are we looking for tagged hosts? Check with target (verified at least one tagged host exists)
            boolean useTagged = false;
            if (target != null) {
                s_logger.info("Host tag " + hostTag + " was specified. Using only tagged hosts.");
                hosts = _hostDao.listByHostTag(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId(), hostTag);
                useTagged = true;
            }

            //Loop through the host untli we find one with capacity, then return it.
            for (HostVO h : hosts) {
                long cluster_id = h.getClusterId();
                ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster_id, "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster_id, "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());

                //Untagged offerings: If exclusive mode is on and this host has a tag,
                //ignore it.
                if (!useTagged) {
                    if (_exclusiveMode && h.getDetail("hostTag") != null) {
                        continue;
                    }
                }

                if (avoid.shouldAvoid(h)) {
                    s_logger.info("Host " + h.getId() + " is in avoid set. Ignoring.");
                    continue;
                }

                //Check if there is already something on the VM.
                List<VMInstanceVO> existingVMs = _vmDao.listByHostId(h.getId());
                if (existingVMs.size() > 0) {
                    s_logger.info("Bare metal host " + h.getId() + " has something running on it already. Adding to avoid set.");
                    avoid.addHost(h.getId());
                    continue;
                }

                if (_capacityMgr.checkIfHostHasCapacity(h.getId(), cpu_requested, ram_requested, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    s_logger.debug("Find host " + h.getId() + " has enough capacity");
                    DataCenter dc = _dcDao.findById(h.getDataCenterId());
                    Pod pod = _podDao.findById(h.getPodId());
                    return new DeployDestination(dc, pod, cluster, h);
                }
            }
        }

        s_logger.warn(String.format("Cannot find enough capacity(requested cpu=%1$s memory=%2$s)", cpu_requested, ram_requested));
        return null;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) {
        return vm.getHypervisorType() == HypervisorType.BareMetal;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String exclusive = _configDao.getValue(Config.BaremetalDeploymentPlannerExclusive.key());

        if (exclusive != null) {
            _exclusiveMode = Boolean.parseBoolean(exclusive);
        }
        else {
            _exclusiveMode = false;
        }

        s_logger.info("Baremetal planner exclusive mode = " + _exclusiveMode);
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
