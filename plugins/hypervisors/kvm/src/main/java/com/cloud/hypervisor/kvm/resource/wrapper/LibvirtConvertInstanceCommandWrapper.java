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

import java.net.ServerSocket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConvertInstanceAnswer;
import com.cloud.agent.api.ConvertInstanceCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareVddkSourceDiskTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.FileUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  ConvertInstanceCommand.class)
public class LibvirtConvertInstanceCommandWrapper extends CommandWrapper<ConvertInstanceCommand, Answer, LibvirtComputingResource> {

    private static final List<Hypervisor.HypervisorType> supportedInstanceConvertSourceHypervisors =
            List.of(Hypervisor.HypervisorType.VMware);
    private static final Pattern SHA1_FINGERPRINT_PATTERN = Pattern.compile("(?i)(?:SHA1\\s+)?Fingerprint\\s*=\\s*([0-9A-F:]+)");

    @Override
    public Answer execute(ConvertInstanceCommand cmd, LibvirtComputingResource serverResource) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        Hypervisor.HypervisorType sourceHypervisorType = sourceInstance.getHypervisorType();
        String sourceInstanceName = sourceInstance.getInstanceName();
        Hypervisor.HypervisorType destinationHypervisorType = cmd.getDestinationHypervisorType();
        DataStoreTO conversionTemporaryLocation = cmd.getConversionTemporaryLocation();
        long timeout = (long) cmd.getWait() * 1000;
        String extraParams = cmd.getExtraParams();
        boolean useVddk = cmd.isUseVddk();
        String originalVMName = cmd.getOriginalVMName();

        if (cmd.getCheckConversionSupport() && !serverResource.hostSupportsInstanceConversion()) {
            String msg = String.format("Cannot convert the instance %s from VMware as the virt-v2v binary is not found. " +
                    "Please install virt-v2v%s on the host before attempting the instance conversion.", sourceInstanceName, serverResource.isUbuntuOrDebianHost()? ", nbdkit" : "");
            logger.info(String.format("(%s) %s", originalVMName, msg));
            return new Answer(cmd, false, msg);
        }

        if (!areSourceAndDestinationHypervisorsSupported(sourceHypervisorType, destinationHypervisorType)) {
            String err = destinationHypervisorType != Hypervisor.HypervisorType.KVM ?
                    String.format("The destination hypervisor type is %s, KVM was expected, cannot handle it", destinationHypervisorType) :
                    String.format("The source hypervisor type %s is not supported for KVM conversion", sourceHypervisorType);
            logger.error(String.format("(%s) %s", originalVMName, err));
            return new Answer(cmd, false, err);
        }

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool temporaryStoragePool = getTemporaryStoragePool(conversionTemporaryLocation, storagePoolMgr);
        boolean directRbdVddkImport = isDirectRbdVddkImport(cmd);
        boolean directLinstorVddkImport = isDirectLinstorVddkImport(cmd);

        logger.info(String.format("(%s) Attempting to convert the instance %s from %s to KVM",
                originalVMName, sourceInstanceName, sourceHypervisorType));
        final String temporaryConvertPath = temporaryStoragePool.getLocalPath();
        final String temporaryConvertUuid = UUID.randomUUID().toString();
        boolean verboseModeEnabled = serverResource.isConvertInstanceVerboseModeEnabled();

        boolean cleanupSecondaryStorage = false;
        boolean ovfExported = false;
        String ovfTemplateDirOnConversionLocation = null;

        try {
            boolean result;
            if (useVddk) {
                logger.info("({}) Using VDDK-based conversion (direct from VMware)", originalVMName);
                String vddkLibDir = resolveVddkSetting(cmd.getVddkLibDir(), serverResource.getVddkLibDir());
                if (StringUtils.isBlank(vddkLibDir)) {
                    String err = String.format("VDDK lib dir is not configured on the host. " +
                            "Set '%s' in agent.properties or in details parameter of the import api call to use VDDK-based conversion.", "vddk.lib.dir");
                    logger.error("({}) {}", originalVMName, err);
                    return new Answer(cmd, false, err);
                }
                String vddkTransports = resolveVddkSetting(cmd.getVddkTransports(), serverResource.getVddkTransports());
                String configuredVddkThumbprint = resolveVddkSetting(cmd.getVddkThumbprint(), serverResource.getVddkThumbprint());
                String passwordOption = serverResource.getDetectedPasswordFileOption();
                if (directRbdVddkImport || directLinstorVddkImport) {
                    if (directRbdVddkImport && !serverResource.hostSupportsVddkRbdDirectImport(cmd.getVddkLibDir())) {
                        String err = String.format("Direct RBD VDDK import requires VDDK, qemu-img RBD support, and virt-v2v in-place support on host %s. " +
                                "Use staged import with temporary conversion storage or select a newer conversion host.", serverResource.getPrivateIp());
                        logger.error("({}) {}", originalVMName, err);
                        return new Answer(cmd, false, err);
                    }
                    if (directLinstorVddkImport && !serverResource.hostSupportsVirtV2vInPlace()) {
                        String err = String.format("Direct Linstor VDDK import requires virt-v2v in-place support on host %s. " +
                                "Use staged import with temporary conversion storage or select a newer conversion host.", serverResource.getPrivateIp());
                        logger.error("({}) {}", originalVMName, err);
                        return new Answer(cmd, false, err);
                    }
                    result = performInstanceConversionUsingVddkDirectToPool(cmd, temporaryStoragePool, originalVMName,
                            vddkLibDir, serverResource.getLibguestfsBackend(), vddkTransports, configuredVddkThumbprint,
                            timeout, verboseModeEnabled, temporaryConvertUuid, serverResource);
                } else {
                    result = performInstanceConversionUsingVddk(sourceInstance, originalVMName, temporaryConvertPath,
                            vddkLibDir, serverResource.getLibguestfsBackend(), vddkTransports, configuredVddkThumbprint,
                            timeout, verboseModeEnabled, extraParams, temporaryConvertUuid, passwordOption);
                }
            } else {
                logger.info("({}) Using OVF-based conversion (export + local convert)", originalVMName);
                String sourceOVFDirPath;
                if (cmd.getExportOvfToConversionLocation()) {
                    String exportInstanceOVAUrl = getExportInstanceOVAUrl(sourceInstance, originalVMName);

                    if (StringUtils.isBlank(exportInstanceOVAUrl)) {
                        String err = String.format("Couldn't export OVA for the VM %s, due to empty url", sourceInstanceName);
                        logger.error("({}) {}", originalVMName, err);
                        return new Answer(cmd, false, err);
                    }

                    int noOfThreads = cmd.getThreadsCountToExportOvf();
                    if (noOfThreads > 1 && !serverResource.ovfExportToolSupportsParallelThreads()) {
                        noOfThreads = 0;
                    }
                    ovfTemplateDirOnConversionLocation = UUID.randomUUID().toString();
                    temporaryStoragePool.createFolder(ovfTemplateDirOnConversionLocation);
                    sourceOVFDirPath = String.format("%s/%s/", temporaryConvertPath, ovfTemplateDirOnConversionLocation);
                    ovfExported = exportOVAFromVMOnVcenter(exportInstanceOVAUrl, sourceOVFDirPath, noOfThreads, originalVMName, timeout);

                    if (!ovfExported) {
                        String err = String.format("Export OVA for the VM %s failed", sourceInstanceName);
                        logger.error("({}) {}", originalVMName, err);
                        return new Answer(cmd, false, err);
                    }
                    sourceOVFDirPath = String.format("%s%s/", sourceOVFDirPath, sourceInstanceName);
                } else {
                    ovfTemplateDirOnConversionLocation = cmd.getTemplateDirOnConversionLocation();
                    sourceOVFDirPath = String.format("%s/%s/", temporaryConvertPath, ovfTemplateDirOnConversionLocation);
                }

                result = performInstanceConversion(originalVMName, sourceOVFDirPath, temporaryConvertPath, temporaryConvertUuid,
                        timeout, verboseModeEnabled, extraParams, serverResource);
            }

            if (!result) {
                String err = String.format("Instance conversion failed for VM %s. Please check virt-v2v logs.", sourceInstanceName);
                logger.error("({}) {}", originalVMName, err);
                return new Answer(cmd, false, err);
            }
            return new ConvertInstanceAnswer(cmd, temporaryConvertUuid);
        } catch (Exception e) {
            String error = String.format("Error converting instance %s from %s, due to: %s", sourceInstanceName, sourceHypervisorType, e.getMessage());
            logger.error("({}) {}", originalVMName, error, e);
            cleanupSecondaryStorage = true;
            return new Answer(cmd, false, error);
        } finally {
            if (ovfExported && StringUtils.isNotBlank(ovfTemplateDirOnConversionLocation)) {
                String sourceOVFDir = String.format("%s/%s", temporaryConvertPath, ovfTemplateDirOnConversionLocation);
                logger.debug("({}) Cleaning up exported OVA at dir: {}", originalVMName, sourceOVFDir);
                FileUtil.deletePath(sourceOVFDir);
            }
            if (cleanupSecondaryStorage && conversionTemporaryLocation instanceof NfsTO) {
                logger.debug("({}) Cleaning up secondary storage temporary location", originalVMName);
                storagePoolMgr.deleteStoragePool(temporaryStoragePool.getType(), temporaryStoragePool.getUuid());
            }
        }
    }

    private boolean isDirectRbdVddkImport(ConvertInstanceCommand cmd) {
        return isDirectVddkImportToPoolType(cmd, Storage.StoragePoolType.RBD);
    }

    private boolean isDirectLinstorVddkImport(ConvertInstanceCommand cmd) {
        return isDirectVddkImportToPoolType(cmd, Storage.StoragePoolType.Linstor);
    }

    private boolean isDirectVddkImportToPoolType(ConvertInstanceCommand cmd, Storage.StoragePoolType poolType) {
        DataStoreTO conversionTemporaryLocation = cmd.getConversionTemporaryLocation();
        return cmd.isUseVddk() && conversionTemporaryLocation instanceof PrimaryDataStoreTO &&
                ((PrimaryDataStoreTO) conversionTemporaryLocation).getPoolType() == poolType;
    }

    protected KVMStoragePool getTemporaryStoragePool(DataStoreTO conversionTemporaryLocation, KVMStoragePoolManager storagePoolMgr) {
        if (conversionTemporaryLocation instanceof NfsTO) {
            NfsTO nfsTO = (NfsTO) conversionTemporaryLocation;
            return storagePoolMgr.getStoragePoolByURI(nfsTO.getUrl());
        } else {
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) conversionTemporaryLocation;
            return storagePoolMgr.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
        }
    }

    protected boolean areSourceAndDestinationHypervisorsSupported(Hypervisor.HypervisorType sourceHypervisorType,
                                                                  Hypervisor.HypervisorType destinationHypervisorType) {
        return destinationHypervisorType == Hypervisor.HypervisorType.KVM &&
                supportedInstanceConvertSourceHypervisors.contains(sourceHypervisorType);
    }

    private String getExportInstanceOVAUrl(RemoteInstanceTO sourceInstance, String originalVMName) {
        String url = null;
        if (sourceInstance.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
            url = getExportOVAUrlFromRemoteInstance(sourceInstance, originalVMName);
        }
        return url;
    }

    private String getExportOVAUrlFromRemoteInstance(RemoteInstanceTO vmwareInstance, String originalVMName) {
        String vcenter = vmwareInstance.getVcenterHost();
        String username = vmwareInstance.getVcenterUsername();
        String password = vmwareInstance.getVcenterPassword();
        String datacenter = vmwareInstance.getDatacenterName();
        String vm = vmwareInstance.getInstanceName();
        String path = vmwareInstance.getInstancePath();

        String encodedUsername = encodeUsername(username);
        String encodedPassword = encodeUsername(password);
        if (StringUtils.isNotBlank(path)) {
            logger.info("({}) VM path: {}", originalVMName, path);
            return String.format("vi://%s:%s@%s/%s/%s/%s",
                    encodedUsername, encodedPassword, vcenter, datacenter, path, vm);
        }
        return String.format("vi://%s:%s@%s/%s/vm/%s",
                encodedUsername, encodedPassword, vcenter, datacenter, vm);
    }

    protected void sanitizeDisksPath(List<LibvirtVMDef.DiskDef> disks) {
        for (LibvirtVMDef.DiskDef disk : disks) {
            String[] diskPathParts = disk.getDiskPath().split("/");
            String relativePath = diskPathParts[diskPathParts.length - 1];
            disk.setDiskPath(relativePath);
        }
    }

    private boolean exportOVAFromVMOnVcenter(String vmExportUrl,
                                             String targetOvfDir,
                                             int noOfThreads,
                                             String originalVMName, long timeout) {
        Script script = new Script("ovftool", timeout, logger);
        script.add("--noSSLVerify");
        if (noOfThreads > 1) {
            script.add(String.format("--parallelThreads=%s", noOfThreads));
        }
        script.add(vmExportUrl);
        script.add(targetOvfDir);

        String logPrefix = String.format("(%s) export ovf", originalVMName);
        OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(logger, logPrefix);
        script.execute(outputLogger);
        int exitValue = script.getExitValue();
        return exitValue == 0;
    }

    protected boolean performInstanceConversion(String originalVMName, String sourceOVFDirPath,
                                                String temporaryConvertFolder,
                                                String temporaryConvertUuid,
                                                long timeout, boolean verboseModeEnabled, String extraParams,
                                                LibvirtComputingResource serverResource) {
        Script script = new Script("virt-v2v", timeout, logger);
        script.add("--root", "first");
        script.add("-i", "ova");
        script.add(sourceOVFDirPath);
        script.add("-o", "local");
        script.add("-os", temporaryConvertFolder);
        script.add("-of", "qcow2");
        script.add("-on", temporaryConvertUuid);
        if (verboseModeEnabled) {
            script.add("-v");
        }
        if (StringUtils.isNotBlank(extraParams)) {
            addExtraParamsToScript(extraParams, script);
        }

        String logPrefix = String.format("(%s) virt-v2v ovf source: %s progress", originalVMName, sourceOVFDirPath);
        OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(logger, logPrefix);
        Map<String, String> convertInstanceEnv = serverResource.getConvertInstanceEnv();
        if (MapUtils.isEmpty(convertInstanceEnv)) {
            script.execute(outputLogger);
        } else {
            script.execute(outputLogger, convertInstanceEnv);
        }
        int exitValue = script.getExitValue();
        return exitValue == 0;
    }

    protected void addExtraParamsToScript(String extraParams, Script script) {
        List<String> separatedArgs = Arrays.asList(extraParams.split(" "));
        int i = 0;
        while (i < separatedArgs.size()) {
            String current = separatedArgs.get(i);
            String next = (i + 1) < separatedArgs.size() ? separatedArgs.get(i + 1) : null;
            if (next == null || next.startsWith("-")) {
                script.add(current);
                i = i + 1;
            } else {
                script.add(current, next);
                i = i + 2;
            }
        }
    }

    protected String encodeUsername(String username) {
        return URLEncoder.encode(username, Charset.defaultCharset());
    }

    private String resolveVddkSetting(String commandValue, String agentValue) {
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(commandValue), StringUtils.trimToNull(agentValue));
    }

    protected boolean performInstanceConversionUsingVddk(RemoteInstanceTO vmwareInstance, String originalVMName,
                                                         String temporaryConvertFolder, String vddkLibDir,
                                                         String libguestfsBackend, String vddkTransports,
                                                         String configuredVddkThumbprint,
                                                         long timeout, boolean verboseModeEnabled, String extraParams,
                                                         String temporaryConvertUuid, String passwordOption) {

        String vcenterPassword = vmwareInstance.getVcenterPassword();
        if (StringUtils.isBlank(vcenterPassword)) {
            logger.error("({}) Could not determine vCenter password for {}", originalVMName, vmwareInstance.getVcenterHost());
            return false;
        }

        String passwordFilePath = String.format("/tmp/v2v.pass.cloud.%s.%s",
                StringUtils.defaultIfBlank(vmwareInstance.getVcenterHost(), "unknown"),
                UUID.randomUUID());
        try {
            Files.writeString(Path.of(passwordFilePath), vcenterPassword);
            Files.setPosixFilePermissions(Path.of(passwordFilePath), Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            logger.debug("({}) Written vCenter password to {}", originalVMName, passwordFilePath);
        } catch (Exception e) {
            logger.error("({}) Failed to write vCenter password file {}: {}", originalVMName, passwordFilePath, e.getMessage());
            return false;
        }

        try {
            String vpxUrl = buildVpxUrl(vmwareInstance);

            StringBuilder cmd = new StringBuilder();

            cmd.append("export LIBGUESTFS_BACKEND=").append(libguestfsBackend).append(" && ");

            cmd.append("virt-v2v ");
            cmd.append("--root first ");
            cmd.append("-ic '").append(vpxUrl).append("' ");
            if (StringUtils.isBlank(passwordOption)) {
                logger.error("({}) Could not determine supported password file option for virt-v2v", originalVMName);
                return false;
            }

            cmd.append(passwordOption).append(" ").append(passwordFilePath).append(" ");
            cmd.append("-it vddk ");
            cmd.append("-io vddk-libdir=").append(vddkLibDir).append(" ");
            String vddkThumbprint = StringUtils.trimToNull(configuredVddkThumbprint);
            if (StringUtils.isBlank(vddkThumbprint)) {
                vddkThumbprint = getVcenterThumbprint(vmwareInstance.getVcenterHost(), timeout, originalVMName);
            }
            if (StringUtils.isBlank(vddkThumbprint)) {
                logger.error("({}) Could not determine vCenter thumbprint for {}", originalVMName, vmwareInstance.getVcenterHost());
                return false;
            }
            cmd.append("-io vddk-thumbprint=").append(vddkThumbprint).append(" ");
            if (StringUtils.isNotBlank(vddkTransports)) {
                cmd.append("-io vddk-transports=").append(vddkTransports).append(" ");
            }
            cmd.append(vmwareInstance.getInstanceName()).append(" ");
            cmd.append("-o local ");
            cmd.append("-os ").append(temporaryConvertFolder).append(" ");
            cmd.append("-of qcow2 ");
            cmd.append("-on ").append(temporaryConvertUuid).append(" ");

            if (verboseModeEnabled) {
                cmd.append("-v ");
            }

            if (StringUtils.isNotBlank(extraParams)) {
                cmd.append(extraParams).append(" ");
            }

            Script script = new Script("/bin/bash", timeout, logger);
            script.add("-c");
            script.add(cmd.toString());

            String logPrefix = String.format("(%s) virt-v2v vddk import", originalVMName);
            OutputInterpreter.LineByLineOutputLogger outputLogger =
                    new OutputInterpreter.LineByLineOutputLogger(logger, logPrefix);

            logger.info("({}) Starting virt-v2v VDDK conversion", originalVMName);
            script.execute(outputLogger);

            int exitValue = script.getExitValue();
            if (exitValue != 0) {
                logger.error("({}) virt-v2v failed with exit code {}", originalVMName, exitValue);
            }

            return exitValue == 0;
        } finally {
            try {
                Files.deleteIfExists(Path.of(passwordFilePath));
                logger.debug("({}) Deleted password file {}", originalVMName, passwordFilePath);
            } catch (Exception e) {
                logger.warn("({}) Failed to delete password file {}: {}", originalVMName, passwordFilePath, e.getMessage());
            }
        }
    }

    protected boolean performInstanceConversionUsingVddkDirectToPool(ConvertInstanceCommand cmd,
                                                                     KVMStoragePool targetPool,
                                                                     String originalVMName,
                                                                     String vddkLibDir,
                                                                     String libguestfsBackend,
                                                                     String vddkTransports,
                                                                     String configuredVddkThumbprint,
                                                                     long timeout,
                                                                     boolean verboseModeEnabled,
                                                                     String temporaryConvertUuid,
                                                                     LibvirtComputingResource serverResource) {
        RemoteInstanceTO vmwareInstance = cmd.getSourceInstance();
        List<VmwareVddkSourceDiskTO> sourceDisks = cmd.getVmwareVddkSourceDisks();
        final boolean linstorTarget = targetPool.getType() == Storage.StoragePoolType.Linstor;
        if (sourceDisks == null || sourceDisks.isEmpty()) {
            logger.error("({}) Direct {} VDDK import requires VMware source disk metadata", originalVMName, targetPool.getType());
            return false;
        }

        if (linstorTarget) {
            probeLinstorQemuAccess(targetPool, temporaryConvertUuid);
        } else {
            probeRbdQemuAccess(targetPool, temporaryConvertUuid);
        }

        String vcenterPassword = vmwareInstance.getVcenterPassword();
        if (StringUtils.isBlank(vcenterPassword)) {
            logger.error("({}) Could not determine vCenter password for {}", originalVMName, vmwareInstance.getVcenterHost());
            return false;
        }

        String vddkThumbprint = StringUtils.trimToNull(configuredVddkThumbprint);
        if (StringUtils.isBlank(vddkThumbprint)) {
            vddkThumbprint = getVcenterThumbprint(vmwareInstance.getVcenterHost(), timeout, originalVMName);
        }
        if (StringUtils.isBlank(vddkThumbprint)) {
            logger.error("({}) Could not determine vCenter thumbprint for {}", originalVMName, vmwareInstance.getVcenterHost());
            return false;
        }

        String passwordFilePath = String.format("/tmp/v2v.rbd.pass.cloud.%s.%s",
                StringUtils.defaultIfBlank(vmwareInstance.getVcenterHost(), "unknown"),
                UUID.randomUUID());
        List<String> createdImages = new ArrayList<>();
        List<String> blockDevicePaths = new ArrayList<>();
        try {
            Files.writeString(Path.of(passwordFilePath), vcenterPassword);
            Files.setPosixFilePermissions(Path.of(passwordFilePath), Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

            String vddkNbdCompression = serverResource.getVddkNbdCompression();
            for (int i = 0; i < sourceDisks.size(); i++) {
                VmwareVddkSourceDiskTO sourceDisk = sourceDisks.get(i);
                if (linstorTarget) {
                    String diskName = buildLinstorDiskName(temporaryConvertUuid, i);
                    createdImages.add(diskName);
                    String devicePath = copyVddkSourceDiskToLinstor(vmwareInstance, sourceDisk, targetPool, diskName, passwordFilePath,
                            vddkLibDir, vddkTransports, vddkNbdCompression, vddkThumbprint, timeout, originalVMName, serverResource.hostSupportsNbdcopy());
                    blockDevicePaths.add(devicePath);
                } else {
                    String imageName = buildRbdImageName(temporaryConvertUuid, i);
                    createdImages.add(imageName);
                    copyVddkSourceDiskToRbd(vmwareInstance, sourceDisk, targetPool, imageName, passwordFilePath,
                            vddkLibDir, vddkTransports, vddkNbdCompression, vddkThumbprint, timeout, originalVMName,
                            serverResource.hostSupportsNbdcopy());
                }
            }

            boolean finalized;
            if (linstorTarget) {
                Path inputXml = Files.createTempFile("cloudstack-vddk-direct-" + temporaryConvertUuid, ".xml");
                Files.writeString(inputXml, buildDirectBlockDeviceLibvirtXml(temporaryConvertUuid, blockDevicePaths));
                finalized = runInPlaceFinalization(inputXml, libguestfsBackend, timeout, verboseModeEnabled, originalVMName, serverResource);
                Files.deleteIfExists(inputXml);
            } else {
                finalized = runRbdInPlaceFinalizationOverNbdBridges(targetPool, createdImages, temporaryConvertUuid,
                        libguestfsBackend, timeout, verboseModeEnabled, originalVMName, serverResource);
            }
            if (!finalized) {
                cleanupDirectImportDisks(targetPool, createdImages, originalVMName);
            }
            return finalized;
        } catch (Exception e) {
            logger.error("({}) Direct {} VDDK import failed: {}", originalVMName, targetPool.getType(), e.getMessage(), e);
            cleanupDirectImportDisks(targetPool, createdImages, originalVMName);
            return false;
        } finally {
            try {
                Files.deleteIfExists(Path.of(passwordFilePath));
            } catch (Exception e) {
                logger.warn("({}) Failed to delete password file {}: {}", originalVMName, passwordFilePath, e.getMessage());
            }
        }
    }

    private void copyVddkSourceDiskToRbd(RemoteInstanceTO vmwareInstance, VmwareVddkSourceDiskTO sourceDisk,
                                         KVMStoragePool targetPool, String imageName, String passwordFilePath,
                                         String vddkLibDir, String vddkTransports, String vddkNbdCompression,
                                         String vddkThumbprint, long timeout, String originalVMName, boolean useNbdcopy) {
        if (StringUtils.isBlank(sourceDisk.getSourceDiskPath())) {
            throw new CloudRuntimeException(String.format("VMware source disk %s does not have a VMDK path", sourceDisk.getDiskId()));
        }
        String rbdImagePath = targetPool.getSourceDir() + "/" + imageName;
        String qemuRbdTarget = KVMPhysicalDisk.RBDStringBuilder(targetPool, rbdImagePath);

        StringBuilder nbdkit = new StringBuilder();
        nbdkit.append("nbdkit -r -U - vddk ");
        nbdkit.append("file=").append(shellQuote(sourceDisk.getSourceDiskPath())).append(" ");
        nbdkit.append("server=").append(shellQuote(vmwareInstance.getVcenterHost())).append(" ");
        nbdkit.append("user=").append(shellQuote(vmwareInstance.getVcenterUsername())).append(" ");
        nbdkit.append("password=+").append(shellQuote(passwordFilePath)).append(" ");
        if (StringUtils.isNotBlank(vmwareInstance.getVmwareMoref())) {
            nbdkit.append("vm=").append(shellQuote("moref=" + vmwareInstance.getVmwareMoref())).append(" ");
        } else {
            nbdkit.append("vm=").append(shellQuote(vmwareInstance.getInstanceName())).append(" ");
        }
        nbdkit.append("libdir=").append(shellQuote(vddkLibDir)).append(" ");
        nbdkit.append("thumbprint=").append(shellQuote(vddkThumbprint)).append(" ");
        if (StringUtils.isNotBlank(vddkTransports)) {
            nbdkit.append("transports=").append(shellQuote(vddkTransports)).append(" ");
        }
        if (StringUtils.isNotBlank(vddkNbdCompression)) {
            nbdkit.append("compression=").append(shellQuote(vddkNbdCompression)).append(" ");
        }

        boolean bridgeCopy = useNbdcopy && sourceDisk.getCapacityBytes() > 0;
        Path scriptFile = null;
        try {
            Script script;
            if (bridgeCopy) {
                // Multi-connection nbdcopy is typically faster than a single-stream qemu-img
                // convert, but nbdcopy needs both ends as NBD. qemu-img cannot create the
                // image here (nbdcopy does not), so pre-create it, then serve it over a
                // localhost qemu-nbd bridge and copy NBD-to-NBD. The fresh raw image reads
                // as zeros, so --destination-is-zero keeps it sparse.
                int port = allocateLocalhostPort();
                Path pidFile = Files.createTempFile("cloudstack-vddk-rbd-nbd-" + imageName + "-", ".pid");
                Files.deleteIfExists(pidFile);
                nbdkit.append("--run ").append(shellQuote(
                        "nbdcopy --destination-is-zero \"$uri\" " + String.format("nbd://localhost:%d", port)));

                StringBuilder scriptBody = new StringBuilder();
                scriptBody.append("#!/bin/bash\n");
                scriptBody.append("set -euo pipefail\n");
                scriptBody.append("cleanup() {\n");
                scriptBody.append("  set +e\n");
                scriptBody.append("  if [[ -s ").append(shellQuote(pidFile.toString())).append(" ]]; then\n");
                scriptBody.append("    pid=$(cat ").append(shellQuote(pidFile.toString())).append(")\n");
                scriptBody.append("    kill \"$pid\" >/dev/null 2>&1 || true\n");
                scriptBody.append("    for attempt in {1..20}; do kill -0 \"$pid\" >/dev/null 2>&1 || break; sleep 0.1; done\n");
                scriptBody.append("  fi\n");
                scriptBody.append("  rm -f ").append(shellQuote(pidFile.toString())).append("\n");
                scriptBody.append("}\n");
                scriptBody.append("trap cleanup EXIT\n");
                scriptBody.append("qemu-img create -f raw ").append(shellQuote(qemuRbdTarget)).append(" ")
                        .append(sourceDisk.getCapacityBytes()).append("\n");
                scriptBody.append("qemu-nbd --fork --persistent --shared=8 --format=raw --bind=127.0.0.1 --port=")
                        .append(port).append(" --pid-file=").append(shellQuote(pidFile.toString()))
                        .append(" ").append(shellQuote(qemuRbdTarget)).append("\n");
                scriptBody.append(nbdkit).append("\n");

                scriptFile = Files.createTempFile("cloudstack-vddk-rbd-copy-" + imageName + "-", ".sh");
                Files.writeString(scriptFile, scriptBody.toString());
                Files.setPosixFilePermissions(scriptFile, Set.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));

                script = new Script("/bin/bash", timeout, logger);
                script.add(scriptFile.toString());
            } else {
                // qemu-img convert creates and writes the RBD image in one step (fallback when
                // nbdcopy is unavailable or the source capacity is unknown).
                nbdkit.append("--run ").append(shellQuote("qemu-img convert -f raw -O raw \"$uri\" " + shellQuote(qemuRbdTarget)));
                script = new Script("/bin/bash", timeout, logger);
                script.add("-c");
                script.add(nbdkit.toString());
            }

            String logPrefix = String.format("(%s) vddk%s to rbd disk %s", originalVMName,
                    bridgeCopy ? "(nbdcopy)" : "", sourceDisk.getDiskId());
            OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(logger, logPrefix);
            logger.info("({}) Copying VMware disk {} to RBD image {}{}", originalVMName, sourceDisk.getSourceDiskPath(),
                    rbdImagePath, bridgeCopy ? " via nbdcopy over qemu-nbd bridge" : "");
            script.execute(outputLogger);
            if (script.getExitValue() != 0) {
                throw new CloudRuntimeException(String.format("Failed to copy VMware disk %s to RBD image %s", sourceDisk.getSourceDiskPath(), rbdImagePath));
            }
        } catch (java.io.IOException e) {
            throw new CloudRuntimeException(String.format("Failed to prepare RBD copy for VMware disk %s: %s",
                    sourceDisk.getDiskId(), e.getMessage()), e);
        } finally {
            deleteQuietly(scriptFile);
        }
    }

    private String copyVddkSourceDiskToLinstor(RemoteInstanceTO vmwareInstance, VmwareVddkSourceDiskTO sourceDisk,
                                               KVMStoragePool targetPool, String diskName, String passwordFilePath,
                                               String vddkLibDir, String vddkTransports, String vddkNbdCompression,
                                               String vddkThumbprint, long timeout, String originalVMName, boolean useNbdcopy) {
        if (StringUtils.isBlank(sourceDisk.getSourceDiskPath())) {
            throw new CloudRuntimeException(String.format("VMware source disk %s does not have a VMDK path", sourceDisk.getDiskId()));
        }
        if (sourceDisk.getCapacityBytes() <= 0) {
            throw new CloudRuntimeException(String.format("VMware source disk %s does not have a valid capacity, " +
                    "which is required to pre-create the Linstor target volume", sourceDisk.getDiskId()));
        }
        KVMPhysicalDisk targetDisk = targetPool.createPhysicalDisk(diskName, QemuImg.PhysicalDiskFormat.RAW,
                Storage.ProvisioningType.THIN, sourceDisk.getCapacityBytes(), null);
        String devicePath = targetDisk.getPath();
        if (StringUtils.isBlank(devicePath)) {
            throw new CloudRuntimeException(String.format("Could not resolve a local device path for Linstor volume %s", diskName));
        }
        StringBuilder command = new StringBuilder();
        command.append("nbdkit -r -U - vddk ");
        command.append("file=").append(shellQuote(sourceDisk.getSourceDiskPath())).append(" ");
        command.append("server=").append(shellQuote(vmwareInstance.getVcenterHost())).append(" ");
        command.append("user=").append(shellQuote(vmwareInstance.getVcenterUsername())).append(" ");
        command.append("password=+").append(shellQuote(passwordFilePath)).append(" ");
        if (StringUtils.isNotBlank(vmwareInstance.getVmwareMoref())) {
            command.append("vm=").append(shellQuote("moref=" + vmwareInstance.getVmwareMoref())).append(" ");
        } else {
            command.append("vm=").append(shellQuote(vmwareInstance.getInstanceName())).append(" ");
        }
        command.append("libdir=").append(shellQuote(vddkLibDir)).append(" ");
        command.append("thumbprint=").append(shellQuote(vddkThumbprint)).append(" ");
        if (StringUtils.isNotBlank(vddkTransports)) {
            command.append("transports=").append(shellQuote(vddkTransports)).append(" ");
        }
        if (StringUtils.isNotBlank(vddkNbdCompression)) {
            command.append("compression=").append(shellQuote(vddkNbdCompression)).append(" ");
        }
        // Full-disk copy into the local raw device: prefer nbdcopy (libnbd) when present -
        // it pipelines many in-flight requests and is typically faster than a single-connection
        // qemu-img convert - and fall back to qemu-img convert otherwise. Skip writing zero
        // blocks (nbdcopy --destination-is-zero / qemu-img --target-is-zero) only when the
        // backend guarantees a freshly created volume reads back as zeros (thin providers);
        // on backends that do not (e.g. LVM-thick) the zeros must be written or stale data from
        // a previously deleted volume would leak into the unwritten regions.
        boolean targetIsZero = targetPool.isVolumeZeroInitialized(diskName);
        String runCommand = useNbdcopy
                ? "nbdcopy " + (targetIsZero ? "--destination-is-zero " : "") + "\"$uri\" " + shellQuote(devicePath)
                : "qemu-img convert -n " + (targetIsZero ? "--target-is-zero " : "") + "-f raw -O raw \"$uri\" " + shellQuote(devicePath);
        command.append("--run ").append(shellQuote(runCommand));

        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(command.toString());
        String logPrefix = String.format("(%s) %s to linstor disk %s", originalVMName,
                useNbdcopy ? "vddk(nbdcopy)" : "vddk", sourceDisk.getDiskId());
        OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(logger, logPrefix);
        logger.info("({}) Copying VMware disk {} to Linstor device {}", originalVMName, sourceDisk.getSourceDiskPath(), devicePath);
        script.execute(outputLogger);
        if (script.getExitValue() != 0) {
            throw new CloudRuntimeException(String.format("Failed to copy VMware disk %s to Linstor device %s", sourceDisk.getSourceDiskPath(), devicePath));
        }
        return devicePath;
    }

    private boolean runInPlaceFinalization(Path inputXml, String libguestfsBackend, long timeout,
                                           boolean verboseModeEnabled, String originalVMName,
                                           LibvirtComputingResource serverResource) {
        String inPlaceCommand = buildInPlaceCommand(inputXml, libguestfsBackend, verboseModeEnabled, serverResource);
        if (inPlaceCommand == null) {
            logger.error("({}) No virt-v2v in-place finalization method is available", originalVMName);
            return false;
        }

        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(inPlaceCommand);
        OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(logger,
                String.format("(%s) virt-v2v in-place", originalVMName));
        script.execute(outputLogger);
        return script.getExitValue() == 0;
    }

    /**
     * Builds the in-place finalization command, resolving the virt-v2v-in-place
     * binary through the server resource (EL9 ships it in /usr/libexec, outside
     * $PATH) and falling back to "virt-v2v --in-place". Returns null when the host
     * has no in-place method.
     */
    private String buildInPlaceCommand(Path inputXml, String libguestfsBackend, boolean verboseModeEnabled,
                                       LibvirtComputingResource serverResource) {
        StringBuilder command = new StringBuilder();
        command.append("export LIBGUESTFS_BACKEND=").append(shellQuote(libguestfsBackend)).append(" && ");
        String inPlaceBinary = serverResource.getVirtV2vInPlaceBinary();
        if (inPlaceBinary != null) {
            // No -O (write updated output XML): nothing consumes it and the option only
            // exists from virt-v2v 2.5 on, so passing it breaks otherwise capable hosts
            // such as Ubuntu 24.04 with virt-v2v-in-place 2.4.
            command.append(inPlaceBinary).append(" --root first -i libvirtxml ")
                    .append(shellQuote(inputXml.toString())).append(" ");
        } else if (serverResource.hostSupportsVirtV2vInPlaceOption()) {
            command.append("virt-v2v --root first -i libvirtxml ")
                    .append(shellQuote(inputXml.toString())).append(" --in-place ");
        } else {
            return null;
        }
        if (verboseModeEnabled) {
            command.append("-v ");
        }
        return command.toString();
    }

    /**
     * Finalizes direct-RBD import disks with in-place virt-v2v over temporary
     * localhost qemu-nbd bridges (one per image), mirroring the CBT cutover
     * approach. Native {@code <disk type='network' protocol='rbd'>} sources cannot
     * be used here: their cephx {@code <auth>} element references a libvirt secret,
     * and virt-v2v's {@code -i libvirtxml} input runs without a libvirt connection,
     * so the secret cannot be resolved and no drives are attached (observed as
     * "you must call guestfs_add_drive before guestfs_launch" on EL9).
     */
    protected boolean runRbdInPlaceFinalizationOverNbdBridges(KVMStoragePool targetPool, List<String> imageNames,
                                                              String temporaryConvertUuid, String libguestfsBackend,
                                                              long timeout, boolean verboseModeEnabled,
                                                              String originalVMName, LibvirtComputingResource serverResource) {
        List<Integer> ports = new ArrayList<>();
        List<Path> pidFiles = new ArrayList<>();
        Path inputXml = null;
        Path scriptPath = null;
        try {
            for (int i = 0; i < imageNames.size(); i++) {
                ports.add(allocateLocalhostPort());
                Path pidFile = Files.createTempFile("cloudstack-vddk-rbd-nbd-" + temporaryConvertUuid + "-", ".pid");
                Files.deleteIfExists(pidFile);
                pidFiles.add(pidFile);
            }

            inputXml = Files.createTempFile("cloudstack-vddk-rbd-" + temporaryConvertUuid, ".xml");
            Files.writeString(inputXml, buildDirectRbdNbdLibvirtXml(temporaryConvertUuid, ports));

            String inPlaceCommand = buildInPlaceCommand(inputXml, libguestfsBackend, verboseModeEnabled, serverResource);
            if (inPlaceCommand == null) {
                logger.error("({}) No virt-v2v in-place finalization method is available", originalVMName);
                return false;
            }

            StringBuilder script = new StringBuilder();
            script.append("#!/bin/bash\n");
            script.append("set -euo pipefail\n");
            script.append("cleanup() {\n");
            script.append("  set +e\n");
            script.append("  for pid_file in");
            for (Path pidFile : pidFiles) {
                script.append(" ").append(shellQuote(pidFile.toString()));
            }
            script.append("; do\n");
            script.append("    if [[ -s \"$pid_file\" ]]; then\n");
            script.append("      pid=$(cat \"$pid_file\")\n");
            script.append("      kill \"$pid\" >/dev/null 2>&1 || true\n");
            script.append("      for attempt in {1..20}; do\n");
            script.append("        kill -0 \"$pid\" >/dev/null 2>&1 || break\n");
            script.append("        sleep 0.1\n");
            script.append("      done\n");
            script.append("    fi\n");
            script.append("    rm -f \"$pid_file\"\n");
            script.append("  done\n");
            script.append("}\n");
            script.append("trap cleanup EXIT\n");
            for (int i = 0; i < imageNames.size(); i++) {
                String qemuRbdPath = KVMPhysicalDisk.RBDStringBuilder(targetPool,
                        targetPool.getSourceDir() + "/" + imageNames.get(i));
                script.append("qemu-nbd --fork --persistent --shared=1 --format=raw --bind=127.0.0.1 --port=")
                        .append(ports.get(i))
                        .append(" --pid-file=").append(shellQuote(pidFiles.get(i).toString()))
                        .append(" ").append(shellQuote(qemuRbdPath)).append("\n");
            }
            script.append(inPlaceCommand).append("\n");

            scriptPath = Files.createTempFile("cloudstack-vddk-rbd-finalize-" + temporaryConvertUuid + "-", ".sh");
            Files.writeString(scriptPath, script.toString());
            Files.setPosixFilePermissions(scriptPath, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));

            Script runner = new Script("/bin/bash", timeout, logger);
            runner.add(scriptPath.toString());
            OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(logger,
                    String.format("(%s) virt-v2v rbd in-place", originalVMName));
            runner.execute(outputLogger);
            return runner.getExitValue() == 0;
        } catch (Exception e) {
            logger.error("({}) RBD in-place finalization over NBD bridges failed: {}", originalVMName, e.getMessage(), e);
            return false;
        } finally {
            deleteQuietly(inputXml);
            deleteQuietly(scriptPath);
            for (Path pidFile : pidFiles) {
                deleteQuietly(pidFile);
            }
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            logger.debug("Unable to delete temporary file {}: {}", path, e.getMessage());
        }
    }

    private int allocateLocalhostPort() throws java.io.IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(false);
            return socket.getLocalPort();
        }
    }

    protected String buildDirectRbdNbdLibvirtXml(String temporaryConvertUuid, List<Integer> ports) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>").append(xmlEscape("cloudstack-vddk-rbd-" + temporaryConvertUuid)).append("</name>\n");
        xml.append("  <memory unit='KiB'>1048576</memory>\n");
        xml.append("  <vcpu>1</vcpu>\n");
        xml.append("  <os><type>hvm</type><boot dev='hd'/></os>\n");
        xml.append("  <devices>\n");
        for (int i = 0; i < ports.size(); i++) {
            xml.append("    <disk type='network' device='disk'>\n");
            xml.append("      <driver name='qemu' type='raw'/>\n");
            xml.append("      <source protocol='nbd'>\n");
            xml.append("        <host name='localhost' port='").append(ports.get(i)).append("'/>\n");
            xml.append("      </source>\n");
            xml.append("      <target dev='").append(diskTargetName(i)).append("' bus='scsi'/>\n");
            xml.append("    </disk>\n");
        }
        xml.append("  </devices>\n");
        xml.append("</domain>\n");
        return xml.toString();
    }

    private String buildDirectBlockDeviceLibvirtXml(String temporaryConvertUuid, List<String> devicePaths) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domain type='kvm'>\n");
        xml.append("  <name>").append(xmlEscape("cloudstack-vddk-direct-" + temporaryConvertUuid)).append("</name>\n");
        xml.append("  <memory unit='KiB'>1048576</memory>\n");
        xml.append("  <vcpu>1</vcpu>\n");
        xml.append("  <os><type>hvm</type><boot dev='hd'/></os>\n");
        xml.append("  <devices>\n");
        for (int i = 0; i < devicePaths.size(); i++) {
            xml.append("    <disk type='block' device='disk'>\n");
            xml.append("      <driver name='qemu' type='raw'/>\n");
            xml.append("      <source dev='").append(xmlEscape(devicePaths.get(i))).append("'/>\n");
            xml.append("      <target dev='").append(diskTargetName(i)).append("' bus='scsi'/>\n");
            xml.append("    </disk>\n");
        }
        xml.append("  </devices>\n");
        xml.append("</domain>\n");
        return xml.toString();
    }

    private String buildRbdImageName(String temporaryConvertUuid, int position) {
        return String.format("%s-disk-%03d", temporaryConvertUuid, position);
    }

    /**
     * Linstor disk names use a shorter position suffix than the RBD naming scheme:
     * LINSTOR resource names are limited to 48 characters and the "cs-" prefix plus
     * a 36-character UUID leaves little room ("-d%02d" keeps the name at 43).
     */
    private String buildLinstorDiskName(String temporaryConvertUuid, int position) {
        return String.format("%s-d%02d", temporaryConvertUuid, position);
    }

    private String diskTargetName(int index) {
        StringBuilder target = new StringBuilder("sd");
        int value = index;
        do {
            target.insert(2, (char) ('a' + (value % 26)));
            value = value / 26 - 1;
        } while (value >= 0);
        return target.toString();
    }

    private void probeRbdQemuAccess(KVMStoragePool pool, String temporaryConvertUuid) {
        String probeName = temporaryConvertUuid + "-probe-" + UUID.randomUUID();
        String rbdImagePath = pool.getSourceDir() + "/" + probeName;
        String qemuRbdPath = KVMPhysicalDisk.RBDStringBuilder(pool, rbdImagePath);
        try {
            Script qemuImg = new Script("qemu-img", 120000, logger);
            qemuImg.add("create", "-f", "raw", qemuRbdPath, "4194304");
            qemuImg.execute();
            if (qemuImg.getExitValue() != 0) {
                throw new CloudRuntimeException(String.format("qemu-img could not create RBD probe image %s", rbdImagePath));
            }

            Script qemuIo = new Script("qemu-io", 120000, logger);
            qemuIo.add("-f", "raw", "-c", "write -P 0x5a 0 4k", "-c", "read -P 0x5a 0 4k", qemuRbdPath);
            qemuIo.execute();
            if (qemuIo.getExitValue() != 0) {
                throw new CloudRuntimeException(String.format("qemu-io could not verify RBD probe image %s", rbdImagePath));
            }
        } finally {
            try {
                pool.deletePhysicalDisk(probeName, Storage.ImageFormat.RAW);
            } catch (Exception e) {
                logger.warn("Failed to delete RBD probe image {} from pool {}: {}", probeName, pool.getUuid(), e.getMessage());
            }
        }
    }

    private void probeLinstorQemuAccess(KVMStoragePool pool, String temporaryConvertUuid) {
        String probeName = temporaryConvertUuid.substring(0, 8) + "-probe";
        try {
            KVMPhysicalDisk probeDisk = pool.createPhysicalDisk(probeName, QemuImg.PhysicalDiskFormat.RAW,
                    Storage.ProvisioningType.THIN, 4194304L, null);
            Script qemuIo = new Script("qemu-io", 120000, logger);
            qemuIo.add("-f", "raw", "-c", "write -P 0x5a 0 4k", "-c", "read -P 0x5a 0 4k", probeDisk.getPath());
            qemuIo.execute();
            if (qemuIo.getExitValue() != 0) {
                throw new CloudRuntimeException(String.format("qemu-io could not verify Linstor probe volume %s on pool %s", probeName, pool.getUuid()));
            }
        } finally {
            try {
                pool.deletePhysicalDisk(probeName, Storage.ImageFormat.RAW);
            } catch (Exception e) {
                logger.warn("Failed to delete Linstor probe volume {} from pool {}: {}", probeName, pool.getUuid(), e.getMessage());
            }
        }
    }

    private void cleanupDirectImportDisks(KVMStoragePool pool, List<String> imageNames, String originalVMName) {
        for (String imageName : imageNames) {
            try {
                logger.info("({}) Cleaning up disk {} after failed direct import", originalVMName, imageName);
                pool.deletePhysicalDisk(imageName, Storage.ImageFormat.RAW);
            } catch (Exception e) {
                logger.warn("({}) Failed to delete disk {} from pool {}: {}", originalVMName, imageName, pool.getUuid(), e.getMessage());
            }
        }
    }

    private String shellQuote(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\"'\"'") + "'";
    }

    private String xmlEscape(String value) {
        return StringUtils.defaultString(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    protected String getVcenterThumbprint(String vcenterHost, long timeout, String originalVMName) {
        if (StringUtils.isBlank(vcenterHost)) {
            return null;
        }

        String endpoint = String.format("%s:443", vcenterHost);
        String command = String.format("openssl s_client -connect '%s' </dev/null 2>/dev/null | " +
                "openssl x509 -fingerprint -sha1 -noout", endpoint);

        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(command);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        script.execute(parser);

        String output = parser.getLines();
        if (script.getExitValue() != 0) {
            logger.error("({}) Failed to fetch vCenter thumbprint for {}", originalVMName, vcenterHost);
            return null;
        }

        String thumbprint = extractSha1Fingerprint(output);
        if (StringUtils.isBlank(thumbprint)) {
            logger.error("({}) Failed to parse vCenter thumbprint from output for {}", originalVMName, vcenterHost);
            return null;
        }
        return thumbprint;
    }

    private String extractSha1Fingerprint(String output) {
        String parsedOutput = StringUtils.trimToEmpty(output);
        if (StringUtils.isBlank(parsedOutput)) {
            return null;
        }

        for (String line : parsedOutput.split("\\R")) {
            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isBlank(trimmedLine)) {
                continue;
            }

            Matcher matcher = SHA1_FINGERPRINT_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }

            // Fallback for raw fingerprint-only output.
            if (trimmedLine.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2})+")) {
                return trimmedLine.toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    /**
     * Build vpx:// URL for virt-v2v
     *
     * Format:
     * vpx://user@vcenter/DC/cluster/host?no_verify=1
     */
    private String buildVpxUrl(RemoteInstanceTO vmwareInstance) {

        String vmName = vmwareInstance.getInstanceName();
        String vcenter = vmwareInstance.getVcenterHost();
        String username = vmwareInstance.getVcenterUsername();
        String datacenter = vmwareInstance.getDatacenterName();
        String cluster = vmwareInstance.getClusterName();
        String host = vmwareInstance.getHostName();

        String encodedUsername = encodeUsername(username);

        StringBuilder url = new StringBuilder();
        url.append("vpx://")
                .append(encodedUsername)
                .append("@")
                .append(vcenter)
                .append("/")
                .append(datacenter);

        if (StringUtils.isNotBlank(cluster)) {
            url.append("/").append(cluster);
        }

        if (StringUtils.isNotBlank(host)) {
            url.append("/").append(host);
        }

        url.append("?no_verify=1");

        logger.info("({}) Using VPX URL: {}", vmName, url);
        return url.toString();
    }
}
