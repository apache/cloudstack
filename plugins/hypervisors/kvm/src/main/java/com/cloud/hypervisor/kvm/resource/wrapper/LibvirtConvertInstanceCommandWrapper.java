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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConvertInstanceAnswer;
import com.cloud.agent.api.ConvertInstanceCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ResourceWrapper(handles =  ConvertInstanceCommand.class)
public class LibvirtConvertInstanceCommandWrapper extends CommandWrapper<ConvertInstanceCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtConvertInstanceCommandWrapper.class);

    private static final List<Hypervisor.HypervisorType> supportedInstanceConvertSourceHypervisors =
            List.of(Hypervisor.HypervisorType.VMware);

    protected static final String checkIfConversionIsSupportedCommand = "which virt-v2v";

    @Override
    public Answer execute(ConvertInstanceCommand cmd, LibvirtComputingResource serverResource) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        Hypervisor.HypervisorType sourceHypervisorType = sourceInstance.getHypervisorType();
        String sourceInstanceName = sourceInstance.getInstanceName();
        Hypervisor.HypervisorType destinationHypervisorType = cmd.getDestinationHypervisorType();
        List<String> destinationStoragePools = cmd.getDestinationStoragePools();
        DataStoreTO conversionTemporaryLocation = cmd.getConversionTemporaryLocation();
        long timeout = (long) cmd.getWait() * 1000;

        if (!isInstanceConversionSupportedOnHost()) {
            String msg = String.format("Cannot convert the instance %s from VMware as the virt-v2v binary is not found. " +
                    "Please install virt-v2v on the host before attempting the instance conversion", sourceInstanceName);
            s_logger.info(msg);
            return new ConvertInstanceAnswer(cmd, false, msg);
        }

        if (!areSourceAndDestinationHypervisorsSupported(sourceHypervisorType, destinationHypervisorType)) {
            String err = destinationHypervisorType != Hypervisor.HypervisorType.KVM ?
                    String.format("The destination hypervisor type is %s, KVM was expected, cannot handle it", destinationHypervisorType) :
                    String.format("The source hypervisor type %s is not supported for KVM conversion", sourceHypervisorType);
            s_logger.error(err);
            return new ConvertInstanceAnswer(cmd, false, err);
        }

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool temporaryStoragePool = getTemporaryStoragePool(conversionTemporaryLocation, storagePoolMgr);

        s_logger.info(String.format("Attempting to convert the instance %s from %s to KVM",
                sourceInstanceName, sourceHypervisorType));
        final String convertInstanceUrl = getConvertInstanceUrl(sourceInstance);
        final String temporaryConvertUuid = UUID.randomUUID().toString();
        final String temporaryPasswordFilePath = createTemporaryPasswordFileAndRetrievePath(sourceInstance);
        final String temporaryConvertPath = temporaryStoragePool.getLocalPath();
        boolean verboseModeEnabled = serverResource.isConvertInstanceVerboseModeEnabled();

        try {
            boolean result = performInstanceConversion(convertInstanceUrl, sourceInstanceName, temporaryPasswordFilePath,
                    temporaryConvertPath, temporaryConvertUuid, timeout, verboseModeEnabled);
            if (!result) {
                String err = String.format("The virt-v2v conversion of the instance %s failed. " +
                                "Please check the agent logs for the virt-v2v output", sourceInstanceName);
                s_logger.error(err);
                return new ConvertInstanceAnswer(cmd, false, err);
            }
            String convertedBasePath = String.format("%s/%s", temporaryConvertPath, temporaryConvertUuid);
            LibvirtDomainXMLParser xmlParser = parseMigratedVMXmlDomain(convertedBasePath);

            List<KVMPhysicalDisk> temporaryDisks = xmlParser == null ?
                    getTemporaryDisksWithPrefixFromTemporaryPool(temporaryStoragePool, temporaryConvertPath, temporaryConvertUuid) :
                    getTemporaryDisksFromParsedXml(temporaryStoragePool, xmlParser, convertedBasePath);

            List<KVMPhysicalDisk> destinationDisks = moveTemporaryDisksToDestination(temporaryDisks,
                    destinationStoragePools, storagePoolMgr);

            cleanupDisksAndDomainFromTemporaryLocation(temporaryDisks, temporaryStoragePool, temporaryConvertUuid);

            UnmanagedInstanceTO convertedInstanceTO = getConvertedUnmanagedInstance(temporaryConvertUuid,
                    destinationDisks, xmlParser);
            return new ConvertInstanceAnswer(cmd, convertedInstanceTO);
        } catch (Exception e) {
            String error = String.format("Error converting instance %s from %s, due to: %s",
                    sourceInstanceName, sourceHypervisorType, e.getMessage());
            s_logger.error(error, e);
            return new ConvertInstanceAnswer(cmd, false, error);
        } finally {
            s_logger.debug("Cleaning up instance conversion temporary password file");
            Script.runSimpleBashScript(String.format("rm -rf %s", temporaryPasswordFilePath));
            if (conversionTemporaryLocation instanceof NfsTO) {
                s_logger.debug("Cleaning up secondary storage temporary location");
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

    protected List<KVMPhysicalDisk> getTemporaryDisksFromParsedXml(KVMStoragePool pool, LibvirtDomainXMLParser xmlParser, String convertedBasePath) {
        List<LibvirtVMDef.DiskDef> disksDefs = xmlParser.getDisks();
        disksDefs = disksDefs.stream().filter(x -> x.getDiskType() == LibvirtVMDef.DiskDef.DiskType.FILE &&
                x.getDeviceType() == LibvirtVMDef.DiskDef.DeviceType.DISK).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(disksDefs)) {
            String err = String.format("Cannot find any disk defined on the converted XML domain %s.xml", convertedBasePath);
            s_logger.error(err);
            throw new CloudRuntimeException(err);
        }
        sanitizeDisksPath(disksDefs);
        return getPhysicalDisksFromDefPaths(disksDefs, pool);
    }

    private List<KVMPhysicalDisk> getPhysicalDisksFromDefPaths(List<LibvirtVMDef.DiskDef> disksDefs, KVMStoragePool pool) {
        List<KVMPhysicalDisk> disks = new ArrayList<>();
        for (LibvirtVMDef.DiskDef diskDef : disksDefs) {
            KVMPhysicalDisk physicalDisk = pool.getPhysicalDisk(diskDef.getDiskPath());
            disks.add(physicalDisk);
        }
        return disks;
    }

    protected List<KVMPhysicalDisk> getTemporaryDisksWithPrefixFromTemporaryPool(KVMStoragePool pool, String path, String prefix) {
        String msg = String.format("Could not parse correctly the converted XML domain, checking for disks on %s with prefix %s", path, prefix);
        s_logger.info(msg);
        pool.refresh();
        List<KVMPhysicalDisk> disksWithPrefix = pool.listPhysicalDisks()
                .stream()
                .filter(x -> x.getName().startsWith(prefix) && !x.getName().endsWith(".xml"))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(disksWithPrefix)) {
            msg = String.format("Could not find any converted disk with prefix %s on temporary location %s", prefix, path);
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return disksWithPrefix;
    }

    private void cleanupDisksAndDomainFromTemporaryLocation(List<KVMPhysicalDisk> disks,
                                                            KVMStoragePool temporaryStoragePool,
                                                            String temporaryConvertUuid) {
        for (KVMPhysicalDisk disk : disks) {
            s_logger.info(String.format("Cleaning up temporary disk %s after conversion from temporary location", disk.getName()));
            temporaryStoragePool.deletePhysicalDisk(disk.getName(), Storage.ImageFormat.QCOW2);
        }
        s_logger.info(String.format("Cleaning up temporary domain %s after conversion from temporary location", temporaryConvertUuid));
        Script.runSimpleBashScript(String.format("rm -f %s/%s*.xml", temporaryStoragePool.getLocalPath(), temporaryConvertUuid));
    }

    protected boolean isInstanceConversionSupportedOnHost() {
        int exitValue = Script.runSimpleBashScriptForExitValue(checkIfConversionIsSupportedCommand);
        return exitValue == 0;
    }

    protected void sanitizeDisksPath(List<LibvirtVMDef.DiskDef> disks) {
        for (LibvirtVMDef.DiskDef disk : disks) {
            String[] diskPathParts = disk.getDiskPath().split("/");
            String relativePath = diskPathParts[diskPathParts.length - 1];
            disk.setDiskPath(relativePath);
        }
    }

    protected List<KVMPhysicalDisk> moveTemporaryDisksToDestination(List<KVMPhysicalDisk> temporaryDisks,
                                                                  List<String> destinationStoragePools,
                                                                  KVMStoragePoolManager storagePoolMgr) {
        List<KVMPhysicalDisk> targetDisks = new ArrayList<>();
        if (temporaryDisks.size() != destinationStoragePools.size()) {
            String warn = String.format("Discrepancy between the converted instance disks (%s) " +
                    "and the expected number of disks (%s)", temporaryDisks.size(), destinationStoragePools.size());
            s_logger.warn(warn);
        }
        for (int i = 0; i < temporaryDisks.size(); i++) {
            String poolPath = destinationStoragePools.get(i);
            KVMStoragePool destinationPool = storagePoolMgr.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, poolPath);
            if (destinationPool == null) {
                String err = String.format("Could not find a storage pool by URI: %s", poolPath);
                s_logger.error(err);
                continue;
            }
            KVMPhysicalDisk sourceDisk = temporaryDisks.get(i);
            if (s_logger.isDebugEnabled()) {
                String msg = String.format("Trying to copy converted instance disk number %s from the temporary location %s" +
                        " to destination storage pool %s", i, sourceDisk.getPool().getLocalPath(), destinationPool.getUuid());
                s_logger.debug(msg);
            }

            String destinationName = UUID.randomUUID().toString();

            KVMPhysicalDisk destinationDisk = storagePoolMgr.copyPhysicalDisk(sourceDisk, destinationName, destinationPool, 7200 * 1000);
            targetDisks.add(destinationDisk);
        }
        return targetDisks;
    }

    private UnmanagedInstanceTO getConvertedUnmanagedInstance(String baseName,
                                                              List<KVMPhysicalDisk> vmDisks,
                                                              LibvirtDomainXMLParser xmlParser) {
        UnmanagedInstanceTO instanceTO = new UnmanagedInstanceTO();
        instanceTO.setName(baseName);
        instanceTO.setDisks(getUnmanagedInstanceDisks(vmDisks, xmlParser));
        instanceTO.setNics(getUnmanagedInstanceNics(xmlParser));
        return instanceTO;
    }

    private List<UnmanagedInstanceTO.Nic> getUnmanagedInstanceNics(LibvirtDomainXMLParser xmlParser) {
        List<UnmanagedInstanceTO.Nic> nics = new ArrayList<>();
        if (xmlParser != null) {
            List<LibvirtVMDef.InterfaceDef> interfaces = xmlParser.getInterfaces();
            for (LibvirtVMDef.InterfaceDef interfaceDef : interfaces) {
                UnmanagedInstanceTO.Nic nic = new UnmanagedInstanceTO.Nic();
                nic.setMacAddress(interfaceDef.getMacAddress());
                nic.setNicId(interfaceDef.getBrName());
                nic.setAdapterType(interfaceDef.getModel().toString());
                nics.add(nic);
            }
        }
        return nics;
    }

    protected List<UnmanagedInstanceTO.Disk> getUnmanagedInstanceDisks(List<KVMPhysicalDisk> vmDisks, LibvirtDomainXMLParser xmlParser) {
        List<UnmanagedInstanceTO.Disk> instanceDisks = new ArrayList<>();
        List<LibvirtVMDef.DiskDef> diskDefs = xmlParser != null ? xmlParser.getDisks() : null;
        for (int i = 0; i< vmDisks.size(); i++) {
            KVMPhysicalDisk physicalDisk = vmDisks.get(i);
            KVMStoragePool storagePool = physicalDisk.getPool();
            UnmanagedInstanceTO.Disk disk = new UnmanagedInstanceTO.Disk();
            disk.setPosition(i);
            Pair<String, String> storagePoolHostAndPath = getNfsStoragePoolHostAndPath(storagePool);
            disk.setDatastoreHost(storagePoolHostAndPath.first());
            disk.setDatastorePath(storagePoolHostAndPath.second());
            disk.setDatastoreName(storagePool.getUuid());
            disk.setDatastoreType(storagePool.getType().name());
            disk.setCapacity(physicalDisk.getVirtualSize());
            disk.setFileBaseName(physicalDisk.getName());
            if (CollectionUtils.isNotEmpty(diskDefs)) {
                LibvirtVMDef.DiskDef diskDef = diskDefs.get(i);
                disk.setController(diskDef.getBusType() != null ? diskDef.getBusType().toString() : LibvirtVMDef.DiskDef.DiskBus.VIRTIO.toString());
            } else {
                // If the job is finished but we cannot parse the XML, the guest VM can use the virtio driver
                disk.setController(LibvirtVMDef.DiskDef.DiskBus.VIRTIO.toString());
            }
            instanceDisks.add(disk);
        }
        return instanceDisks;
    }

    protected Pair<String, String> getNfsStoragePoolHostAndPath(KVMStoragePool storagePool) {
        String sourceHostIp = null;
        String sourcePath = null;
        String storagePoolMountPoint = Script.runSimpleBashScript(String.format("mount | grep %s", storagePool.getLocalPath()));
        if (StringUtils.isNotEmpty(storagePoolMountPoint)) {
            String[] res = storagePoolMountPoint.strip().split(" ");
            res = res[0].split(":");
            sourceHostIp = res[0].strip();
            sourcePath = res[1].strip();
        }
        return new Pair<>(sourceHostIp, sourcePath);
    }

    protected boolean performInstanceConversion(String convertInstanceUrl, String sourceInstanceName,
                                              String temporaryPasswordFilePath,
                                              String temporaryConvertFolder,
                                              String temporaryConvertUuid,
                                              long timeout, boolean verboseModeEnabled) {
        Script script = new Script("virt-v2v", timeout, s_logger);
        script.add("--root", "first");
        script.add("-ic", convertInstanceUrl);
        script.add(sourceInstanceName);
        script.add("--password-file", temporaryPasswordFilePath);
        script.add("-o", "local");
        script.add("-os", temporaryConvertFolder);
        script.add("-of", "qcow2");
        script.add("-on", temporaryConvertUuid);
        if (verboseModeEnabled) {
            script.add("-v");
        }

        String logPrefix = String.format("virt-v2v source: %s %s progress", convertInstanceUrl, sourceInstanceName);
        OutputInterpreter.LineByLineOutputLogger outputLogger = new OutputInterpreter.LineByLineOutputLogger(s_logger, logPrefix);
        script.execute(outputLogger);
        int exitValue = script.getExitValue();
        return exitValue == 0;
    }

    private String createTemporaryPasswordFileAndRetrievePath(RemoteInstanceTO sourceInstance) {
        String password = null;
        if (sourceInstance.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
            password = sourceInstance.getVcenterPassword();
        }
        String passwordFile = String.format("/tmp/vmw-%s", UUID.randomUUID());
        String msg = String.format("Creating a temporary password file for VMware instance %s conversion on: %s", sourceInstance.getInstanceName(), passwordFile);
        s_logger.debug(msg);
        Script.runSimpleBashScriptForExitValueAvoidLogging(String.format("echo \"%s\" > %s", password, passwordFile));
        return passwordFile;
    }

    private String getConvertInstanceUrl(RemoteInstanceTO sourceInstance) {
        String url = null;
        if (sourceInstance.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
            url = getConvertInstanceUrlFromVmware(sourceInstance);
        }
        return url;
    }

    private String getConvertInstanceUrlFromVmware(RemoteInstanceTO vmwareInstance) {
        String vcenter = vmwareInstance.getVcenterHost();
        String datacenter = vmwareInstance.getDatacenterName();
        String username = vmwareInstance.getVcenterUsername();
        String host = vmwareInstance.getHostName();
        String cluster = vmwareInstance.getClusterName();

        String encodedUsername = encodeUsername(username);
        return String.format("vpx://%s@%s/%s/%s/%s?no_verify=1",
                encodedUsername, vcenter, datacenter, cluster, host);
    }
    protected LibvirtDomainXMLParser parseMigratedVMXmlDomain(String installPath) throws IOException {
        String xmlPath = String.format("%s.xml", installPath);
        if (!new File(xmlPath).exists()) {
            String err = String.format("Conversion failed. Unable to find the converted XML domain, expected %s", xmlPath);
            s_logger.error(err);
            throw new CloudRuntimeException(err);
        }
        InputStream is = new BufferedInputStream(new FileInputStream(xmlPath));
        String xml = IOUtils.toString(is, Charset.defaultCharset());
        final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        try {
            parser.parseDomainXML(xml);
            return parser;
        } catch (RuntimeException e) {
            String err = String.format("Error parsing the converted instance XML domain at %s: %s", xmlPath, e.getMessage());
            s_logger.error(err, e);
            s_logger.debug(xml);
            return null;
        }
    }

    protected String encodeUsername(String username) {
        return URLEncoder.encode(username, Charset.defaultCharset());
    }
}
