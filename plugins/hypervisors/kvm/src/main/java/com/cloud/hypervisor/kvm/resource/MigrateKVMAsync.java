/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.kvm.resource;

import java.util.concurrent.Callable;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

public class MigrateKVMAsync implements Callable<Domain> {

    private final LibvirtComputingResource libvirtComputingResource;

    private Domain dm = null;
    private Connect dconn = null;
    private String dxml = "";
    private String vmName = "";
    private String destIp = "";
    private boolean migrateStorage;
    private boolean autoConvergence;

    /**
     * Do not pause the domain during migration. The domain's memory will be transferred to the destination host while the domain is running. The migration may never converge if the domain is changing its memory faster then it can be transferred. The domain can be manually paused anytime during migration using virDomainSuspend.
     * @value 1
     * @see Libvirt <a href="https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainMigrateFlags">virDomainMigrateFlags</a> documentation
     */
    private static final long VIR_MIGRATE_LIVE = 1L;
    /**
     * Migrate full disk images in addition to domain's memory. By default only non-shared non-readonly disk images are transferred. The VIR_MIGRATE_PARAM_MIGRATE_DISKS parameter can be used to specify which disks should be migrated. This flag and VIR_MIGRATE_NON_SHARED_INC are mutually exclusive.
     * @value 64
     * @see Libvirt <a href="https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainMigrateFlags">virDomainMigrateFlags</a> documentation
     */
    private static final long VIR_MIGRATE_NON_SHARED_DISK = 64L;
    /**
     * Compress migration data. The compression methods can be specified using VIR_MIGRATE_PARAM_COMPRESSION. A hypervisor default method will be used if this parameter is omitted. Individual compression methods can be tuned via their specific VIR_MIGRATE_PARAM_COMPRESSION_* parameters.
     * @value 2048
     * @see Libvirt <a href="https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainMigrateFlags">virDomainMigrateFlags</a> documentation
     */
    private static final long VIR_MIGRATE_COMPRESSED = 2048L;
    /**
     * Enable algorithms that ensure a live migration will eventually converge. This usually means the domain will be slowed down to make sure it does not change its memory faster than a hypervisor can transfer the changed memory to the destination host. VIR_MIGRATE_PARAM_AUTO_CONVERGE_* parameters can be used to tune the algorithm.
     * @value 8192
     * @see Libvirt <a href="https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainMigrateFlags">virDomainMigrateFlags</a> documentation
     */
    private static final long VIR_MIGRATE_AUTO_CONVERGE = 8192L;

    /**
     *  Libvirt 1.0.3 supports compression flag for migration.
     */
    private static final int LIBVIRT_VERSION_SUPPORTS_MIGRATE_COMPRESSED = 1000003;

    public MigrateKVMAsync(final LibvirtComputingResource libvirtComputingResource, final Domain dm, final Connect dconn, final String dxml,
                           final boolean migrateStorage, final boolean autoConvergence, final String vmName, final String destIp) {
        this.libvirtComputingResource = libvirtComputingResource;

        this.dm = dm;
        this.dconn = dconn;
        this.dxml = dxml;
        this.migrateStorage = migrateStorage;
        this.autoConvergence = autoConvergence;
        this.vmName = vmName;
        this.destIp = destIp;
    }

    @Override
    public Domain call() throws LibvirtException {
        long flags = VIR_MIGRATE_LIVE;

        if (dconn.getLibVirVersion() >= LIBVIRT_VERSION_SUPPORTS_MIGRATE_COMPRESSED) {
            flags += VIR_MIGRATE_COMPRESSED;
        }

        if (migrateStorage) {
            flags += VIR_MIGRATE_NON_SHARED_DISK;
        }

        if (autoConvergence && dconn.getLibVirVersion() >= 1002003) {
            flags += VIR_MIGRATE_AUTO_CONVERGE;
        }

        return dm.migrate(dconn, flags, dxml, vmName, "tcp:" + destIp, libvirtComputingResource.getMigrateSpeed());
    }
}
