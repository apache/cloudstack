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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloud.hypervisor.kvm.resource.LibvirtCapXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.utils.script.Script;

public class KVMHostInfo {

    protected static Logger LOGGER = LogManager.getLogger(KVMHostInfo.class);

    private int totalCpus;
    private int allocatableCpus;
    private int cpusockets;
    private long cpuSpeed;
    private long totalMemory;
    private long reservedMemory;
    private long overCommitMemory;
    private List<String> capabilities = new ArrayList<>();

    private static String cpuInfoFreqFileName = "/sys/devices/system/cpu/cpu0/cpufreq/base_frequency";

    public KVMHostInfo(long reservedMemory, long overCommitMemory, long manualSpeed, int reservedCpus) {
        this.cpuSpeed = manualSpeed;
        this.reservedMemory = reservedMemory;
        this.overCommitMemory = overCommitMemory;
        this.getHostInfoFromLibvirt();
        this.totalMemory = new MemStat(this.getReservedMemory(), this.getOverCommitMemory()).getTotal();
        this.allocatableCpus = totalCpus - reservedCpus;
        if (allocatableCpus < 1) {
            LOGGER.warn(String.format("Aggressive reserved CPU config leaves no usable CPUs for VMs! Total system CPUs: %d, Reserved: %d, Allocatable: %d", totalCpus, reservedCpus, allocatableCpus));
            allocatableCpus = 0;
        }
    }

    public int getTotalCpus() {
        return this.totalCpus;
    }

    public int getAllocatableCpus() {
        return this.allocatableCpus;
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

    protected static long getCpuSpeed(final String cpabilities, final NodeInfo nodeInfo) {
        long speed = 0L;
        speed = getCpuSpeedFromCommandLscpu();
        if(speed > 0L) {
            return speed;
        }

        speed = getCpuSpeedFromFile();
        if(speed > 0L) {
            return speed;
        }

        speed = getCpuSpeedFromHostCapabilities(cpabilities);
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

    protected static long getCpuSpeedFromHostCapabilities(final String capabilities) {
        LOGGER.info("Fetching CPU speed from \"host capabilities\"");
        long speed = 0L;
        try {
            DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(capabilities)));
            Element rootElement = doc.getDocumentElement();
            NodeList nodes = rootElement.getElementsByTagName("cpu");
            Node node = nodes.item(0);
            nodes = ((Element)node).getElementsByTagName("counter");
            for (int i = 0; i < nodes.getLength(); i++) {
                node = nodes.item(i);
                NamedNodeMap attributes = node.getAttributes();
                Node nameNode = attributes.getNamedItem("name");
                Node freqNode = attributes.getNamedItem("frequency");
                if (nameNode != null && "tsc".equals(nameNode.getNodeValue()) && freqNode != null && StringUtils.isNotEmpty(freqNode.getNodeValue())) {
                    speed = Long.parseLong(freqNode.getNodeValue()) / 1000000;
                    LOGGER.info(String.format("Retrieved value [%s] from \"host capabilities\". This corresponds to a CPU speed of [%s] MHz.", freqNode.getNodeValue(), speed));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to fetch CPU speed from \"host capabilities\"", ex);
            speed = 0L;
        }
        return speed;
    }

    private void getHostInfoFromLibvirt() {
        try {
            final Connect conn = LibvirtConnection.getConnection();
            final NodeInfo hosts = conn.nodeInfo();
            final String capabilities = conn.getCapabilities();
            if (this.cpuSpeed == 0) {
                this.cpuSpeed = getCpuSpeed(capabilities, hosts);
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
            this.totalCpus = hosts.cpus;

            final LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
            parser.parseCapabilitiesXML(capabilities);
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
