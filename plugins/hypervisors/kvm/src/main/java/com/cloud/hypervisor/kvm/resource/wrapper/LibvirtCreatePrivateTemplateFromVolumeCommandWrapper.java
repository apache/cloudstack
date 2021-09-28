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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles =  CreatePrivateTemplateFromVolumeCommand.class)
public final class LibvirtCreatePrivateTemplateFromVolumeCommandWrapper extends CommandWrapper<CreatePrivateTemplateFromVolumeCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCreatePrivateTemplateFromVolumeCommandWrapper.class);

    @Override
    public Answer execute(final CreatePrivateTemplateFromVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String secondaryStorageURL = command.getSecondaryStorageUrl();

        KVMStoragePool secondaryStorage = null;
        KVMStoragePool primary = null;
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
        try {
            final String templateFolder = command.getAccountId() + File.separator + command.getTemplateId() + File.separator;
            final String templateInstallFolder = "/template/tmpl/" + templateFolder;

            secondaryStorage = storagePoolMgr.getStoragePoolByURI(secondaryStorageURL);

            try {
                primary = storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPrimaryStoragePoolNameLabel());
            } catch (final CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primary =
                            storagePoolMgr.createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(),
                                    command.getPool().getUserInfo(), command.getPool().getType());
                } else {
                    return new CreatePrivateTemplateAnswer(command, false, e.getMessage());
                }
            }

            final KVMPhysicalDisk disk = primary.getPhysicalDisk(command.getVolumePath());
            final String tmpltPath = secondaryStorage.getLocalPath() + File.separator + templateInstallFolder;
            final StorageLayer storage = libvirtComputingResource.getStorage();
            storage.mkdirs(tmpltPath);

            if (primary.getType() != StoragePoolType.RBD) {
                final String createTmplPath = libvirtComputingResource.createTmplPath();
                final int cmdsTimeout = libvirtComputingResource.getCmdsTimeout();

                final Script scriptCommand = new Script(createTmplPath, cmdsTimeout, s_logger);
                scriptCommand.add("-f", disk.getPath());
                scriptCommand.add("-t", tmpltPath);
                scriptCommand.add("-n", command.getUniqueName() + ".qcow2");

                final String result = scriptCommand.execute();

                if (result != null) {
                    s_logger.debug("failed to create template: " + result);
                    return new CreatePrivateTemplateAnswer(command, false, result);
                }
            } else {
                s_logger.debug("Converting RBD disk " + disk.getPath() + " into template " + command.getUniqueName());

                final QemuImgFile srcFile =
                        new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(primary.getSourceHost(), primary.getSourcePort(), primary.getAuthUserName(),
                                primary.getAuthSecret(), disk.getPath()));
                srcFile.setFormat(PhysicalDiskFormat.RAW);

                final QemuImgFile destFile = new QemuImgFile(tmpltPath + "/" + command.getUniqueName() + ".qcow2");
                destFile.setFormat(PhysicalDiskFormat.QCOW2);

                final QemuImg q = new QemuImg(0);
                try {
                    q.convert(srcFile, destFile);
                } catch (final QemuImgException | LibvirtException e) {
                    s_logger.error("Failed to create new template while converting " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " +
                            e.getMessage());
                }

                final File templateProp = new File(tmpltPath + "/template.properties");
                if (!templateProp.exists()) {
                    templateProp.createNewFile();
                }

                String templateContent = "filename=" + command.getUniqueName() + ".qcow2" + System.getProperty("line.separator");

                final DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
                final Date date = new Date();
                templateContent += "snapshot.name=" + dateFormat.format(date) + System.getProperty("line.separator");

                try(FileOutputStream templFo = new FileOutputStream(templateProp);) {
                    templFo.write(templateContent.getBytes("UTF-8"));
                    templFo.flush();
                }catch(final IOException ex)
                {
                    s_logger.error("CreatePrivateTemplateAnswer:Exception:"+ex.getMessage());
                }

            }

            final Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, storage);
            final Processor qcow2Processor = new QCOW2Processor();

            qcow2Processor.configure("QCOW2 Processor", params);

            final FormatInfo info = qcow2Processor.process(tmpltPath, null, command.getUniqueName());

            final TemplateLocation loc = new TemplateLocation(storage, tmpltPath);
            loc.create(1, true, command.getUniqueName());
            loc.addFormat(info);
            loc.save();

            return new CreatePrivateTemplateAnswer(command, true, null, templateInstallFolder + command.getUniqueName() + ".qcow2", info.virtualSize, info.size,
                    command.getUniqueName(), ImageFormat.QCOW2);
        } catch (final InternalErrorException e) {
            return new CreatePrivateTemplateAnswer(command, false, e.toString());
        } catch (final IOException e) {
            return new CreatePrivateTemplateAnswer(command, false, e.toString());
        } catch (final ConfigurationException e) {
            return new CreatePrivateTemplateAnswer(command, false, e.toString());
        } catch (final CloudRuntimeException e) {
            return new CreatePrivateTemplateAnswer(command, false, e.toString());
        } finally {
            if (secondaryStorage != null) {
                storagePoolMgr.deleteStoragePool(secondaryStorage.getType(), secondaryStorage.getUuid());
            }
        }
    }
}
