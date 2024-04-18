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
import com.cloud.agent.api.storage.CheckAndRepairVolumeCommand;
import com.cloud.agent.api.storage.CheckAndRepairVolumeAnswer;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.cloudstack.utils.qemu.QemuObject.EncryptFormat;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ResourceWrapper(handles =  CheckAndRepairVolumeCommand.class)
public class LibvirtCheckAndRepairVolumeCommandWrapper extends CommandWrapper<CheckAndRepairVolumeCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(CheckAndRepairVolumeCommand command, LibvirtComputingResource serverResource) {
        final String volumeId = command.getPath();
        final String repair = command.getRepair();
        final StorageFilerTO spool = command.getPool();

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool pool = storagePoolMgr.getStoragePool(spool.getType(), spool.getUuid());
        final KVMPhysicalDisk vol = pool.getPhysicalDisk(volumeId);
        byte[] passphrase = command.getPassphrase();

        try {
            CheckAndRepairVolumeAnswer answer = null;
            String checkVolumeResult = null;
            if (QemuImg.PhysicalDiskFormat.RAW.equals(vol.getFormat())) {
                checkVolumeResult = "Volume format RAW is not supported to check and repair";
                String jsonStringFormat = String.format("{ \"message\": \"%s\" }", checkVolumeResult);
                answer = new CheckAndRepairVolumeAnswer(command, true, checkVolumeResult);
                answer.setVolumeCheckExecutionResult(jsonStringFormat);

                return answer;
            } else {
                answer = checkVolume(vol, command, serverResource);
                checkVolumeResult =  answer.getVolumeCheckExecutionResult();
            }

            CheckAndRepairVolumeAnswer resultAnswer = checkIfRepairLeaksIsRequired(command, checkVolumeResult, vol.getName());
            // resultAnswer is not null when repair is not required, so return from here
            if (resultAnswer != null) {
                return resultAnswer;
            }

            if (StringUtils.isNotEmpty(repair)) {
                answer = repairVolume(vol, command, serverResource, checkVolumeResult);
            }

            return answer;
        } catch (Exception e) {
            return new CheckAndRepairVolumeAnswer(command, false, e.toString());
        } finally {
            if (passphrase != null) {
                Arrays.fill(passphrase, (byte) 0);
            }
        }
    }

    private CheckAndRepairVolumeAnswer checkVolume(KVMPhysicalDisk vol, CheckAndRepairVolumeCommand command, LibvirtComputingResource serverResource) {
        EncryptFormat encryptFormat = EncryptFormat.enumValue(command.getEncryptFormat());
        byte[] passphrase = command.getPassphrase();
        String checkVolumeResult = checkAndRepairVolume(vol, null, encryptFormat, passphrase, serverResource);
        logger.info(String.format("Check Volume result for the volume %s is %s", vol.getName(), checkVolumeResult));
        CheckAndRepairVolumeAnswer answer = new CheckAndRepairVolumeAnswer(command, true, checkVolumeResult);
        answer.setVolumeCheckExecutionResult(checkVolumeResult);

        return answer;
    }

    private CheckAndRepairVolumeAnswer repairVolume(KVMPhysicalDisk vol, CheckAndRepairVolumeCommand command, LibvirtComputingResource serverResource, String checkVolumeResult) {
        EncryptFormat encryptFormat = EncryptFormat.enumValue(command.getEncryptFormat());
        byte[] passphrase = command.getPassphrase();
        final String repair = command.getRepair();

        String repairVolumeResult = checkAndRepairVolume(vol, repair, encryptFormat, passphrase, serverResource);
        String finalResult = (checkVolumeResult != null ? checkVolumeResult.concat(",") : "") + repairVolumeResult;
        logger.info(String.format("Repair Volume result for the volume %s is %s", vol.getName(), repairVolumeResult));

        CheckAndRepairVolumeAnswer answer = new CheckAndRepairVolumeAnswer(command, true, finalResult);
        answer.setVolumeRepairExecutionResult(repairVolumeResult);
        answer.setVolumeCheckExecutionResult(checkVolumeResult);

        return answer;
    }

    private CheckAndRepairVolumeAnswer checkIfRepairLeaksIsRequired(CheckAndRepairVolumeCommand command, String checkVolumeResult, String volumeName) {
        final String repair = command.getRepair();
        int leaks = 0;
        if (StringUtils.isNotEmpty(checkVolumeResult) && StringUtils.isNotEmpty(repair) && repair.equals("leaks")) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(checkVolumeResult);
            } catch (JsonProcessingException e) {
                String msg = String.format("Error processing response %s during check volume %s", checkVolumeResult, e.getMessage());
                logger.info(msg);

                return skipRepairVolumeCommand(command, checkVolumeResult, msg);
            }
            JsonNode leaksNode = jsonNode.get("leaks");
            if (leaksNode != null) {
                leaks = leaksNode.asInt();
            }

            if (leaks == 0) {
                String msg = String.format("No leaks found while checking for the volume %s, so skipping repair", volumeName);
                return skipRepairVolumeCommand(command, checkVolumeResult, msg);
            }
        }

        return null;
    }

    private CheckAndRepairVolumeAnswer skipRepairVolumeCommand(CheckAndRepairVolumeCommand command, String checkVolumeResult, String msg) {
        logger.info(msg);
        String jsonStringFormat = String.format("{ \"message\": \"%s\" }", msg);
        String finalResult = (checkVolumeResult != null ? checkVolumeResult.concat(",") : "") + jsonStringFormat;
        CheckAndRepairVolumeAnswer answer = new CheckAndRepairVolumeAnswer(command, true, finalResult);
        answer.setVolumeRepairExecutionResult(jsonStringFormat);
        answer.setVolumeCheckExecutionResult(checkVolumeResult);

        return answer;
    }

    protected String checkAndRepairVolume(final KVMPhysicalDisk vol, final String repair, final EncryptFormat encryptFormat, byte[] passphrase, final LibvirtComputingResource libvirtComputingResource) throws CloudRuntimeException {
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
