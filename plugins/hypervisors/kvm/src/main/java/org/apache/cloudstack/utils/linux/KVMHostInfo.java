// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.linux;

import com.cloud.hypervisor.kvm.resource.LibvirtCapXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.utils.script.Script;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class KVMHostInfo {

    private static final Logger LOGGER = Logger.getLogger(KVMHostInfo.class);

    private int cpus;
    private int cpusockets;
    private long cpuSpeed;
    private long totalMemory;
    private long reservedMemory;
    private long overCommitMemory;
    private List<String> capabilities = new ArrayList<>();

    private static String cpuInfoFreqFileName = "/sys/devices/system/cpu/cpu0/cpufreq/base_frequency";

    public KVMHostInfo(long reservedMemory, long overCommitMemory, long manualSpeed) {
        this.cpuSpeed = manualSpeed;
        this.reservedMemory = reservedMemory;
        this.overCommitMemory = overCommitMemory;
        this.getHostInfoFromLibvirt();
        this.totalMemory = new MemStat(this.getReservedMemory(), this.getOverCommitMemory()).getTotal();
    }

    public int getCpus() {
        return this.cpus;
    }

    public int getCpuSockets() {
        return this.cpusockets;
    }

    public long getCpuSpeed() {
        return this.cpuSpeed;
    }

    public long getTotalMemory() {
        return this.totalMemory;
    }

    public long getReservedMemory() {
        return this.reservedMemory;
    }

    public long getOverCommitMemory() {
        return this.overCommitMemory;
    }

    public List<String> getCapabilities() {
        return this.capabilities;
    }

    protected static long getCpuSpeed(final NodeInfo nodeInfo) {
        long speed = 0L;
        speed = getCpuSpeedFromCommandLscpu();
        if(speed > 0L) {
            return speed;
        }

        speed = getCpuSpeedFromFile();
        if(speed > 0L) {
            return speed;
        }

        LOGGER.info(String.format("Using the value [%s] provided by Libvirt.", nodeInfo.mhz));
        speed = nodeInfo.mhz;
        return speed;
    }

    private static long getCpuSpeedFromCommandLscpu() {
        try {
            LOGGER.info("Fetching CPU speed from command \"lscpu\".");
            String command = "lscpu | grep -i 'Model name' | head -n 1 | egrep -o '[[:digit:]].[[:digit:]]+GHz' | sed 's/GHz//g'";
            String result = Script.runSimpleBashScript(command);
            long speed = (long) (Float.parseFloat(result) * 1000);
            LOGGER.info(String.format("Command [%s] resulted in the value [%s] for CPU speed.", command, speed));
            return speed;
        } catch (NullPointerException | NumberFormatException e) {
            LOGGER.error(String.format("Unable to retrieve the CPU speed from lscpu."), e);
            return 0L;
        }
    }

    private static long getCpuSpeedFromFile() {
        LOGGER.info(String.format("Fetching CPU speed from file [%s].", cpuInfoFreqFileName));
        try (Reader reader = new FileReader(cpuInfoFreqFileName)) {
            Long cpuInfoFreq = Long.parseLong(IOUtils.toString(reader).trim());
            LOGGER.info(String.format("Retrieved value [%s] from file [%s]. This corresponds to a CPU speed of [%s] MHz.", cpuInfoFreq, cpuInfoFreqFileName, cpuInfoFreq / 1000));
            return cpuInfoFreq / 1000;
        } catch (IOException | NumberFormatException e) {
            LOGGER.error(String.format("Unable to retrieve the CPU speed from file [%s]", cpuInfoFreqFileName), e);
            return 0L;
        }
    }

    private void getHostInfoFromLibvirt() {
        try {
            final Connect conn = LibvirtConnection.getConnection();
            final NodeInfo hosts = conn.nodeInfo();
            if (this.cpuSpeed == 0) {
                this.cpuSpeed = getCpuSpeed(hosts);
            } else {
                LOGGER.debug(String.format("Using existing configured CPU frequency %s", this.cpuSpeed));
            }

            /*
             * Some CPUs report a single socket and multiple NUMA cells.
             * We need to multiply them to get the correct socket count.
             */
            this.cpusockets = hosts.sockets;
            if (hosts.nodes > 0) {
                this.cpusockets = hosts.sockets * hosts.nodes;
            }
            this.cpus = hosts.cpus;

            final LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
            parser.parseCapabilitiesXML(conn.getCapabilities());
            final ArrayList<String> oss = parser.getGuestOsType();
            for (final String s : oss) {
                /*
                 * Even host supports guest os type more than hvm, we only
                 * report hvm to management server
                 */
                String hvmCapability = "hvm";
                if (s.equalsIgnoreCase(hvmCapability)) {
                    if (!this.capabilities.contains(hvmCapability)) {
                        this.capabilities.add(hvmCapability);
                    }
                }
            }

            /*
                Any modern Qemu/KVM supports snapshots
                We used to check if this was supported, but that is no longer required
            */
            this.capabilities.add("snapshot");
            conn.close();
        } catch (final LibvirtException e) {
            LOGGER.error("Caught libvirt exception while fetching host information", e);
        }
    }
}
