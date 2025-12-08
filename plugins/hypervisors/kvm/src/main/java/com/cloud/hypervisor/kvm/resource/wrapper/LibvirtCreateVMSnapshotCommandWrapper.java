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

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CreateVMSnapshotCommand.class)
public final class LibvirtCreateVMSnapshotCommandWrapper extends CommandWrapper<CreateVMSnapshotCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final CreateVMSnapshotCommand cmd, final LibvirtComputingResource libvirtComputingResource) {
        String vmName = cmd.getVmName();
        String vmSnapshotName = cmd.getTarget().getSnapshotName();

        Domain dm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            dm = libvirtComputingResource.getDomain(conn, vmName);

            if (dm == null) {
                return new CreateVMSnapshotAnswer(cmd, false,
                        "Create VM Snapshot Failed due to can not find vm: " + vmName);
            }

            DomainState domainState = dm.getInfo().state ;
            if (domainState != DomainState.VIR_DOMAIN_RUNNING) {
                return new CreateVMSnapshotAnswer(cmd, false,
                        "Create VM Snapshot Failed due to  vm is not running: " + vmName + " with domainState = " + domainState);
            }

            String vmSnapshotXML = "<domainsnapshot>" + "  <name>" + vmSnapshotName + "</name>"
                    + "  <memory snapshot='internal' />" + "</domainsnapshot>";

            dm.snapshotCreateXML(vmSnapshotXML);

            return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), cmd.getVolumeTOs());
        } catch (LibvirtException e) {
            String msg = " Create VM snapshot failed due to " + e.toString();
            logger.warn(msg, e);
            return new CreateVMSnapshotAnswer(cmd, false, msg);
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
