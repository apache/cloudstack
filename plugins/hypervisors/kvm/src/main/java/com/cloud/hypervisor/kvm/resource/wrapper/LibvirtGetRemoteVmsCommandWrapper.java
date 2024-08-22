//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetRemoteVmsAnswer;
import com.cloud.agent.api.GetRemoteVmsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockInfo;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ResourceWrapper(handles = GetRemoteVmsCommand.class)
public final class LibvirtGetRemoteVmsCommandWrapper extends CommandWrapper<GetRemoteVmsCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final GetRemoteVmsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String remoteIp = command.getRemoteIp();
        String hypervisorURI = "qemu+tcp://" + remoteIp + "/system";
        HashMap<String, UnmanagedInstanceTO> unmanagedInstances = new HashMap<>();
        try {
            Connect conn = LibvirtConnection.getConnection(hypervisorURI);
            final List<String> allVmNames = libvirtComputingResource.getAllVmNames(conn);
            logger.info(String.format("Found %d VMs on the remote host %s", allVmNames.size(), remoteIp));
            for (String name : allVmNames) {
                final Domain domain = libvirtComputingResource.getDomain(conn, name);
                final DomainInfo.DomainState ps = domain.getInfo().state;
                final VirtualMachine.PowerState state = libvirtComputingResource.convertToPowerState(ps);

                logger.debug(String.format("Remote VM %s - powerstate: %s, state: %s", domain.getName(), ps.toString(), state.toString()));

                if (state == VirtualMachine.PowerState.PowerOff) {
                    try {
                        UnmanagedInstanceTO instance = getUnmanagedInstance(libvirtComputingResource, domain, conn);
                        unmanagedInstances.put(instance.getName(), instance);
                    } catch (Exception e) {
                        logger.error("Couldn't fetch remote VM " + domain.getName() + " details, due to: " + e.getMessage(), e);
                    }
                }
                domain.free();
            }
            logger.debug("Found " + unmanagedInstances.size() + " stopped VMs on remote host " + remoteIp);
            return new GetRemoteVmsAnswer(command, "", unmanagedInstances);
        } catch (final LibvirtException e) {
            logger.error("Failed to list stopped VMs on remote host " + remoteIp + ", due to: " + e.getMessage(), e);
            if (e.getMessage().toLowerCase().contains("connection refused")) {
                return new Answer(command, false, "Unable to connect to remote host " + remoteIp + ", please check the libvirtd tcp connectivity and retry");
            }
            return new Answer(command, false, "Unable to list stopped VMs on remote host " + remoteIp + ", due to: " + e.getMessage());
        }
    }

    private UnmanagedInstanceTO getUnmanagedInstance(LibvirtComputingResource libvirtComputingResource, Domain domain, Connect conn) {
        try {
            final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
            parser.parseDomainXML(domain.getXMLDesc(1));

            final UnmanagedInstanceTO instance = new UnmanagedInstanceTO();
            instance.setName(domain.getName());
            if (parser.getCpuModeDef() != null) {
                instance.setCpuCoresPerSocket(parser.getCpuModeDef().getCoresPerSocket());
            }
            Long memory = domain.getMaxMemory();
            instance.setMemory(memory.intValue()/1024);
            if (parser.getCpuTuneDef() !=null) {
                instance.setCpuSpeed(parser.getCpuTuneDef().getShares());
            }
            instance.setPowerState(getPowerState(libvirtComputingResource.getVmState(conn,domain.getName())));
            instance.setNics(getUnmanagedInstanceNics(parser.getInterfaces()));
            instance.setDisks(getUnmanagedInstanceDisks(parser.getDisks(),libvirtComputingResource, domain));
            instance.setVncPassword(parser.getVncPasswd() + "aaaaaaaaaaaaaa"); // Suffix back extra characters for DB compatibility

            return instance;
        } catch (Exception e) {
            logger.debug("Unable to retrieve remote unmanaged instance info,  due to: " + e.getMessage(), e);
            throw new CloudRuntimeException("Unable to retrieve remote unmanaged instance info, due to: " + e.getMessage());
        }
    }

    private UnmanagedInstanceTO.PowerState getPowerState(VirtualMachine.PowerState vmPowerState) {
        switch (vmPowerState) {
            case PowerOn:
                return UnmanagedInstanceTO.PowerState.PowerOn;
            case PowerOff:
                return UnmanagedInstanceTO.PowerState.PowerOff;
            default:
                return UnmanagedInstanceTO.PowerState.PowerUnknown;
        }
    }

    private List<UnmanagedInstanceTO.Nic> getUnmanagedInstanceNics(List<LibvirtVMDef.InterfaceDef> interfaces) {
        final ArrayList<UnmanagedInstanceTO.Nic> nics = new ArrayList<>(interfaces.size());
        int counter = 0;
        for (LibvirtVMDef.InterfaceDef interfaceDef : interfaces) {
            final UnmanagedInstanceTO.Nic nic = new UnmanagedInstanceTO.Nic();
            nic.setNicId(String.valueOf(counter++));
            nic.setMacAddress(interfaceDef.getMacAddress());
            nic.setAdapterType(interfaceDef.getModel().toString());
            nic.setNetwork(interfaceDef.getDevName());
            nic.setPciSlot(interfaceDef.getSlot().toString());
            nic.setVlan(interfaceDef.getVlanTag());
            nics.add(nic);
        }
        return nics;
    }

    private List<UnmanagedInstanceTO.Disk> getUnmanagedInstanceDisks(List<LibvirtVMDef.DiskDef> disksInfo,
                                                                     LibvirtComputingResource libvirtComputingResource,
                                                                     Domain dm){
        final ArrayList<UnmanagedInstanceTO.Disk> disks = new ArrayList<>(disksInfo.size());
        int counter = 0;
        for (LibvirtVMDef.DiskDef diskDef : disksInfo) {
            if (diskDef.getDeviceType() != LibvirtVMDef.DiskDef.DeviceType.DISK) {
                continue;
            }

            final UnmanagedInstanceTO.Disk disk = new UnmanagedInstanceTO.Disk();

            disk.setPosition(counter);

            Long size;
            try {
                DomainBlockInfo blockInfo = dm.blockInfo(diskDef.getSourcePath());
                size = blockInfo.getCapacity();
            } catch (LibvirtException e) {
                throw new RuntimeException(e);
            }

            disk.setCapacity(size);
            disk.setDiskId(String.valueOf(counter++));
            disk.setLabel(diskDef.getDiskLabel());
            disk.setController(diskDef.getBusType().toString());

            Pair<String, String> sourceHostPath = getSourceHostPath(libvirtComputingResource, diskDef.getSourcePath());
            if (sourceHostPath != null) {
                disk.setDatastoreHost(sourceHostPath.first());
                disk.setDatastorePath(sourceHostPath.second());
            } else {
                disk.setDatastorePath(diskDef.getSourcePath());
                disk.setDatastoreHost(diskDef.getSourceHost());
            }

            disk.setDatastoreType(diskDef.getDiskType().toString());
            disk.setDatastorePort(diskDef.getSourceHostPort());
            disks.add(disk);
        }
        return disks;
    }

    private Pair<String, String> getSourceHostPath(LibvirtComputingResource libvirtComputingResource, String diskPath) {
        int pathEnd = diskPath.lastIndexOf("/");
        if (pathEnd >= 0) {
            diskPath = diskPath.substring(0, pathEnd);
            return libvirtComputingResource.getSourceHostPath(diskPath);
        }
        return null;
    }
}
