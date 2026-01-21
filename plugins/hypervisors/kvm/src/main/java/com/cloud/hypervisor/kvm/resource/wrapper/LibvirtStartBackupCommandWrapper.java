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
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.backup.StartBackupAnswer;
import org.apache.cloudstack.backup.StartBackupCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StartBackupCommand.class)
public class LibvirtStartBackupCommandWrapper extends CommandWrapper<StartBackupCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(StartBackupCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        String toCheckpointId = cmd.getToCheckpointId();
        String fromCheckpointId = cmd.getFromCheckpointId();
        int nbdPort = cmd.getNbdPort();

        try {
            Connect conn = LibvirtConnection.getConnection();
            Domain dm = conn.domainLookupByName(vmName);

            if (dm == null) {
                return new StartBackupAnswer(cmd, false, "Domain not found: " + vmName);
            }

            DomainInfo info = dm.getInfo();
            if (info.state != DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                return new StartBackupAnswer(cmd, false, "VM is not running");
            }

            // Create backup XML
            String backupXml = createBackupXml(cmd, fromCheckpointId, nbdPort);
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

            // Get checkpoint creation time - using current time for POC
            long checkpointCreateTime = System.currentTimeMillis();

            // Build device mappings from domblklist
            Map<Long, String> deviceMappings = getDeviceMappings(vmName, cmd.getDiskVolumePaths(), resource);

            return new StartBackupAnswer(cmd, true, "Backup started successfully",
                checkpointCreateTime, deviceMappings);

        } catch (Exception e) {
            return new StartBackupAnswer(cmd, false, "Error starting backup: " + e.getMessage());
        }
    }

    private String createBackupXml(StartBackupCommand cmd, String fromCheckpointId, int nbdPort) {
        StringBuilder xml = new StringBuilder();
        xml.append("<domainbackup mode=\"pull\">\n");

        if (fromCheckpointId != null && !fromCheckpointId.isEmpty()) {
            xml.append("  <incremental>").append(fromCheckpointId).append("</incremental>\n");
        }

        xml.append(String.format("  <server transport=\"tcp\" name=\"%s\" port=\"%s\"/>\n", cmd.getHostIpAddress(), nbdPort));
        xml.append("  <disks>\n");

        // Add disk entries - simplified for POC
        Map<Long, String> diskPaths = cmd.getDiskVolumePaths();
        int diskIndex = 0;
        for (Map.Entry<Long, String> entry : diskPaths.entrySet()) {
            String deviceName = "vd" + (char)('a' + diskIndex);
            String scratchFile = "/var/tmp/scratch-" + entry.getKey() + ".qcow2";
            xml.append("    <disk name=\"").append(deviceName).append("\" type=\"file\" exportname=\"")
               .append(deviceName).append("\">\n");
            xml.append("      <scratch file=\"").append(scratchFile).append("\"/>\n");
            xml.append("    </disk>\n");
            diskIndex++;
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

    private Map<Long, String> getDeviceMappings(String vmName, Map<Long, String> diskPaths,
                                                 LibvirtComputingResource resource) {
        Map<Long, String> mappings = new HashMap<>();

        // Simplified for POC - map volumeIds to device names in order
        int diskIndex = 0;
        for (Long volumeId : diskPaths.keySet()) {
            String deviceName = "vd" + (char)('a' + diskIndex);
            mappings.put(volumeId, deviceName);
            diskIndex++;
        }

        return mappings;
    }
}
