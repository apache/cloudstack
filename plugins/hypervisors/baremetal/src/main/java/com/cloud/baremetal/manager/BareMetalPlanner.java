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

import com.cloud.utils.NumbersUtil;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityManager;
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
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public class BareMetalPlanner extends AdapterBase implements DeploymentPlanner {
    private static final Logger s_logger = Logger.getLogger(BareMetalPlanner.class);
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected HostPodDao _podDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected HostDao _hostDao;
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

        if (vm.getLastHostId() != null && haVmTag == null) {
            HostVO h = _hostDao.findById(vm.getLastHostId());
            DataCenter dc = _dcDao.findById(h.getDataCenterId());
            Pod pod = _podDao.findById(h.getPodId());
            Cluster c = _clusterDao.findById(h.getClusterId());
            s_logger.debug("Start baremetal vm " + vm.getId() + " on last stayed host " + h.getId());
            return new DeployDestination(dc, pod, c, h);
        }

        if (haVmTag != null) {
            hostTag = haVmTag;
        } else if (offering.getHostTag() != null) {
            String[] tags = offering.getHostTag().split(",");
            if (tags.length > 0) {
                hostTag = tags[0];
            }
        }

        List<ClusterVO> clusters = _clusterDao.listByDcHyType(vm.getDataCenterId(), HypervisorType.BareMetal.toString());
        int cpu_requested;
        long ram_requested;
        HostVO target = null;
        List<HostVO> hosts;
        for (ClusterVO cluster : clusters) {
            hosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());
            if (hostTag != null) {
                for (HostVO h : hosts) {
                    _hostDao.loadDetails(h);
                    if (h.getDetail("hostTag") != null && h.getDetail("hostTag").equalsIgnoreCase(hostTag)) {
                        target = h;
                        break;
                    }
                }
            }
        }

        if (target == null) {
            s_logger.warn("Cannot find host with tag " + hostTag + " use capacity from service offering");
            cpu_requested = offering.getCpu() * offering.getSpeed();
            ram_requested = offering.getRamSize() * 1024L * 1024L;
        } else {
            cpu_requested = target.getCpus() * target.getSpeed().intValue();
            ram_requested = target.getTotalMemory();
        }

        for (ClusterVO cluster : clusters) {
            if (haVmTag == null) {
                hosts = _resourceMgr.listAllUpAndEnabledNonHAHosts(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());
            } else {
                s_logger.warn("Cannot find HA host with tag " + haVmTag + " in cluster id=" + cluster.getId() + ", pod id=" + cluster.getPodId() + ", data center id=" +
                    cluster.getDataCenterId());
                return null;
            }
            for (HostVO h : hosts) {
                long cluster_id = h.getClusterId();
                ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster_id, "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster_id, "memoryOvercommitRatio");
                Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());

                if (_capacityMgr.checkIfHostHasCapacity(h.getId(), cpu_requested, ram_requested, false, cpuOvercommitRatio, memoryOvercommitRatio, true)) {
                    s_logger.debug("Find host " + h.getId() + " has enough capacity");
                    DataCenter dc = _dcDao.findById(h.getDataCenterId());
                    Pod pod = _podDao.findById(h.getPodId());
                    return new DeployDestination(dc, pod, cluster, h);
                }
            }
        }

        s_logger.warn(String.format("Cannot find enough capacity(requested cpu=%1$s memory=%2$s)", cpu_requested, NumbersUtil.toHumanReadableSize(ram_requested)));
        return null;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) {
        return vm.getHypervisorType() == HypervisorType.BareMetal;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
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
