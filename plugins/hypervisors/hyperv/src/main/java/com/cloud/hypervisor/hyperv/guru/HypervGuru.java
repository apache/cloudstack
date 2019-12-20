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
package com.cloud.hypervisor.hyperv.guru;

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.hypervisor.hyperv.manager.HypervManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Implementation of Hypervisor guru for Hyper-V.
 **/
public class HypervGuru extends HypervisorGuruBase implements HypervisorGuru {

    @Inject
    private GuestOSDao _guestOsDao;
    @Inject HypervManager _hypervMgr;
    @Inject NetworkDao _networkDao;
    @Inject NetworkModel _networkMgr;
    int MaxNicSupported = 8;
    @Override
    public final HypervisorType getHypervisorType() {
        return HypervisorType.Hyperv;
    }

    /**
     * Prevent direct creation.
     */
    protected HypervGuru() {
        super();
    }

    @Override
    public final VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        List<NicProfile> nicProfiles = vm.getNics();

        if(vm.getVirtualMachine().getType() ==  VirtualMachine.Type.DomainRouter) {

            NicProfile publicNicProfile = null;
            NicProfile controlNicProfile = null;
            NicProfile profile = null;
            for(NicProfile nicProfile : nicProfiles) {
                if(nicProfile.getTrafficType() == TrafficType.Public) {
                    publicNicProfile = nicProfile;
                    break;
                }
                else if (nicProfile.getTrafficType() == TrafficType.Control) {
                    controlNicProfile = nicProfile;
                }
            }

            if(publicNicProfile != null || controlNicProfile != null) {
                NicTO[] nics = to.getNics();
                // reserve extra NICs
                NicTO[] expandedNics = new NicTO[MaxNicSupported];
                int i = 0;
                int deviceId = -1;
                for(i = 0; i < nics.length; i++) {
                    expandedNics[i] = nics[i];
                    if(nics[i].getDeviceId() > deviceId)
                        deviceId = nics[i].getDeviceId();
                }
                deviceId++;

                long networkId = 0;
                if(publicNicProfile != null ) {
                    networkId= publicNicProfile.getNetworkId();
                    profile = publicNicProfile;
                }
                else {
                    networkId =  controlNicProfile.getNetworkId();
                    profile = controlNicProfile;
                }

                NetworkVO network = _networkDao.findById(networkId);
                // for Hyperv Hot Nic plug is not supported and it will support upto 8 nics.
                // creating the VR with extra nics (actual nics(3) + extra nics) will be 8
                for(; i < MaxNicSupported; i++) {
                    NicTO nicTo = new NicTO();
                    nicTo.setDeviceId(deviceId++);
                    nicTo.setBroadcastType(BroadcastDomainType.Vlan);
                    nicTo.setType(TrafficType.Public);
                    nicTo.setIp("0.0.0.0");
                    nicTo.setNetmask("255.255.255.255");
                    nicTo.setName(profile.getName());

                    try {
                        String mac = _networkMgr.getNextAvailableMacAddressInNetwork(networkId);
                        nicTo.setMac(mac);
                    } catch (InsufficientAddressCapacityException e) {
                        throw new CloudRuntimeException("unable to allocate mac address on network: " + networkId);
                    }
                    nicTo.setDns1(profile.getIPv4Dns1());
                    nicTo.setDns2(profile.getIPv4Dns2());
                    if (publicNicProfile != null && publicNicProfile.getIPv4Gateway() != null) {
                        nicTo.setGateway(publicNicProfile.getIPv4Gateway());
                    } else {
                        nicTo.setGateway(network.getGateway());
                    }
                    nicTo.setDefaultNic(false);
                    nicTo.setMtu(profile.getMtu());
                    nicTo.setBroadcastUri(profile.getBroadCastUri());
                    nicTo.setIsolationuri(profile.getIsolationUri());

                    Integer networkRate = _networkMgr.getNetworkRate(network.getId(), null);
                    nicTo.setNetworkRateMbps(networkRate);

                    expandedNics[i] = nicTo;
                }
                to.setNics(expandedNics);
            }

            StringBuffer sbMacSequence = new StringBuffer();
            for(NicTO nicTo : sortNicsByDeviceId(to.getNics())) {
                sbMacSequence.append(nicTo.getMac()).append("|");
            }

            if (!sbMacSequence.toString().isEmpty()) {
                sbMacSequence.deleteCharAt(sbMacSequence.length() - 1);
                String bootArgs = to.getBootArgs();
                to.setBootArgs(bootArgs + " nic_macs=" + sbMacSequence.toString());
            }

        }

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());

        return to;
    }

    private NicTO[] sortNicsByDeviceId(NicTO[] nics) {

        List<NicTO> listForSort = new ArrayList<NicTO>();
        for (NicTO nic : nics) {
            listForSort.add(nic);
        }
        Collections.sort(listForSort, new Comparator<NicTO>() {

            @Override
            public int compare(NicTO arg0, NicTO arg1) {
                if (arg0.getDeviceId() < arg1.getDeviceId()) {
                    return -1;
                } else if (arg0.getDeviceId() == arg1.getDeviceId()) {
                    return 0;
                }

                return 1;
            }
        });

        return listForSort.toArray(new NicTO[0]);
    }

    @Override
    public final boolean trackVmHostChange() {
        return false;
    }

    @Override
    public Map<String, String> getClusterSettings(long vmId) {
        return null;
    }

}
