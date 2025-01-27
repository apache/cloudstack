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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ImportConvertedInstanceAnswer;
import com.cloud.agent.api.ImportConvertedInstanceCommand;
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
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  ImportConvertedInstanceCommand.class)
public class LibvirtImportConvertedInstanceCommandWrapper extends CommandWrapper<ImportConvertedInstanceCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(ImportConvertedInstanceCommand cmd, LibvirtComputingResource serverResource) {
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        Hypervisor.HypervisorType sourceHypervisorType = sourceInstance.getHypervisorType();
        String sourceInstanceName = sourceInstance.getInstanceName();
        List<String> destinationStoragePools = cmd.getDestinationStoragePools();
        DataStoreTO conversionTemporaryLocation = cmd.getConversionTemporaryLocation();
        final String temporaryConvertUuid = cmd.getTemporaryConvertUuid();

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool temporaryStoragePool = getTemporaryStoragePool(conversionTemporaryLocation, storagePoolMgr);
        final String temporaryConvertPath = temporaryStoragePool.getLocalPath();

        try {
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
            return new ImportConvertedInstanceAnswer(cmd, convertedInstanceTO);
        } catch (Exception e) {
            String error = String.format("Error converting instance %s from %s, due to: %s",
                    sourceInstanceName, sourceHypervisorType, e.getMessage());
            logger.error(error, e);
            return new ImportConvertedInstanceAnswer(cmd, false, error);
        } finally {
            if (conversionTemporaryLocation instanceof NfsTO) {
                logger.debug("Cleaning up secondary storage temporary location");
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

    protected List<KVMPhysicalDisk> getTemporaryDisksFromParsedXml(KVMStoragePool pool, LibvirtDomainXMLParser xmlParser, String convertedBasePath) {
        List<LibvirtVMDef.DiskDef> disksDefs = xmlParser.getDisks();
        disksDefs = disksDefs.stream().filter(x -> x.getDiskType() == LibvirtVMDef.DiskDef.DiskType.FILE &&
                x.getDeviceType() == LibvirtVMDef.DiskDef.DeviceType.DISK).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(disksDefs)) {
            String err = String.format("Cannot find any disk defined on the converted XML domain %s.xml", convertedBasePath);
            logger.error(err);
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
        logger.info(msg);
        pool.refresh();
        List<KVMPhysicalDisk> disksWithPrefix = pool.listPhysicalDisks()
                .stream()
                .filter(x -> x.getName().startsWith(prefix) && !x.getName().endsWith(".xml"))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(disksWithPrefix)) {
            msg = String.format("Could not find any converted disk with prefix %s on temporary location %s", prefix, path);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return disksWithPrefix;
    }

    private void cleanupDisksAndDomainFromTemporaryLocation(List<KVMPhysicalDisk> disks,
                                                            KVMStoragePool temporaryStoragePool,
                                                            String temporaryConvertUuid) {
        for (KVMPhysicalDisk disk : disks) {
            logger.info(String.format("Cleaning up temporary disk %s after conversion from temporary location", disk.getName()));
            temporaryStoragePool.deletePhysicalDisk(disk.getName(), Storage.ImageFormat.QCOW2);
        }
        logger.info(String.format("Cleaning up temporary domain %s after conversion from temporary location", temporaryConvertUuid));
        FileUtil.deleteFiles(temporaryStoragePool.getLocalPath(), temporaryConvertUuid, ".xml");
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
            logger.warn(warn);
        }
        for (int i = 0; i < temporaryDisks.size(); i++) {
            String poolPath = destinationStoragePools.get(i);
            KVMStoragePool destinationPool = storagePoolMgr.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, poolPath);
            if (destinationPool == null) {
                String err = String.format("Could not find a storage pool by URI: %s", poolPath);
                logger.error(err);
                continue;
            }
            if (destinationPool.getType() != Storage.StoragePoolType.NetworkFilesystem) {
                String err = String.format("Storage pool by URI: %s is not an NFS storage", poolPath);
                logger.error(err);
                continue;
            }
            KVMPhysicalDisk sourceDisk = temporaryDisks.get(i);
            if (logger.isDebugEnabled()) {
                String msg = String.format("Trying to copy converted instance disk number %s from the temporary location %s" +
                        " to destination storage pool %s", i, sourceDisk.getPool().getLocalPath(), destinationPool.getUuid());
                logger.debug(msg);
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
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{Script.getExecutableAbsolutePath("mount")});
        commands.add(new String[]{Script.getExecutableAbsolutePath("grep"), storagePool.getLocalPath()});
        String storagePoolMountPoint = Script.executePipedCommands(commands, 0).second();
        logger.debug(String.format("NFS Storage pool: %s - local path: %s, mount point: %s", storagePool.getUuid(), storagePool.getLocalPath(), storagePoolMountPoint));
        if (StringUtils.isNotEmpty(storagePoolMountPoint)) {
            String[] res = storagePoolMountPoint.strip().split(" ");
            res = res[0].split(":");
            if (res.length > 1) {
                sourceHostIp = res[0].strip();
                sourcePath = res[1].strip();
            }
        }
        return new Pair<>(sourceHostIp, sourcePath);
    }

    protected LibvirtDomainXMLParser parseMigratedVMXmlDomain(String installPath) throws IOException {
        String xmlPath = String.format("%s.xml", installPath);
        if (!new File(xmlPath).exists()) {
            String err = String.format("Conversion failed. Unable to find the converted XML domain, expected %s", xmlPath);
            logger.error(err);
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
            logger.error(err, e);
            logger.debug(xml);
            return null;
        }
    }
}
