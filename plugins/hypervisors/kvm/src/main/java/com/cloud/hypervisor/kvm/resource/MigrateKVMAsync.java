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

import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.TypedIntParameter;
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
    private boolean encryptMigration;
    private boolean parallelMigration;
    private int parallelConnections;
    private boolean allowUnsafeMigration;
    private String compressionMethod;
    private String migrateListenAddress = null;

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

    // Encrypt the migration connection using the TLS environment configured in qemu.conf
    // (migrate_tls_x509_cert_dir / default_tls_x509_cert_dir). Without this, guest RAM, and full
    // disk contents during storage migration, cross the network in plaintext TCP.
    private static final long VIR_MIGRATE_TLS = 65536L; // 1 << 16

    // Libvirt 3.2.0 supports VIR_MIGRATE_TLS.
    private static final int LIBVIRT_VERSION_SUPPORTS_MIGRATE_TLS = 3002000;

    // Use multiple parallel network connections (multifd) to transfer memory. Without this,
    // migration uses a single TCP stream and cannot fill a fast (25/40/100GbE) link.
    private static final long VIR_MIGRATE_PARALLEL = 131072L; // 1 << 17

    // Libvirt 5.2.0 supports VIR_MIGRATE_PARALLEL.
    private static final int LIBVIRT_VERSION_SUPPORTS_PARALLEL = 5002000;

    // Migrate even if libvirt considers the migration unsafe (e.g. a disk cache mode other
    // than none/directsync). On coherent shared storage such as Ceph RBD this is safe, but libvirt
    // refuses such a migration unless this flag is set.
    private static final long VIR_MIGRATE_UNSAFE = 512L; // 1 << 9

    public MigrateKVMAsync(final LibvirtComputingResource libvirtComputingResource, final Domain dm, final Connect dconn, final String dxml,
            final boolean migrateStorage, final boolean migrateNonSharedInc, final boolean autoConvergence, final boolean encryptMigration,
            final boolean parallelMigration, final boolean allowUnsafeMigration, final String compressionMethod,
            final int parallelConnections, final String vmName, final String destIp, final String migrateListenAddress, Set<String> migrateDiskLabels) {
        this.libvirtComputingResource = libvirtComputingResource;

        this.dm = dm;
        this.dconn = dconn;
        this.dxml = dxml;
        this.migrateStorage = migrateStorage;
        this.migrateNonSharedInc = migrateNonSharedInc;
        this.autoConvergence = autoConvergence;
        this.encryptMigration = encryptMigration;
        this.parallelMigration = parallelMigration;
        this.parallelConnections = parallelConnections;
        this.allowUnsafeMigration = allowUnsafeMigration;
        this.compressionMethod = compressionMethod;
        this.vmName = vmName;
        this.destIp = destIp;
        this.migrateListenAddress = migrateListenAddress;
        this.migrateDiskLabels = migrateDiskLabels;
    }

    @Override
    public Domain call() throws LibvirtException {
        long flags = buildMigrateFlags(dconn.getLibVirVersion());

        TypedParameter [] parameters = createTypedParameterList(dconn.getLibVirVersion());

        logger.debug(String.format("Migrating [%s] with flags [%s], destination [%s] and speed [%s]. The disks with the following labels will be migrated [%s].", vmName, flags,
                destIp, libvirtComputingResource.getMigrateSpeed(), migrateDiskLabels));

        return dm.migrate(dconn, parameters, flags);

    }

    // extracted from call() so the flag computation (including the new VIR_MIGRATE_TLS)
    // is unit-testable without a live libvirt connection.
    protected long buildMigrateFlags(final long libvirtVersion) {
        long flags = VIR_MIGRATE_LIVE;

        // legacy compression (VIR_MIGRATE_COMPRESSED, which QEMU maps to xbzrle) is INCOMPATIBLE
        // with multifd (VIR_MIGRATE_PARALLEL), QEMU refuses to combine them, so setting both would fail
        // every parallel migration. Skip legacy compression whenever multifd is enabled.
        if (libvirtVersion >= LIBVIRT_VERSION_SUPPORTS_MIGRATE_COMPRESSED && !parallelMigration) {
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

        if (autoConvergence && libvirtVersion >= LIBVIRT_VERSION_SUPPORTS_AUTO_CONVERGE) {
            flags |= VIR_MIGRATE_AUTO_CONVERGE;
        }

        if (encryptMigration) {
            if (libvirtVersion < LIBVIRT_VERSION_SUPPORTS_MIGRATE_TLS) {
                throw new CloudRuntimeException(String.format(
                        "Live migration of %s requires encryption but libvirt %d does not support TLS migration (needs >= %d); failing instead of sending the memory stream in plaintext.",
                        vmName, libvirtVersion, LIBVIRT_VERSION_SUPPORTS_MIGRATE_TLS));
            }
            flags |= VIR_MIGRATE_TLS;
        }

        if (parallelMigration && libvirtVersion >= LIBVIRT_VERSION_SUPPORTS_PARALLEL) {
            flags |= VIR_MIGRATE_PARALLEL;
        }

        // permit migration of VMs libvirt deems unsafe (e.g. writeback disk cache) when the
        // operator asserts the storage is coherent (Ceph RBD). Opt-in; off by default.
        if (allowUnsafeMigration) {
            flags |= VIR_MIGRATE_UNSAFE;
        }

        return flags;
    }

    protected TypedParameter[] createTypedParameterList(final long libvirtVersion) {
        int sizeOfMigrateDiskLabels = 0;
        if (migrateDiskLabels != null) {
            sizeOfMigrateDiskLabels = migrateDiskLabels.size();
        }

        // Each tuning parameter must be gated on the same condition as the flag that activates it, or libvirt
        // rejects the migration (e.g. the parallel-connections param without VIR_MIGRATE_PARALLEL).
        final boolean hasCompressionMethod = StringUtils.isNotBlank(compressionMethod) && !parallelMigration
                && libvirtVersion >= LIBVIRT_VERSION_SUPPORTS_MIGRATE_COMPRESSED;
        final boolean bindMigrateListenAddress = StringUtils.isNotBlank(migrateListenAddress);
        final boolean setParallelConnections = parallelMigration && parallelConnections > 0
                && libvirtVersion >= LIBVIRT_VERSION_SUPPORTS_PARALLEL;
        final int fixedParams = 4 + (hasCompressionMethod ? 1 : 0) + (bindMigrateListenAddress ? 1 : 0) + (setParallelConnections ? 1 : 0);

        TypedParameter[] parameters = new TypedParameter[fixedParams + sizeOfMigrateDiskLabels];
        parameters[0] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_DEST_NAME, vmName);
        parameters[1] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_DEST_XML, dxml);
        parameters[2] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_URI, "tcp:" + destIp);
        parameters[3] = new TypedUlongParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_BANDWIDTH, libvirtComputingResource.getMigrateSpeed());
        int nextParam = 4;
        if (hasCompressionMethod) {
            parameters[nextParam++] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_COMPRESSION, compressionMethod);
        }
        if (bindMigrateListenAddress) {
            parameters[nextParam++] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_LISTEN_ADDRESS, migrateListenAddress);
        }
        if (setParallelConnections) {
            parameters[nextParam++] = new TypedIntParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_PARALLEL_CONNECTIONS, parallelConnections);
        }

        if (sizeOfMigrateDiskLabels == 0) {
            return parameters;
        }

        Iterator<String> iterator = migrateDiskLabels.iterator();
        for (int i = 0; i < sizeOfMigrateDiskLabels; i++) {
            parameters[fixedParams + i] = new TypedStringParameter(Domain.DomainMigrateParameters.VIR_MIGRATE_PARAM_MIGRATE_DISKS, iterator.next());
        }

        return parameters;
    }

}
