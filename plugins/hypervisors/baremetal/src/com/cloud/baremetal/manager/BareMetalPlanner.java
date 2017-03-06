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

import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
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
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

@Local(value = DeploymentPlanner.class)
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
    protected ClusterDetailsDao _clusterDetailsDao;

    private boolean markHostAsUsed(HostVO host, VirtualMachineProfile vm) {
        // Note: if vm fails to start, the cleanup code is in
        // BaremetalManagerImpl.postStateTransitionEvent
        _hostDao.acquireInLockTable(host.getId(), 600);
        try {
            if (host.getDetail("vmName") != null) {
                return false;
            }

            host.setDetail("vmName", vm.getInstanceName());
            _hostDao.saveDetails(host);
            return true;
        } finally {
            _hostDao.releaseFromLockTable(host.getId());
        }
    }

    @Override
    public DeployDestination plan(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();

        if (vm.getLastHostId() != null) {
            HostVO h = _hostDao.findById(vm.getLastHostId());
            DataCenter dc = _dcDao.findById(h.getDataCenterId());
            Pod pod = _podDao.findById(h.getPodId());
            Cluster c = _clusterDao.findById(h.getClusterId());
            s_logger.debug("Start baremetal vm " + vm.getId() + " on last stayed host " + h.getId());
            return new DeployDestination(dc, pod, c, h);
        }

        if (offering.getHostTag() == null) {
            throw new CloudRuntimeException("baremetal computing offering must have a host tag");
        }

        String[] tags = offering.getHostTag().split(",");
        if (tags.length == 0) {
            throw new CloudRuntimeException("baremetal computing offering must have a host tag");
        }

        String hostTag = tags[0];
        List<ClusterVO> clusters = _clusterDao.listByDcHyType(vm.getDataCenterId(), HypervisorType.BareMetal.toString());
        List<HostVO> hosts;
        for (ClusterVO cluster : clusters) {
            hosts = _hostDao.listByHostTag(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId(), hostTag);
            for (HostVO h : hosts) {
                _hostDao.loadDetails(h);
                if (h.getDetail("vmName") != null) {
                    s_logger.debug(String.format("skip baremetal host[id:%s] as it already has vm[%s]", h.getId(), h.getDetail("vmName")));
                    continue;
                }

                s_logger.debug("Found host " + h.getId() + " has enough capacity");
                DataCenter dc = _dcDao.findById(h.getDataCenterId());
                Pod pod = _podDao.findById(h.getPodId());
                if (!markHostAsUsed(h, vmProfile)) {
                    s_logger.debug(String.format("failed to take host[id:%s], someone else took it; let's find another one", h.getId()));
                    continue;
                }
                return new DeployDestination(dc, pod, cluster, h);
            }
        }

        s_logger.warn("Cannot find host with tag " + hostTag + " use capacity from service offering");
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
