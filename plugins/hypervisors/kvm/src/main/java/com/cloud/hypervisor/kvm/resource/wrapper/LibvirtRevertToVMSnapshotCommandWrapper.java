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

import java.util.List;

import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

@ResourceWrapper(handles =  RevertToVMSnapshotCommand.class)
public final class LibvirtRevertToVMSnapshotCommandWrapper extends CommandWrapper<RevertToVMSnapshotCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final RevertToVMSnapshotCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        String vmName = cmd.getVmName();
        List<VolumeObjectTO> listVolumeTo = cmd.getVolumeTOs();
        VMSnapshot.Type vmSnapshotType = cmd.getTarget().getType();
        Boolean snapshotMemory = vmSnapshotType == VMSnapshot.Type.DiskAndMemory;
        VirtualMachine.PowerState vmState = null;

        Domain dm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            dm = libvirtComputingResource.getDomain(conn, vmName);

            if (dm == null) {
                return new RevertToVMSnapshotAnswer(cmd, false,
                        "Revert to VM Snapshot Failed due to can not find vm: " + vmName);
            }

            DomainSnapshot snapshot = dm.snapshotLookupByName(cmd.getTarget().getSnapshotName());
            if (snapshot == null)
                return new RevertToVMSnapshotAnswer(cmd, false, "Cannot find vmSnapshot with name: " + cmd.getTarget().getSnapshotName());

            dm.revertToSnapshot(snapshot);
            snapshot.free();

            if (!snapshotMemory) {
                dm.destroy();
                if (dm.isPersistent() == 1)
                    dm.undefine();
                vmState = VirtualMachine.PowerState.PowerOff;
            } else {
                vmState = VirtualMachine.PowerState.PowerOn;
            }

            return new RevertToVMSnapshotAnswer(cmd, listVolumeTo, vmState);
        } catch (LibvirtException e) {
            String msg = " Revert to VM snapshot failed due to " + e.toString();
            logger.warn(msg, e);
            return new RevertToVMSnapshotAnswer(cmd, false, msg);
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                };
            }
        }
    }
}
