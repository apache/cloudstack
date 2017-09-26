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
package com.cloud.agent.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.StringUtils;

@Local(value = {ServerResource.class})
public class DummyResource implements ServerResource {
    String _name;
    Host.Type _type;
    boolean _negative;
    IAgentControl _agentControl;
    Map<String, Object> _params;

    @Override
    public void disconnected() {
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof CheckNetworkCommand) {
            return new CheckNetworkAnswer((CheckNetworkCommand)cmd, true, null);
        }
        System.out.println("Received Command: " + cmd.toString());
        Answer answer = new Answer(cmd, !_negative, "response");
        System.out.println("Replying with: " + answer.toString());
        return answer;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(_type, id);
    }

    @Override
    public Type getType() {
        return _type;
    }

    protected String getConfiguredProperty(String key, String defaultValue) {
        String val = (String)_params.get(key);
        return val == null ? defaultValue : val;
    }

    protected Long getConfiguredProperty(String key, Long defaultValue) {
        String val = (String)_params.get(key);

        if (val != null) {
            Long result = Long.parseLong(val);
            return result;
        }
        return defaultValue;
    }

    protected List<Object> getHostInfo() {
        final ArrayList<Object> info = new ArrayList<Object>();
        long speed = getConfiguredProperty("cpuspeed", 4000L);
        long cpus = getConfiguredProperty("cpus", 4L);
        long ram = getConfiguredProperty("memory", 16000L * 1024L * 1024L);
        long dom0ram = Math.min(ram / 10, 768 * 1024 * 1024L);

        String cap = getConfiguredProperty("capabilities", "hvm");
        info.add((int)cpus);
        info.add(speed);
        info.add(ram);
        info.add(cap);
        info.add(dom0ram);
        return info;

    }

    protected void fillNetworkInformation(final StartupCommand cmd) {

        cmd.setPrivateIpAddress(getConfiguredProperty("private.ip.address", "127.0.0.1"));
        cmd.setPrivateMacAddress(getConfiguredProperty("private.mac.address", "8A:D2:54:3F:7C:C3"));
        cmd.setPrivateNetmask(getConfiguredProperty("private.ip.netmask", "255.255.255.0"));

        cmd.setStorageIpAddress(getConfiguredProperty("private.ip.address", "127.0.0.1"));
        cmd.setStorageMacAddress(getConfiguredProperty("private.mac.address", "8A:D2:54:3F:7C:C3"));
        cmd.setStorageNetmask(getConfiguredProperty("private.ip.netmask", "255.255.255.0"));
        cmd.setGatewayIpAddress(getConfiguredProperty("gateway.ip.address", "127.0.0.1"));

    }

    private Map<String, String> getVersionStrings() {
        Map<String, String> result = new HashMap<String, String>();
        String hostOs = (String)_params.get("Host.OS");
        String hostOsVer = (String)_params.get("Host.OS.Version");
        String hostOsKernVer = (String)_params.get("Host.OS.Kernel.Version");
        result.put("Host.OS", hostOs == null ? "Fedora" : hostOs);
        result.put("Host.OS.Version", hostOsVer == null ? "14" : hostOsVer);
        result.put("Host.OS.Kernel.Version", hostOsKernVer == null ? "2.6.35.6-45.fc14.x86_64" : hostOsKernVer);
        return result;
    }

    protected StoragePoolInfo initializeLocalStorage() {
        String hostIp = getConfiguredProperty("private.ip.address", "127.0.0.1");
        String localStoragePath = getConfiguredProperty("local.storage.path", "/mnt");
        String lh = hostIp + localStoragePath;
        String uuid = UUID.nameUUIDFromBytes(lh.getBytes(StringUtils.getPreferredCharset())).toString();

        String capacity = getConfiguredProperty("local.storage.capacity", "1000000000");
        String available = getConfiguredProperty("local.storage.avail", "10000000");

        return new StoragePoolInfo(uuid, hostIp, localStoragePath, localStoragePath, StoragePoolType.Filesystem, Long.parseLong(capacity), Long.parseLong(available));

    }

    @Override
    public StartupCommand[] initialize() {
        final List<Object> info = getHostInfo();

        final StartupRoutingCommand cmd =
            new StartupRoutingCommand((Integer)info.get(0), (Long)info.get(1), (Long)info.get(2), (Long)info.get(4), (String)info.get(3), HypervisorType.KVM,
                RouterPrivateIpStrategy.HostLocal);
        fillNetworkInformation(cmd);
        cmd.getHostDetails().putAll(getVersionStrings());
        cmd.setCluster(getConfiguredProperty("cluster", "1"));
        StoragePoolInfo pi = initializeLocalStorage();
        StartupStorageCommand sscmd = new StartupStorageCommand();
        sscmd.setPoolInfo(pi);
        sscmd.setGuid(pi.getUuid());
        sscmd.setDataCenter((String)_params.get("zone"));
        sscmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);

        return new StartupCommand[] {cmd, sscmd};
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        _name = name;

        String value = (String)params.get("type");
        _type = Host.Type.valueOf(value);

        value = (String)params.get("negative.reply");
        _negative = Boolean.parseBoolean(value);
        setParams(params);
        return true;
    }

    public void setParams(Map<String, Object> params) {
        this._params = params;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public IAgentControl getAgentControl() {
        return _agentControl;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        _agentControl = agentControl;
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }
}
