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

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConvertInstanceAnswer;
import com.cloud.agent.api.ConvertInstanceCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.FileUtil;
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
                            "Set '%s' in agent.properties or in details parameter of the import api calll to use VDDK-based conversion.", "vddk.lib.dir");
                    logger.error("({}) {}", originalVMName, err);
                    return new Answer(cmd, false, err);
                }
                String vddkTransports = resolveVddkSetting(cmd.getVddkTransports(), serverResource.getVddkTransports());
                String configuredVddkThumbprint = resolveVddkSetting(cmd.getVddkThumbprint(), serverResource.getVddkThumbprint());
                String passwordOption = serverResource.getDetectedPasswordFileOption();
                result = performInstanceConversionUsingVddk(sourceInstance, originalVMName, temporaryConvertPath,
                        vddkLibDir, serverResource.getLibguestfsBackend(), vddkTransports, configuredVddkThumbprint,
                        timeout, verboseModeEnabled, extraParams, temporaryConvertUuid, passwordOption);
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
            logger.debug("({}) Written vCenter password to {}", originalVMName, passwordFilePath);
        } catch (Exception e) {
            logger.error("({}) Failed to write vCenter password file {}: {}", originalVMName, passwordFilePath, e.getMessage());
            return false;
        }

        try {
            String vpxUrl = buildVpxUrl(vmwareInstance, originalVMName);

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
            cmd.append(originalVMName).append(" ");
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
    private String buildVpxUrl(RemoteInstanceTO vmwareInstance, String originalVMName) {

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

        logger.info("({}) Using VPX URL: {}", originalVMName, url);
        return url.toString();
    }
}
