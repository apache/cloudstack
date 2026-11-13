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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.backup.StartBackupAnswer;
import org.apache.cloudstack.backup.StartBackupCommand;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.libvirt.Domain;
import org.libvirt.DomainJobInfo;
import org.libvirt.LibvirtException;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StartBackupCommand.class)
public class LibvirtStartBackupCommandWrapper extends CommandWrapper<StartBackupCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    /** virDomainJobType VIR_DOMAIN_JOB_NONE. */
    static final int NO_JOB = 0;

    @Override
    public Answer execute(StartBackupCommand cmd, LibvirtComputingResource resource) {
        if (cmd.isStoppedVM()) {
            return handleStoppedVmBackup(cmd, cmd.getToCheckpointId());
        }
        return handleRunningVmBackup(cmd, resource);
    }

    public Answer handleRunningVmBackup(StartBackupCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        String toCheckpointId = cmd.getToCheckpointId();
        String fromCheckpointId = cmd.getFromCheckpointId();
        Long fromCheckpointCreateTime = cmd.getFromCheckpointCreateTime();
        String socket = cmd.getSocket();

        try {
            if (StringUtils.isNotBlank(fromCheckpointId)) {
                Answer redefineAnswer = ensureFromCheckpointExists(cmd, fromCheckpointId, fromCheckpointCreateTime);
                if (redefineAnswer != null) {
                    return redefineAnswer;
                }
            }

            File dir = new File("/tmp/imagetransfer");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            Map<String, String> diskPathLabelMap = resource.getDiskPathLabelMap(vmName);
            Map<String, String> diskPathScratchFileMap = getScratchFiles(cmd.getDiskPathUuidMap(), diskPathLabelMap);
            String scratchError = prepareScratchFiles(vmName, diskPathScratchFileMap, resource);
            if (scratchError != null) {
                return new StartBackupAnswer(cmd, false, scratchError);
            }

            // Create backup XML
            String backupXml = createBackupXml(cmd, fromCheckpointId, socket, diskPathLabelMap, diskPathScratchFileMap, resource);
            String checkpointXml = createCheckpointXml(toCheckpointId);

            // Write XMLs to temp files
            File backupXmlFile = File.createTempFile("backup-", ".xml");
            File checkpointXmlFile = File.createTempFile("checkpoint-", ".xml");

            try (FileWriter writer = new FileWriter(backupXmlFile)) {
                writer.write(backupXml);
            }
            try (FileWriter writer = new FileWriter(checkpointXmlFile)) {
                writer.write(checkpointXml);
            }

            // Execute virsh backup-begin
            String backupCmd = String.format("virsh backup-begin %s %s --checkpointxml %s",
                vmName, backupXmlFile.getAbsolutePath(), checkpointXmlFile.getAbsolutePath());

            Script script = new Script("/bin/bash");
            script.add("-c");
            script.add(backupCmd);
            String result = script.execute();

            backupXmlFile.delete();
            checkpointXmlFile.delete();

            if (result != null) {
                return new StartBackupAnswer(cmd, false, "Backup begin failed: " + result);
            }

            long checkpointCreateTime = getCheckpointCreateTime();
            return new StartBackupAnswer(cmd, true, "Backup started successfully", checkpointCreateTime);

        } catch (Exception e) {
            return new StartBackupAnswer(cmd, false, "Error starting backup: " + e.getMessage());
        }
    }

    private Answer ensureFromCheckpointExists(StartBackupCommand cmd, String fromCheckpointId, Long fromCheckpointCreateTime) {
        String vmName = cmd.getVmName();
        Script dumpScript = new Script("/bin/bash");
        dumpScript.add("-c");
        dumpScript.add(String.format("virsh checkpoint-dumpxml --domain %s --checkpointname %s --no-domain",
            vmName, fromCheckpointId));
        if (dumpScript.execute() == null) {
            return null;
        }
        if (fromCheckpointCreateTime == null) {
            return new StartBackupAnswer(cmd, false, "From checkpoint create time is null for checkpoint " + fromCheckpointId);
        }

        String redefineXml = createCheckpointXmlForRedefine(fromCheckpointId, fromCheckpointCreateTime);
        File redefineFile;
        try {
            redefineFile = File.createTempFile("checkpoint-redefine-", ".xml");
        } catch (Exception e) {
            return new StartBackupAnswer(cmd, false, "Failed to create temp file for checkpoint redefine: " + e.getMessage());
        }
        try (FileWriter writer = new FileWriter(redefineFile)) {
            writer.write(redefineXml);
        } catch (Exception e) {
            redefineFile.delete();
            return new StartBackupAnswer(cmd, false, "Failed to write checkpoint redefine XML: " + e.getMessage());
        }
        String createCmd = String.format(LibvirtComputingResource.CHECKPOINT_CREATE_COMMAND, vmName, redefineFile.getAbsolutePath());
        Script createScript = new Script("/bin/bash");
        createScript.add("-c");
        createScript.add(createCmd);
        String result = createScript.execute();
        redefineFile.delete();
        if (result != null) {
            return new StartBackupAnswer(cmd, false, "Failed to redefine from-checkpoint " + fromCheckpointId + ": " + result);
        }
        return null;
    }

    private String createCheckpointXmlForRedefine(String checkpointName, Long createTime) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domaincheckpoint>\n");
        xml.append("  <name>").append(checkpointName).append("</name>\n");
        xml.append("  <creationTime>").append(createTime).append("</creationTime>\n");
        xml.append("</domaincheckpoint>");
        return xml.toString();
    }

    private String createBackupXml(StartBackupCommand cmd, String fromCheckpointId, String socket, Map<String, String> diskPathLabelMap,
                                   Map<String, String> diskPathScratchFileMap, LibvirtComputingResource resource) throws LibvirtException {
        StringBuilder xml = new StringBuilder();
        xml.append("<domainbackup mode=\"pull\">\n");

        xml.append(String.format("  <server transport=\"unix\" socket=\"/tmp/imagetransfer/%s.sock\"/>\n", socket));

        xml.append("  <disks>\n");

        Map<String, String> diskPathUuidMap = cmd.getDiskPathUuidMap();
        Map<String, Boolean> diskPathHasFromCheckpointMap = new HashMap<>();
        if (StringUtils.isNotBlank(fromCheckpointId)) {
            Domain vm = null;
            try {
                vm = resource.getDomain(resource.getLibvirtUtilitiesHelper().getConnection(), cmd.getVmName());
                if (vm != null) {
                    diskPathHasFromCheckpointMap = getVmDiskPathHasFromCheckpointMap(vm, fromCheckpointId);
                } else {
                    logger.warn("Failed to get domain for VM [{}] while evaluating export bitmap [{}]. Falling back to full Backup",
                            cmd.getVmName(), fromCheckpointId);
                }
            } finally {
                if (vm != null) {
                   vm.free();
                }
            }
        }

        for (Map.Entry<String, String> entry : diskPathLabelMap.entrySet()) {
            String diskPath = entry.getKey();
            if (!diskPathUuidMap.containsKey(diskPath)) {
                continue;
            }
            String diskName = entry.getValue();
            String export = diskPathUuidMap.get(diskPath);
            String scratchFile = diskPathScratchFileMap.get(diskPath);
            xml.append("    <disk name=\"").append(diskName).append("\" type=\"file\" exportname=\"").append(export);
            if (StringUtils.isNotBlank(fromCheckpointId) && Boolean.TRUE.equals(diskPathHasFromCheckpointMap.get(diskPath))) {
                xml.append("\" backupmode=\"incremental\"")
                        .append(" incremental=\"").append(fromCheckpointId)
                        .append("\" exportbitmap=\"").append(fromCheckpointId);
            }
            xml.append("\">\n");
            xml.append("      <scratch file=\"").append(scratchFile).append("\"/>\n");
            xml.append("    </disk>\n");
        }

        xml.append("  </disks>\n");
        xml.append("</domainbackup>");

        return xml.toString();
    }

     // Scratch file of each disk in the backup next to the disk on its primary storage
    static Map<String, String> getScratchFiles(Map<String, String> diskPathUuidMap, Map<String, String> diskPathLabelMap) {
        Map<String, String> scratchFiles = new HashMap<>();
        for (Map.Entry<String, String> entry : diskPathUuidMap.entrySet()) {
            if (!diskPathLabelMap.containsKey(entry.getKey())) {
                continue;
            }
            String directory = new File(entry.getKey()).getParent();
            scratchFiles.put(entry.getKey(), new File(directory, "backup-scratch-" + entry.getValue() + ".qcow2").getPath());
        }
        return scratchFiles;
    }

    String prepareScratchFiles(String vmName, Map<String, String> diskPathScratchFileMap, LibvirtComputingResource resource) {
        Domain dm = null;
        try {
            dm = resource.getDomain(resource.getLibvirtUtilitiesHelper().getConnection(), vmName);
            return prepareScratchFiles(dm, diskPathScratchFileMap);
        } catch (LibvirtException e) {
            return "Failed to prepare the backup scratch files: " + e.getMessage();
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException e) {
                    logger.debug("Failed to free domain {}", vmName, e);
                }
            }
        }
    }

    String prepareScratchFiles(Domain dm, Map<String, String> diskPathScratchFileMap) throws LibvirtException {
        DomainJobInfo job = dm.getJobInfo();
        if (job != null && job.getType() != NO_JOB) {
            // libvirt refuses a backup anyway; never touch the scratch files of a running one.
            return "The VM has an active job, a backup cannot start";
        }
        Map<String, Long> requiredSpace = new HashMap<>();
        for (Map.Entry<String, String> entry : diskPathScratchFileMap.entrySet()) {
            String scratchFile = entry.getValue();
            if (!deleteStaleScratchFile(scratchFile)) {
                return "Failed to remove the stale backup scratch file " + scratchFile;
            }
            long capacity = dm.blockInfo(entry.getKey()).getCapacity();
            // 1% on top of the virtual size covers the qcow2 metadata of the scratch file.
            requiredSpace.merge(new File(scratchFile).getParent(), capacity + capacity / 100, Long::sum);
        }
        for (Map.Entry<String, Long> entry : requiredSpace.entrySet()) {
            long usable = getUsableSpace(entry.getKey());
            if (usable < entry.getValue()) {
                return String.format("Not enough free space for the backup scratch files in %s: %d bytes needed, %d available",
                        entry.getKey(), entry.getValue(), usable);
            }
        }
        return null;
    }

    boolean deleteStaleScratchFile(String scratchFile) {
        try {
            if (Files.deleteIfExists(Paths.get(scratchFile))) {
                logger.warn("Removed the stale backup scratch file {}", scratchFile);
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to remove the stale backup scratch file {}", scratchFile, e);
            return false;
        }
    }

    long getUsableSpace(String directory) {
        return new File(directory).getUsableSpace();
    }

    private String createCheckpointXml(String checkpointId) {
        return "<domaincheckpoint>\n" +
               "  <name>" + checkpointId + "</name>\n" +
               "</domaincheckpoint>";
    }

    private Answer handleStoppedVmBackup(StartBackupCommand cmd, String toCheckpointId) {
        Map<String, String> diskPathUuidMap = cmd.getDiskPathUuidMap();
        Map<String, byte[]> diskPathPassphraseMap = cmd.getDiskPathPassphraseMap();
        for (Map.Entry<String, String> entry : diskPathUuidMap.entrySet()) {
            String diskPath = entry.getKey();
            Script script = new Script("qemu-img");
            byte[] passphrase = diskPathPassphraseMap.get(diskPath);

            script.add("bitmap");
            script.add("--add");

            if (passphrase != null && passphrase.length > 0) {
                KeyFile srcKey;
                try {
                    srcKey = new KeyFile(passphrase);
                } catch (IOException ex) {
                    return new StartBackupAnswer(cmd, false, "Failed to create KeyFile while adding bitmap " + toCheckpointId + " to disk " + diskPath);
                }
                script.add("--object");
                script.add(String.format("secret,id=sec0,file=%s", srcKey));
                script.add("--image-opts");
                script.add(String.format("driver=qcow2,file.driver=file,file.filename=%s,encrypt.key-secret=sec0", diskPath));
            } else {
                script.add(diskPath);
            }

            script.add(toCheckpointId);
            String result = script.execute();
            if (result != null) {
                return new StartBackupAnswer(cmd, false,
                    "Failed to add bitmap " + toCheckpointId + " to disk " + diskPath + ": " + result);
            }
        }
        long checkpointCreateTime = getCheckpointCreateTime();
        return new StartBackupAnswer(cmd, true, "Stopped VM backup: checkpoint bitmap added successfully",
            checkpointCreateTime);
    }

    private long getCheckpointCreateTime() {
        return System.currentTimeMillis() / 1000;
    }

    private Map<String, Boolean> getVmDiskPathHasFromCheckpointMap(Domain vm, String fromCheckpointId) throws LibvirtException {
        Map<String, Boolean> diskPathHasFromCheckpointMap = new HashMap<>();
        String queryBlock = vm.qemuMonitorCommand(QemuCommand.buildQemuCommand("query-block", null), 0);
        JSONObject response = new JSONObject(queryBlock);
        JSONArray blocks = response.optJSONArray("return");
        if (blocks == null) {
            logger.warn("Couldn't get bitmap information for the VM [{}]. Falling back to full Backup", vm.getName());
            return diskPathHasFromCheckpointMap;
        }
        for (int i = 0; i < blocks.length(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            JSONObject inserted = block.optJSONObject("inserted");
            if (inserted == null) {
                continue;
            }
            String file = inserted.optString("file");
            if (StringUtils.isBlank(file)) {
                continue;
            }
            JSONArray dirtyBitmaps = inserted.optJSONArray("dirty-bitmaps");
            boolean hasFromCheckpointBitmap = false;
            if (dirtyBitmaps != null) {
                for (int j = 0; j < dirtyBitmaps.length(); j++) {
                    JSONObject dirtyBitmap = dirtyBitmaps.optJSONObject(j);
                    if (dirtyBitmap == null) {
                        continue;
                    }
                    String bitmapName = dirtyBitmap.optString("name");
                    if (fromCheckpointId.equals(bitmapName)) {
                        hasFromCheckpointBitmap = true;
                        break;
                    }
                }
            }
            diskPathHasFromCheckpointMap.put(file, hasFromCheckpointBitmap);
        }
        return diskPathHasFromCheckpointMap;
    }
}
