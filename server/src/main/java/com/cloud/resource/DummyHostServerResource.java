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
package com.cloud.resource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.net.MacAddress;

public class DummyHostServerResource extends ServerResourceBase {

    private String _name;
    private String _zone;
    private String _pod;
    private String _guid;
    private String _url;
    private int _instanceId;
    private final int _prefix = 0x55;

    private static volatile int s_nextSequence = 1;

    @Override
    protected String getDefaultScriptsDir() {
        return "/dummy";
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return new Answer(cmd, false, "Unsupported in dummy host server resource");
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingRoutingCommand(com.cloud.host.Host.Type.Routing, id, new HashMap<String, HostVmStateReportEntry>());
    }

    @Override
    public Type getType() {
        return com.cloud.host.Host.Type.Routing;
    }

    @Override
    public StartupCommand[] initialize() {

        StartupRoutingCommand cmd = new StartupRoutingCommand();
        cmd.setCpus(1);
        cmd.setSpeed(1000L);
        cmd.setMemory(1000000L);
        cmd.setDom0MinMemory(256L);
        cmd.setCaps("hvm");
        cmd.setGuid(_guid);
        cmd.setDataCenter(_zone);
        cmd.setPod(_pod);
        cmd.setHypervisorType(HypervisorType.None);
        cmd.setAgentTag("vmops-simulator");
        cmd.setName(_url);
        cmd.setPrivateIpAddress(this.getHostPrivateIp());
        cmd.setPrivateMacAddress(this.getHostMacAddress().toString());
        cmd.setPrivateNetmask("255.255.0.0");
        cmd.setIqn("iqn:" + _url);
        cmd.setStorageIpAddress(getHostStoragePrivateIp());
        cmd.setStorageMacAddress(getHostStorageMacAddress().toString());
        cmd.setStorageIpAddressDeux(getHostStoragePrivateIp2());
        cmd.setStorageMacAddressDeux(getHostStorageMacAddress2().toString());
        cmd.setPublicIpAddress(getHostStoragePrivateIp());
        cmd.setPublicMacAddress(getHostStorageMacAddress().toString());
        cmd.setPublicNetmask("255.255.0.0");
        cmd.setVersion(DummyHostServerResource.class.getPackage().getImplementationVersion());

        return new StartupCommand[] {cmd};
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _guid = (String)params.get("guid");
        _zone = (String)params.get("zone");
        _pod = (String)params.get("pod");
        _url = (String)params.get("url");

        _instanceId = getNextSequenceId();
        return true;
    }

    public synchronized static int getNextSequenceId() {
        return s_nextSequence++;
    }

    public MacAddress getHostMacAddress() {
        long address = 0;

        address = (_prefix & 0xff);
        address <<= 40;
        address |= _instanceId;
        return new MacAddress(address);
    }

    public String getHostPrivateIp() {
        int id = _instanceId;

        return "172.16." + String.valueOf((id >> 8) & 0xff) + "." + String.valueOf(id & 0xff);
    }

    public MacAddress getHostStorageMacAddress() {
        long address = 0;

        address = (_prefix & 0xff);
        address <<= 40;
        address |= (_instanceId | (1L << 31)) & 0xffffffff;
        return new MacAddress(address);
    }

    public MacAddress getHostStorageMacAddress2() {
        long address = 0;

        address = (_prefix & 0xff);
        address <<= 40;
        address |= (_instanceId | (3L << 30)) & 0xffffffff;
        return new MacAddress(address);
    }

    public String getHostStoragePrivateIp() {
        int id = _instanceId;
        id |= 1 << 15;

        return "172.16." + String.valueOf((id >> 8) & 0xff) + "." + String.valueOf(id & 0xff);
    }

    public String getHostStoragePrivateIp2() {
        int id = _instanceId;
        id |= 3 << 14;

        return "172.16." + String.valueOf((id >> 8) & 0xff) + "." + String.valueOf((id) & 0xff);
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
