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
package com.cloud.agent.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.collections.CollectionUtils;

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

public class ExternalServerPlanner extends AdapterBase implements DeploymentPlanner {

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
    protected ResourceManager _resourceMgr;
    @Inject
    VMTemplateDetailsDao _tmpDetailsDao;

    @Override
    public DeployDestination plan(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();

        String haVmTag = (String)vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);

        if (vm.getLastHostId() != null) {
            HostVO h = _hostDao.findById(vm.getLastHostId());
            DataCenter dc = _dcDao.findById(h.getDataCenterId());
            Pod pod = _podDao.findById(h.getPodId());
            Cluster c = _clusterDao.findById(h.getClusterId());
            logger.debug(String.format("Start external VM instance %s on last used host %d", vm.getId(), h.getId()));
            return new DeployDestination(dc, pod, c, h);
        }

        String hostTag = null;
        if (haVmTag != null) {
            hostTag = haVmTag;
        } else if (offering.getHostTag() != null) {
            String[] tags = offering.getHostTag().split(",");
            if (tags.length > 0) {
                hostTag = tags[0];
            }
        }

        VMTemplateDetailVO provisionerDetail = _tmpDetailsDao.findDetail(vmProfile.getTemplateId(), ApiConstants.EXTERNAL_PROVISIONER);
        String provisioner = provisionerDetail.getValue();
        List<ClusterVO> clusters = _clusterDao.listByDatacenterExternalHypervisorProvisioner(vm.getDataCenterId(), provisioner);
        HostVO target = null;
        List<HostVO> hosts;
        for (ClusterVO cluster : clusters) {
            hosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());
            if (hostTag != null) {
                for (HostVO host : hosts) {
                    _hostDao.loadHostTags(host);
                    List<String> hostTags = host.getHostTags();
                    if (hostTags.contains(hostTag)) {
                        target = host;
                        break;
                    }
                }
            } else {
                if (CollectionUtils.isNotEmpty(hosts)) {
                    Collections.shuffle(hosts);
                    target = hosts.get(0);
                }
            }
        }

        if (target != null) {
            DataCenter dc = _dcDao.findById(target.getDataCenterId());
            Pod pod = _podDao.findById(target.getPodId());
            Cluster cluster = _clusterDao.findById(target.getClusterId());
            return new DeployDestination(dc, pod, cluster, target);
        }

        logger.warn(String.format("Cannot find suitable host for deploying external instance %s", vmProfile.getInstanceName()));
        return null;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) {
        return vm.getHypervisorType() == HypervisorType.External;
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
