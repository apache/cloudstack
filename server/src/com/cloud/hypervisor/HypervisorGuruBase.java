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
package com.cloud.hypervisor;

import java.util.List;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public abstract class HypervisorGuruBase extends AdapterBase implements HypervisorGuru {
    protected HypervisorGuruBase() {
        super();
    }

    protected NicTO toNicTO(NicProfile profile) {
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
        return to;
    }


    protected <T extends VirtualMachine> VirtualMachineTO toVirtualMachineTO(VirtualMachineProfile<T> vmProfile) {

        ServiceOffering offering = vmProfile.getServiceOffering();  
        VirtualMachine vm = vmProfile.getVirtualMachine();

        VirtualMachineTO to = new VirtualMachineTO(vm.getId(), vm.getInstanceName(), vm.getType(), offering.getCpu(), offering.getSpeed(), 
                offering.getRamSize() * 1024l * 1024l, offering.getRamSize() * 1024l * 1024l, null, null, vm.isHaEnabled(), vm.limitCpuUse(), vm.getVncPassword());
        to.setBootArgs(vmProfile.getBootArgs());

        List<NicProfile> nicProfiles = vmProfile.getNics();
        NicTO[] nics = new NicTO[nicProfiles.size()];
        int i = 0;
        for (NicProfile nicProfile : nicProfiles) {
            nics[i++] = toNicTO(nicProfile);
        }

        to.setNics(nics);
        to.setDisks(vmProfile.getDisks().toArray(new VolumeTO[vmProfile.getDisks().size()]));

        if(vmProfile.getTemplate().getBits() == 32) {
            to.setArch("i686");
        } else {
            to.setArch("x86_64");
        }

        to.setDetails(vm.getDetails());

        return to;
    }

    @Override
    public long getCommandHostDelegation(long hostId, Command cmd) {
        return hostId;
    }
}
