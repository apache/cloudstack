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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
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

    @Override
    public Answer execute(ConvertInstanceCommand cmd, LibvirtComputingResource serverResource) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        Hypervisor.HypervisorType sourceHypervisorType = sourceInstance.getHypervisorType();
        String sourceInstanceName = sourceInstance.getInstanceName();
        Hypervisor.HypervisorType destinationHypervisorType = cmd.getDestinationHypervisorType();
        DataStoreTO conversionTemporaryLocation = cmd.getConversionTemporaryLocation();
        long timeout = (long) cmd.getWait() * 1000;
        String extraParams = cmd.getExtraParams();
        String originalVMName = cmd.getOriginalVMName(); // For logging purposes, as the sourceInstance may have been cloned

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

        String ovfTemplateDirOnConversionLocation;
        String sourceOVFDirPath;
        boolean ovfExported = false;
        if (cmd.getExportOvfToConversionLocation()) {
            String exportInstanceOVAUrl = getExportInstanceOVAUrl(sourceInstance, originalVMName);
            if (StringUtils.isBlank(exportInstanceOVAUrl)) {
                String err = String.format("Couldn't export OVA for the VM %s, due to empty url", sourceInstanceName);
                logger.error(String.format("(%s) %s", originalVMName, err));
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
                logger.error(String.format("(%s) %s", originalVMName, err));
                return new Answer(cmd, false, err);
            }
            sourceOVFDirPath = String.format("%s%s/", sourceOVFDirPath, sourceInstanceName);
        } else {
            ovfTemplateDirOnConversionLocation = cmd.getTemplateDirOnConversionLocation();
            sourceOVFDirPath = String.format("%s/%s/", temporaryConvertPath, ovfTemplateDirOnConversionLocation);
        }

        logger.info(String.format("(%s) Attempting to convert the OVF %s of the instance %s from %s to KVM",
                originalVMName, ovfTemplateDirOnConversionLocation, sourceInstanceName, sourceHypervisorType));

        final String temporaryConvertUuid = UUID.randomUUID().toString();
        boolean verboseModeEnabled = serverResource.isConvertInstanceVerboseModeEnabled();

        boolean cleanupSecondaryStorage = false;
        try {
            boolean result = performInstanceConversion(originalVMName, sourceOVFDirPath, temporaryConvertPath, temporaryConvertUuid,
                    timeout, verboseModeEnabled, extraParams, serverResource);
            if (!result) {
                String err = String.format(
                        "The virt-v2v conversion for the OVF %s failed. Please check the agent logs " +
                                "for the virt-v2v output. Please try on a different kvm host which " +
                                "has a different virt-v2v version.",
                        ovfTemplateDirOnConversionLocation);
                logger.error(String.format("(%s) %s", originalVMName, err));
                return new Answer(cmd, false, err);
            }
            return new ConvertInstanceAnswer(cmd, temporaryConvertUuid);
        } catch (Exception e) {
            String error = String.format("Error converting instance %s from %s, due to: %s",
                    sourceInstanceName, sourceHypervisorType, e.getMessage());
            logger.error(String.format("(%s) %s", originalVMName, error), e);
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
        script.execute(outputLogger);
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
}
