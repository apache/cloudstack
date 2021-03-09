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

    private static String cpuInfoMaxFreqFileName = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";

    public KVMHostInfo(long reservedMemory, long overCommitMemory) {
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
        try (Reader reader = new FileReader(cpuInfoMaxFreqFileName)) {
            Long cpuInfoMaxFreq = Long.parseLong(IOUtils.toString(reader).trim());
            LOGGER.info(String.format("Retrieved value [%s] from file [%s]. This corresponds to a CPU speed of [%s] MHz.", cpuInfoMaxFreq, cpuInfoMaxFreqFileName, cpuInfoMaxFreq / 1000));
            return cpuInfoMaxFreq / 1000;
        } catch (IOException | NumberFormatException e) {
            LOGGER.error(String.format("Unable to retrieve the CPU speed from file [%s]. Using the value [%s] provided by the Libvirt.", cpuInfoMaxFreqFileName, nodeInfo.mhz), e);
            return nodeInfo.mhz;
        }
    }

    private void getHostInfoFromLibvirt() {
        try {
            final Connect conn = LibvirtConnection.getConnection();
            final NodeInfo hosts = conn.nodeInfo();
            this.cpuSpeed = getCpuSpeed(hosts);

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
