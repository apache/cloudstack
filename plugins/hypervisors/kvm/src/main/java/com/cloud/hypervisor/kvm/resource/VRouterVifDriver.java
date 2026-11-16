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
package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.script.Script;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;
import org.libvirt.LibvirtException;

import java.io.File;
import java.util.Map;

import javax.naming.ConfigurationException;

public class VRouterVifDriver extends VifDriverBase {
    protected Logger logger = LogManager.getLogger(getClass());
    private String createTapDeviceScript;
    private String deleteTapDeviceScript;

    @Override
    public void configure(final Map<String, Object> params) throws ConfigurationException {

        super.configure(params);

        String tungstenScriptsDir = (String) params.get("tungsten.scripts.dir");
        tungstenScriptsDir = tungstenScriptsDir == null ? _libvirtComputingResource.getDefaultTungstenScriptsDir() : tungstenScriptsDir;

        createTapDeviceScript = Script.findScript(tungstenScriptsDir, "create_tap_device.sh");
        deleteTapDeviceScript = Script.findScript(tungstenScriptsDir, "delete_tap_device.sh");

        if (createTapDeviceScript == null) {
            throw new ConfigurationException("Unable to find create_tap_device.sh");
        }

        if (deleteTapDeviceScript == null) {
            throw new ConfigurationException("Unable to find delete_tap_device.sh");
        }
    }

    @Override
    public LibvirtVMDef.InterfaceDef plug(final NicTO nic, final String guestOsType, final String nicAdapter,
        final Map<String, String> extraConfig) throws InternalErrorException, LibvirtException {

        final String tapDeviceName = TungstenUtils.getTapName(nic.getMac());
        final String script = createTapDeviceScript;

        final Script command = new Script(script, Duration.standardSeconds(300), logger);
        command.add(tapDeviceName);

        final String result = command.execute();
        if (result != null) {
            throw new InternalErrorException("Failed to create tap device " + tapDeviceName + ": " + result);
        }

        final LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.defEthernet(tapDeviceName, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter));

        return intf;
    }

    @Override
    public void unplug(final LibvirtVMDef.InterfaceDef iface, boolean deleteBr) {
        final String tapDeviceName = TungstenUtils.getTapName(iface.getMacAddress());
        final String script = deleteTapDeviceScript;

        final Script command = new Script(script, Duration.standardSeconds(300), logger);
        command.add(tapDeviceName);

        final String result = command.execute();
        if (result != null) {
            logger.error("Failed to delete tap device " + tapDeviceName + ": " + result);
        }
    }

    @Override
    public void attach(final LibvirtVMDef.InterfaceDef iface) {
        // not use
    }

    @Override
    public void detach(final LibvirtVMDef.InterfaceDef iface) {
        // not use
    }

    @Override
    public void createControlNetwork(final String privBrName) {
        // not use
    }

    @Override
    public boolean isExistingBridge(String bridgeName) {
        File f = new File("/sys/devices/virtual/net/" + bridgeName);
        File bridge = new File("/sys/devices/virtual/net/" + bridgeName + "/bridge");
        return f.exists() && ! bridge.exists();
    }

    @Override
    public void deleteBr(NicTO nic) {
        // not use
    }
}
