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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.cloud.utils.script.Script;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.hypervisor.kvm.resource.LibvirtGpuDef;
import com.cloud.hypervisor.kvm.resource.LibvirtXMLParser;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.DomainJobInfo;
import org.libvirt.LibvirtException;
import org.libvirt.StorageVol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.DpdkTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.disconnecthook.MigrationCancelHook;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.MigrateKVMAsync;
import com.cloud.hypervisor.kvm.resource.VifDriver;

@ResourceWrapper(handles =  MigrateCommand.class)
public final class LibvirtMigrateCommandWrapper extends CommandWrapper<MigrateCommand, Answer, LibvirtComputingResource> {

    private static final String GRAPHICS_ELEM_END = "/graphics>";
    private static final String GRAPHICS_ELEM_START = "<graphics";
    private static final String CONTENTS_WILDCARD = "(?s).*";
    private static final String CDROM_LABEL = "hdc";

    protected String createMigrationURI(final String destinationIp, final LibvirtComputingResource libvirtComputingResource) {
        if (StringUtils.isEmpty(destinationIp)) {
            throw new CloudRuntimeException("Provided libvirt destination ip is invalid");
        }
        return String.format("%s://%s/system", libvirtComputingResource.isHostSecured() ? "qemu+tls" : "qemu+tcp", destinationIp);
    }

    @Override
    public Answer execute(final MigrateCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final Map<String, Boolean> vlanToPersistenceMap = command.getVlanToPersistenceMap();
        final String destinationUri = createMigrationURI(command.getDestinationIp(), libvirtComputingResource);
        final List<MigrateDiskInfo> migrateDiskInfoList = command.getMigrateDiskInfoList();
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Trying to migrate VM [%s] to destination host: [%s].", vmName, destinationUri));
        }

        // opt-in CPU-compatibility precheck (virsh cpu-compare) run BEFORE any migration setup,
        // so an incompatible destination fails fast with a clear message and the source domain is never
        // touched. Fail-open: if the check cannot run it does not block the migration.
        if (Boolean.TRUE.equals(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.MIGRATE_CPU_PRECHECK_ENABLED))) {
            final String cpuError = precheckDestinationCpu(vmName, destinationUri, libvirtComputingResource);
            if (cpuError != null) {
                logger.warn(cpuError);
                return new MigrateAnswer(command, false, cpuError, null);
            }
        }

        String result = null;
        Command.State commandState = null;

        List<InterfaceDef> ifaces = null;
        List<DiskDef> disks = new ArrayList<>();
        VirtualMachineTO to = null;

        Domain dm = null;
        Connect dconn = null;
        Domain destDomain = null;
        Connect conn = null;
        String xmlDesc = null;
        List<Ternary<String, Boolean, String>> vmsnapshots = null;
        MigrationCancelHook cancelHook = null;

        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            ifaces = libvirtComputingResource.getInterfaces(conn, vmName);
            disks = libvirtComputingResource.getDisks(conn, vmName);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Found domain with name [%s]. Starting VM migration to host [%s].", vmName, destinationUri));
            }
            to = command.getVirtualMachine();

            dm = conn.domainLookupByName(vmName);
            /*
                We replace the private IP address with the address of the destination host.
                This is because the VNC listens on the private IP address of the hypervisor,
                but that address is of course different on the target host.

                MigrateCommand.getDestinationIp() returns the private IP address of the target
                hypervisor. So it's safe to use.

                The Domain.migrate method from libvirt supports passing a different XML
                description for the instance to be used on the target host.

                This is supported by libvirt-java from version 0.50.0

                CVE-2015-3252: Get XML with sensitive information suitable for migration by using
                               VIR_DOMAIN_XML_MIGRATABLE flag (value = 8)
                               https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainXMLFlags

                               Use VIR_DOMAIN_XML_SECURE (value = 1) prior to v1.0.0.
             */
            final int xmlFlag = conn.getLibVirVersion() >= 1000000 ? 8 : 1; // 1000000 equals v1.0.0

            final String target = command.getDestinationIp();
            xmlDesc = dm.getXMLDesc(xmlFlag);
            if (logger.isDebugEnabled()) {
                logger.debug("VM {} with XML configuration {} will be migrated to host {}.", vmName, maskSensitiveInfoInXML(xmlDesc), target);
            }

            // Limit the VNC password in case the length is greater than 8 characters
            // Since libvirt version 8 VNC passwords are limited to 8 characters
            String vncPassword = org.apache.commons.lang3.StringUtils.truncate(to.getVncPassword(), 8);
            xmlDesc = replaceIpForVNCInDescFileAndNormalizePassword(xmlDesc, target, vncPassword, vmName);

            // Replace Config Drive ISO path
            String oldIsoVolumePath = getOldVolumePath(disks, vmName);
            String newIsoVolumePath = getNewVolumePathIfDatastoreHasChanged(libvirtComputingResource, conn, to);
            if (newIsoVolumePath != null && !newIsoVolumePath.equals(oldIsoVolumePath)) {
                logger.debug(String.format("Editing mount path of ISO from %s to %s", oldIsoVolumePath, newIsoVolumePath));
                xmlDesc = replaceDiskSourceFile(xmlDesc, newIsoVolumePath, vmName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Replaced disk mount point {} with {} in Instance {} XML configuration. New XML configuration is {}.", oldIsoVolumePath, newIsoVolumePath, vmName, maskSensitiveInfoInXML(xmlDesc));
                }
            }

            // Replace CDROM ISO path
            String oldCdromIsoPath = getOldVolumePathForCdrom(disks, vmName);
            String newCdromIsoPath = getNewVolumePathForCdrom(libvirtComputingResource, conn, to);
            if (newCdromIsoPath != null && !newCdromIsoPath.equals(oldCdromIsoPath)) {
                xmlDesc = replaceCdromIsoPath(xmlDesc, vmName, oldCdromIsoPath, newCdromIsoPath);
            }

            // delete the metadata of vm snapshots before migration
            vmsnapshots = libvirtComputingResource.cleanVMSnapshotMetadata(dm);

            // Verify Format of backing file
            for (DiskDef disk : disks) {
                if (disk.getDeviceType() == DiskDef.DeviceType.DISK
                        && disk.getDiskFormatType() == DiskDef.DiskFmtType.QCOW2) {
                    libvirtComputingResource.setBackingFileFormat(disk.getDiskPath());
                }
            }

            Map<String, MigrateCommand.MigrateDiskInfo> mapMigrateStorage = command.getMigrateStorage();
            // migrateStorage is declared as final because the replaceStorage method may mutate mapMigrateStorage, but
            // migrateStorage's value should always only be associated with the initial state of mapMigrateStorage.
            final boolean migrateStorage = MapUtils.isNotEmpty(mapMigrateStorage);
            final boolean migrateStorageManaged = command.isMigrateStorageManaged();
            Set<String> migrateDiskLabels = null;

            if (migrateStorage) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Changing VM {} volumes during migration to host: {}.", vmName, target);
                }
                xmlDesc = replaceStorage(xmlDesc, mapMigrateStorage, migrateStorageManaged);
                if (logger.isDebugEnabled()) {
                    logger.debug("Changed VM {} XML configuration of used storage. New XML configuration is {}.", vmName, maskSensitiveInfoInXML(xmlDesc));
                }
                migrateDiskLabels = getMigrateStorageDeviceLabels(disks, mapMigrateStorage);
            }

            Map<String, DpdkTO> dpdkPortsMapping = command.getDpdkInterfaceMapping();
            if (MapUtils.isNotEmpty(dpdkPortsMapping)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Changing VM {} DPDK interfaces during migration to host: {}.", vmName, target);
                }
                xmlDesc = replaceDpdkInterfaces(xmlDesc, dpdkPortsMapping);
                if (logger.isDebugEnabled()) {
                    logger.debug("Changed VM {} XML configuration of DPDK interfaces. New XML configuration is {}.", vmName, maskSensitiveInfoInXML(xmlDesc));
                }
            }

            xmlDesc = updateVmSharesIfNeeded(command, xmlDesc, libvirtComputingResource);

            xmlDesc = updateGpuDevicesIfNeeded(command, xmlDesc, libvirtComputingResource);

            dconn = libvirtUtilitiesHelper.retrieveQemuConnection(destinationUri);

            if (to.getType() == VirtualMachine.Type.User) {
                libvirtComputingResource.detachAndAttachConfigDriveISO(conn, vmName);
            }

            //run migration in thread so we can monitor it
            logger.info("Starting live migration of instance {} to destination host {} having the final XML configuration: {}.", vmName, dconn.getURI(), maskSensitiveInfoInXML(xmlDesc));
            final ExecutorService executor = Executors.newFixedThreadPool(1);
            boolean migrateNonSharedInc = command.isMigrateNonSharedInc() && !migrateStorageManaged;

            // add cancel hook before we start. If migration fails to start and hook is called, it's non-fatal
            cancelHook = new MigrationCancelHook(dm);
            libvirtComputingResource.addDisconnectHook(cancelHook);

            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.PROCESSING);

            // Encrypt the migration data stream when the effective policy resolves to "Required". A blank or
            // "Disabled" MS policy defers to the per-host migrate.encryption.policy (the MS ConfigKey default
            // is "Disabled", not blank), so the per-host setting is never dead code; requires
            // migrate_tls_x509_* configured in qemu.conf.
            final String hostEncryptionPolicy = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.MIGRATE_ENCRYPTION_POLICY);
            final boolean encryptMigration = resolveEncryptMigration(command.getMigrationEncryptionPolicy(), hostEncryptionPolicy);
            final boolean parallelMigration = Boolean.TRUE.equals(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.MIGRATE_PARALLEL_ENABLED));
            // allow libvirt-"unsafe" migrations (e.g. writeback cache on coherent Ceph storage).
            final boolean allowUnsafeMigration = Boolean.TRUE.equals(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.MIGRATE_ALLOW_UNSAFE));
            // optional migration compression method (xbzrle or mt); blank leaves libvirt's default.
            final String compressionMethod = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.MIGRATE_COMPRESSION_METHOD);
            final int parallelConnections = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.MIGRATE_PARALLEL_CONNECTIONS);
            // when a dedicated migration network is configured, the management server resolves the destination
            // host's migration-NIC IP and sets it on the command; route the data stream (URI + listen address)
            // there instead of the management IP. The libvirt control connection stays on the management IP.
            final String configuredMigrateIp = command.getMigrateIp();
            final boolean dedicatedMigrationNetwork = StringUtils.isNotBlank(configuredMigrateIp);
            final String migrateDataIp = dedicatedMigrationNetwork ? configuredMigrateIp : command.getDestinationIp();
            if (dedicatedMigrationNetwork) {
                logger.info("Live migration of VM {} will use dedicated migration address {} for the data stream instead of the management address {}.",
                        vmName, migrateDataIp, command.getDestinationIp());
            }
            final Callable<Domain> worker = new MigrateKVMAsync(libvirtComputingResource, dm, dconn, xmlDesc,
                    migrateStorage, migrateNonSharedInc,
                    command.isAutoConvergence(), encryptMigration, parallelMigration, allowUnsafeMigration, compressionMethod, parallelConnections, vmName, migrateDataIp,
                    dedicatedMigrationNetwork ? configuredMigrateIp : null, migrateDiskLabels);
            final Future<Domain> migrateThread = executor.submit(worker);
            executor.shutdown();
            long sleeptime = 0;
            final int migrateDowntime = libvirtComputingResource.getMigrateDowntime();
            boolean isMigrateDowntimeSet = false;

            final int migrateWait = libvirtComputingResource.getMigrateWait();
            logger.info("vm.migrate.wait value set to: {} secs for VM: {}", migrateWait, vmName);

            final int migratePauseAfter = libvirtComputingResource.getMigratePauseAfter();
            logger.info("vm.migrate.pauseafter value set to: {} ms for VM: {}", migratePauseAfter, vmName);

            while (!executor.isTerminated()) {
                Thread.sleep(100);
                sleeptime += 100;
                if (!isMigrateDowntimeSet && migrateDowntime > 0 && sleeptime >= 1000) { // wait 1s before attempting to set downtime on migration, since I don't know of a VIR_DOMAIN_MIGRATING state
                    try {
                        final int setDowntime = dm.migrateSetMaxDowntime(migrateDowntime);
                        if (setDowntime == 0 ) {
                            isMigrateDowntimeSet = true;
                            logger.debug("Set max downtime for migration of " + vmName + " to " + String.valueOf(migrateDowntime) + "ms");
                        }
                    } catch (final LibvirtException e) {
                        logger.debug("Failed to set max downtime for migration, perhaps migration completed? Error: " + e.getMessage());
                    }
                }
                if (sleeptime % 1000 == 0) {
                    // surface migration progress (percent of migration data transferred) in the periodic log.
                    int progressPercent = -1;
                    try {
                        final DomainJobInfo job = dm.getJobInfo();
                        progressPercent = computeMigrationProgressPercent(job.getDataProcessed(), job.getDataRemaining());
                    } catch (final LibvirtException e) {
                        logger.trace("Could not read migration job info for progress reporting: {}", e.getMessage());
                    }
                    logger.info("Waiting for migration of {} to complete, waited {}ms, progress: {}", vmName, sleeptime,
                            progressPercent < 0 ? "unknown" : progressPercent + "%");
                }

                // abort the vm migration if the job is executed more than vm.migrate.wait
                if (migrateWait > 0 && sleeptime > migrateWait * 1000) {
                    DomainState state = null;
                    try {
                        state = dm.getInfo().state;
                        logger.info("VM domain state when trying to abort migration : {}", state);
                    } catch (final LibvirtException e) {
                        logger.info("Couldn't get VM domain state after " + sleeptime + "ms: " + e.getMessage());
                    }
                    if (state != null && (state == DomainState.VIR_DOMAIN_RUNNING || state == DomainState.VIR_DOMAIN_PAUSED)) {
                        try {
                            DomainJobInfo job = dm.getJobInfo();
                            logger.warn("Aborting migration of VM {} with domain job [{}] due to timeout after {} seconds. " +
                                    "Job stats: data processed={} bytes, data remaining={} bytes", vmName, job, migrateWait, job.getDataProcessed(), job.getDataRemaining());
                            dm.abortJob();
                            result = String.format("Migration of VM [%s] was cancelled by CloudStack due to time out after %d seconds.", vmName, migrateWait);
                            commandState = Command.State.FAILED;
                            libvirtComputingResource.createOrUpdateLogFileForCommand(command, commandState);
                            logger.debug(result);
                            break;
                        } catch (final LibvirtException e) {
                            // Do NOT mark the migration failed here: abortJob throws both when the migration just
                            // completed (no active job left to abort) and on a transient error while it is still
                            // running. Log and let the loop continue - a completed migration ends the loop with
                            // destDomain != null and is reported successful (no split brain), while a still-running
                            // one is retried on the next pass and ultimately bounded by migrateThread.get below.
                            logger.warn(String.format("Could not abort the migration job of VM [%s] after the vm.migrate.wait timeout: %s", vmName, e.getMessage()), e);
                        }
                    }
                }

                // pause vm if we meet the vm.migrate.pauseafter threshold and not already paused
                if (migratePauseAfter > 0 && sleeptime > migratePauseAfter) {
                    DomainState state = null;
                    try {
                        state = dm.getInfo().state;
                        logger.info("VM domain state when trying to pause VM for migration: {}", state);
                    } catch (final LibvirtException e) {
                        logger.info("Couldn't get VM domain state after " + sleeptime + "ms: " + e.getMessage());
                    }
                    if (state != null && state == DomainState.VIR_DOMAIN_RUNNING) {
                        try {
                            logger.info("Pausing VM " + vmName + " due to property vm.migrate.pauseafter setting to " + migratePauseAfter + "ms to complete migration");
                            dm.suspend();
                        } catch (final LibvirtException e) {
                            // pause could be racy if it attempts to pause right when vm is finished, simply warn
                            logger.info("Failed to pause vm " + vmName + " : " + e.getMessage());
                        }
                    }
                }
            }
            logger.info(String.format("Migration thread of VM [%s] finished.", vmName));

            destDomain = migrateThread.get(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.VM_MIGRATE_DOMAIN_RETRIEVE_TIMEOUT), TimeUnit.SECONDS);

            if (destDomain != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Cleaning the disks of VM [%s] in the source pool after VM migration finished.", vmName));
                }
                // The guest is now on the destination and the source domain is about to be undefined, so the
                // migration (the move) has succeeded. If it could not be brought out of PAUSED we must NOT report
                // failure - that would make the management server believe the VM is still on the source and could
                // trigger HA against a host that no longer runs it (split brain). Surface it loudly instead; the
                // power-state sync and the operator can resume the paused guest on the destination.
                final String resumeFailure = resumeDomainIfPaused(destDomain, vmName);
                if (resumeFailure != null) {
                    logger.warn(String.format("VM [%s] migrated to the destination but is still PAUSED there: [%s]. The migration is reported as successful because the VM now lives on the destination; resume it on the destination host.", vmName, resumeFailure));
                }

                // For cross-pool CLVM migration, skip deactivation so the source LV stays
                // active (in shared mode) and deletion can route directly to the source host
                // without fanning out across the cluster to find an inactive LV.
                if (to != null && !command.isClvmCrossPoolMigration()) {
                    LibvirtComputingResource.modifyClvmVolumesStateForMigration(disks, to, LibvirtComputingResource.ClvmVolumeState.DEACTIVATE);
                }

                deleteOrDisconnectDisksOnSourcePool(libvirtComputingResource, migrateDiskInfoList, disks);
                libvirtComputingResource.cleanOldSecretsByDiskDef(conn, disks);
            }

        } catch (final LibvirtException e) {
            logger.error(String.format("Can't migrate domain [%s] due to: [%s].", vmName, e.getMessage()), e);
            result = e.getMessage();
            if (result.startsWith("unable to connect to server") && result.endsWith("refused")) {
                logger.debug("Migration failed as connection to destination [{}] was refused. Please check libvirt configuration compatibility and firewall rules on the source and destination hosts.", destinationUri);
                result = String.format("Failed to migrate domain [%s].", vmName);
            }
        } catch (final InterruptedException
            | ExecutionException
            | TimeoutException
            | IOException
            | ParserConfigurationException
            | SAXException
            | TransformerException
            | URISyntaxException e) {
            logger.error(String.format("Can't migrate domain [%s] due to: [%s].", vmName, e.getMessage()), e);
            if (result == null) {
                result = "Exception during migrate: " + e.getMessage();
            }
        } finally {
            if (cancelHook != null) {
                libvirtComputingResource.removeDisconnectHook(cancelHook);
            }
            try {
                if (dm != null && result != null) {
                    // restore vm snapshots in case of failed migration
                    if (vmsnapshots != null) {
                        libvirtComputingResource.restoreVMSnapshotMetadata(dm, vmName, vmsnapshots);
                    }
                }
                if (dm != null) {
                    if (dm.isPersistent() == 1) {
                        dm.undefine();
                    }
                    dm.free();
                }
                if (dconn != null) {
                    dconn.close();
                }
                if (destDomain != null) {
                    destDomain.free();
                }
                // Revert CLVM volumes to exclusive mode on failure
                if (to != null && result != null) {
                    LibvirtComputingResource.modifyClvmVolumesStateForMigration(disks, to, LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE);
                }
            } catch (final LibvirtException e) {
                logger.trace("Ignoring libvirt error.", e);
            }
        }

        if (result == null) {
            logger.info("Post-migration cleanup for VM {}: ", vmName);
            cleanupSourceNetworkingAfterMigration(conn, vmName, ifaces, vlanToPersistenceMap, libvirtComputingResource);
            commandState = Command.State.COMPLETED;
            libvirtComputingResource.createOrUpdateLogFileForCommand(command, commandState);
        } else if (commandState == null) {
            logger.error("Migration of VM {} failed with result: {}", vmName, result);
            commandState = Command.State.FAILED;
            libvirtComputingResource.createOrUpdateLogFileForCommand(command, commandState);
        }

        return new MigrateAnswer(command, result == null, result, null);
    }

    // cap the CPU precheck so a firewalled/unreachable destination fails fast instead of
    // hanging virsh forever (the opposite of the "fail fast" the precheck is meant to provide).
    private static final int CPU_PRECHECK_TIMEOUT_SECONDS = 30;
    private static final Pattern SAFE_MIGRATION_URI = Pattern.compile("qemu\\+(tcp|tls)://[A-Za-z0-9._:\\[\\]-]+/system");

    /**
     * effective migration-encryption decision. A non-Disabled MS-central policy wins; a blank or
     * "Disabled" MS policy defers to the per-host agent property (so the per-host setting is never dead code).
     */
    protected boolean resolveEncryptMigration(final String commandPolicy, final String hostPolicy) {
        String effective = commandPolicy;
        if (StringUtils.isBlank(effective) || "Disabled".equalsIgnoreCase(effective.trim())) {
            effective = hostPolicy;
        }
        // Only the exact "Required" policy enables TLS; anything else (Disabled, blank or an unrecognised value)
        // stays plaintext, so a typo never silently turns encryption on. libvirt has no opportunistic/fallback
        // TLS mode, so Required means TLS or the migration fails - there is no partial state to model here.
        return effective != null && "Required".equalsIgnoreCase(effective.trim());
    }

    // matches the domain's top-level <cpu> element (paired or self-closing). \b after "cpu"
    // excludes <cputune>; non-greedy .*? is safe because <cpu> does not nest another <cpu>.
    private static final Pattern CPU_ELEMENT_PATTERN = Pattern.compile("<cpu\\b[^>]*/>|<cpu\\b.*?</cpu>", Pattern.DOTALL);

    /**
     * pull the VM's &lt;cpu&gt; definition out of its domain XML so it can be checked against a
     * destination host with {@code virsh cpu-compare}. Returns null when the VM has no explicit CPU
     * model (any host can then run it).
     */
    protected String extractCpuElement(final String domainXml) {
        if (domainXml == null) {
            return null;
        }
        final Matcher matcher = CPU_ELEMENT_PATTERN.matcher(domainXml);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * interpret {@code virsh cpu-compare} output. libvirt prints "incompatible" / "not a
     * superset" when the host cannot run the given CPU. Fail-open: blank/unknown output is treated as
     * compatible, so a check that could not run never blocks a migration.
     */
    protected boolean isCpuCompareOutputCompatible(final String virshOutput) {
        return LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(virshOutput);
    }

    protected String runDestinationCpuCompare(final String cpuXml, final String destinationUri) throws IOException {
        if (destinationUri == null || !SAFE_MIGRATION_URI.matcher(destinationUri).matches()) {
            throw new IOException("Unexpected destination URI for the CPU precheck: " + destinationUri);
        }
        final File tmp = File.createTempFile("cloudstack-cpucheck-", ".xml");
        try {
            Files.write(tmp.toPath(), cpuXml.getBytes(StandardCharsets.UTF_8));
            // full result with a forced zero exit so the complete verdict is parsed, not just virsh's first line.
            return Script.runSimpleBashScriptWithFullResult(String.format("timeout %d virsh -c %s cpu-compare %s 2>&1 || true",
                    CPU_PRECHECK_TIMEOUT_SECONDS, destinationUri, tmp.getAbsolutePath()), CPU_PRECHECK_TIMEOUT_SECONDS + 10);
        } finally {
            if (!tmp.delete()) {
                tmp.deleteOnExit();
            }
        }
    }

    /**
     * opt-in CPU-compatibility precheck, run BEFORE any migration setup so a rejection fails
     * fast and never touches the source domain. Returns an error message if the destination host CPU
     * is incompatible, or null if it is compatible / the check could not run (best-effort, fail-open).
     */
    protected String precheckDestinationCpu(final String vmName, final String destinationUri, final LibvirtComputingResource libvirtComputingResource) {
        try {
            final LibvirtUtilitiesHelper helper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = helper.getConnectionByVmName(vmName);
            final Domain dm = conn.domainLookupByName(vmName);
            final int xmlFlag = conn.getLibVirVersion() >= 1000000 ? 8 : 1;
            final String cpuXml = extractCpuElement(dm.getXMLDesc(xmlFlag));
            if (cpuXml == null) {
                return null;
            }
            final String output = runDestinationCpuCompare(cpuXml, destinationUri);
            if (!isCpuCompareOutputCompatible(output)) {
                return String.format("Cannot migrate VM [%s]: the destination host CPU is not compatible with the VM's CPU " +
                        "(virsh cpu-compare: %s). Choose a destination host with a compatible or superset CPU.",
                        vmName, output == null ? "no result" : output.trim());
            }
            return null;
        } catch (final Exception e) {
            logger.warn(String.format("CPU-compatibility precheck for VM [%s] could not run; proceeding with migration: %s", vmName, e.getMessage()));
            return null;
        }
    }

    /**
     * migration progress as a 0-100 percent of migration data transferred, from libvirt job stats.
     * Returns -1 when the total is not yet known, so the caller logs "unknown" rather than a
     * misleading 0%.
     */
    protected int computeMigrationProgressPercent(final long dataProcessed, final long dataRemaining) {
        final long total = dataProcessed + dataRemaining;
        if (total <= 0) {
            return -1;
        }
        return (int) Math.min(100, (dataProcessed * 100) / total);
    }

    private DomainState getDestDomainState(Domain destDomain, String vmName) {
        DomainState dmState = null;
        try {
            dmState = destDomain.getInfo().state;
        } catch (final LibvirtException e) {
            logger.info("Failed to get domain state for VM: " + vmName + " due to: " + e.getMessage());
        }
        return dmState;
    }

    /**
     * Resume a destination domain that libvirt left paused after migration. The migration itself has already
     * succeeded (the guest now lives on the destination), so the caller keeps the result successful; this returns a
     * message only so the caller can surface the still-paused state as a warning for the operator to resume.
     *
     * @return {@code null} if the domain is running (or was never paused); otherwise a message describing the
     *         still-not-running state on the destination.
     */
    protected String resumeDomainIfPaused(Domain destDomain, String vmName) {
        DomainState dmState = getDestDomainState(destDomain, vmName);
        if (dmState != DomainState.VIR_DOMAIN_PAUSED) {
            return null;
        }
        logger.info("Resuming VM " + vmName + " on destination after migration");
        try {
            destDomain.resume();
        } catch (final Exception e) {
            logger.error("Failed to resume vm " + vmName + " on destination after migration due to : " + e.getMessage());
        }
        DomainState afterState = getDestDomainState(destDomain, vmName);
        if (afterState == DomainState.VIR_DOMAIN_RUNNING) {
            return null;
        }
        return String.format("the guest is on the destination but not running (state: %s); resume it on the destination host", afterState);
    }

    /**
     * Tears down source-side networking after a successful migration. The guest is already live on the
     * destination, so a failure here must not flip a successful migration to FAILED (which would make
     * orchestration roll back or mark a running VM inconsistent); the error is logged and swallowed.
     */
    protected void cleanupSourceNetworkingAfterMigration(Connect conn, String vmName, List<InterfaceDef> ifaces,
            Map<String, Boolean> vlanToPersistenceMap, LibvirtComputingResource libvirtComputingResource) {
        try {
            libvirtComputingResource.destroyNetworkRulesForVM(conn, vmName);
            for (final InterfaceDef iface : ifaces) {
                String vlanId = libvirtComputingResource.getVlanIdFromBridgeName(iface.getBrName());
                // the traffic type of each interface is unknown here, so inform all vif drivers
                for (final VifDriver vifDriver : libvirtComputingResource.getAllVifDrivers()) {
                    vifDriver.unplug(iface, libvirtComputingResource.shouldDeleteBridge(vlanToPersistenceMap, vlanId));
                }
            }
        } catch (final Exception e) {
            logger.warn("Migration of VM [{}] succeeded, but source-side network cleanup failed: {}. " +
                    "Keeping the migration result as successful.", vmName, e.getMessage(), e);
        }
    }

    /**
     * Gets the disk labels (vda, vdb...) of the disks mapped for migration on mapMigrateStorage.
     * @param diskDefinitions list of all the disksDefinitions of the VM.
     * @param mapMigrateStorage map of the disks that should be migrated.
     * @return set with the labels of the disks that should be migrated.
     * */
    protected Set<String> getMigrateStorageDeviceLabels(List<DiskDef> diskDefinitions, Map<String, MigrateCommand.MigrateDiskInfo> mapMigrateStorage) {
        HashSet<String> setOfLabels = new HashSet<>();
        logger.debug("Searching for disk labels of disks [{}].", mapMigrateStorage.keySet());
        for (String fileName : mapMigrateStorage.keySet()) {
            for (DiskDef diskDef : diskDefinitions) {
                String diskPath = diskDef.getDiskPath();
                if (diskPath != null && diskPath.contains(fileName)) {
                    setOfLabels.add(diskDef.getDiskLabel());
                    logger.debug("Found label [{}] for disk [{}].", diskDef.getDiskLabel(), fileName);
                    break;
                }
            }
        }

        return setOfLabels;
    }

        String updateGpuDevicesIfNeeded(MigrateCommand migrateCommand, String xmlDesc, LibvirtComputingResource libvirtComputingResource)
            throws ParserConfigurationException, IOException, SAXException, TransformerException {
        GPUDeviceTO gpuDevice = migrateCommand.getVirtualMachine().getGpuDevice();
        if (gpuDevice == null || CollectionUtils.isEmpty(gpuDevice.getGpuDevices())) {
            logger.debug("No GPU device to update for VM [{}].", migrateCommand.getVmName());
            return xmlDesc;
        }

        List<VgpuTypesInfo> devices = gpuDevice.getGpuDevices();
        logger.info("Updating GPU devices for VM [{}] during migration. Number of devices: {}",
                    migrateCommand.getVmName(), devices.size());

        // Parse XML and find devices element
        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document document;
        try (InputStream inputStream = IOUtils.toInputStream(xmlDesc, StandardCharsets.UTF_8)) {
            document = docBuilder.parse(inputStream);
        }

        NodeList devicesList = document.getElementsByTagName("devices");
        if (devicesList.getLength() == 0) {
            logger.warn("No devices section found in XML for VM [{}]", migrateCommand.getVmName());
            return xmlDesc;
        }

        Element devicesElement = (Element) devicesList.item(0);

        // Remove existing GPU hostdev elements and add new ones
        removeExistingGpuHostdevElements(devicesElement);
        addNewGpuHostdevElements(document, devicesElement, devices);

        String newXmlDesc = LibvirtXMLParser.getXml(document);
        logger.debug("Updated XML configuration for VM [{}] with new GPU devices", migrateCommand.getVmName());

        return newXmlDesc;
    }

    /**
     * Removes existing GPU hostdev elements from the devices section.
     * GPU devices are identified as hostdev elements with type='pci' or type='mdev'.
     */
    private void removeExistingGpuHostdevElements(Element devicesElement) {
        NodeList hostdevNodes = devicesElement.getElementsByTagName("hostdev");
        List<Node> nodesToRemove = new ArrayList<>();

        for (int i = 0; i < hostdevNodes.getLength(); i++) {
            Node hostdevNode = hostdevNodes.item(i);
            if (hostdevNode.getNodeType() == Node.ELEMENT_NODE) {
                Element hostdevElement = (Element) hostdevNode;
                String hostdevType = hostdevElement.getAttribute("type");

                // Remove hostdev elements that represent GPU devices (type='pci' or type='mdev')
                if ("pci".equals(hostdevType) || "mdev".equals(hostdevType)) {
                    // Additional check: ensure this is actually a GPU device by checking mode='subsystem'
                    String mode = hostdevElement.getAttribute("mode");
                    if ("subsystem".equals(mode)) {
                        nodesToRemove.add(hostdevNode);
                    }
                }
            }
        }

        // Remove the nodes
        for (Node node : nodesToRemove) {
            devicesElement.removeChild(node);
        }

        logger.debug("Removed {} existing GPU hostdev elements", nodesToRemove.size());
    }

    /**
     * Adds new GPU hostdev elements to the devices section based on the GPU devices
     * allocated on the destination host.
     */
    private void addNewGpuHostdevElements(Document document, Element devicesElement, List<VgpuTypesInfo> devices)
            throws ParserConfigurationException, IOException, SAXException {
        if (devices.isEmpty()) {
            return;
        }

        // Reuse parser for efficiency
        DocumentBuilderFactory factory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        for (VgpuTypesInfo deviceInfo : devices) {
            Element hostdevElement = createGpuHostdevElement(document, deviceInfo, builder);
            devicesElement.appendChild(hostdevElement);
            logger.debug("Added new GPU hostdev element for device: {} (type: {}, busAddress: {})",
                         deviceInfo.getDeviceName(), deviceInfo.getDeviceType(), deviceInfo.getBusAddress());
        }
    }

    /**
     * Creates a hostdev element for a GPU device using LibvirtGpuDef.
     */
    private Element createGpuHostdevElement(Document document, VgpuTypesInfo deviceInfo, DocumentBuilder builder)
            throws IOException, SAXException {
        // Generate GPU XML using LibvirtGpuDef
        LibvirtGpuDef gpuDef = new LibvirtGpuDef();
        gpuDef.defGpu(deviceInfo);
        String gpuXml = gpuDef.toString();

        // Parse and import into target document
        try (InputStream xmlStream = IOUtils.toInputStream(gpuXml, StandardCharsets.UTF_8)) {
            Document gpuDocument = builder.parse(xmlStream);
            Element hostdevElement = gpuDocument.getDocumentElement();
            return (Element) document.importNode(hostdevElement, true);
        }
    }

    /**
     * Checks if the CPU shares are equal in the source host and destination host.
     *  <ul>
     *      <li>
     *          If both hosts utilize cgroup v1; then, the shares value of the VM is equal in both hosts, and there is no need to update the VM CPU shares value for the
     *          migration.</li>
     *      <li>
     *          If, at least, one of the hosts utilize cgroup v2, the VM CPU shares must be recalculated for the migration, accordingly to
     *          method {@link LibvirtComputingResource#calculateCpuShares(VirtualMachineTO)}.
     *      </li>
     *  </ul>
     */
    protected String updateVmSharesIfNeeded(MigrateCommand migrateCommand, String xmlDesc, LibvirtComputingResource libvirtComputingResource)
            throws ParserConfigurationException, IOException, SAXException, TransformerException {
        Integer newVmCpuShares = migrateCommand.getNewVmCpuShares();
        int currentCpuShares = libvirtComputingResource.calculateCpuShares(migrateCommand.getVirtualMachine());

        if (newVmCpuShares == currentCpuShares) {
            logger.info(String.format("Current CPU shares [%s] is equal in both hosts; therefore, there is no need to update the CPU shares for the new host.",
                    currentCpuShares));
            return xmlDesc;
        }

        InputStream inputStream = IOUtils.toInputStream(xmlDesc, StandardCharsets.UTF_8);
        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document document = docBuilder.parse(inputStream);

        Element root = document.getDocumentElement();
        Node sharesNode = root.getElementsByTagName("shares").item(0);
        String currentShares = sharesNode.getTextContent();

        logger.info(String.format("VM [%s] will have CPU shares altered from [%s] to [%s] as part of migration because the cgroups version differs between hosts.",
                migrateCommand.getVmName(), currentShares, newVmCpuShares));
        sharesNode.setTextContent(String.valueOf(newVmCpuShares));
        return LibvirtXMLParser.getXml(document);
    }

    /**
     * Replace DPDK source path and target before migrations
     */
    protected String replaceDpdkInterfaces(String xmlDesc, Map<String, DpdkTO> dpdkPortsMapping) throws TransformerException, ParserConfigurationException, IOException, SAXException {
        InputStream in = IOUtils.toInputStream(xmlDesc);

        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);

        // Get the root element
        Node domainNode = doc.getFirstChild();

        NodeList domainChildNodes = domainNode.getChildNodes();

        for (int i = 0; i < domainChildNodes.getLength(); i++) {
            Node domainChildNode = domainChildNodes.item(i);

            if ("devices".equals(domainChildNode.getNodeName())) {
                NodeList devicesChildNodes = domainChildNode.getChildNodes();

                for (int x = 0; x < devicesChildNodes.getLength(); x++) {
                    Node deviceChildNode = devicesChildNodes.item(x);

                    if ("interface".equals(deviceChildNode.getNodeName())) {
                        Node interfaceNode = deviceChildNode;
                        NamedNodeMap attributes = interfaceNode.getAttributes();
                        Node interfaceTypeAttr = attributes.getNamedItem("type");

                        if ("vhostuser".equals(interfaceTypeAttr.getNodeValue())) {
                            NodeList diskChildNodes = interfaceNode.getChildNodes();

                            String mac = null;
                            for (int y = 0; y < diskChildNodes.getLength(); y++) {
                                Node diskChildNode = diskChildNodes.item(y);
                                if (!"mac".equals(diskChildNode.getNodeName())) {
                                    continue;
                                }
                                mac = diskChildNode.getAttributes().getNamedItem("address").getNodeValue();
                            }

                            if (StringUtils.isNotBlank(mac)) {
                                DpdkTO to = dpdkPortsMapping.get(mac);

                                for (int z = 0; z < diskChildNodes.getLength(); z++) {
                                    Node diskChildNode = diskChildNodes.item(z);

                                    if ("target".equals(diskChildNode.getNodeName())) {
                                        Node targetNode = diskChildNode;
                                        Node targetNodeAttr = targetNode.getAttributes().getNamedItem("dev");
                                        targetNodeAttr.setNodeValue(to.getPort());
                                    } else if ("source".equals(diskChildNode.getNodeName())) {
                                        Node sourceNode = diskChildNode;
                                        NamedNodeMap attrs = sourceNode.getAttributes();
                                        Node path = attrs.getNamedItem("path");
                                        path.setNodeValue(to.getPath() + "/" + to.getPort());
                                        Node mode = attrs.getNamedItem("mode");
                                        mode.setNodeValue(to.getMode());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return LibvirtXMLParser.getXml(doc);
    }

    /**
     * In case of a local file, it deletes the file on the source host/storage pool. Otherwise (for instance iScsi) it disconnects the disk on the source storage pool. </br>
     * This method must be executed after a successful migration to a target storage pool, cleaning up the source storage.
     */
    protected void deleteOrDisconnectDisksOnSourcePool(final LibvirtComputingResource libvirtComputingResource, final List<MigrateDiskInfo> migrateDiskInfoList,
            List<DiskDef> disks) {
        for (DiskDef disk : disks) {
            MigrateCommand.MigrateDiskInfo migrateDiskInfo = searchDiskDefOnMigrateDiskInfoList(migrateDiskInfoList, disk);
            if (migrateDiskInfo != null && migrateDiskInfo.isSourceDiskOnStorageFileSystem()) {
                deleteLocalVolume(disk.getDiskPath());
            } else {
                libvirtComputingResource.cleanupDisk(disk);
            }
        }
    }

    /**
     * Deletes the local volume from the storage pool.
     */
    protected void deleteLocalVolume(String localPath) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            StorageVol storageVolLookupByPath = conn.storageVolLookupByPath(localPath);
            storageVolLookupByPath.delete(0);
        } catch (LibvirtException e) {
            logger.error(String.format("Cannot delete local volume [%s] due to: %s", localPath, e));
        }
    }

    /**
     * Searches for a {@link MigrateDiskInfo} with the path matching the {@link DiskDef} path.
     */
    protected MigrateDiskInfo searchDiskDefOnMigrateDiskInfoList(List<MigrateDiskInfo> migrateDiskInfoList, DiskDef disk) {
        for (MigrateDiskInfo migrateDiskInfo : migrateDiskInfoList) {
            if (StringUtils.contains(disk.getDiskPath(), migrateDiskInfo.getSerialNumber())) {
                return migrateDiskInfo;
            }
        }
        logger.debug(String.format("Cannot find Disk [uuid: %s] on the list of disks to be migrated", disk.getDiskPath()));
        return null;
    }

    /**
     * This function assumes an qemu machine description containing a single graphics element like
     *     <graphics type='vnc' port='5900' autoport='yes' listen='10.10.10.1'>
     *       <listen type='address' address='10.10.10.1'/>
     *     </graphics>
     * @param xmlDesc the qemu xml description
     * @param target the ip address to migrate to
     * @param vncPassword if set, the VNC password truncated to 8 characters
     * @return the new xmlDesc
     */
    String replaceIpForVNCInDescFileAndNormalizePassword(String xmlDesc, final String target, String vncPassword, String vmName) {
        final int begin = xmlDesc.indexOf(GRAPHICS_ELEM_START);
        if (begin >= 0) {
            final int end = xmlDesc.lastIndexOf(GRAPHICS_ELEM_END) + GRAPHICS_ELEM_END.length();
            if (end > begin) {
                String originalGraphElem = xmlDesc.substring(begin, end);
                String graphElem = xmlDesc.substring(begin, end);
                graphElem = graphElem.replaceAll("listen='[a-zA-Z0-9\\.]*'", "listen='" + target + "'");
                graphElem = graphElem.replaceAll("address='[a-zA-Z0-9\\.]*'", "address='" + target + "'");
                if (org.apache.commons.lang3.StringUtils.isNotBlank(vncPassword)) {
                    graphElem = graphElem.replaceAll("passwd='([^\\s]+)'", "passwd='" + vncPassword + "'");
                }
                xmlDesc = xmlDesc.replaceAll(GRAPHICS_ELEM_START + CONTENTS_WILDCARD + GRAPHICS_ELEM_END, graphElem);
                logger.debug("Replaced the VNC IP address {} with {} in VM {}.", maskSensitiveInfoInXML(originalGraphElem), maskSensitiveInfoInXML(graphElem), vmName);
            }
        }
        return xmlDesc;
    }

    /**
     * Pass in a list of the disks to update in the XML (xmlDesc). Each disk passed in needs to have a serial number. If any disk's serial number in the
     * list does not match a disk in the XML, an exception should be thrown.
     * In addition to the serial number, each disk in the list needs the following info:
     * <ul>
     *  <li>The value of the 'type' of the disk (ex. file, block)
     *  <li>The value of the 'type' of the driver of the disk (ex. qcow2, raw)
     *  <li>The source of the disk needs an attribute that is either 'file' or 'dev' as well as its corresponding value.
     * </ul>
     */
    protected String replaceStorage(String xmlDesc, Map<String, MigrateCommand.MigrateDiskInfo> migrateStorage,
                                  boolean migrateStorageManaged)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        InputStream in = IOUtils.toInputStream(xmlDesc);

        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);

        // Get the root element
        Node domainNode = doc.getFirstChild();

        NodeList domainChildNodes = domainNode.getChildNodes();

        for (int i = 0; i < domainChildNodes.getLength(); i++) {
            Node domainChildNode = domainChildNodes.item(i);

            if ("devices".equals(domainChildNode.getNodeName())) {
                NodeList devicesChildNodes = domainChildNode.getChildNodes();

                for (int x = 0; x < devicesChildNodes.getLength(); x++) {
                    Node deviceChildNode = devicesChildNodes.item(x);

                    if ("disk".equals(deviceChildNode.getNodeName())) {
                        Node diskNode = deviceChildNode;

                        String sourceText = getSourceText(diskNode);

                        String path = getPathFromSourceText(migrateStorage.keySet(), sourceText);

                        if (path != null) {
                            MigrateCommand.MigrateDiskInfo migrateDiskInfo = migrateStorage.get(path);

                            NamedNodeMap diskNodeAttributes = diskNode.getAttributes();
                            Node diskNodeAttribute = diskNodeAttributes.getNamedItem("type");

                            diskNodeAttribute.setTextContent(migrateDiskInfo.getDiskType().toString());

                            NodeList diskChildNodes = diskNode.getChildNodes();

                            for (int z = 0; z < diskChildNodes.getLength(); z++) {
                                Node diskChildNode = diskChildNodes.item(z);

                                boolean shouldUpdateDriverType = shouldUpdateDriverTypeForMigration(
                                    migrateStorageManaged, migrateDiskInfo);

                                if (shouldUpdateDriverType && "driver".equals(diskChildNode.getNodeName())) {
                                    Node driverNode = diskChildNode;

                                    NamedNodeMap driverNodeAttributes = driverNode.getAttributes();
                                    Node driverNodeAttribute = driverNodeAttributes.getNamedItem("type");

                                    driverNodeAttribute.setTextContent(migrateDiskInfo.getDriverType().toString());
                                } else if ("source".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);

                                    Element newChildSourceNode = doc.createElement("source");

                                    newChildSourceNode.setAttribute(migrateDiskInfo.getSource().toString(), migrateDiskInfo.getSourceText());

                                    diskNode.appendChild(newChildSourceNode);
                                } else if (migrateStorageManaged && "auth".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);
                                } else if ("backingStore".equals(diskChildNode.getNodeName()) && migrateDiskInfo.getBackingStoreText() != null) {
                                    for (int b = 0; b < diskChildNode.getChildNodes().getLength(); b++) {
                                        Node backingChild = diskChildNode.getChildNodes().item(b);
                                        if ("source".equals(backingChild.getNodeName())) {
                                            diskChildNode.removeChild(backingChild);
                                            Element newChildBackingElement = doc.createElement("source");
                                            newChildBackingElement.setAttribute(migrateDiskInfo.getSource().toString(), migrateDiskInfo.getBackingStoreText());
                                            diskChildNode.appendChild(newChildBackingElement);
                                        }
                                    }
                                } else if ("encryption".equals(diskChildNode.getNodeName())) {
                                    for (int s = 0; s < diskChildNode.getChildNodes().getLength(); s++) {
                                        Node encryptionChild = diskChildNode.getChildNodes().item(s);
                                        if ("secret".equals(encryptionChild.getNodeName())) {
                                            NamedNodeMap secretAttributes = encryptionChild.getAttributes();
                                            Node uuidAttribute = secretAttributes.getNamedItem("uuid");
                                            String volumeFileName = FilenameUtils.getBaseName(migrateDiskInfo.getSourceText());
                                            String newSecretUuid = LibvirtComputingResource.generateSecretUUIDFromString(volumeFileName);
                                            uuidAttribute.setTextContent(newSecretUuid);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return LibvirtXMLParser.getXml(doc);
    }

    private  String getOldVolumePath(List<DiskDef> disks, String vmName) {
        String oldIsoVolumePath = null;
        for (DiskDef disk : disks) {
            if (disk.getDiskPath() != null && disk.getDiskPath().contains(vmName)) {
                oldIsoVolumePath = disk.getDiskPath();
                break;
            }
        }
        return oldIsoVolumePath;
    }

    private String getNewVolumePathIfDatastoreHasChanged(LibvirtComputingResource libvirtComputingResource, Connect conn, VirtualMachineTO to) throws LibvirtException, URISyntaxException {
        DiskTO newDisk = null;
        for (DiskTO disk : to.getDisks()) {
            if (disk.getPath() != null && disk.getPath().contains("configdrive")) {
                newDisk = disk;
                break;
            }
        }

        String newIsoVolumePath = null;
        if (newDisk != null) {
            newIsoVolumePath = libvirtComputingResource.getVolumePath(conn, newDisk, to.isConfigDriveOnHostCache());
        }
        return newIsoVolumePath;
    }

    private String getOldVolumePathForCdrom(List<DiskDef> disks, String vmName) {
        String oldIsoVolumePath = null;
        for (DiskDef disk : disks) {
            if (DiskDef.DeviceType.CDROM.equals(disk.getDeviceType())
                    && CDROM_LABEL.equals(disk.getDiskLabel())
                    && disk.getDiskPath() != null) {
                oldIsoVolumePath = disk.getDiskPath();
                break;
            }
        }
        return oldIsoVolumePath;
    }

    private String getNewVolumePathForCdrom(LibvirtComputingResource libvirtComputingResource, Connect conn, VirtualMachineTO to) throws LibvirtException, URISyntaxException {
        DiskTO newDisk = null;
        for (DiskTO disk : to.getDisks()) {
            DataTO data = disk.getData();
            if (disk.getDiskSeq() == 3 && data != null && data.getPath() != null) {
                newDisk = disk;
                break;
            }
        }

        String newIsoVolumePath = null;
        if (newDisk != null) {
            newIsoVolumePath = libvirtComputingResource.getVolumePath(conn, newDisk);
        }
        return newIsoVolumePath;
    }

    protected String replaceCdromIsoPath(String xmlDesc, String vmName, String oldIsoVolumePath, String newIsoVolumePath) throws IOException, ParserConfigurationException, TransformerException, SAXException {
        InputStream in = IOUtils.toInputStream(xmlDesc);

        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);

        // Get the root element
        Node domainNode = doc.getFirstChild();

        NodeList domainChildNodes = domainNode.getChildNodes();

        for (int i = 0; i < domainChildNodes.getLength(); i++) {
            Node domainChildNode = domainChildNodes.item(i);
            if ("devices".equals(domainChildNode.getNodeName())) {
                NodeList devicesChildNodes = domainChildNode.getChildNodes();
                for (int x = 0; x < devicesChildNodes.getLength(); x++) {
                    Node deviceChildNode = devicesChildNodes.item(x);
                    if ("disk".equals(deviceChildNode.getNodeName())) {
                        Node diskNode = deviceChildNode;
                        NodeList diskChildNodes = diskNode.getChildNodes();
                        for (int z = 0; z < diskChildNodes.getLength(); z++) {
                            Node diskChildNode = diskChildNodes.item(z);
                            if ("source".equals(diskChildNode.getNodeName())) {
                                NamedNodeMap sourceNodeAttributes = diskChildNode.getAttributes();
                                Node sourceNodeAttribute = sourceNodeAttributes.getNamedItem("file");
                                if (oldIsoVolumePath != null && sourceNodeAttribute != null
                                        && oldIsoVolumePath.equals(sourceNodeAttribute.getNodeValue())) {
                                    diskNode.removeChild(diskChildNode);
                                    Element newChildSourceNode = doc.createElement("source");
                                    newChildSourceNode.setAttribute("file", newIsoVolumePath);
                                    diskNode.appendChild(newChildSourceNode);
                                    logger.debug(String.format("Replaced ISO path [%s] with [%s] in VM [%s] XML configuration.", oldIsoVolumePath, newIsoVolumePath, vmName));
                                    return LibvirtXMLParser.getXml(doc);
                                }
                            }
                        }
                    }
                }
            }
        }

        return LibvirtXMLParser.getXml(doc);
    }

    private String getPathFromSourceText(Set<String> paths, String sourceText) {
        if (paths != null && StringUtils.isNotBlank(sourceText)) {
            for (String path : paths) {
                if (sourceText.contains(path)) {
                    return path;
                }
            }
        }

        return null;
    }

    private String getSourceText(Node diskNode) {
        NodeList diskChildNodes = diskNode.getChildNodes();

        for (int i = 0; i < diskChildNodes.getLength(); i++) {
            Node diskChildNode = diskChildNodes.item(i);

            if ("source".equals(diskChildNode.getNodeName())) {
                NamedNodeMap diskNodeAttributes = diskChildNode.getAttributes();

                Node diskNodeAttribute = diskNodeAttributes.getNamedItem("file");

                if (diskNodeAttribute != null) {
                    return diskNodeAttribute.getTextContent();
                }

                diskNodeAttribute = diskNodeAttributes.getNamedItem("dev");

                if (diskNodeAttribute != null) {
                    return diskNodeAttribute.getTextContent();
                }

                diskNodeAttribute = diskNodeAttributes.getNamedItem("protocol");

                if (diskNodeAttribute != null) {
                    String textContent = diskNodeAttribute.getTextContent();

                    if ("rbd".equalsIgnoreCase(textContent)) {
                        diskNodeAttribute = diskNodeAttributes.getNamedItem("name");

                        if (diskNodeAttribute != null) {
                            return diskNodeAttribute.getTextContent();
                        }
                    }
                }
            }
        }

        return null;
    }

    private String replaceDiskSourceFile(String xmlDesc, String isoPath, String vmName) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        InputStream in = IOUtils.toInputStream(xmlDesc);

        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);

        // Get the root element
        Node domainNode = doc.getFirstChild();

        NodeList domainChildNodes = domainNode.getChildNodes();

        for (int i = 0; i < domainChildNodes.getLength(); i++) {
            Node domainChildNode = domainChildNodes.item(i);

            if ("devices".equals(domainChildNode.getNodeName())) {
                NodeList devicesChildNodes = domainChildNode.getChildNodes();
                if (findDiskNode(doc, devicesChildNodes, vmName, isoPath)) {
                    break;
                }
            }
        }
        return LibvirtXMLParser.getXml(doc);
    }

    private boolean findDiskNode(Document doc, NodeList devicesChildNodes, String vmName, String isoPath) {
        for (int x = 0; x < devicesChildNodes.getLength(); x++) {
            Node deviceChildNode = devicesChildNodes.item(x);
            if ("disk".equals(deviceChildNode.getNodeName())) {
                Node diskNode = deviceChildNode;
                if (findSourceNode(doc, diskNode, vmName, isoPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findSourceNode(Document doc, Node diskNode, String vmName, String isoPath) {
        NodeList diskChildNodes = diskNode.getChildNodes();
        for (int z = 0; z < diskChildNodes.getLength(); z++) {
            Node diskChildNode = diskChildNodes.item(z);
            if ("source".equals(diskChildNode.getNodeName())) {
                Node sourceNode = diskChildNode;
                NamedNodeMap sourceNodeAttributes = sourceNode.getAttributes();
                Node sourceNodeAttribute = sourceNodeAttributes.getNamedItem("file");
                if (sourceNodeAttribute != null && sourceNodeAttribute.getNodeValue().contains(vmName)) {
                    diskNode.removeChild(diskChildNode);
                    Element newChildSourceNode = doc.createElement("source");
                    newChildSourceNode.setAttribute("file", isoPath);
                    diskNode.appendChild(newChildSourceNode);
                    return true;
                }
            }
        }
        return false;
    }

    public static String maskSensitiveInfoInXML(String xmlDesc) {
        if (xmlDesc == null) return null;
        return xmlDesc.replaceAll("(graphics\\s+[^>]*type=['\"]vnc['\"][^>]*passwd=['\"])([^'\"]*)(['\"])",
                "$1*****$3");
    }

    /**
     * Checks if any of the destination disks in the migration target a CLVM or CLVM_NG storage pool.
     * This is used to determine if incremental migration should be disabled to avoid libvirt
     * precreate errors with QCOW2-on-LVM setups.
     *
     * @param mapMigrateStorage the map containing migration disk information with destination pool types
     * @return true if any destination disk targets CLVM or CLVM_NG, false otherwise
     */
    protected boolean hasClvmDestinationDisks(Map<String, MigrateCommand.MigrateDiskInfo> mapMigrateStorage) {
        if (MapUtils.isEmpty(mapMigrateStorage)) {
            return false;
        }

        try {
            for (Map.Entry<String, MigrateCommand.MigrateDiskInfo> entry : mapMigrateStorage.entrySet()) {
                MigrateCommand.MigrateDiskInfo diskInfo = entry.getValue();
               if (isClvmBlockDevice(diskInfo)) {
                    logger.debug("Found disk targeting CLVM/CLVM_NG destination pool");
                    return true;
               }
            }
        } catch (final Exception e) {
            logger.debug("Failed to check for CLVM destination disks: {}. Assuming no CLVM disks.", e.getMessage());
        }

        return false;
    }

    private boolean isClvmBlockDevice(MigrateCommand.MigrateDiskInfo diskInfo) {
        if (diskInfo == null ||diskInfo.getDestPoolType() == null) {
            return false;
        }
        return (Storage.StoragePoolType.CLVM.equals(diskInfo.getDestPoolType()) || Storage.StoragePoolType.CLVM_NG.equals(diskInfo.getDestPoolType()));
    }

    /**
     * Determines if the driver type should be updated during migration based on CLVM involvement.
     * The driver type needs to be updated when:
     * - Managed storage is being migrated, OR
     * - Source pool is CLVM or CLVM_NG, OR
     * - Destination pool is CLVM or CLVM_NG
     *
     * This ensures the libvirt XML driver type matches the destination format (raw/qcow2/etc).
     *
     * @param migrateStorageManaged true if migrating managed storage
     * @param migrateDiskInfo the migration disk information containing source and destination pool types
     * @return true if driver type should be updated, false otherwise
     */
    private boolean shouldUpdateDriverTypeForMigration(boolean migrateStorageManaged,
                                                        MigrateCommand.MigrateDiskInfo migrateDiskInfo) {
        boolean sourceIsClvm = Storage.StoragePoolType.CLVM == migrateDiskInfo.getSourcePoolType() ||
                Storage.StoragePoolType.CLVM_NG == migrateDiskInfo.getSourcePoolType();

        boolean destIsClvm = Storage.StoragePoolType.CLVM == migrateDiskInfo.getDestPoolType() ||
                Storage.StoragePoolType.CLVM_NG == migrateDiskInfo.getDestPoolType();

        boolean isClvmRelatedMigration = sourceIsClvm || destIsClvm;
        return migrateStorageManaged || isClvmRelatedMigration;
    }
}
