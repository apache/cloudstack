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
package org.apache.cloudstack.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.command.admin.network.AddNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkDeviceCmd;
import org.apache.cloudstack.api.command.admin.network.ListNetworkDeviceCmd;
import org.apache.cloudstack.api.response.NetworkDeviceResponse;

import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.utils.component.Manager;

public interface ExternalNetworkDeviceManager extends Manager {

    public static class NetworkDevice {
        private String _name;
        private String _provider;
        private static List<NetworkDevice> supportedNetworkDevices = new ArrayList<NetworkDevice>();

        public static final NetworkDevice ExternalDhcp = new NetworkDevice("ExternalDhcp", null);
        public static final NetworkDevice PxeServer = new NetworkDevice("PxeServer", null);
        public static final NetworkDevice NetscalerMPXLoadBalancer = new NetworkDevice("NetscalerMPXLoadBalancer", Network.Provider.Netscaler.getName());
        public static final NetworkDevice NetscalerVPXLoadBalancer = new NetworkDevice("NetscalerVPXLoadBalancer", Network.Provider.Netscaler.getName());
        public static final NetworkDevice NetscalerSDXLoadBalancer = new NetworkDevice("NetscalerSDXLoadBalancer", Network.Provider.Netscaler.getName());
        public static final NetworkDevice F5BigIpLoadBalancer = new NetworkDevice("F5BigIpLoadBalancer", Network.Provider.F5BigIp.getName());
        public static final NetworkDevice JuniperSRXFirewall = new NetworkDevice("JuniperSRXFirewall", Network.Provider.JuniperSRX.getName());
        public static final NetworkDevice PaloAltoFirewall = new NetworkDevice("PaloAltoFirewall", Network.Provider.PaloAlto.getName());
        public static final NetworkDevice NiciraNvp = new NetworkDevice("NiciraNvp", Network.Provider.NiciraNvp.getName());
        public static final NetworkDevice CiscoVnmc = new NetworkDevice("CiscoVnmc", Network.Provider.CiscoVnmc.getName());
        public static final NetworkDevice OpenDaylightController = new NetworkDevice("OpenDaylightController", Network.Provider.Opendaylight.getName());
        public static final NetworkDevice BrocadeVcs = new NetworkDevice("BrocadeVcs", Network.Provider.BrocadeVcs.getName());
        public static final NetworkDevice GloboDns = new NetworkDevice("GloboDns", Network.Provider.GloboDns.getName());

        public NetworkDevice(String deviceName, String ntwkServiceprovider) {
            _name = deviceName;
            _provider = ntwkServiceprovider;
            supportedNetworkDevices.add(this);
        }

        public String getName() {
            return _name;
        }

        public String getNetworkServiceProvder() {
            return _provider;
        }

        public static NetworkDevice getNetworkDevice(String devicerName) {
            for (NetworkDevice device : supportedNetworkDevices) {
                if (device.getName().equalsIgnoreCase(devicerName)) {
                    return device;
                }
            }
            return null;
        }
    }

    public Host addNetworkDevice(AddNetworkDeviceCmd cmd);

    public NetworkDeviceResponse getApiResponse(Host device);

    public List<Host> listNetworkDevice(ListNetworkDeviceCmd cmd);

    public boolean deleteNetworkDevice(DeleteNetworkDeviceCmd cmd);

}
