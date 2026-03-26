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

import com.amazonaws.util.CollectionUtils;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.TakeBackupCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ResourceWrapper(handles = TakeBackupCommand.class)
public class LibvirtTakeBackupCommandWrapper extends CommandWrapper<TakeBackupCommand, Answer, LibvirtComputingResource> {
    private static final Integer EXIT_CLEANUP_FAILED = 20;
    @Override
    public Answer execute(TakeBackupCommand command, LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final String backupPath = command.getBackupPath();
        final String backupRepoType = command.getBackupRepoType();
        final String backupRepoAddress = command.getBackupRepoAddress();
        final String mountOptions = command.getMountOptions();
        List<PrimaryDataStoreTO> volumePools = command.getVolumePools();
        final List<String> volumePaths = command.getVolumePaths();
        KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        List<String> diskPaths = new ArrayList<>();
        if (Objects.nonNull(volumePaths)) {
            for (int idx = 0; idx < volumePaths.size(); idx++) {
                PrimaryDataStoreTO volumePool = volumePools.get(idx);
                String volumePath = volumePaths.get(idx);
                if (volumePool.getPoolType() != Storage.StoragePoolType.RBD) {
                    diskPaths.add(volumePath);
                } else {
                    KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
                    String rbdDestVolumeFile = KVMPhysicalDisk.RBDStringBuilder(volumeStoragePool, volumePath);
                    diskPaths.add(rbdDestVolumeFile);
                }
            }
        }

        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(libvirtComputingResource.getNasBackupPath());
        cmdArgs.add("-o"); cmdArgs.add("backup");
        cmdArgs.add("-v"); cmdArgs.add(vmName);
        cmdArgs.add("-t"); cmdArgs.add(backupRepoType);
        cmdArgs.add("-s"); cmdArgs.add(backupRepoAddress);
        cmdArgs.add("-m"); cmdArgs.add(Objects.nonNull(mountOptions) ? mountOptions : "");
        cmdArgs.add("-p"); cmdArgs.add(backupPath);
        cmdArgs.add("-q"); cmdArgs.add(command.getQuiesce() != null && command.getQuiesce() ? "true" : "false");
        cmdArgs.add("-d"); cmdArgs.add(diskPaths.isEmpty() ? "" : String.join(",", diskPaths));

        // Append optional enhancement flags from management server config
        File passphraseFile = null;
        Map<String, String> details = command.getDetails();
        if (details != null) {
            if ("true".equals(details.get("compression"))) {
                cmdArgs.add("-c");
            }
            if ("true".equals(details.get("encryption"))) {
                String passphrase = details.get("encryption_passphrase");
                if (passphrase != null && !passphrase.isEmpty()) {
                    try {
                        passphraseFile = File.createTempFile("cs-backup-enc-", ".key");
                        passphraseFile.deleteOnExit();
                        try (FileWriter fw = new FileWriter(passphraseFile)) {
                            fw.write(passphrase);
                        }
                        cmdArgs.add("-e"); cmdArgs.add(passphraseFile.getAbsolutePath());
                    } catch (IOException e) {
                        logger.error("Failed to create encryption passphrase file", e);
                        return new BackupAnswer(command, false, "Failed to create encryption passphrase file: " + e.getMessage());
                    }
                }
            }
            String bwLimit = details.get("bandwidth_limit");
            if (bwLimit != null && !"0".equals(bwLimit)) {
                cmdArgs.add("-b"); cmdArgs.add(bwLimit);
            }
            if ("true".equals(details.get("integrity_check"))) {
                cmdArgs.add("--verify");
            }
        }

        List<String[]> commands = new ArrayList<>();
        commands.add(cmdArgs.toArray(new String[0]));

        Pair<Integer, String> result = Script.executePipedCommands(commands, libvirtComputingResource.getCmdsTimeout());

        // Clean up passphrase file after backup completes
        if (passphraseFile != null && passphraseFile.exists()) {
            passphraseFile.delete();
        }

        if (result.first() != 0) {
            logger.debug("Failed to take VM backup: " + result.second());
            BackupAnswer answer = new BackupAnswer(command, false, result.second().trim());
            if (result.first() == EXIT_CLEANUP_FAILED) {
                logger.debug("Backup cleanup failed");
                answer.setNeedsCleanup(true);
            }
            return answer;
        }

        long backupSize = 0L;
        if (CollectionUtils.isNullOrEmpty(diskPaths)) {
            List<String> outputLines = Arrays.asList(result.second().trim().split("\n"));
            if (!outputLines.isEmpty()) {
                backupSize = Long.parseLong(outputLines.get(outputLines.size() - 1).trim());
            }
        } else {
            String[] outputLines = result.second().trim().split("\n");
            for(String line : outputLines) {
                backupSize = backupSize + Long.parseLong(line.split(" ")[0].trim());
            }
        }

        BackupAnswer answer = new BackupAnswer(command, true, result.second().trim());
        answer.setSize(backupSize);
        return answer;
    }
}
