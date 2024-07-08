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

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Ternary;

@ResourceWrapper(handles =  OvsFetchInterfaceCommand.class)
public final class LibvirtOvsFetchInterfaceCommandWrapper extends CommandWrapper<OvsFetchInterfaceCommand, Answer, LibvirtComputingResource> {


    private String getSubnetMaskForAddress(NetworkInterface networkInterface, InetAddress inetAddress) {
        for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            if (!inetAddress.equals(address.getAddress())) {
                continue;
            }
            int prefixLength = address.getNetworkPrefixLength();
            int mask = 0xffffffff << (32 - prefixLength);
            return String.format("%d.%d.%d.%d",
                    (mask >>> 24) & 0xff,
                    (mask >>> 16) & 0xff,
                    (mask >>> 8) & 0xff,
                    mask & 0xff);
        }
        return "";
    }

    private String getMacAddress(NetworkInterface networkInterface) throws SocketException {
        byte[] macBytes = networkInterface.getHardwareAddress();
        if (macBytes == null) {
            return "";
        }
        StringBuilder macAddress = new StringBuilder();
        for (byte b : macBytes) {
            macAddress.append(String.format("%02X:", b));
        }
        if (macAddress.length() > 0) {
            macAddress.deleteCharAt(macAddress.length() - 1);  // Remove trailing colon
        }
        return macAddress.toString();
    }

    public Ternary<String, String, String> getInterfaceDetails(String interfaceName) throws SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
        if (networkInterface == null) {
            logger.warn(String.format("Network interface: '%s' not found", interfaceName));
            return new Ternary<>(null, null, null);
        }
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            if (inetAddress instanceof java.net.Inet4Address) {
                String ipAddress = inetAddress.getHostAddress();
                String subnetMask = getSubnetMaskForAddress(networkInterface, inetAddress);
                String macAddress = getMacAddress(networkInterface);
                return new Ternary<>(ipAddress, subnetMask, macAddress);
            }
        }
        return new Ternary<>(null, null, null);
    }

    @Override
    public Answer execute(final OvsFetchInterfaceCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String label = "'" + command.getLabel() + "'";

        logger.debug("Will look for network with name-label:" + label);
        try {
            Ternary<String, String, String> interfaceDetails = getInterfaceDetails(label);
            return new OvsFetchInterfaceAnswer(command, true, "Interface " + label
                    + " retrieved successfully", interfaceDetails.first(), interfaceDetails.second(),
                    interfaceDetails.third());

        } catch (final Exception e) {
            logger.warn("Caught execption when fetching interface", e);
            return new OvsFetchInterfaceAnswer(command, false, "EXCEPTION:"
                    + e.getMessage());
        }
    }
}
