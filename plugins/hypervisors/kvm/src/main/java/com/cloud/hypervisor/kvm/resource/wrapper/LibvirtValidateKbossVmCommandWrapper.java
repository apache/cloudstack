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
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.json.JsonSanitizer;
import org.apache.cloudstack.backup.ValidateKbossVmAnswer;
import org.apache.cloudstack.backup.ValidateKbossVmCommand;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@ResourceWrapper(handles =  ValidateKbossVmCommand.class)
public class LibvirtValidateKbossVmCommandWrapper extends CommandWrapper<ValidateKbossVmCommand, Answer, LibvirtComputingResource> {

    private static final String SCREENSHOT_COMMAND = "virsh screenshot --domain %s --file %s";
    private static final String GUEST_SYNC_COMMAND = "{\"execute\": \"guest-sync\", \"arguments\":{\"id\":%s}}";
    private static final String GUEST_EXEC_COMMAND = "{\"execute\": \"guest-exec\", \"arguments\":{\"path\":\"%s\",\"arg\":%s,\"capture-output\":true}}";
    private static final String GUEST_EXEC_STATUS_COMMAND = "{\"execute\": \"guest-exec-status\", \"arguments\":{\"pid\":%s}}";

    @Override
    public Answer execute(ValidateKbossVmCommand command, LibvirtComputingResource serverResource) {
        VirtualMachineTO vmTo = command.getVm();
        KVMStoragePool secondaryStorage = null;
        KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        try {
            Domain vm = serverResource.getDomain(serverResource.getLibvirtUtilitiesHelper().getConnection(), vmTo.getName());
            secondaryStorage = storagePoolMgr.getStoragePoolByURI(command.getBackupDeltaTO().getDataStore().getUrl());
            logger.info("Validating VM [{}].", vm.getName());
            boolean bootValidated = waitForBoot(command, vm);
            String screenshotPath = takeScreenshot(command, vm, secondaryStorage, serverResource);
            String scriptResult = runScript(command, vm);
            return new  ValidateKbossVmAnswer(command, bootValidated, screenshotPath, scriptResult);
        } catch (LibvirtException e) {
            logger.error("Received libvirt exception while trying to validate VM [{}].", vmTo.getName(), e);
            return new Answer(command, e);
        } finally {
            if (secondaryStorage != null) {
                storagePoolMgr.deleteStoragePool(secondaryStorage.getType(), secondaryStorage.getUuid());
            }
        }
    }

    private boolean waitForBoot(ValidateKbossVmCommand cmd, Domain vm) throws LibvirtException {
        if (!cmd.isWaitForBoot()) {
            return false;
        }
        Random random = new Random();
        Integer bootTimeout = cmd.getBootTimeout();
        logger.debug("Waiting for validation VM [{}] to boot. We will wait for [{}] seconds at most.", vm.getName(), bootTimeout);
        while (bootTimeout > 0) {
            bootTimeout -= 5;
            int randomInt = random.nextInt();
            try {
                String result = vm.qemuAgentCommand(String.format(GUEST_SYNC_COMMAND, randomInt), 1, 0);
                if (result.contains(String.valueOf(randomInt))) {
                    logger.info("Validation VM [{}] has booted.", vm.getName());
                    return true;
                }
            } catch (LibvirtException ex) {
                if (!ex.getMessage().contains(LibvirtComputingResource.AGENT_IS_NOT_CONNECTED)) {
                    logger.error("Got an unexpected Libvirt Exception, giving up on validating VM [{}].", vm.getName(), ex);
                    throw ex;
                }
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                logger.debug("Got interrupted while waiting for VM [{}] to boot. Ignoring.", vm.getName());
            }
        }
        logger.debug("Boot wait timed out for VM [{}].", vm.getName());
        return false;
    }

    private String takeScreenshot(ValidateKbossVmCommand command, Domain vm, KVMStoragePool secondaryStorage, LibvirtComputingResource resource) throws LibvirtException {
        if (!command.isTakeScreenshot()) {
            return null;
        }
        String vmName = vm.getName();
        try {
            logger.info("Waiting [{}] seconds to take screenshot of validation VM [{}].", command.getScreenshotWait(), vm.getName());
            Thread.sleep(command.getScreenshotWait() * 1000L);
        } catch (InterruptedException e) {
            logger.debug("Got interrupted while waiting to take screenshot of validation VM [{}].", vm.getName());
        }
        logger.info("Taking screenshot of VM [{}].", vmName);
        String ssPath = secondaryStorage.getLocalPathFor(command.getBackupDeltaTO().getPath()) + String.format("-screenshot-%s", DateUtil.getDateInSystemTimeZone());
        if (Script.runSimpleBashScript(String.format(SCREENSHOT_COMMAND, vmName, ssPath)) == null) {
            throw new CloudRuntimeException(String.format("Got an unexpected error while trying to screenshot validation VM [%s].", vmName));
        }
        try {
            File inputFile = new File(ssPath);
            String pngPath = ssPath + ".png";
            File outputFile = new File(pngPath);
            BufferedImage image = ImageIO.read(inputFile);

            String warnMessage = String.format("Unable to convert screenshot at [%s] to png. Leaving it as is.", ssPath);
            if (image == null) {
                logger.warn(warnMessage);
                return ssPath.substring(ssPath.indexOf("backups"));
            }
            boolean result = ImageIO.write(image, "png", outputFile);

            if (result) {
                logger.debug("Successfully converted image at [{}] to PNG at [{}].", ssPath, pngPath);
                Files.deleteIfExists(Path.of(ssPath));
                return pngPath.substring(ssPath.indexOf("backups"));
            } else {
                logger.warn(warnMessage);
                return ssPath.substring(ssPath.indexOf("backups"));
            }
        } catch (IOException ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    private String runScript(ValidateKbossVmCommand command, Domain vm) throws LibvirtException {
        if (!command.isExecuteScript()) {
            return null;
        }
        String script = command.getScriptToExecute();
        if (script == null) {
            return null;
        }
        String arguments = command.getScriptArguments();
        if (arguments == null) {
            arguments = "[]";
        } else {
            arguments = "[\"" + arguments.replace(",", "\",\"") + "\"]";
        }
        logger.debug("Running validation script [{}] with arguments [{}] on dummy validation VM [{}].", script, arguments, vm.getName());
        String guestCommand = String.format(GUEST_EXEC_COMMAND, script, arguments);
        String sanitizedGuestCommand = JsonSanitizer.sanitize(guestCommand);
        String execResult;
        try {
            execResult = vm.qemuAgentCommand(sanitizedGuestCommand, command.getScriptTimeout(), 0);
        } catch (LibvirtException ex) {
            return ex.getMessage();
        }
        JsonObject root = new JsonParser().parse(execResult).getAsJsonObject();
        JsonObject ret = root.getAsJsonObject("return");
        String pid = ret.get("pid").getAsString();

        String expectedResult = command.getExpectedResult();
        Integer timeout = command.getScriptTimeout();
        while (timeout > 0) {
            timeout -= 5;
            try {
                String statusResult = vm.qemuAgentCommand(String.format(GUEST_EXEC_STATUS_COMMAND, pid), 1, 0);
                root = new JsonParser().parse(statusResult).getAsJsonObject();
                ret = root.getAsJsonObject("return");

                boolean exited = ret.get("exited").getAsBoolean();
                if (exited) {
                    int exitCode = ret.get("exitcode").getAsInt();
                    String outData64Coded = ret.get("out-data").getAsString();
                    logger.debug("Script [{}] with arguments [{}] that ran on validation VM [{}] exited with code [{}] and had output [{}].", script, arguments, vm.getName(),
                            exitCode, outData64Coded);
                    if (expectedResult.equals("0") && exitCode == 0) {
                        return null;
                    }
                    if (outData64Coded.equals(expectedResult)) {
                        return null;
                    }
                    return outData64Coded;
                }
            } catch (LibvirtException ex) {
                logger.error("Caught unexpected Libvirt exception while waiting for validation script result of VM [{}]. Will try again later.", vm.getName(), ex);
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                logger.debug("Got interrupted while waiting for execution of validation script for VM [{}]. Ignoring.", vm.getName());
            }
        }
        logger.error("Script [{}] that was executed on validation VM [{}] has timed out, giving up.", script, vm.getName());
        return "Timeout";
    }
}
