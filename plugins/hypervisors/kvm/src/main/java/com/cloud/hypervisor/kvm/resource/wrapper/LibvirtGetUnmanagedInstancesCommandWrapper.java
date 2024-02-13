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

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockInfo;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ResourceWrapper(handles=GetUnmanagedInstancesCommand.class)
public final class LibvirtGetUnmanagedInstancesCommandWrapper extends CommandWrapper<GetUnmanagedInstancesCommand, GetUnmanagedInstancesAnswer, LibvirtComputingResource> {

    private static final int requiredVncPasswordLength = 22;

    @Override
    public GetUnmanagedInstancesAnswer execute(GetUnmanagedInstancesCommand command, LibvirtComputingResource libvirtComputingResource) {
        logger.info("Fetching unmanaged instance on host");

        HashMap<String, UnmanagedInstanceTO> unmanagedInstances = new HashMap<>();
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnection();
            final List<Domain> domains = getDomains(command, libvirtComputingResource, conn);

            for (Domain domain : domains) {
                UnmanagedInstanceTO instance = getUnmanagedInstance(libvirtComputingResource, domain, conn);
                if (instance != null) {
                    unmanagedInstances.put(instance.getName(), instance);
                    domain.free();
                }
            }
        } catch (Exception e) {
            String err = String.format("Error listing unmanaged instances: %s", e.getMessage());
            logger.error(err, e);
            return new GetUnmanagedInstancesAnswer(command, err);
        }

        return new GetUnmanagedInstancesAnswer(command, "OK", unmanagedInstances);
    }

    private List<Domain> getDomains(GetUnmanagedInstancesCommand command,
                                    LibvirtComputingResource libvirtComputingResource,
                                    Connect conn) throws LibvirtException, CloudRuntimeException {
        final List<Domain> domains = new ArrayList<>();
        final String vmNameCmd = command.getInstanceName();
        if (StringUtils.isNotBlank(vmNameCmd)) {
            final Domain domain = libvirtComputingResource.getDomain(conn, vmNameCmd);
            if (domain == null) {
                String msg = String.format("VM %s not found", vmNameCmd);
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }

            checkIfVmExists(vmNameCmd,domain);
            checkIfVmIsManaged(command,vmNameCmd,domain);

            domains.add(domain);
        } else {
            final List<String> allVmNames = libvirtComputingResource.getAllVmNames(conn);
            for (String name : allVmNames) {
                if (!command.hasManagedInstance(name)) {
                    final Domain domain = libvirtComputingResource.getDomain(conn, name);
                    domains.add(domain);
                }
            }
        }
        return domains;
    }

    private void checkIfVmExists(String vmNameCmd,final Domain domain) throws LibvirtException {
        if (StringUtils.isNotEmpty(vmNameCmd) &&
                !vmNameCmd.equals(domain.getName())) {
            logger.error("GetUnmanagedInstancesCommand: exact vm name not found " + vmNameCmd);
            throw new CloudRuntimeException("GetUnmanagedInstancesCommand: exact vm name not found " + vmNameCmd);
        }
    }

    private void checkIfVmIsManaged(GetUnmanagedInstancesCommand command,String vmNameCmd,final Domain domain) throws LibvirtException {
        if (command.hasManagedInstance(domain.getName())) {
            logger.error("GetUnmanagedInstancesCommand: vm already managed " + vmNameCmd);
            throw new CloudRuntimeException("GetUnmanagedInstancesCommand:  vm already managed " + vmNameCmd);
        }
    }
    private UnmanagedInstanceTO getUnmanagedInstance(LibvirtComputingResource libvirtComputingResource, Domain domain, Connect conn) {
        try {
            final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
            parser.parseDomainXML(domain.getXMLDesc(1));

            final UnmanagedInstanceTO instance = new UnmanagedInstanceTO();
            instance.setName(domain.getName());

            instance.setCpuCores((int) LibvirtComputingResource.countDomainRunningVcpus(domain));
            instance.setCpuSpeed(parser.getCpuTuneDef().getShares()/instance.getCpuCores());

            if (parser.getCpuModeDef() != null) {
                instance.setCpuCoresPerSocket(parser.getCpuModeDef().getCoresPerSocket());
            }
            instance.setPowerState(getPowerState(libvirtComputingResource.getVmState(conn,domain.getName())));
            instance.setMemory((int) LibvirtComputingResource.getDomainMemory(domain) / 1024);
            instance.setNics(getUnmanagedInstanceNics(parser.getInterfaces()));
            instance.setDisks(getUnmanagedInstanceDisks(parser.getDisks(),libvirtComputingResource, conn, domain.getName()));
            instance.setVncPassword(getFormattedVncPassword(parser.getVncPasswd()));

            return instance;
        } catch (Exception e) {
            logger.info("Unable to retrieve unmanaged instance info. " + e.getMessage(), e);
            return null;
        }
    }

    protected String getFormattedVncPassword(String vncPasswd) {
        if (StringUtils.isBlank(vncPasswd)) {
            return null;
        }
        String randomChars = RandomStringUtils.random(requiredVncPasswordLength - vncPasswd.length(), true, false);
        return String.format("%s%s", vncPasswd, randomChars);
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

    private List<UnmanagedInstanceTO.Disk> getUnmanagedInstanceDisks(List<LibvirtVMDef.DiskDef> disksInfo, LibvirtComputingResource libvirtComputingResource, Connect conn, String domainName) {
        final ArrayList<UnmanagedInstanceTO.Disk> disks = new ArrayList<>(disksInfo.size());
        int counter = 0;
        for (LibvirtVMDef.DiskDef diskDef : disksInfo) {
            if (diskDef.getDeviceType() != LibvirtVMDef.DiskDef.DeviceType.DISK) {
                continue;
            }

            final UnmanagedInstanceTO.Disk disk = new UnmanagedInstanceTO.Disk();
            Long size = null;
            try {
                Domain dm = conn.domainLookupByName(domainName);
                DomainBlockInfo blockInfo = dm.blockInfo(diskDef.getDiskLabel());
                size = blockInfo.getCapacity();
            } catch (LibvirtException e) {
                throw new RuntimeException(e);
            }

            disk.setPosition(counter);
            disk.setCapacity(size);
            disk.setDiskId(String.valueOf(counter++));
            disk.setLabel(diskDef.getDiskLabel());
            disk.setController(diskDef.getBusType().toString());

            Pair<String, String> sourceHostPath = getSourceHostPath(libvirtComputingResource, diskDef.getSourcePath());
            if (sourceHostPath != null) {
                disk.setDatastoreHost(sourceHostPath.first());
                disk.setDatastorePath(sourceHostPath.second());
            } else {
                int pathEnd = diskDef.getSourcePath().lastIndexOf("/");
                if (pathEnd >= 0) {
                    disk.setDatastorePath(diskDef.getSourcePath().substring(0, pathEnd));
                } else {
                    disk.setDatastorePath(diskDef.getSourcePath());
                }
                disk.setDatastoreHost(diskDef.getSourceHost());
            }

            disk.setDatastoreType(diskDef.getDiskType().toString());
            disk.setDatastorePort(diskDef.getSourceHostPort());
            disk.setImagePath(diskDef.getSourcePath());
            disk.setDatastoreName(disk.getDatastorePath());
            disk.setFileBaseName(getDiskRelativePath(diskDef));
            disks.add(disk);
        }
        return disks;
    }

    protected String getDiskRelativePath(LibvirtVMDef.DiskDef diskDef) {
        if (diskDef == null || diskDef.getDiskType() == null || diskDef.getDiskType() == LibvirtVMDef.DiskDef.DiskType.BLOCK) {
            return null;
        }
        String sourcePath = diskDef.getSourcePath();
        if (StringUtils.isBlank(sourcePath)) {
            return null;
        }
        if (!sourcePath.contains("/")) {
            return sourcePath;
        }
        return sourcePath.substring(sourcePath.lastIndexOf("/") + 1);
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
