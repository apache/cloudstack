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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.network.packetcapture.PacketCaptureAnswer;
import org.apache.cloudstack.network.packetcapture.PacketCaptureCommand;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

/**
 * Starts, stops or queries the packet capture systemd unit
 * (cloudstack-pcap@&lt;interface&gt;.service by default) for a NIC of a
 * running VM. Before starting the unit, the NIC context is written to an
 * environment file so the capture script can decide what and where to capture.
 */
@ResourceWrapper(handles = PacketCaptureCommand.class)
public class LibvirtPacketCaptureCommandWrapper extends CommandWrapper<PacketCaptureCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(PacketCaptureCommand command, LibvirtComputingResource resource) {
        if (StringUtils.isBlank(command.getVmName()) || StringUtils.isBlank(command.getMacAddress())) {
            return new PacketCaptureAnswer(command, false, "VM name and NIC MAC address are required", false);
        }

        InterfaceDef nicDevice = resolveNicDevice(command, resource);
        String unit = getUnitName(nicDevice);

        switch (command.getAction()) {
        case START:
            if (nicDevice == null) {
                return new PacketCaptureAnswer(command, false, String.format(
                        "no interface with MAC address %s found on a running domain %s", command.getMacAddress(), command.getVmName()), false);
            }
            return start(command, nicDevice, unit);
        case STOP:
            if (nicDevice == null) {
                // The VM is not running (anymore) on this host; the unit died with the tap device.
                return new PacketCaptureAnswer(command, true, "no running capture found", false);
            }
            return stop(command, nicDevice, unit);
        case STATUS:
            boolean running = nicDevice != null && systemctl("is-active", "--quiet", unit) == null;
            return new PacketCaptureAnswer(command, true, null, running);
        default:
            return new PacketCaptureAnswer(command, false, "unknown action " + command.getAction(), false);
        }
    }

    private Answer start(PacketCaptureCommand command, InterfaceDef nicDevice, String unit) {
        try {
            writeEnvironmentFile(command, nicDevice);
        } catch (IOException e) {
            logger.error("Failed to write packet capture environment file for NIC {} of VM {}", nicDevice.getDevName(), command.getVmName(), e);
            return new PacketCaptureAnswer(command, false, "failed to write environment file: " + e.getMessage(), false);
        }
        String result = systemctl("start", unit);
        if (result != null) {
            return new PacketCaptureAnswer(command, false, String.format("failed to start %s: %s", unit, result), false);
        }
        logger.info("Started packet capture unit {} for VM {}", unit, command.getVmName());
        return new PacketCaptureAnswer(command, true, null, true);
    }

    private Answer stop(PacketCaptureCommand command, InterfaceDef nicDevice, String unit) {
        String result = systemctl("stop", unit);
        if (result != null) {
            return new PacketCaptureAnswer(command, false, String.format("failed to stop %s: %s", unit, result), true);
        }
        try {
            Files.deleteIfExists(getEnvironmentFile(nicDevice.getDevName()));
        } catch (IOException e) {
            logger.warn("Failed to delete packet capture environment file for {}", nicDevice.getDevName(), e);
        }
        logger.info("Stopped packet capture unit {} for VM {}", unit, command.getVmName());
        return new PacketCaptureAnswer(command, true, null, false);
    }

    /**
     * Finds the host-side interface of the NIC by matching the MAC address on
     * the running domain. Returns null when the domain is not running on this
     * host or has no interface with the MAC address.
     */
    private InterfaceDef resolveNicDevice(PacketCaptureCommand command, LibvirtComputingResource resource) {
        try {
            Connect conn = resource.getLibvirtUtilitiesHelper().getConnectionByVmName(command.getVmName());
            for (InterfaceDef iface : resource.getInterfaces(conn, command.getVmName())) {
                if (command.getMacAddress().equalsIgnoreCase(iface.getMacAddress())) {
                    return iface;
                }
            }
        } catch (LibvirtException e) {
            logger.debug("Unable to look up interfaces of VM {}: {}", command.getVmName(), e.getMessage());
        }
        return null;
    }

    private String getUnitName(InterfaceDef nicDevice) {
        String service = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.PACKET_CAPTURE_SERVICE);
        return String.format("%s@%s.service", service, nicDevice == null ? "" : nicDevice.getDevName());
    }

    private Path getEnvironmentFile(String deviceName) {
        String envDir = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.PACKET_CAPTURE_ENV_DIR);
        return Paths.get(envDir, String.format("pcap-%s.env", deviceName));
    }

    private void writeEnvironmentFile(PacketCaptureCommand command, InterfaceDef nicDevice) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("CS_VM_NAME=" + StringUtils.defaultString(command.getVmName()));
        lines.add("CS_VM_UUID=" + StringUtils.defaultString(command.getVmUuid()));
        lines.add("CS_NIC_UUID=" + StringUtils.defaultString(command.getNicUuid()));
        lines.add("CS_NIC_MAC=" + StringUtils.defaultString(command.getMacAddress()));
        lines.add("CS_NIC_DEV=" + StringUtils.defaultString(nicDevice.getDevName()));
        lines.add("CS_NIC_BRIDGE=" + StringUtils.defaultString(nicDevice.getBrName()));
        lines.add("CS_NIC_IP4=" + StringUtils.defaultString(command.getIp4Address()));
        lines.add("CS_NIC_IP6=" + StringUtils.defaultString(command.getIp6Address()));
        lines.add("CS_NETWORK_UUID=" + StringUtils.defaultString(command.getNetworkUuid()));

        Path file = getEnvironmentFile(nicDevice.getDevName());
        Files.createDirectories(file.getParent());
        Files.write(file, lines);
    }

    /**
     * Runs systemctl with the given arguments. Returns null on success, the
     * error message otherwise (Script semantics).
     */
    private String systemctl(String... args) {
        Script script = new Script("/bin/systemctl", logger);
        for (String arg : args) {
            script.add(arg);
        }
        return script.execute();
    }
}
