/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.platform.orchestration;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.cloud.entity.api.NetworkEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.engine.vm.VMEntityManager;
import org.apache.cloudstack.engine.vm.VirtualMachineOrchestrator;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.dao.NetworkDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;


@Component
public class CloudOrchestrator implements OrchestrationService {

    @Inject
    VirtualMachineOrchestrator _vmOrchestrator;

	@Inject
	private VMEntityManager vmEntityManager;

	@Inject
	private VirtualMachineManager _itMgr;

	@Inject
	protected VMTemplateDao _templateDao = null;

    @Inject
    protected VMInstanceDao _vmDao;

    @Inject
    protected UserVmDao _userVmDao = null;

	@Inject
	protected ServiceOfferingDao _serviceOfferingDao;

	@Inject
	protected DiskOfferingDao _diskOfferingDao = null;

	@Inject
	protected NetworkDao _networkDao;

	@Inject
	protected AccountDao _accountDao = null;

    protected CloudOrchestrator() {
	}

    @Override
    public VolumeEntity createVolume() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateEntity registerTemplate(String name, URL path, String os, Hypervisor hypervisor) {
        return null;
    }

    @Override
    public void destroyNetwork(String networkUuid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroyVolume(String volumeEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public VirtualMachineEntity createVirtualMachine(
            String id,
            String owner,
            String templateId,
            String hostName,
            String displayName,
            String hypervisor,
            int cpu,
            int speed,
            long memory,
            Long diskSize,
            List<String> computeTags,
            List<String> rootDiskTags,
            Map<String, NicProfile> networkNicMap, DeploymentPlan plan) throws InsufficientCapacityException {

        return _vmOrchestrator.create(id,
                owner,
                templateId,
                hostName,
                displayName,
                Hypervisor.HypervisorType.valueOf(hypervisor),
                cpu,
                speed,
                memory,
                diskSize,
                computeTags,
                rootDiskTags,
                networkNicMap,
                plan);
    }

    @Override
    public VirtualMachineEntity createVirtualMachineFromScratch(String id, String owner, String isoId, String hostName, String displayName, String hypervisor, String os, int cpu, int speed, long memory,Long diskSize,
            List<String> computeTags, List<String> rootDiskTags, Map<String, NicProfile> networkNicMap, DeploymentPlan plan)  throws InsufficientCapacityException {

        return _vmOrchestrator.createFromScratch(
                id,
                owner,
                isoId,
                hostName,
                displayName,
                Hypervisor.HypervisorType.valueOf(hypervisor),
                os,
                cpu,
                speed,
                memory,
                diskSize,
                computeTags,
                rootDiskTags,
                networkNicMap,
                plan);
    }

    @Override
    public NetworkEntity createNetwork(String id, String name, String domainName, String cidr, String gateway) {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public VirtualMachineEntity getVirtualMachine(String id) {
        return _vmOrchestrator.get(id);
	}

}
