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
package org.apache.cloudstack.agent.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.commons.collections.CollectionUtils;

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
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public class ExternalServerPlanner extends AdapterBase implements DeploymentPlanner {

    @Inject
    protected DataCenterDao dcDao;
    @Inject
    protected HostPodDao podDao;
    @Inject
    protected ClusterDao clusterDao;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected ResourceManager resourceMgr;
    @Inject
    ClusterDetailsDao clusterDetailsDao;

    @Override
    public DeployDestination plan(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();
        VirtualMachineTemplate template = vmProfile.getTemplate();
        Long extensionId = template.getExtensionId();

        String haVmTag = (String)vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);

        if (vm.getLastHostId() != null) {
            HostVO h = hostDao.findById(vm.getLastHostId());
            DataCenter dc = dcDao.findById(h.getDataCenterId());
            Pod pod = podDao.findById(h.getPodId());
            Cluster c = clusterDao.findById(h.getClusterId());
            logger.debug("Start external {} on last used {}", vm, h);
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

        List<ClusterVO> clusters = clusterDao.listClustersByDcId(vm.getDataCenterId());
        HostVO target = null;
        List<HostVO> hosts;
        for (ClusterVO cluster : clusters) {
            ClusterDetailsVO clusterExtensionDetail = clusterDetailsDao.findDetail(cluster.getId(), ApiConstants.EXTENSION_ID);
            if (clusterExtensionDetail != null && clusterExtensionDetail.getValue().equals(String.valueOf(extensionId))) {
                hosts = resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());
                if (hostTag != null) {
                    for (HostVO host : hosts) {
                        hostDao.loadHostTags(host);
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
        }

        if (target != null) {
            DataCenter dc = dcDao.findById(target.getDataCenterId());
            Pod pod = podDao.findById(target.getPodId());
            Cluster cluster = clusterDao.findById(target.getClusterId());
            return new DeployDestination(dc, pod, cluster, target);
        }

        logger.warn("Cannot find suitable host for deploying external instance {}", vmProfile.getInstanceName());
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
