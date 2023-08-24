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
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.agent.api.to.OVFInformationTO;
import com.cloud.agent.api.to.VmwareVmForMigrationTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ResourceWrapper(handles =  DownloadCommand.class)
public class LibvirtDownloadCommandWrapper extends CommandWrapper<DownloadCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtDownloadCommandWrapper.class);

    @Override
    public Answer execute(DownloadCommand cmd, LibvirtComputingResource serverResource) {
        s_logger.info("Executing DownloadCommand");
        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        final long templateId = cmd.getId();
        final long accountId = cmd.getAccountId();
        String templateDescription = cmd.getDescription();
        String templateUniqueName = cmd.getName();

        VmwareVmForMigrationTO vmForMigrationTO = cmd.getVmwareVmForMigrationTO();
        String vcenter = vmForMigrationTO.getVcenter();
        String datacenter = vmForMigrationTO.getDatacenter();
        String username = vmForMigrationTO.getUsername();
        String password = vmForMigrationTO.getPassword();
        String host = vmForMigrationTO.getHost();
        String vmName = vmForMigrationTO.getVmName();
        String cluster = vmForMigrationTO.getCluster();
        String secondaryStorageUrl = vmForMigrationTO.getSecondaryStorageUrl();

        KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl);
        String mountPoint = secondaryPool.getLocalPath();
        s_logger.info(String.format("Secondary storage pool: uuid = %s, local path = %s", secondaryPool.getUuid(), mountPoint));

        String encodedUsername = encodeUsername(username);
        s_logger.info(String.format("Encoded username: %s", encodedUsername));
        String url = String.format("vpx://%s@%s/%s/%s/%s?no_verify=1",
                encodedUsername, vcenter, datacenter, cluster, host);

        s_logger.info(String.format("Migrating VM from vCenter: %s", vcenter));

        String passwordFile = String.format("/tmp/vmw-%s", UUID.randomUUID());
        Script.runSimpleBashScript(String.format("echo \"%s\" > %s", password, passwordFile));

        final String templateFolder = String.format("template/tmpl/%s/%s/", accountId, templateId);
        final String templateInstallFolder = String.format("%s/%s", mountPoint, templateFolder);
        final String baseName = UUID.randomUUID().toString();
        final String installBasePath = templateInstallFolder + baseName;
        s_logger.info(String.format("Creating template folder %s", templateInstallFolder));
        Script.runSimpleBashScript(String.format("mkdir -p %s", templateInstallFolder));

        try {
            int timeout = cmd.getWait();
            Script script = new Script("virt-v2v", timeout, s_logger);
            script.add("--root", "first");
            script.add("-ic", url);
            script.add(vmName);
            script.add("--password-file", passwordFile);
            script.add("-o", "local");
            script.add("-os", templateInstallFolder);
            script.add("-of", "qcow2");
            script.add("-on", baseName);
            String result = script.execute();
            s_logger.info(String.format("Execution result: %s", result));

            s_logger.info("Finished downloading template from Vmware migration, checking for template sizes");
            List<LibvirtVMDef.DiskDef> disks = getConvertedVmDisks(installBasePath);
            if (disks.size() < 1) {
                String err = String.format("Cannot find any disk for the migrated VM %s on path: %s", vmName, installBasePath);
                s_logger.error(err);
                return new DownloadAnswer(err, VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
            }

            List<DatadiskTO> vmDisks = getVMDisksFromDiskDefList(disks, mountPoint);
            List<DatadiskTO> dataDisks = getVMDataDisks(vmDisks);
            DatadiskTO rootDisk = vmDisks.get(0);
            String installPath = rootDisk.getPath();
            long virtualSize = rootDisk.getVirtualSize();
            long physicalSize = rootDisk.getFileSize();

            createTemplatePropertiesFileAfterMigration(templateInstallFolder, templateId, accountId,
                    templateDescription, installPath, virtualSize, physicalSize, templateUniqueName);

            DownloadAnswer answer = new DownloadAnswer(null, 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                    null, installPath, virtualSize, physicalSize, null);
            answer.setOvfInformationTO(new OVFInformationTO(dataDisks));
            return answer;
        } catch (Exception e) {
            String error = String.format("Error migrating VM from vcenter %s, %s", vcenter, e.getMessage());
            s_logger.error(error, e);
            return new DownloadAnswer(error, VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
        } finally {
            Script.runSimpleBashScript(String.format("rm -rf %s", passwordFile));
        }
    }

    private void createTemplatePropertiesFileAfterMigration(String templateInstallFolder, long templateId,
                                                            long accountId, String templateDescription,
                                                            String installPath, long virtualSize,
                                                            long physicalSize, String templateUniqueName) throws IOException {
        String templateFilePath = String.format("%s%s", templateInstallFolder, "template.properties");
        final File templateProp = new File(templateFilePath);
        if (!templateProp.exists()) {
            templateProp.createNewFile();
        }
        String pathPrefix = String.format("template/tmpl/%s/%s", accountId, templateId);
        String relativePath = removeMountPointFromDiskPath(installPath, pathPrefix);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%s=%s%s", "uniquename", templateUniqueName, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "id", templateId, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "description", templateDescription, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "filename", relativePath, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "qcow2.filename", relativePath, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "size", physicalSize, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "qcow2.size", physicalSize, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "virtualsize", virtualSize, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "qcow2.virtualsize", virtualSize, System.getProperty("line.separator")));
        stringBuilder.append(String.format("%s=%s%s", "qcow2", "true", System.getProperty("line.separator")));
        String fileContent = stringBuilder.toString();

        try(FileOutputStream templFo = new FileOutputStream(templateProp)){
            templFo.write(fileContent.getBytes());
            templFo.flush();
        } catch (final IOException e) {
            String err = String.format("Cannot create template.properties file for template %s", templateId);
            s_logger.error(err, e);
        }
    }

    /**
     * Exclude the root disk (first element in the list) and return the rest of the disks
     */
    protected List<DatadiskTO> getVMDataDisks(List<DatadiskTO> vmDisks) {
        List<DatadiskTO> dataDisks = new ArrayList<>();
        if (vmDisks.size() > 1) {
            for (int index = 1; index < vmDisks.size(); index++) {
                DatadiskTO datadiskTO = vmDisks.get(index);
                dataDisks.add(datadiskTO);
            }
        }
        return dataDisks;
    }

    protected List<DatadiskTO> getVMDisksFromDiskDefList(List<LibvirtVMDef.DiskDef> disks, String mountPoint) {
        List<DatadiskTO> datadiskTOList = new ArrayList<>();
        int deviceId = 0;
        for (LibvirtVMDef.DiskDef disk : disks) {
            String diskPath = disk.getDiskPath();
            String relativePath = removeMountPointFromDiskPath(diskPath, mountPoint);
            long virtualSize = LibvirtUtilitiesHelper.getVirtualSizeFromFile(diskPath);
            long physicalSize = new File(diskPath).length();

            DatadiskTO datadiskTO = new DatadiskTO(relativePath, virtualSize, physicalSize, deviceId);
            datadiskTOList.add(datadiskTO);
            deviceId++;
        }
        return datadiskTOList;
    }

    protected String removeMountPointFromDiskPath(String diskPath, String mountPoint) {
        if (!diskPath.startsWith(mountPoint)) {
            String err = String.format("Expected the mount point %s to be present on the disk path %s", mountPoint, diskPath);
            throw new CloudRuntimeException(err);
        }
        return diskPath.substring(mountPoint.length() + 1);
    }

    protected List<LibvirtVMDef.DiskDef> getConvertedVmDisks(String installPath) throws IOException {
        String xmlPath = String.format("%s.xml", installPath);
        InputStream is = new BufferedInputStream(new FileInputStream(xmlPath));
        String xml = IOUtils.toString(is, Charset.defaultCharset());
        return getDisksFromXml(xml);
    }

    protected List<LibvirtVMDef.DiskDef> getDisksFromXml(String xml) {
        final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
        parser.parseDomainXML(xml);
        return parser.getDisks();
    }

    protected String encodeUsername(String username) {
        return URLEncoder.encode(username, Charset.defaultCharset());
    }
}
