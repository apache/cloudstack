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

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.TypedParameter;
import org.libvirt.TypedStringParameter;
import org.libvirt.TypedUlongParameter;

public class MigrateKVMAsync implements Callable<Domain> {
    protected Logger logger = LogManager.getLogger(getClass());

    private final LibvirtComputingResource libvirtComputingResource;

    private Domain dm = null;
    private Connect dconn = null;
    private String dxml = "";
    private String vmName = "";
    private String destIp = "";
    private boolean migrateStorage;
    private boolean migrateNonSharedInc;
    private boolean autoConvergence;

    protected Set<String> migrateDiskLabels;

    // Libvirt Migrate Flags reference:
    // https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainMigrateFlags

    // Do not pause the domain during migration. The domain's memory will be
    // transferred to the destination host while the domain is running. The migration
    // may never converge if the domain is changing its memory faster then it can be
    // transferred. The domain can be manually paused anytime during migration using
    // virDomainSuspend.
    private static final long VIR_MIGRATE_LIVE = 1L;

    // Define the domain as persistent on the destination host after successful
    // migration. If the domain was persistent on the source host and
    // VIR_MIGRATE_UNDEFINE_SOURCE is not used, it will end up persistent on both
    // hosts.
    private static final long VIR_MIGRATE_PERSIST_DEST = 8L;

    // Migrate full disk images in addition to domain's memory. By default only
    // non-shared non-readonly disk images are transferred. The
    // VIR_MIGRATE_PARAM_MIGRATE_DISKS parameter can be used to specify which disks
    // should be migrated. This flag and VIR_MIGRATE_NON_SHARED_INC are mutually
    // exclusive.
    private static final long VIR_MIGRATE_NON_SHARED_DISK = 64L;

    // Migrate disk images in addition to domain's memory. This is similar to
    // VIR_MIGRATE_NON_SHARED_DISK, but only the top level of each disk's backing chain
    // is copied. That is, the rest of the backing chain is expected to be present on
    // the destination and to be exactly the same as on the source host. This flag and
    // VIR_MIGRATE_NON_SHARED_DISK are mutually exclusive.
    private static final long VIR_MIGRATE_NON_SHARED_INC = 128L;

    // Compress migration data. The compression methods can be specified using
    // VIR_MIGRATE_PARAM_COMPRESSION. A hypervisor default method will be used if this
    // parameter is omitted. Individual compression methods can be tuned via their
    // specific VIR_MIGRATE_PARAM_COMPRESSION_* parameters.
    private static final long VIR_MIGRATE_COMPRESSED = 2048L;

    // Enable algorithms that ensure a live migration will eventually converge.
    // This usually means the domain will be slowed down to make sure it does not
    // change its memory faster than a hypervisor can transfer the changed memory to
    // the destination host. VIR_MIGRATE_PARAM_AUTO_CONVERGE_* parameters can be used
    // to tune the algorithm.
    private static final long VIR_MIGRATE_AUTO_CONVERGE = 8192L;

    // Libvirt 1.0.3 supports compression flag for migration.
    private static final int LIBVIRT_VERSION_SUPPORTS_MIGRATE_COMPRESSED = 1000003;

    // Libvirt 1.2.3 supports auto converge.
    private static final int LIBVIRT_VERSION_SUPPORTS_AUTO_CONVERGE = 1002003;

    public MigrateKVMAsync(final LibvirtComputingResource libvirtComputingResource, final Domain dm, final Connect dconn, final String dxml,
            final boolean migrateStorage, final boolean migrateNonSharedInc, final boolean autoConvergence, final String vmName, final String destIp, Set<String> migrateDiskLabels) {
        this.libvirtComputingResource = libvirtComputingResource;

        this.dm = dm;
        this.dconn = dconn;
        this.dxml = dxml;
        this.migrateStorage = migrateStorage;
        this.migrateNonSharedInc = migrateNonSharedInc;
        this.autoConvergence = autoConvergence;
        this.vmName = vmName;
        this.destIp = destIp;
        this.migrateDiskLabels = migrateDiskLabels;
    }

    @Override
    public Domain call() throws LibvirtException {
        long flags = VIR_MIGRATE_LIVE;

        if (dconn.getLibVirVersion() >= LIBVIRT_VERSION_SUPPORTS_MIGRATE_COMPRESSED) {
            flags |= VIR_MIGRATE_COMPRESSED;
        }

        if (migrateStorage) {
            if (migrateNonSharedInc) {
                flags |= VIR_MIGRATE_PERSIST_DEST;
                flags |= VIR_MIGRATE_NON_SHARED_INC;
                logger.debug("Setting VIR_MIGRATE_NON_SHARED_INC for linked clone migration.");
            } else {
                flags |= VIR_MIGRATE_NON_SHARED_DISK;
                logger.debug("Setting VIR_MIGRATE_NON_SHARED_DISK for full clone migration.");
            }
        }

        if (autoConvergence && dconn.getLibVirVersion() >= LIBVIRT_VERSION_SUPPORTS_AUTO_CONVERGE) {
            flags |= VIR_MIGRATE_AUTO_CONVERGE;
        }

        TypedParameter [] parameters = createTypedParameterList();

        logger.debug(String.format("Migrating [%s] with flags [%s], destination [%s] and speed [%s]. The disks with the following labels will be migrated [%s].", vmName, flags,
                destIp, libvirtComputingResource.getMigrateSpeed(), migrateDiskLabels));

        return dm.migrate(dconn, parameters, flags);

    }

    protected TypedParameter[] createTypedParameterList() {
        int sizeOfMigrateDiskLabels = 0;
        if (migrateDiskLabels != null) {
            sizeOfMigrateDiskLabels = migrateDiskLabels.size();
        }

        TypedParameter[] parameters = new TypedParameter[4 + sizeOfMigrateDiskLabels];
        parameters[0] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_DEST_NAME, vmName);
        parameters[1] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_DEST_XML, dxml);
        parameters[2] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_URI, "tcp:" + destIp);
        parameters[3] = new TypedUlongParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_BANDWIDTH, libvirtComputingResource.getMigrateSpeed());

        if (sizeOfMigrateDiskLabels == 0) {
            return parameters;
        }

        Iterator<String> iterator = migrateDiskLabels.iterator();
        for (int i = 0; i < sizeOfMigrateDiskLabels; i++) {
            parameters[4 + i] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_MIGRATE_DISKS, iterator.next());
        }

        return parameters;
    }

}
