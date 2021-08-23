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

import java.io.File;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.to.DpdkTO;
import com.cloud.hypervisor.kvm.resource.LibvirtKvmAgentHook;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles =  StopCommand.class)
public final class LibvirtStopCommandWrapper extends CommandWrapper<StopCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtStopCommandWrapper.class);
    private static final String CMDLINE_PATH = "/var/cache/cloud/cmdline";
    private static final String CMDLINE_BACKUP_PATH = "/var/cache/cloud/cmdline.backup";

    @Override
    public Answer execute(final StopCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final Map<String, Boolean> vlanToPersistenceMap = command.getVlanToPersistenceMap();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

        if (command.checkBeforeCleanup()) {
            try {
                final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
                final Domain vm = conn.domainLookupByName(command.getVmName());
                if (vm != null && vm.getInfo().state == DomainState.VIR_DOMAIN_RUNNING) {
                    return new StopAnswer(command, "vm is still running on host", false);
                }
            } catch (final Exception e) {
                s_logger.debug("Failed to get vm status in case of checkboforecleanup is true", e);
            }
        }
        File pemFile = new File(LibvirtComputingResource.SSHPRVKEYPATH);
        try {
            if(vmName.startsWith("s-") || vmName.startsWith("v-")){
                //move the command line file to backup.
                s_logger.debug("backing up the cmdline");
                try{
                    Pair<Boolean, String> ret = SshHelper.sshExecute(command.getControlIp(), 3922, "root", pemFile, null,"cp -f "+CMDLINE_PATH+" "+CMDLINE_BACKUP_PATH);
                    if(!ret.first()){
                        s_logger.debug("Failed to backup cmdline file due to "+ret.second());
                    }
                } catch (Exception e){
                    s_logger.debug("Failed to backup cmdline file due to "+e.getMessage());
                }
            }

            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);

            final List<DiskDef> disks = libvirtComputingResource.getDisks(conn, vmName);
            final List<InterfaceDef> ifaces = libvirtComputingResource.getInterfaces(conn, vmName);

            libvirtComputingResource.destroyNetworkRulesForVM(conn, vmName);
            final String result = libvirtComputingResource.stopVM(conn, vmName, command.isForceStop());

            performAgentStopHook(vmName, libvirtComputingResource);

            if (result == null) {
                if (disks != null && disks.size() > 0) {
                    for (final DiskDef disk : disks) {
                        libvirtComputingResource.cleanupDisk(disk);
                    }
                }
                else {
                    // When using iSCSI-based managed storage, if the user shuts a VM down from the guest OS (as opposed to doing so from CloudStack),
                    // info needs to be passed to the KVM agent to have it disconnect KVM from the applicable iSCSI volumes.
                    List<Map<String, String>> volumesToDisconnect = command.getVolumesToDisconnect();

                    if (volumesToDisconnect != null) {
                        for (Map<String, String> volumeToDisconnect : volumesToDisconnect) {
                            libvirtComputingResource.cleanupDisk(volumeToDisconnect);
                        }
                    }
                }

                if (CollectionUtils.isEmpty(ifaces)) {
                    Map<String, DpdkTO> dpdkInterfaceMapping = command.getDpdkInterfaceMapping();
                    if (MapUtils.isNotEmpty(dpdkInterfaceMapping)) {
                        for (DpdkTO to : dpdkInterfaceMapping.values()) {
                            String portToRemove = to.getPort();
                            String cmd = String.format("ovs-vsctl del-port %s", portToRemove);
                            s_logger.debug("Removing DPDK port: " + portToRemove);
                            Script.runSimpleBashScript(cmd);
                        }
                    }
                } else {
                    for (final InterfaceDef iface : ifaces) {
                        String vlanId = libvirtComputingResource.getVlanIdFromBridgeName(iface.getBrName());
                        // We don't know which "traffic type" is associated with
                        // each interface at this point, so inform all vif drivers
                        for (final VifDriver vifDriver : libvirtComputingResource.getAllVifDrivers()) {
                            vifDriver.unplug(iface, libvirtComputingResource.shouldDeleteBridge(vlanToPersistenceMap, vlanId));
                        }
                    }
                }
            }

            return new StopAnswer(command, result, true);
        } catch (final LibvirtException e) {
            s_logger.debug("unable to stop VM:"+vmName+" due to"+e.getMessage());
            try{
                if(vmName.startsWith("s-") || vmName.startsWith("v-"))
                    s_logger.debug("restoring cmdline file from backup");
                Pair<Boolean, String> ret = SshHelper.sshExecute(command.getControlIp(), 3922, "root", pemFile, null, "mv "+CMDLINE_BACKUP_PATH+" "+CMDLINE_PATH);
                if(!ret.first()){
                    s_logger.debug("unable to restore cmdline due to "+ret.second());
                }
            }catch (final Exception ex){
                s_logger.debug("unable to restore cmdline due to:"+ex.getMessage());
            }
            return new StopAnswer(command, e.getMessage(), false);
        }
    }

    private void performAgentStopHook(String vmName, final LibvirtComputingResource libvirtComputingResource) {
        try {
            LibvirtKvmAgentHook onStopHook = libvirtComputingResource.getStopHook();
            onStopHook.handle(vmName);
        } catch (Exception e) {
            s_logger.warn("Exception occurred when handling LibVirt VM onStop hook: {}", e);
        }
    }
}
