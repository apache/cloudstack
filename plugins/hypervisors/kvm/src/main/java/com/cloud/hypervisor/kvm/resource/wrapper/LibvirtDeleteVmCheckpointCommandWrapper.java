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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.backup.DeleteVmCheckpointCommand;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.cloudstack.utils.qemu.QemuCommand;
import org.apache.cloudstack.utils.qemu.QemuImageFilename;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

// Deletes a checkpoint of the KVM backup export: the libvirt checkpoint of a running VM and the dirty bitmap of the
// same name in every layer of the disk images. A missing bitmap is not an error, so a delete can be retried.
@ResourceWrapper(handles = DeleteVmCheckpointCommand.class)
public class LibvirtDeleteVmCheckpointCommandWrapper extends CommandWrapper<DeleteVmCheckpointCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final Logger LOGGER = LogManager.getLogger(LibvirtDeleteVmCheckpointCommandWrapper.class);

    @Override
    public Answer execute(DeleteVmCheckpointCommand cmd, LibvirtComputingResource resource) {
        if (cmd.isStoppedVM()) {
            return deleteBitmapsOnDisks(cmd);
        }
        return deleteDomainCheckpoint(cmd, resource);
    }

    // Running VM: libvirt's delete removes the bitmap from every layer of the listed disks. When it fails (forgotten
    // checkpoint, a listed disk lost its bitmap) qemu is asked which disks still carry it and libvirt is told again.
    private Answer deleteDomainCheckpoint(DeleteVmCheckpointCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();
        String checkpointId = cmd.getCheckpointId();
        String result = virsh("checkpoint-delete", vmName, checkpointId);
        if (result == null) {
            return new Answer(cmd, true, "Checkpoint deleted");
        }
        logger.info("Deleting checkpoint {} of VM {} failed ({}), redefining it from the disks that carry its bitmap", checkpointId, vmName, result);

        Map<String, Boolean> diskHasBitmap;
        try {
            diskHasBitmap = findDisksWithBitmap(resource, vmName, checkpointId);
        } catch (Exception e) {
            logger.error("Failed to look up the bitmap of checkpoint {} on the disks of VM {}", checkpointId, vmName, e);
            return new Answer(cmd, false, "Failed to delete checkpoint " + checkpointId + ": " + result + "; bitmap lookup failed: " + e.getMessage());
        }
        List<String> disksWithBitmap = new ArrayList<>();
        diskHasBitmap.forEach((label, has) -> { if (has) { disksWithBitmap.add(label); } });

        if (disksWithBitmap.isEmpty()) {
            // Nothing left in the images; drop libvirt's record if it still has one.
            String dump = virsh("checkpoint-dumpxml", "--domain", vmName, "--checkpointname", checkpointId, "--no-domain");
            if (dump == null) {
                String metadata = virsh("checkpoint-delete", vmName, checkpointId, "--metadata");
                if (metadata != null) {
                    return new Answer(cmd, false, "Failed to delete checkpoint " + checkpointId + ": " + metadata);
                }
            } else if (!dump.toLowerCase().contains("checkpoint not found")) {
                return new Answer(cmd, false, "Failed to check whether libvirt still knows checkpoint " + checkpointId + ": " + dump);
            }
            return new Answer(cmd, true, "Checkpoint not present");
        }

        String redefine = redefineCheckpoint(vmName, checkpointId, cmd.getCheckpointCreateTime(), diskHasBitmap);
        if (redefine != null) {
            return new Answer(cmd, false, "Failed to redefine checkpoint " + checkpointId + " with disks " + disksWithBitmap + ": " + redefine);
        }
        result = virsh("checkpoint-delete", vmName, checkpointId);
        if (result != null) {
            return new Answer(cmd, false, "Failed to delete checkpoint " + checkpointId + " after redefining it with disks " + disksWithBitmap + ": " + result);
        }
        return new Answer(cmd, true, "Checkpoint deleted, bitmap removed from disks " + disksWithBitmap);
    }

    private static String virsh(String... args) {
        Script script = new Script("virsh", LOGGER);
        script.add(args);
        return script.execute();
    }

    // Redefines the checkpoint in libvirt with the disks that carry its bitmap; a listed disk without it is refused.
    static String redefineCheckpoint(String vmName, String checkpointId, Long createTime, Map<String, Boolean> diskHasBitmap) {
        StringBuilder xml = new StringBuilder("<domaincheckpoint>\n");
        xml.append("  <name>").append(checkpointId).append("</name>\n");
        // libvirt requires a creation time on a redefine but does not check it.
        xml.append("  <creationTime>").append(createTime != null ? createTime : System.currentTimeMillis() / 1000).append("</creationTime>\n");
        xml.append("  <disks>\n");
        for (Map.Entry<String, Boolean> disk : diskHasBitmap.entrySet()) {
            xml.append(String.format("    <disk name='%s' checkpoint='%s'/>\n", disk.getKey(), disk.getValue() ? "bitmap" : "no"));
        }
        xml.append("  </disks>\n</domaincheckpoint>\n");
        File xmlFile;
        try {
            xmlFile = File.createTempFile("checkpoint-redefine-", ".xml");
            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(xml.toString());
            }
        } catch (IOException e) {
            return "could not write the checkpoint XML: " + e.getMessage();
        }
        try {
            return virsh("checkpoint-create", "--domain", vmName, "--xmlfile", xmlFile.getAbsolutePath(), "--redefine");
        } finally {
            xmlFile.delete();
        }
    }

    // Whether each disk of the running domain has the bitmap in any layer of its chain. Asked through QMP, since
    // qemu-img info on an image a running qemu holds shows stale bitmap metadata.
    static Map<String, Boolean> findDisksWithBitmap(LibvirtComputingResource resource, String vmName, String bitmapName) throws LibvirtException {
        Map<String, String> diskPathLabelMap = resource.getDiskPathLabelMap(vmName);
        Domain vm = resource.getDomain(resource.getLibvirtUtilitiesHelper().getConnection(), vmName);
        try {
            String nodes = vm.qemuMonitorCommand(QemuCommand.buildQemuCommand("query-named-block-nodes", null), 0);
            return findDisksWithBitmap(nodes, diskPathLabelMap, bitmapName);
        } finally {
            vm.free();
        }
    }

    // Only qcow2 nodes are looked at: the protocol node of the same file has neither bitmaps nor a backing chain.
    static Map<String, Boolean> findDisksWithBitmap(String namedBlockNodes, Map<String, String> diskPathLabelMap, String bitmapName) {
        JSONArray nodes = new JSONObject(namedBlockNodes).optJSONArray("return");
        if (nodes == null) {
            throw new CloudRuntimeException("query-named-block-nodes returned no node list: " + namedBlockNodes);
        }
        Set<String> filesWithBitmap = new HashSet<>();
        Map<String, JSONObject> imageByFile = new LinkedHashMap<>();
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            // An encrypted image is reported under a json: pseudo-filename, not its path.
            String file = QemuImageFilename.plain(node.optString("file", null));
            if (file == null || !"qcow2".equals(node.optString("drv"))) {
                continue;
            }
            JSONObject image = node.optJSONObject("image");
            if (image != null) {
                imageByFile.put(file, image);
            }
            JSONArray bitmaps = node.optJSONArray("dirty-bitmaps");
            for (int j = 0; bitmaps != null && j < bitmaps.length(); j++) {
                if (bitmapName.equals(bitmaps.getJSONObject(j).optString("name"))) {
                    filesWithBitmap.add(file);
                }
            }
        }
        Map<String, Boolean> diskHasBitmap = new LinkedHashMap<>();
        for (Map.Entry<String, String> disk : diskPathLabelMap.entrySet()) {
            boolean has = false;
            for (JSONObject image = imageByFile.get(disk.getKey()); image != null; image = image.optJSONObject("backing-image")) {
                if (filesWithBitmap.contains(QemuImageFilename.plain(image.optString("filename")))) {
                    has = true;
                    break;
                }
            }
            diskHasBitmap.put(disk.getValue(), has);
        }
        return diskHasBitmap;
    }

    // Stopped VM: removes the persistent bitmap from every layer of each volume's chain that lists it. The template
    // at the bottom of a root volume's chain never carries one and is never opened for writing.
    private Answer deleteBitmapsOnDisks(DeleteVmCheckpointCommand cmd) {
        String checkpointId = cmd.getCheckpointId();
        Map<String, String> diskPathUuidMap = cmd.getDiskPathUuidMap();
        if (diskPathUuidMap == null || diskPathUuidMap.isEmpty()) {
            return new Answer(cmd, false, "No disks provided for bitmap removal");
        }
        Map<String, byte[]> diskPathPassphraseMap = cmd.getDiskPathPassphraseMap() != null ? cmd.getDiskPathPassphraseMap() : Collections.emptyMap();
        List<String> removedFrom = new ArrayList<>();
        for (String diskPath : diskPathUuidMap.keySet()) {
            byte[] passphrase = diskPathPassphraseMap.get(diskPath);
            List<String> layers;
            try {
                layers = queryChainLayersWithBitmap(diskPath, checkpointId);
            } catch (Exception e) {
                return new Answer(cmd, false, "Failed to read the backing chain of disk " + diskPath + ": " + e.getMessage());
            }
            for (String layer : layers) {
                String result;
                try {
                    result = removeBitmapFromImage(layer, passphrase, checkpointId);
                } catch (IOException e) {
                    return new Answer(cmd, false, "Failed to create the key file for removing bitmap " + checkpointId + " from disk " + layer);
                }
                if (result == null) {
                    removedFrom.add(layer);
                } else if (result.contains(String.format("Dirty bitmap '%s' not found", checkpointId))) {
                    logger.info("Bitmap {} is not present on {}: {}", checkpointId, layer, result);
                } else {
                    return new Answer(cmd, false, "Failed to remove bitmap " + checkpointId + " from disk " + layer + ": " + result);
                }
            }
        }
        return new Answer(cmd, true, removedFrom.isEmpty() ? "Checkpoint not present" : "Checkpoint bitmap removed from " + removedFrom);
    }

    // The files of the volume's backing chain, top first, whose bitmap list contains the name.
    private List<String> queryChainLayersWithBitmap(String diskPath, String bitmapName) {
        Script script = new Script("qemu-img", logger);
        script.add("info", "-U", "--backing-chain", "--output=json", diskPath);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException(result);
        }
        return findChainLayersWithBitmap(parser.getLines(), bitmapName);
    }

    static List<String> findChainLayersWithBitmap(String backingChainInfo, String bitmapName) {
        List<String> layers = new ArrayList<>();
        // Script merges stderr into stdout, so a qemu-img warning may precede the JSON.
        int start = backingChainInfo.indexOf('[');
        if (start < 0) {
            throw new CloudRuntimeException("qemu-img info returned no JSON: " + backingChainInfo);
        }
        JSONArray chain = new JSONArray(backingChainInfo.substring(start));
        for (int i = 0; i < chain.length(); i++) {
            JSONObject image = chain.getJSONObject(i);
            JSONObject specific = image.optJSONObject("format-specific");
            JSONObject data = specific != null ? specific.optJSONObject("data") : null;
            JSONArray bitmaps = data != null ? data.optJSONArray("bitmaps") : null;
            for (int j = 0; bitmaps != null && j < bitmaps.length(); j++) {
                if (bitmapName.equals(bitmaps.getJSONObject(j).optString("name"))) {
                    layers.add(image.getString("filename"));
                    break;
                }
            }
        }
        return layers;
    }

    private String removeBitmapFromImage(String diskPath, byte[] passphrase, String bitmapName) throws IOException {
        Script script = new Script("qemu-img", logger);
        script.add("bitmap", "--remove");
        if (passphrase == null || passphrase.length == 0) {
            script.add(diskPath, bitmapName);
            return script.execute();
        }
        // The key file holds the plaintext passphrase, so it only lives for the duration of the call.
        try (KeyFile key = new KeyFile(passphrase)) {
            script.add("--object", String.format("secret,id=sec0,file=%s", key));
            script.add("--image-opts", String.format("driver=qcow2,file.driver=file,file.filename=%s,encrypt.key-secret=sec0", diskPath));
            script.add(bitmapName);
            return script.execute();
        }
    }
}
