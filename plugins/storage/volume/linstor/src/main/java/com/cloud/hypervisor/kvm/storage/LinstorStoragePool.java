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

import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

public class LinstorStoragePool implements KVMStoragePool {
    private static final Logger LOGGER = LogManager.getLogger(LinstorStoragePool.class);
    private final String _uuid;
    private final String _sourceHost;
    private final int _sourcePort;
    private final Storage.StoragePoolType _storagePoolType;
    private final StorageAdaptor _storageAdaptor;
    private final String _resourceGroup;
    private final String localNodeName;

    public LinstorStoragePool(String uuid, String host, int port, String resourceGroup,
                              Storage.StoragePoolType storagePoolType, StorageAdaptor storageAdaptor) {
        _uuid = uuid;
        _sourceHost = host;
        _sourcePort = port;
        _storagePoolType = storagePoolType;
        _storageAdaptor = storageAdaptor;
        _resourceGroup = resourceGroup;
        localNodeName = getHostname();
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, QemuImg.PhysicalDiskFormat format,
                                              Storage.ProvisioningType provisioningType, long size, byte[] passphrase)
    {
        return _storageAdaptor.createPhysicalDisk(name, this, format, provisioningType, size, passphrase);
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, Storage.ProvisioningType provisioningType, long size, byte[] passphrase)
    {
        return _storageAdaptor.createPhysicalDisk(volumeUuid,this, getDefaultFormat(), provisioningType, size, passphrase);
    }

    @Override
    public boolean connectPhysicalDisk(String volumeUuid, Map<String, String> details)
    {
        return _storageAdaptor.connectPhysicalDisk(volumeUuid, this, details);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid)
    {
        return _storageAdaptor.getPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid)
    {
        return _storageAdaptor.disconnectPhysicalDisk(volumeUuid, this);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, Storage.ImageFormat format)
    {
        return _storageAdaptor.deletePhysicalDisk(volumeUuid, this, format);
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks()
    {
        return _storageAdaptor.listPhysicalDisks(_uuid, this);
    }

    @Override
    public String getUuid()
    {
        return _uuid;
    }

    @Override
    public long getCapacity()
    {
        return ((LinstorStorageAdaptor)_storageAdaptor).getCapacity(this);
    }

    @Override
    public long getUsed()
    {
        return ((LinstorStorageAdaptor)_storageAdaptor).getUsed(this);
    }

    @Override
    public long getAvailable()
    {
        return ((LinstorStorageAdaptor)_storageAdaptor).getAvailable(this);
    }

    @Override
    public boolean refresh()
    {
        return _storageAdaptor.refresh(this);
    }

    @Override
    public boolean isExternalSnapshot()
    {
        return true;
    }

    @Override
    public String getLocalPath()
    {
        return null;
    }

    @Override
    public String getSourceHost()
    {
        return _sourceHost;
    }

    @Override
    public String getSourceDir()
    {
        return null;
    }

    @Override
    public int getSourcePort()
    {
        return _sourcePort;
    }

    @Override
    public String getAuthUserName()
    {
        return null;
    }

    @Override
    public String getAuthSecret()
    {
        return null;
    }

    @Override
    public Storage.StoragePoolType getType()
    {
        return _storagePoolType;
    }

    @Override
    public boolean delete()
    {
        return _storageAdaptor.deleteStoragePool(this);
    }

    @Override
    public QemuImg.PhysicalDiskFormat getDefaultFormat()
    {
        return QemuImg.PhysicalDiskFormat.RAW;
    }

    @Override
    public boolean createFolder(String path)
    {
        return _storageAdaptor.createFolder(_uuid, path);
    }

    @Override
    public boolean supportsConfigDriveIso()
    {
        return false;
    }

    @Override
    public Map<String, String> getDetails() {
        return null;
    }

    public String getResourceGroup() {
        return _resourceGroup;
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
    public String createHeartBeatCommand(HAStoragePool pool, String hostPrivateIp,
            boolean hostValidation) {
        LOGGER.trace(String.format("Linstor.createHeartBeatCommand: %s, %s, %b", pool.getPoolIp(), hostPrivateIp, hostValidation));
        boolean isStorageNodeUp = checkingHeartBeat(pool, null);
        if (!isStorageNodeUp && !hostValidation) {
            //restart the host
            LOGGER.debug(String.format("The host [%s] will be restarted because the health check failed for the storage pool [%s]", hostPrivateIp, pool.getPool().getType()));
            Script cmd = new Script(pool.getPool().getHearthBeatPath(), Duration.millis(HeartBeatUpdateTimeout), LOGGER);
            cmd.add("-c");
            cmd.execute();
            return "Down";
        }
        return isStorageNodeUp ? null : "Down";
    }

    @Override
    public String getStorageNodeId() {
        // only called by storpool
        return null;
    }

    static String getHostname() {
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        Script sc = new Script("hostname", Duration.millis(10000L), LOGGER);
        String res = sc.execute(parser);
        if (res != null) {
            throw new CloudRuntimeException(String.format("Unable to run 'hostname' command: %s", res));
        }
        String response = parser.getLines();
        return response.trim();
    }

    @Override
    public Boolean checkingHeartBeat(HAStoragePool pool, HostTO host) {
        String hostName;
        if (host == null) {
            hostName = localNodeName;
        } else {
            hostName = host.getParent();
            if (hostName == null) {
                LOGGER.error("No hostname set in host.getParent()");
                return false;
            }
        }

        return checkHostUpToDateAndConnected(hostName);
    }

    private String executeDrbdSetupStatus(OutputInterpreter.AllLinesParser parser) {
        Script sc = new Script("drbdsetup", Duration.millis(HeartBeatUpdateTimeout), LOGGER);
        sc.add("status");
        sc.add("--json");
        return sc.execute(parser);
    }

    private boolean checkDrbdSetupStatusOutput(String output, String otherNodeName) {
        JsonParser jsonParser = new JsonParser();
        JsonArray jResources = (JsonArray) jsonParser.parse(output);
        for (JsonElement jElem : jResources) {
            JsonObject jRes = (JsonObject) jElem;
            JsonArray jConnections = jRes.getAsJsonArray("connections");
            for (JsonElement jConElem : jConnections) {
                JsonObject jConn = (JsonObject) jConElem;
                if (jConn.getAsJsonPrimitive("name").getAsString().equals(otherNodeName)
                        && jConn.getAsJsonPrimitive("connection-state").getAsString().equalsIgnoreCase("Connected")) {
                    return true;
                }
            }
        }
        LOGGER.warn(String.format("checkDrbdSetupStatusOutput: no resource connected to %s.", otherNodeName));
        return false;
    }

    private String executeDrbdEventsNow(OutputInterpreter.AllLinesParser parser) {
        Script sc = new Script("drbdsetup", Duration.millis(HeartBeatUpdateTimeout), LOGGER);
        sc.add("events2");
        sc.add("--now");
        return sc.execute(parser);
    }

    private boolean checkDrbdEventsNowOutput(String output) {
        boolean healthy = output.lines().noneMatch(line -> line.matches(".*role:Primary .* promotion_score:0.*"));
        if (!healthy) {
            LOGGER.warn("checkDrbdEventsNowOutput: primary resource with promotion score==0; HA false");
        }
        return healthy;
    }

    private boolean checkHostUpToDateAndConnected(String hostName) {
        LOGGER.trace(String.format("checkHostUpToDateAndConnected: %s/%s", localNodeName, hostName));
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();

        if (localNodeName.equalsIgnoreCase(hostName)) {
            String res = executeDrbdEventsNow(parser);
            if (res != null) {
                return false;
            }
            return checkDrbdEventsNowOutput(parser.getLines());
        } else {
            // check drbd connections
            String res = executeDrbdSetupStatus(parser);
            if (res != null) {
                return false;
            }
            try {
                return checkDrbdSetupStatusOutput(parser.getLines(), hostName);
            } catch (JsonIOException | JsonSyntaxException e) {
                LOGGER.error("Error parsing drbdsetup status --json", e);
            }
        }
        return false;
    }

    @Override
    public Boolean vmActivityCheck(HAStoragePool pool, HostTO host, Duration activityScriptTimeout, String volumeUUIDListString, String vmActivityCheckPath, long duration) {
        LOGGER.trace(String.format("Linstor.vmActivityCheck: %s, %s", pool.getPoolIp(), host.getPrivateNetwork().getIp()));
        return checkingHeartBeat(pool, host);
    }
}
