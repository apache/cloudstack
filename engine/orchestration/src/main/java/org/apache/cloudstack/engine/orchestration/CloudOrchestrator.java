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
package org.apache.cloudstack.engine.orchestration;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.NetworkEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VMEntityManager;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntityImpl;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.DiskOfferingInfo;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class CloudOrchestrator implements OrchestrationService {

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
    protected UserVmDetailsDao _userVmDetailsDao = null;

    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;

    @Inject
    protected DiskOfferingDao _diskOfferingDao = null;

    @Inject
    protected NetworkDao _networkDao;

    @Inject
    protected AccountDao _accountDao = null;

    @Inject
    VolumeOrchestrationService _volumeMgr;

    public CloudOrchestrator() {
    }

    public VirtualMachineEntity createFromScratch(String uuid, String iso, String os, String hypervisor, String hostName, int cpu, int speed, long memory,
        List<String> networks, List<String> computeTags, Map<String, String> details, String owner) {
        return null;
    }

    public String reserve(String vm, String planner, Long until) throws InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    public String deploy(String reservationId) {
        // TODO Auto-generated method stub
        return null;
    }

    public void joinNetwork(String network1, String network2) {
        // TODO Auto-generated method stub

    }

    public void createNetwork() {
        // TODO Auto-generated method stub

    }

    public void destroyNetwork() {
        // TODO Auto-generated method stub

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
    public VirtualMachineEntity createVirtualMachine(String id, String owner, String templateId, String hostName, String displayName, String hypervisor, int cpu,
        int speed, long memory, Long diskSize, List<String> computeTags, List<String> rootDiskTags, Map<String, List<NicProfile>> networkNicMap, DeploymentPlan plan,
        Long rootDiskSize, Map<String, Map<Integer, String>> extraDhcpOptionMap, Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap) throws InsufficientCapacityException {

        // VirtualMachineEntityImpl vmEntity = new VirtualMachineEntityImpl(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, networks,
        // vmEntityManager);

        LinkedHashMap<NetworkVO, List<? extends NicProfile>> networkIpMap = new LinkedHashMap<NetworkVO, List<? extends NicProfile>>();
        for (String uuid : networkNicMap.keySet()) {
            NetworkVO network = _networkDao.findByUuid(uuid);
            if(network != null){
                networkIpMap.put(network, networkNicMap.get(uuid));
            }
        }

        VirtualMachineEntityImpl vmEntity = ComponentContext.inject(VirtualMachineEntityImpl.class);
        vmEntity.init(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, new ArrayList<String>(networkNicMap.keySet()));

        HypervisorType hypervisorType = HypervisorType.valueOf(hypervisor);

        //load vm instance and offerings and call virtualMachineManagerImpl
        VMInstanceVO vm = _vmDao.findByUuid(id);

        // If the template represents an ISO, a disk offering must be passed in, and will be used to create the root disk
        // Else, a disk offering is optional, and if present will be used to create the data disk

        DiskOfferingInfo rootDiskOfferingInfo = new DiskOfferingInfo();
        List<DiskOfferingInfo> dataDiskOfferings = new ArrayList<DiskOfferingInfo>();

        ServiceOfferingVO computeOffering = _serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());

        Long diskOfferingId = computeOffering.getDiskOfferingId();
        if (diskOfferingId != null) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
            }
            rootDiskOfferingInfo.setDiskOffering(diskOffering);
            rootDiskOfferingInfo.setSize(rootDiskSize);

            if (diskOffering.isCustomizedIops() != null && diskOffering.isCustomizedIops()) {
                Map<String, String> userVmDetails = _userVmDetailsDao.listDetailsKeyPairs(vm.getId());

                if (userVmDetails != null) {
                    String minIops = userVmDetails.get("minIops");
                    String maxIops = userVmDetails.get("maxIops");

                    rootDiskOfferingInfo.setMinIops(minIops != null && minIops.trim().length() > 0 ? Long.parseLong(minIops) : null);
                    rootDiskOfferingInfo.setMaxIops(maxIops != null && maxIops.trim().length() > 0 ? Long.parseLong(maxIops) : null);
                }
            }
            Long size = null;
            if (diskOffering.getDiskSize() == 0) {
                size = diskSize;
                if (size == null) {
                    throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
                }
                _volumeMgr.validateVolumeSizeRange(size * 1024 * 1024 * 1024);
            }

            DiskOfferingInfo dataDiskOfferingInfo = new DiskOfferingInfo();

            dataDiskOfferingInfo.setDiskOffering(diskOffering);
            dataDiskOfferingInfo.setSize(size);

            if (diskOffering.isCustomizedIops() != null && diskOffering.isCustomizedIops()) {
                Map<String, String> userVmDetails = _userVmDetailsDao.listDetailsKeyPairs(vm.getId());

                if (userVmDetails != null) {
                    String minIops = userVmDetails.get("minIopsDo");
                    String maxIops = userVmDetails.get("maxIopsDo");

                    dataDiskOfferingInfo.setMinIops(minIops != null && minIops.trim().length() > 0 ? Long.parseLong(minIops) : null);
                    dataDiskOfferingInfo.setMaxIops(maxIops != null && maxIops.trim().length() > 0 ? Long.parseLong(maxIops) : null);
                }
            }

            dataDiskOfferings.add(dataDiskOfferingInfo);
        }

        if (dataDiskTemplateToDiskOfferingMap != null && !dataDiskTemplateToDiskOfferingMap.isEmpty()) {
            for (Entry<Long, DiskOffering> datadiskTemplateToDiskOffering : dataDiskTemplateToDiskOfferingMap.entrySet()) {
                DiskOffering diskOffering = datadiskTemplateToDiskOffering.getValue();
                if (diskOffering == null) {
                    throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
                }
                if (diskOffering.getDiskSize() == 0) { // Custom disk offering is not supported for volumes created from datadisk templates
                    throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
                }
            }
        }

        _itMgr.allocate(vm.getInstanceName(), _templateDao.findById(new Long(templateId)), computeOffering, rootDiskOfferingInfo, dataDiskOfferings, networkIpMap, plan,
            hypervisorType, extraDhcpOptionMap, dataDiskTemplateToDiskOfferingMap);

        return vmEntity;
    }

    @Override
    public VirtualMachineEntity createVirtualMachineFromScratch(String id, String owner, String isoId, String hostName, String displayName, String hypervisor, String os,
        int cpu, int speed, long memory, Long diskSize, List<String> computeTags, List<String> rootDiskTags, Map<String, List<NicProfile>> networkNicMap, DeploymentPlan plan, Map<String, Map<Integer, String>> extraDhcpOptionMap)
        throws InsufficientCapacityException {

        // VirtualMachineEntityImpl vmEntity = new VirtualMachineEntityImpl(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, networks, vmEntityManager);
        VirtualMachineEntityImpl vmEntity = ComponentContext.inject(VirtualMachineEntityImpl.class);
        vmEntity.init(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, new ArrayList<String>(networkNicMap.keySet()));

        //load vm instance and offerings and call virtualMachineManagerImpl
        VMInstanceVO vm = _vmDao.findByUuid(id);

        ServiceOfferingVO computeOffering = _serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());

        DiskOfferingInfo rootDiskOfferingInfo = new DiskOfferingInfo();

        Long diskOfferingId = computeOffering.getDiskOfferingId();
        if (diskOfferingId == null) {
            throw new InvalidParameterValueException("Installing from ISO requires a disk offering to be specified for the root disk.");
        }
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
        if (diskOffering == null) {
            throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
        }
        rootDiskOfferingInfo.setDiskOffering(diskOffering);

        Long size = null;
        if (diskOffering.getDiskSize() == 0) {
            size = diskSize;
            if (size == null) {
                throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
            }
            _volumeMgr.validateVolumeSizeRange(size * 1024 * 1024 * 1024);
        }

        rootDiskOfferingInfo.setDiskOffering(diskOffering);
        rootDiskOfferingInfo.setSize(size);

        if (diskOffering.isCustomizedIops() != null && diskOffering.isCustomizedIops()) {
            Map<String, String> userVmDetails = _userVmDetailsDao.listDetailsKeyPairs(vm.getId());

            if (userVmDetails != null) {
                String minIops = userVmDetails.get("minIopsDo");
                String maxIops = userVmDetails.get("maxIopsDo");

                rootDiskOfferingInfo.setMinIops(minIops != null && minIops.trim().length() > 0 ? Long.parseLong(minIops) : null);
                rootDiskOfferingInfo.setMaxIops(maxIops != null && maxIops.trim().length() > 0 ? Long.parseLong(maxIops) : null);
            }
        }

        LinkedHashMap<Network, List<? extends NicProfile>> networkIpMap = new LinkedHashMap<Network, List<? extends NicProfile>>();
        for (String uuid : networkNicMap.keySet()) {
            NetworkVO network = _networkDao.findByUuid(uuid);
            if(network != null){
                networkIpMap.put(network, networkNicMap.get(uuid));
            }
        }

        HypervisorType hypervisorType = HypervisorType.valueOf(hypervisor);

        _itMgr.allocate(vm.getInstanceName(), _templateDao.findById(new Long(isoId)), computeOffering, rootDiskOfferingInfo, new ArrayList<DiskOfferingInfo>(), networkIpMap, plan, hypervisorType, extraDhcpOptionMap, null);

        return vmEntity;
    }

    @Override
    public NetworkEntity createNetwork(String id, String name, String domainName, String cidr, String gateway) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualMachineEntity getVirtualMachine(String id) {
        VirtualMachineEntityImpl vmEntity = new VirtualMachineEntityImpl(id, vmEntityManager);
        return vmEntity;
    }

}
