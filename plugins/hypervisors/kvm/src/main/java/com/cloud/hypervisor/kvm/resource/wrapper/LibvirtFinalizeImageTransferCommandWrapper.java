//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Map;

import org.apache.cloudstack.backup.FinalizeImageTransferCommand;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.ImageServerControlSocket;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = FinalizeImageTransferCommand.class)
public class LibvirtFinalizeImageTransferCommandWrapper extends CommandWrapper<FinalizeImageTransferCommand, Answer, LibvirtComputingResource> {
    /** what qemu-img info calls the external data file of a qcow2 image */
    private static final String QEMU_IMG_INFO_DATA_FILE = "data_file";

    protected Logger logger = LogManager.getLogger(getClass());
    private void resetService(String unitName) {
        Script resetScript = new Script("/bin/bash", logger);
        resetScript.add("-c");
        resetScript.add(String.format("systemctl reset-failed %s || true", unitName));
        resetScript.execute();
    }

    private boolean stopImageServer(int imageServerPort, LibvirtComputingResource resource) {
        String unitName = resource.IMAGE_SERVER_SYSTEMD_UNIT_NAME;

        Script checkScript = new Script("/bin/bash", logger);
        checkScript.add("-c");
        checkScript.add(String.format("systemctl is-active --quiet %s", unitName));
        String checkResult = checkScript.execute();
        if (checkResult != null) {
            logger.info("Image server not running, resetting failed state");
            resetService(unitName);
            removeFirewallRule(imageServerPort);
            return true;
        }

        Script stopScript = new Script("/bin/bash", logger);
        stopScript.add("-c");
        stopScript.add(String.format("systemctl stop %s", unitName));
        stopScript.execute();
        resetService(unitName);
        logger.info("Image server {} stopped", unitName);

        removeFirewallRule(imageServerPort);

        return true;
    }

    private void removeFirewallRule(int port) {
        String rule = String.format("-p tcp -m state --state NEW -m tcp --dport %d -j ACCEPT", port);
        Script removeScript = new Script("/bin/bash", logger);
        removeScript.add("-c");
        removeScript.add(String.format("iptables -D INPUT %s || true", rule));
        String result = removeScript.execute();
        if (result != null && !result.isEmpty() && !result.contains("iptables: Bad rule")) {
            logger.debug("Firewall rule removal result for port {}: {}", port, result);
        } else {
            logger.info("Firewall rule removed for port {} (or did not exist)", port);
        }
    }

    public Answer execute(FinalizeImageTransferCommand cmd, LibvirtComputingResource resource) {
        final String transferId = cmd.getTransferId();
        final int imageServerPort = LibvirtComputingResource.IMAGE_SERVER_DEFAULT_PORT;
        if (StringUtils.isBlank(transferId)) {
            return new Answer(cmd, false, "transferId is empty.");
        }

        int activeTransfers = ImageServerControlSocket.unregisterTransfer(transferId);
        LibvirtCreateImageTransferCommandWrapper.deleteExtentsFile(transferId);
        String details = "Image transfer finalized.";
        if (activeTransfers < 0) {
            logger.warn("Could not reach image server to unregister transfer {}; assuming server is down", transferId);
            stopImageServer(imageServerPort, resource);
            details = "Image transfer finalized (server unreachable, forced stop).";
        } else if (activeTransfers == 0) {
            stopImageServer(imageServerPort, resource);
        }

        if (cmd.getConvertSource() != null) {
            // the image server has let go of the uploaded image by now
            String error = convertUploadedImage(cmd, resource);
            if (error != null) {
                return new Answer(cmd, false, error);
            }
        }
        return new Answer(cmd, true, details);
    }

    /**
     * Converts the qcow2 image uploaded to a temporary device into the volume, see
     * {@code PrimaryDataStoreDriver.createUploadStaging}.
     * @return null if done, else the error
     */
    protected String convertUploadedImage(FinalizeImageTransferCommand cmd, LibvirtComputingResource resource) {
        final String source = cmd.getConvertSource();
        try {
            KVMStoragePool pool = resource.getStoragePoolMgr().getStoragePool(cmd.getConvertPoolType(), cmd.getConvertPoolUuid());
            KVMPhysicalDisk volume = pool.getPhysicalDisk(cmd.getConvertVolumePath());
            long timeout = cmd.getWait() > 0 ? cmd.getWait() * 1000L : resource.getCmdsTimeout();
            QemuImg qemu = createQemuImg(timeout, pool.isVolumeZeroInitialized(cmd.getConvertVolumePath()));
            QemuImgFile image = new QemuImgFile(source, QemuImg.PhysicalDiskFormat.QCOW2);
            // the image comes from outside, it must not pull other files of the host into the volume
            Map<String, String> info = qemu.info(image);
            if (!"qcow2".equals(info.get(QemuImg.FILE_FORMAT)) || info.containsKey(QemuImg.BACKING_FILE)
                    || info.containsKey(QEMU_IMG_INFO_DATA_FILE)) {
                return String.format("The image uploaded to %s is no qcow2 image without backing and data file", source);
            }
            qemu.convert(image, new QemuImgFile(volume.getPath(), QemuImg.PhysicalDiskFormat.RAW), true);
            logger.info("Converted the image uploaded to {} into {}", source, volume.getPath());
            return null;
        } catch (QemuImgException | LibvirtException | CloudRuntimeException e) {
            logger.error("Failed to convert the image uploaded to {} into volume {}", source, cmd.getConvertVolumePath(), e);
            return "Failed to convert the uploaded image into the volume: " + e.getMessage();
        }
    }

    protected QemuImg createQemuImg(long timeout, boolean targetIsZero) throws LibvirtException {
        return new QemuImg(timeout, targetIsZero, true);
    }
}
