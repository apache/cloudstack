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
package com.cloud.hypervisor;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

public abstract class HypervisorGuruBase extends AdapterBase implements HypervisorGuru {

    @Inject
    VMTemplateDetailsDao _templateDetailsDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VMInstanceDao _virtualMachineDao;
    @Inject
    UserVmDetailsDao _userVmDetailsDao;
    @Inject
    NicSecondaryIpDao _nicSecIpDao;
    @Inject
    ConfigurationServer _configServer;

    protected HypervisorGuruBase() {
        super();
    }

    @Override
    public NicTO toNicTO(NicProfile profile) {
        NicTO to = new NicTO();
        to.setDeviceId(profile.getDeviceId());
        to.setBroadcastType(profile.getBroadcastType());
        to.setType(profile.getTrafficType());
        to.setIp(profile.getIp4Address());
        to.setNetmask(profile.getNetmask());
        to.setMac(profile.getMacAddress());
        to.setDns1(profile.getDns1());
        to.setDns2(profile.getDns2());
        to.setGateway(profile.getGateway());
        to.setDefaultNic(profile.isDefaultNic());
        to.setBroadcastUri(profile.getBroadCastUri());
        to.setIsolationuri(profile.getIsolationUri());
        to.setNetworkRateMbps(profile.getNetworkRate());
        to.setName(profile.getName());
        to.setSecurityGroupEnabled(profile.isSecurityGroupEnabled());

        // Workaround to make sure the TO has the UUID we need for Niciri integration
        NicVO nicVO = _nicDao.findById(profile.getId());
        to.setUuid(nicVO.getUuid());
        //check whether the this nic has secondary ip addresses set
        //set nic secondary ip address in NicTO which are used for security group
        // configuration. Use full when vm stop/start
        List<String> secIps = null;
        if (nicVO.getSecondaryIp()) {
            secIps = _nicSecIpDao.getSecondaryIpAddressesForNic(nicVO.getId());
        }
        to.setNicSecIps(secIps);
        return to;
    }

    protected VirtualMachineTO toVirtualMachineTO(VirtualMachineProfile vmProfile) {

        ServiceOffering offering = vmProfile.getServiceOffering();
        VirtualMachine vm = vmProfile.getVirtualMachine();
        Long minMemory = (long)(offering.getRamSize() / vmProfile.getMemoryOvercommitRatio());
        int minspeed = (int)(offering.getSpeed() / vmProfile.getCpuOvercommitRatio());
        int maxspeed = (offering.getSpeed());
        VirtualMachineTO to =
            new VirtualMachineTO(vm.getId(), vm.getInstanceName(), vm.getType(), offering.getCpu(), minspeed, maxspeed, minMemory * 1024l * 1024l,
                offering.getRamSize() * 1024l * 1024l, null, null, vm.isHaEnabled(), vm.limitCpuUse(), vm.getVncPassword());
        to.setBootArgs(vmProfile.getBootArgs());

        List<NicProfile> nicProfiles = vmProfile.getNics();
        NicTO[] nics = new NicTO[nicProfiles.size()];
        int i = 0;
        for (NicProfile nicProfile : nicProfiles) {
            nics[i++] = toNicTO(nicProfile);
        }

        to.setNics(nics);
        to.setDisks(vmProfile.getDisks().toArray(new DiskTO[vmProfile.getDisks().size()]));

        if (vmProfile.getTemplate().getBits() == 32) {
            to.setArch("i686");
        } else {
            to.setArch("x86_64");
        }

        Map<String, String> detailsInVm = _userVmDetailsDao.listDetailsKeyPairs(vm.getId());
        if (detailsInVm != null) {
            to.setDetails(detailsInVm);
        }
        // Workaround to make sure the TO has the UUID we need for Niciri integration
        VMInstanceVO vmInstance = _virtualMachineDao.findById(to.getId());
        // check if XStools/VMWare tools are present in the VM and dynamic scaling feature is enabled (per zone/global)
        Boolean isDynamicallyScalable = vmInstance.isDynamicallyScalable() && UserVmManager.EnableDynamicallyScaleVm.valueIn(vm.getDataCenterId());
        to.setEnableDynamicallyScaleVm(isDynamicallyScalable);
        to.setUuid(vmInstance.getUuid());

        //
        return to;
    }

    @Override
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
    }

    @Override
    public List<Command> finalizeExpunge(VirtualMachine vm) {
        return null;
    }

    @Override
    public List<Command> finalizeExpungeNics(VirtualMachine vm, List<NicProfile> nics) {
        return null;
    }

    @Override
    public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
        return null;
    }
}
