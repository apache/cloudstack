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

import org.apache.cloudstack.ca.PostCertificateRenewalCommand;
import org.apache.cloudstack.ca.SetupCertificateAnswer;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  PostCertificateRenewalCommand.class)
public final class LibvirtPostCertificateRenewalCommandWrapper extends CommandWrapper<PostCertificateRenewalCommand, Answer, LibvirtComputingResource> {

    /**
     * QMP {@code display-reload} command asking QEMU to reload the VNC display's TLS credentials off disk.
     * {@code tls-certs: true} is required, otherwise QEMU reloads the display without touching the certificates.
     * Added in QEMU 6.0 by commit 9cc07651655ee86eca41059f5ead8c4e5607c734 ("qmp: add new qmp display-reload",
     * merged 2021-03-23) - https://github.com/qemu/qemu/commit/9cc07651655ee86eca41059f5ead8c4e5607c734
     */
    private static final String QEMU_MONITOR_DISPLAY_RELOAD_VNC_TLS_CERTS_COMMAND =
            "{\"execute\":\"display-reload\",\"arguments\":{\"type\":\"vnc\",\"tls-certs\":true}}";

    /** Minimum QEMU version supporting {@link #QEMU_MONITOR_DISPLAY_RELOAD_VNC_TLS_CERTS_COMMAND}. */
    private static final long MIN_QEMU_VERSION_FOR_VNC_TLS_CERT_RELOAD = 6000000L;

    @Override
    public Answer execute(final PostCertificateRenewalCommand command, final LibvirtComputingResource serverResource) {
        logger.info("Restarting libvirt after certificate provisioning/renewal");
        if (command != null) {
            pushRenewedVncCertificateToRunningVms(serverResource);
            restartLibvirtd();
            return new SetupCertificateAnswer(true);
       }
        return new SetupCertificateAnswer(false);
    }

    private void restartLibvirtd() {
        final int timeout = 30000;
        Script script = new Script(true, "service", timeout, logger);
        script.add("libvirtd");
        script.add("restart");
        script.execute();
    }

    /**
     * The VNC TLS certificate on KVM is the host's agent certificate, applied host-wide via libvirtd's
     * {@code vnc_tls_x509_cert_dir} setting. Restarting libvirtd does not affect VMs already running, since QEMU
     * only loads that certificate once, at VM start - so reload it live on every running VM here, instead of
     * leaving them on the previous (possibly expired) certificate until stopped/started or migrated.
     */
    private void pushRenewedVncCertificateToRunningVms(final LibvirtComputingResource serverResource) {
        final long qemuVersion = serverResource.getHypervisorQemuVersion();
        if (qemuVersion < MIN_QEMU_VERSION_FOR_VNC_TLS_CERT_RELOAD) {
            logger.warn("QEMU {} on this host does not support reloading the VNC TLS certificate of a running VM (QEMU >= {} required), " +
                    "running VMs will keep using the previous certificate until they are stopped/started or migrated",
                    qemuVersion, MIN_QEMU_VERSION_FOR_VNC_TLS_CERT_RELOAD);
            return;
        }

        final Connect conn;
        final int[] domainIds;
        try {
            conn = serverResource.getLibvirtUtilitiesHelper().getConnection();
            domainIds = conn.listDomains();
        } catch (final LibvirtException e) {
            logger.warn("Unable to list running VMs to reload their renewed VNC certificate", e);
            return;
        }

        for (final int domainId : domainIds) {
            Domain vm = null;
            String vmName = null;
            try {
                vm = conn.domainLookupByID(domainId);
                vmName = vm.getName();
                vm.qemuMonitorCommand(QEMU_MONITOR_DISPLAY_RELOAD_VNC_TLS_CERTS_COMMAND, 0);
                logger.debug("Reloaded VNC TLS certificate for VM [{}]", vmName);
            } catch (final Exception e) {
                logger.warn("Failed to reload the renewed VNC certificate for VM [{}], it will keep using the previous " +
                        "certificate until it is stopped/started or migrated", vmName, e);
            } finally {
                if (vm != null) {
                    try {
                        vm.free();
                    } catch (final LibvirtException e) {
                        logger.trace("Ignoring libvirt error.", e);
                    }
                }
            }
        }
    }
}
