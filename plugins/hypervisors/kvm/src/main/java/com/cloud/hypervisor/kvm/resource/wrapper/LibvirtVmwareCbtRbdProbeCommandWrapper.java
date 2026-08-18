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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtRbdProbeCommand;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = VmwareCbtRbdProbeCommand.class)
public class LibvirtVmwareCbtRbdProbeCommandWrapper extends CommandWrapper<VmwareCbtRbdProbeCommand, Answer, LibvirtComputingResource> {

    static final String PROBE_IMAGE_PREFIX = "cloudstack-cbt-probe-";
    static final String BLOCK_DEVICE_PROBE_PREFIX = "cbt-probe-";
    private static final Pattern PROBE_IMAGE_NAME_PATTERN = Pattern.compile("^" + PROBE_IMAGE_PREFIX +
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern BLOCK_DEVICE_PROBE_NAME_PATTERN = Pattern.compile("^" + BLOCK_DEVICE_PROBE_PREFIX +
            "[0-9a-fA-F]{8}$");
    private static final long PROBE_SIZE_BYTES = 4L * 1024L * 1024L;

    @Override
    public Answer execute(VmwareCbtRbdProbeCommand cmd, LibvirtComputingResource serverResource) {
        String probeImageName = StringUtils.trimToNull(cmd.getProbeImageName());
        if (cmd.getTargetStorageType() == VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE) {
            return executeBlockDeviceProbe(cmd, probeImageName, serverResource);
        }
        try {
            validateCommand(cmd, probeImageName);
            KVMStoragePool targetPool = getTargetStoragePool(cmd, serverResource.getStoragePoolMgr());
            String qemuRbdPath = KVMPhysicalDisk.RBDStringBuilder(targetPool, getRbdImagePath(targetPool, probeImageName));

            Exception probeFailure = null;
            try {
                executeProbeCommand(buildCreateCommand(qemuRbdPath), getTimeout(cmd), "create", targetPool, probeImageName);
                executeProbeCommand(buildWriteReadCommand(qemuRbdPath), getTimeout(cmd), "write/read", targetPool, probeImageName);
            } catch (Exception e) {
                probeFailure = e;
                throw e;
            } finally {
                try {
                    cleanupProbeImage(targetPool, probeImageName);
                } catch (RuntimeException e) {
                    if (probeFailure == null) {
                        throw e;
                    }
                    logger.warn("Unable to clean up RBD probe image {} after a failed probe: {}", probeImageName,
                            StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
                }
            }

            String message = String.format("Selected KVM host can create, write, read and delete temporary RBD image %s in storage pool %s.",
                    probeImageName, cmd.getDestinationStoragePoolUuid());
            logger.info(message);
            return new Answer(cmd, true, message);
        } catch (Exception e) {
            String message = String.format("Cannot verify VMware CBT access to selected RBD primary storage pool %s on host %s: %s",
                    cmd.getDestinationStoragePoolUuid(), serverResource.getPrivateIp(),
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.warn(message, e);
            return new Answer(cmd, false, message);
        }
    }

    private Answer executeBlockDeviceProbe(VmwareCbtRbdProbeCommand cmd, String probeVolumeName,
                                           LibvirtComputingResource serverResource) {
        try {
            validateBlockDeviceCommand(cmd, probeVolumeName);
            KVMStoragePool targetPool = getBlockDeviceTargetStoragePool(cmd, serverResource.getStoragePoolMgr());

            Exception probeFailure = null;
            try {
                KVMPhysicalDisk probeDisk = targetPool.createPhysicalDisk(probeVolumeName,
                        org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat.RAW,
                        Storage.ProvisioningType.THIN, PROBE_SIZE_BYTES, null);
                String devicePath = probeDisk == null ? null : probeDisk.getPath();
                if (StringUtils.isBlank(devicePath)) {
                    throw new IllegalStateException(String.format("could not resolve a local device path for probe volume %s", probeVolumeName));
                }
                executeProbeCommand(buildWriteReadCommand(devicePath), getTimeout(cmd), "write/read", targetPool, probeVolumeName);
            } catch (Exception e) {
                probeFailure = e;
                throw e;
            } finally {
                try {
                    cleanupProbeImage(targetPool, probeVolumeName);
                } catch (RuntimeException e) {
                    if (probeFailure == null) {
                        throw e;
                    }
                    logger.warn("Unable to clean up block device probe volume {} after a failed probe: {}", probeVolumeName,
                            StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
                }
            }

            String message = String.format("Selected KVM host can create, write, read and delete temporary block device volume %s in storage pool %s.",
                    probeVolumeName, cmd.getDestinationStoragePoolUuid());
            logger.info(message);
            return new Answer(cmd, true, message);
        } catch (Exception e) {
            String message = String.format("Cannot verify VMware CBT access to selected block device primary storage pool %s on host %s: %s",
                    cmd.getDestinationStoragePoolUuid(), serverResource.getPrivateIp(),
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            logger.warn(message, e);
            return new Answer(cmd, false, message);
        }
    }

    private void validateBlockDeviceCommand(VmwareCbtRbdProbeCommand cmd, String probeVolumeName) {
        if (cmd.getDestinationStoragePoolType() != Storage.StoragePoolType.Linstor) {
            throw new IllegalArgumentException("VMware CBT block device probe requires a Linstor destination storage pool");
        }
        if (StringUtils.isBlank(cmd.getDestinationStoragePoolUuid())) {
            throw new IllegalArgumentException("destination storage pool UUID is required");
        }
        if (StringUtils.isBlank(probeVolumeName) || !BLOCK_DEVICE_PROBE_NAME_PATTERN.matcher(probeVolumeName).matches()) {
            throw new IllegalArgumentException(String.format("probe volume name must match %s<8 hex characters>", BLOCK_DEVICE_PROBE_PREFIX));
        }
    }

    private KVMStoragePool getBlockDeviceTargetStoragePool(VmwareCbtRbdProbeCommand cmd, KVMStoragePoolManager storagePoolMgr) {
        KVMStoragePool targetPool = storagePoolMgr.getStoragePool(cmd.getDestinationStoragePoolType(), cmd.getDestinationStoragePoolUuid());
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.Linstor) {
            throw new IllegalArgumentException(String.format("selected Linstor storage pool %s is not available on this KVM host",
                    cmd.getDestinationStoragePoolUuid()));
        }
        return targetPool;
    }

    private void validateCommand(VmwareCbtRbdProbeCommand cmd, String probeImageName) {
        if (cmd.getDestinationStoragePoolType() != Storage.StoragePoolType.RBD) {
            throw new IllegalArgumentException("VMware CBT RBD probe requires an RBD destination storage pool");
        }
        if (StringUtils.isBlank(cmd.getDestinationStoragePoolUuid())) {
            throw new IllegalArgumentException("destination RBD storage pool UUID is required");
        }
        if (StringUtils.isBlank(probeImageName) || !PROBE_IMAGE_NAME_PATTERN.matcher(probeImageName).matches()) {
            throw new IllegalArgumentException(String.format("probe image name must match %s<uuid>", PROBE_IMAGE_PREFIX));
        }
    }

    private KVMStoragePool getTargetStoragePool(VmwareCbtRbdProbeCommand cmd, KVMStoragePoolManager storagePoolMgr) {
        KVMStoragePool targetPool = storagePoolMgr.getStoragePool(cmd.getDestinationStoragePoolType(), cmd.getDestinationStoragePoolUuid());
        if (targetPool == null || targetPool.getType() != Storage.StoragePoolType.RBD) {
            throw new IllegalArgumentException(String.format("selected RBD storage pool %s is not available on this KVM host",
                    cmd.getDestinationStoragePoolUuid()));
        }
        return targetPool;
    }

    private String getRbdImagePath(KVMStoragePool targetPool, String imageName) {
        return String.format("%s/%s", StringUtils.removeEnd(targetPool.getSourceDir(), "/"), imageName);
    }

    private String buildCreateCommand(String qemuRbdPath) {
        return String.format("qemu-img create -f raw %s %s", shellQuote(qemuRbdPath), PROBE_SIZE_BYTES);
    }

    private String buildWriteReadCommand(String qemuRbdPath) {
        return String.format("qemu-io -f raw -c %s -c %s %s",
                shellQuote("write -P 0x5a 0 4k"), shellQuote("read -P 0x5a 0 4k"), shellQuote(qemuRbdPath));
    }

    protected void executeProbeCommand(String command, long timeout, String operation, KVMStoragePool targetPool,
                                       String probeImageName) {
        int exitValue = runBashCommand(command, timeout);
        if (exitValue != 0) {
            throw new IllegalStateException(String.format("qemu RBD probe %s failed with exit code %s. Verify CloudStack RBD primary-storage configuration, Ceph monitor connectivity, qemu RBD block-driver support, librados/librbd client libraries, Java RADOS/RBD bindings and Ceph authentication for pool %s.",
                    operation, exitValue, targetPool.getUuid()));
        }
    }

    protected int runBashCommand(String command, long timeout) {
        int boundedTimeout = (int)Math.min(Integer.MAX_VALUE, Math.max(0L, timeout));
        return Script.runSimpleBashScriptForExitValue(command, boundedTimeout, true);
    }

    protected void cleanupProbeImage(KVMStoragePool targetPool, String probeImageName) {
        try {
            targetPool.deletePhysicalDisk(probeImageName, Storage.ImageFormat.RAW);
        } catch (RuntimeException e) {
            throw new IllegalStateException(String.format("RBD probe cleanup failed for temporary image %s. Verify Java RADOS/RBD bindings and Ceph authentication for pool %s. Manual cleanup may be required.",
                    probeImageName, targetPool.getUuid()), e);
        }
    }

    private long getTimeout(VmwareCbtRbdProbeCommand cmd) {
        return Math.max(1L, cmd.getWait()) * 1000L;
    }

    private String shellQuote(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\"'\"'") + "'";
    }
}
