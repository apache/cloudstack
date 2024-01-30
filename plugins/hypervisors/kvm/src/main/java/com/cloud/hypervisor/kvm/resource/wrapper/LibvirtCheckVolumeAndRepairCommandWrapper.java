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
import com.cloud.agent.api.storage.CheckVolumeAndRepairCommand;
import com.cloud.agent.api.storage.CheckVolumeAndRepairAnswer;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ResourceWrapper(handles =  CheckVolumeAndRepairCommand.class)
public class LibvirtCheckVolumeAndRepairCommandWrapper extends CommandWrapper<CheckVolumeAndRepairCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCheckVolumeAndRepairCommandWrapper.class);

    @Override
    public Answer execute(CheckVolumeAndRepairCommand command, LibvirtComputingResource serverResource) {
        final String volumeId = command.getPath();
        final String repair = command.getRepair();
        final StorageFilerTO spool = command.getPool();

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool pool = storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());

        final KVMPhysicalDisk vol = pool.getPhysicalDisk(volumeId);
        QemuObject.EncryptFormat encryptFormat = QemuObject.EncryptFormat.enumValue(command.getEncryptFormat());
        byte[] passphrase = command.getPassphrase();
        try {
            String checkVolumeResult = checkVolumeAndRepair(vol, repair, encryptFormat, passphrase, serverResource);
            s_logger.info(String.format("Check Volume result for the volume %s is %s", vol.getName(), checkVolumeResult));
            CheckVolumeAndRepairAnswer answer = new CheckVolumeAndRepairAnswer(command, true, checkVolumeResult);
            answer.setVolumeCheckExecutionResult(checkVolumeResult);

            int leaks = 0;
            if (StringUtils.isNotEmpty(checkVolumeResult) && StringUtils.isNotEmpty(repair) && repair.equals("leaks")) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(checkVolumeResult);
                JsonNode leaksNode = jsonNode.get("leaks");
                if (leaksNode != null) {
                    leaks = jsonNode.asInt();
                }

                if (leaks == 0) {
                    String msg = String.format("no leaks found while checking for the volume %s, so skipping repair", vol.getName());
                    s_logger.info(msg);
                    String jsonStringFormat = String.format("{ \"message\": \"%s\" }", msg);
                    String finalResult = (checkVolumeResult != null ? checkVolumeResult.concat(",") : "") + jsonStringFormat;
                    answer = new CheckVolumeAndRepairAnswer(command, true, finalResult);
                    answer.setVolumeRepairExecutionResult(jsonStringFormat);
                    answer.setVolumeCheckExecutionResult(checkVolumeResult);

                    return answer;
                }
            }

            if (StringUtils.isNotEmpty(repair)) {
                String repairVolumeResult = checkVolumeAndRepair(vol, repair, encryptFormat, passphrase, serverResource);
                String finalResult = (checkVolumeResult != null ? checkVolumeResult.concat(",") : "") + repairVolumeResult;
                s_logger.info(String.format("Repair Volume result for the volume %s is %s", vol.getName(), repairVolumeResult));

                answer = new CheckVolumeAndRepairAnswer(command, true, finalResult);
                answer.setVolumeRepairExecutionResult(repairVolumeResult);
                answer.setVolumeCheckExecutionResult(checkVolumeResult);
            }
            return answer;
        } catch (Exception e) {
            return new CheckVolumeAndRepairAnswer(command, false, e.toString());
        } finally {
            if (passphrase != null) {
                Arrays.fill(passphrase, (byte) 0);
            }
        }
    }

    protected String checkVolumeAndRepair(final KVMPhysicalDisk vol, final String repair, final QemuObject.EncryptFormat encryptFormat, byte[] passphrase, final LibvirtComputingResource libvirtComputingResource) throws CloudRuntimeException {
        List<QemuObject> passphraseObjects = new ArrayList<>();
        QemuImageOptions imgOptions = null;
        if (ArrayUtils.isEmpty(passphrase)) {
            passphrase = null;
        }
        try (KeyFile keyFile = new KeyFile(passphrase)) {
            if (passphrase != null) {
                passphraseObjects.add(
                        QemuObject.prepareSecretForQemuImg(vol.getFormat(), encryptFormat, keyFile.toString(), "sec0", null)
                );
                imgOptions = new QemuImageOptions(vol.getFormat(), vol.getPath(),"sec0");
            }
            QemuImg q = new QemuImg(libvirtComputingResource.getCmdsTimeout());
            QemuImgFile file = new QemuImgFile(vol.getPath());
            return q.checkAndRepair(file, imgOptions, passphraseObjects, repair);
        } catch (QemuImgException | LibvirtException ex) {
            throw new CloudRuntimeException("Failed to run qemu-img for check volume", ex);
        } catch (IOException ex) {
            throw new CloudRuntimeException("Failed to create keyfile for encrypted volume for check volume operation", ex);
        }
    }
}
