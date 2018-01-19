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
package com.cloud.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.user.Account;
import com.cloud.utils.db.EntityManager;

/**
 * Implementation of VirtualMachineProfile.
 *
 */
public class VirtualMachineProfileImpl implements VirtualMachineProfile {

    VirtualMachine _vm;
    ServiceOffering _offering;
    VirtualMachineTemplate _template;
    UserVmDetailVO _userVmDetails;
    Map<Param, Object> _params;
    List<NicProfile> _nics = new ArrayList<NicProfile>();
    List<DiskTO> _disks = new ArrayList<DiskTO>();
    StringBuilder _bootArgs = new StringBuilder();
    Account _owner;
    BootloaderType _bootloader;
    Float cpuOvercommitRatio = 1.0f;
    Float memoryOvercommitRatio = 1.0f;

    VirtualMachine.Type _type;

    List<String[]> vmData = null;

    String configDriveLabel = null;
    String configDriveIsoBaseLocation = "/tmp/";
    String configDriveIsoRootFolder = null;
    String configDriveIsoFile = null;

    public VirtualMachineProfileImpl(VirtualMachine vm, VirtualMachineTemplate template, ServiceOffering offering, Account owner, Map<Param, Object> params) {
        _vm = vm;
        _template = template;
        _offering = offering;
        _params = params;
        _owner = owner;
        if (_params == null) {
            _params = new HashMap<Param, Object>();
        }
        if (vm != null)
            _type = vm.getType();
    }

    public VirtualMachineProfileImpl(VirtualMachine vm) {
        this(vm, null, null, null, null);
    }

    public VirtualMachineProfileImpl(VirtualMachine.Type type) {
        _type = type;
    }

    @Override
    public String toString() {
        return _vm.toString();
    }

    @Override
    public VirtualMachine getVirtualMachine() {
        return _vm;
    }

    @Override
    public ServiceOffering getServiceOffering() {
        if (_offering == null) {
            _offering = s_entityMgr.findById(ServiceOffering.class, _vm.getServiceOfferingId());
        }
        return _offering;
    }

    @Override
    public void setParameter(Param name, Object value) {
        _params.put(name, value);
    }

    @Override
    public void setBootLoaderType(BootloaderType bootLoader) {
        _bootloader = bootLoader;
    }

    @Override
    public VirtualMachineTemplate getTemplate() {
        if (_template == null && _vm != null) {
            _template = s_entityMgr.findByIdIncludingRemoved(VirtualMachineTemplate.class, _vm.getTemplateId());
        }
        return _template;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return _vm.getHypervisorType();
    }

    @Override
    public long getTemplateId() {
        return _vm.getTemplateId();
    }

    @Override
    public long getServiceOfferingId() {
        return _vm.getServiceOfferingId();
    }

    @Override
    public long getId() {
        return _vm.getId();
    }

    @Override
    public String getUuid() {
        return _vm.getUuid();
    }

    public void setNics(List<NicProfile> nics) {
        _nics = nics;
    }

    public void setDisks(List<DiskTO> disks) {
        _disks = disks;
    }

    @Override
    public List<NicProfile> getNics() {
        return _nics;
    }

    @Override
    public List<DiskTO> getDisks() {
        return _disks;
    }

    @Override
    public void addNic(int index, NicProfile nic) {
        _nics.add(index, nic);
    }

    @Override
    public void addDisk(int index, DiskTO disk) {
        _disks.add(index, disk);
    }

    @Override
    public StringBuilder getBootArgsBuilder() {
        return _bootArgs;
    }

    @Override
    public void addBootArgs(String... args) {
        for (String arg : args) {
            _bootArgs.append(arg).append(" ");
        }
    }

    @Override
    public VirtualMachine.Type getType() {
        return _type;
    }

    @Override
    public Account getOwner() {
        if (_owner == null) {
            _owner = s_entityMgr.findById(Account.class, _vm.getAccountId());
        }
        return _owner;
    }

    @Override
    public String getBootArgs() {
        return _bootArgs.toString();
    }

    static EntityManager s_entityMgr;

    static void init(EntityManager entityMgr) {
        s_entityMgr = entityMgr;
    }

    @Override
    public void addNic(NicProfile nic) {
        _nics.add(nic);
    }

    @Override
    public void addDisk(DiskTO disk) {
        _disks.add(disk);
    }

    @Override
    public Object getParameter(Param name) {
        return _params.get(name);
    }

    @Override
    public String getHostName() {
        return _vm.getHostName();
    }

    @Override
    public String getInstanceName() {
        return _vm.getInstanceName();
    }

    @Override
    public BootloaderType getBootLoaderType() {
        return _bootloader;
    }

    @Override
    public Map<Param, Object> getParameters() {
        return _params;
    }

    public void setServiceOffering(ServiceOfferingVO offering) {
        _offering = offering;
    }

    public void setCpuOvercommitRatio(Float cpuOvercommitRatio) {
        this.cpuOvercommitRatio = cpuOvercommitRatio;

    }

    public void setMemoryOvercommitRatio(Float memoryOvercommitRatio) {
        this.memoryOvercommitRatio = memoryOvercommitRatio;

    }

    @Override
    public Float getCpuOvercommitRatio() {
        return cpuOvercommitRatio;
    }

    @Override
    public Float getMemoryOvercommitRatio() {
        return memoryOvercommitRatio;
    }

    @Override
    public List<String[]> getVmData() {
        return vmData;
    }

    @Override
    public void setVmData(List<String[]> vmData) {
        this.vmData = vmData;
    }

    @Override
    public String getConfigDriveLabel() {
        return configDriveLabel;
    }

    @Override
    public void setConfigDriveLabel(String configDriveLabel) {
        this.configDriveLabel = configDriveLabel;
    }

    @Override
    public String getConfigDriveIsoRootFolder() {
        return configDriveIsoRootFolder;
    }

    @Override
    public void setConfigDriveIsoRootFolder(String configDriveIsoRootFolder) {
        this.configDriveIsoRootFolder = configDriveIsoRootFolder;
    }

    public String getConfigDriveIsoBaseLocation() {
        return configDriveIsoBaseLocation;
    }

    @Override
    public String getConfigDriveIsoFile() {
        return configDriveIsoFile;
    }

    @Override
    public void setConfigDriveIsoFile(String isoFile) {
        this.configDriveIsoFile = isoFile;
    }
}
