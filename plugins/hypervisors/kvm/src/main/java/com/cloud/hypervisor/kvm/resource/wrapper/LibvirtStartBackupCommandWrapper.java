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
import java.util.Map;

import org.apache.cloudstack.backup.StartBackupAnswer;
import org.apache.cloudstack.backup.StartBackupCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.StringUtils;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StartBackupCommand.class)
public class LibvirtStartBackupCommandWrapper extends CommandWrapper<StartBackupCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

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

            // Create backup XML
            String backupXml = createBackupXml(cmd, fromCheckpointId, socket, resource);
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

//            backupXmlFile.delete();
//            checkpointXmlFile.delete();

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

    private String createBackupXml(StartBackupCommand cmd, String fromCheckpointId, String socket, LibvirtComputingResource resource) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domainbackup mode=\"pull\">\n");

        if (StringUtils.isNotBlank(fromCheckpointId)) {
            xml.append("  <incremental>").append(fromCheckpointId).append("</incremental>\n");
        }

        xml.append(String.format("  <server transport=\"unix\" socket=\"/tmp/imagetransfer/%s.sock\"/>\n", socket));

        xml.append("  <disks>\n");

        Map<String, String> diskPathUuidMap = cmd.getDiskPathUuidMap();
        Map<String, String> diskPathLabelMap = resource.getDiskPathLabelMap(cmd.getVmName());

        for (Map.Entry<String, String> entry : diskPathLabelMap.entrySet()) {
            if (!diskPathUuidMap.containsKey(entry.getKey())) {
                continue;
            }
            String diskName = entry.getValue();
            String export = diskPathUuidMap.get(entry.getKey());
            // todo: use UUID here as well?
            String scratchFile = "/var/tmp/scratch-" + export + ".qcow2";
            xml.append("    <disk name=\"").append(diskName).append("\" type=\"file\" exportname=\"").append(export);
            if (StringUtils.isNotBlank(fromCheckpointId)) {
                String exportBitmap = export + "-" + fromCheckpointId.substring(0, 4);
                xml.append("\" exportbitmap=\"").append(exportBitmap);
            }
            xml.append("\">\n");
            xml.append("      <scratch file=\"").append(scratchFile).append("\"/>\n");
            xml.append("    </disk>\n");
        }

        xml.append("  </disks>\n");
        xml.append("</domainbackup>");

        return xml.toString();
    }

    private String createCheckpointXml(String checkpointId) {
        return "<domaincheckpoint>\n" +
               "  <name>" + checkpointId + "</name>\n" +
               "</domaincheckpoint>";
    }

    private Answer handleStoppedVmBackup(StartBackupCommand cmd, String toCheckpointId) {
        String vmName = cmd.getVmName();
        Map<String, String> diskPathUuidMap = cmd.getDiskPathUuidMap();
        for (Map.Entry<String, String> entry : diskPathUuidMap.entrySet()) {
            String diskPath = entry.getKey();
            Script script = new Script("sudo");
            script.add("qemu-img");
            script.add("bitmap");
            script.add("--add");
            script.add(diskPath);
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
}
