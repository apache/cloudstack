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
package com.cloud.hypervisor.kvm.storage;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class StorPoolStoragePool implements KVMStoragePool {
    protected Logger logger = LogManager.getLogger(StorPoolStoragePool.class);
    private String _uuid;
    private String _sourceHost;
    private int _sourcePort;
    private StoragePoolType _storagePoolType;
    private StorageAdaptor _storageAdaptor;
    private String _authUsername;
    private String _authSecret;
    private String _sourceDir;
    private String _localPath;
    private String storageNodeId = getStorPoolConfigParam("SP_OURID");

    public StorPoolStoragePool(String uuid, String host, int port, StoragePoolType storagePoolType, StorageAdaptor storageAdaptor) {
        _uuid = uuid;
        _sourceHost = host;
        _sourcePort = port;
        _storagePoolType = storagePoolType;
        _storageAdaptor = storageAdaptor;
    }

    @Override
    public String getUuid() {
        return _uuid;
    }

    @Override
    public String getSourceHost() {
        return _sourceHost;
    }

    @Override
    public int getSourcePort() {
        return _sourcePort;
    }

    @Override
    public long getCapacity() {
        return 100L*(1024L*1024L*1024L*1024L*1024L);
    }

    @Override
    public long getUsed() {
        return 0;
    }

    @Override
    public long getAvailable() {
        return 0;
    }

    @Override
    public StoragePoolType getType() {
        return _storagePoolType;
    }

    @Override
    public String getAuthUserName() {
        return _authUsername;
    }

    @Override
    public String getAuthSecret() {
        return _authSecret;
    }

    @Override
    public String getSourceDir() {
        return _sourceDir;
    }

    @Override
    public String getLocalPath() {
        return _localPath;
    }

    @Override
    public PhysicalDiskFormat getDefaultFormat() {
        return PhysicalDiskFormat.RAW;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        return _storageAdaptor.createPhysicalDisk(name, this, format, provisioningType, size, passphrase);
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        return _storageAdaptor.createPhysicalDisk(name, this, null, provisioningType, size, passphrase);
    }

    @Override
    public boolean connectPhysicalDisk(String name, Map<String, String> details) {
        return _storageAdaptor.connectPhysicalDisk(name, this, details);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid) {
        return _storageAdaptor.getPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid) {
        return _storageAdaptor.disconnectPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format) {
        return _storageAdaptor.deletePhysicalDisk(volumeUuid, this, format);
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks() {
        return _storageAdaptor.listPhysicalDisks(_uuid, this);
    }

    @Override
    public boolean refresh() {
        return _storageAdaptor.refresh(this);
    }

    @Override
    public boolean delete() {
        return _storageAdaptor.deleteStoragePool(this);
    }

    @Override
    public boolean createFolder(String path) {
        return _storageAdaptor.createFolder(_uuid, path);
    }

    @Override
    public boolean isExternalSnapshot() {
        return false;
    }

    public boolean supportsConfigDriveIso() {
        return false;
    }

    @Override
    public Map<String, String> getDetails() {
        return null;
    }

    @Override
    public boolean isPoolSupportHA() {
        return true;
    }

    @Override
    public String getHearthBeatPath() {
        String kvmScriptsDir = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_SCRIPTS_DIR);
        return Script.findScript(kvmScriptsDir, "kvmspheartbeat.sh");
    }

    @Override
    public String createHeartBeatCommand(HAStoragePool primaryStoragePool, String hostPrivateIp, boolean hostValidation) {
        boolean isStorageNodeUp = checkingHeartBeat(primaryStoragePool, null);
        if (!isStorageNodeUp && !hostValidation) {
            //restart the host
            logger.debug(String.format("The host [%s] will be restarted because the health check failed for the storage pool [%s]", hostPrivateIp, primaryStoragePool.getPool().getType()));
            Script cmd = new Script(primaryStoragePool.getPool().getHearthBeatPath(), HeartBeatUpdateTimeout, logger);
            cmd.add("-c");
            cmd.execute();
            return "Down";
        }
        return isStorageNodeUp ? null : "Down";
    }

    @Override
    public String getStorageNodeId() {
        return storageNodeId;
    }

    public static final String getStorPoolConfigParam(String param) {
        Script sc = new Script("storpool_confget", 0, LogManager.getLogger(StorPoolStoragePool.class));
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();

        String configParam = null;
        final String err = sc.execute(parser);
        if (err != null) {
            StorPoolStorageAdaptor.SP_LOG("Could not execute storpool_confget. Error: %s", err);
            return configParam;
        }

        for (String line: parser.getLines().split("\n")) {
            String[] toks = line.split("=");
            if( toks.length != 2 ) {
                continue;
            }
            if (toks[0].equals(param)) {
                configParam = toks[1];
                return configParam;
            }
        }
        return configParam;
    }

    @Override
    public Boolean checkingHeartBeat(HAStoragePool pool, HostTO host) {
        boolean isNodeWorking = false;
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();

        String res = executeStorPoolServiceListCmd(parser);

        if (res != null) {
            return isNodeWorking;
        }
        String response = parser.getLines();

        Integer hostStorageNodeId = null;
        if (host == null) {
            hostStorageNodeId = Integer.parseInt(storageNodeId);
        } else {
            hostStorageNodeId = host.getParent() != null ? Integer.parseInt(host.getParent()) : null;
        }
        if (hostStorageNodeId == null) {
            return isNodeWorking;
        }
        try {
            isNodeWorking = checkIfNodeIsRunning(response, hostStorageNodeId);
        } catch (JsonIOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
        return isNodeWorking;
    }

    private boolean checkIfNodeIsRunning(String response, Integer hostStorageNodeId) {
        boolean isNodeWorking = false;
        JsonParser jsonParser = new JsonParser();
        JsonObject stats = (JsonObject) jsonParser.parse(response);
        JsonObject data = stats.getAsJsonObject("data");
        if (data != null) {
            JsonObject clients = data.getAsJsonObject("clients");
            for (Entry<String, JsonElement> element : clients.entrySet()) {
                String storageNodeStatus = element.getValue().getAsJsonObject().get("status").getAsString();
                int nodeId = element.getValue().getAsJsonObject().get("nodeId").getAsInt();
                if (hostStorageNodeId == nodeId) {
                    if (storageNodeStatus.equals("running")) {
                        return true;
                    } else {
                        return isNodeWorking;
                    }
                }
            }
        }
        return isNodeWorking;
    }

    private String executeStorPoolServiceListCmd(OutputInterpreter.AllLinesParser parser) {
        Script sc = new Script("storpool", 0, logger);
        sc.add("-j");
        sc.add("service");
        sc.add("list");
        String res = sc.execute(parser);
        return res;
    }

    @Override
    public Boolean vmActivityCheck(HAStoragePool pool, HostTO host, Duration activityScriptTimeout, String volumeUuidListString, String vmActivityCheckPath, long duration) {
        return checkingHeartBeat(pool, host);
    }
}
