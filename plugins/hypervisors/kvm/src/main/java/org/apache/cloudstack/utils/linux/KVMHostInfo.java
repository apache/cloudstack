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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.script.Script;

public class KVMHostInfo {

    protected static Logger LOGGER = LogManager.getLogger(KVMHostInfo.class);

    private int totalCpus;
    private int allocatableCpus;
    private int cpusockets;
    private long cpuSpeed;
    private long cpuMaxSpeed;
    private String cpuArch;
    private long totalMemory;
    private long reservedMemory;
    private long overCommitMemory;
    private List<String> capabilities = new ArrayList<>();
    private static final String LSCPU_CPU_INFO_COMMAND = "lscpu | grep -iE 'Model name|CPU max MHz|CPU dynamic MHz'";
    private static final String LSCPU_MODEL_NAME_FIELD = "Model name";
    private static final String LSCPU_CPU_MAX_MHZ_FIELD = "CPU max MHz";
    private static final String LSCPU_CPU_DYNAMIC_MHZ_FIELD = "CPU dynamic MHz";
    private static final Pattern LSCPU_MODEL_NAME_FREQUENCY_PATTERN = Pattern.compile("([0-9]\\.[0-9]+)GHz");
    private static final int MHZ_PER_GHZ = 1000;
    private static final String cpuArchRetrieveExecutable = "arch";
    private static final List<String> cpuInfoFreqFileNames = List.of("/sys/devices/system/cpu/cpu0/cpufreq/base_frequency","/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");

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

    /**
     * @return the maximum frequency of the CPU, in MHz, which may include the turbo frequency; or 0 if it could not be retrieved. This value is only
     * informative; {@link #getCpuSpeed()} is the one used to calculate the capacity of the host.
     */
    public long getCpuMaxSpeed() {
        return this.cpuMaxSpeed;
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

    public String getCpuArch() {
        return cpuArch;
    }

    public static boolean isHostS390x() {
       return "s390x".equals(System.getProperty("os.arch"));
    }

    protected static long getCpuSpeed(final String lscpuOutput, final String capabilities, final NodeInfo nodeInfo) {
        long speed = getBaseCpuSpeedFromLscpuOutput(lscpuOutput);
        if (speed > 0L) {
            return speed;
        }

        speed = getCpuSpeedFromFile();
        if (speed > 0L) {
            return speed;
        }

        speed = getCpuMaxSpeedFromLscpuOutput(lscpuOutput);
        if (speed > 0L) {
            return speed;
        }

        speed = getCpuSpeedFromHostCapabilities(capabilities);
        if (speed > 0L) {
            return speed;
        }

        LOGGER.info("Using the value [{}] provided by Libvirt.", nodeInfo.mhz);
        return nodeInfo.mhz;
    }

    /**
     * Retrieves the base CPU speed from the output of the {@code lscpu} command. The frequency advertised in the model name is preferred because the CPU speed
     * is used to calculate the capacity of the host and the CPU quota of the VMs with CPU cap enabled; thus, it must be a frequency that the CPU is able to
     * sustain. On s390x, the "CPU dynamic MHz" value is used instead.
     *
     * @return the base CPU speed, in MHz, or 0 if it could not be retrieved.
     */
    protected static long getBaseCpuSpeedFromLscpuOutput(final String lscpuOutput) {
        return getBaseCpuSpeedFromLscpuOutput(lscpuOutput, isHostS390x());
    }

    /**
     * @param lscpuOutput the output of the {@code lscpu} command.
     * @param isS390x whether the host CPU architecture is s390x; on s390x the base speed is read from the "CPU dynamic MHz" field.
     * @return the base CPU speed, in MHz, or 0 if it could not be retrieved.
     */
    protected static long getBaseCpuSpeedFromLscpuOutput(final String lscpuOutput, final boolean isS390x) {
        if (isS390x) {
            final long speed = (long) NumbersUtil.parseFloat(getLscpuFieldValue(lscpuOutput, LSCPU_CPU_DYNAMIC_MHZ_FIELD), 0f);
            logCpuSpeedFromLscpuField(LSCPU_CPU_DYNAMIC_MHZ_FIELD, speed);
            return speed;
        }
        final String modelName = getLscpuFieldValue(lscpuOutput, LSCPU_MODEL_NAME_FIELD);
        final Matcher matcher = LSCPU_MODEL_NAME_FREQUENCY_PATTERN.matcher(StringUtils.defaultString(modelName));
        if (matcher.find()) {
            final long speed = (long) (NumbersUtil.parseFloat(matcher.group(1), 0f) * MHZ_PER_GHZ);
            logCpuSpeedFromLscpuField(LSCPU_MODEL_NAME_FIELD, speed);
            return speed;
        }
        logCpuSpeedFromLscpuField(LSCPU_MODEL_NAME_FIELD, 0L);
        return 0L;
    }

    /**
     * @return the maximum frequency of the CPU, in MHz, from the output of the {@code lscpu} command; or 0 if it could not be retrieved. This value may
     * include the turbo frequency; it is only informative.
     */
    protected static long getCpuMaxSpeedFromLscpuOutput(final String lscpuOutput) {
        final long speed = (long) NumbersUtil.parseFloat(getLscpuFieldValue(lscpuOutput, LSCPU_CPU_MAX_MHZ_FIELD), 0f);
        logCpuSpeedFromLscpuField(LSCPU_CPU_MAX_MHZ_FIELD, speed);
        return speed;
    }

    private static void logCpuSpeedFromLscpuField(final String field, final long speed) {
        if (speed > 0L) {
            LOGGER.info("Retrieved the CPU speed [{}] MHz from the \"{}\" field of the lscpu command output.", speed, field);
        } else {
            LOGGER.debug("Unable to retrieve the CPU speed from the \"{}\" field of the lscpu command output.", field);
        }
    }

    private static String getLscpuFieldValue(final String lscpuOutput, final String field) {
        if (StringUtils.isBlank(lscpuOutput)) {
            return null;
        }
        for (final String line : lscpuOutput.split("\n")) {
            if (StringUtils.startsWithIgnoreCase(line.trim(), field)) {
                return StringUtils.substringAfter(line, ":").trim();
            }
        }
        return null;
    }

    private static String getLscpuOutput() {
        LOGGER.info("Fetching CPU speed from command \"lscpu\".");
        return Script.executeCommand(LSCPU_CPU_INFO_COMMAND).first();
    }

    protected static long getCpuSpeedFromFile() {
        for (final String cpuInfoFreqFileName:  cpuInfoFreqFileNames) {
            LOGGER.info(String.format("Fetching CPU speed from file [%s].", cpuInfoFreqFileName));
            try (Reader reader = new FileReader(cpuInfoFreqFileName)) {
                Long cpuInfoFreq = Long.parseLong(IOUtils.toString(reader).trim());
                LOGGER.info(String.format("Retrieved value [%s] from file [%s]. This corresponds to a CPU speed of [%s] MHz.", cpuInfoFreq, cpuInfoFreqFileName, cpuInfoFreq / 1000));
                return cpuInfoFreq / 1000;
            } catch (IOException | NumberFormatException e) {
                LOGGER.error(String.format("Unable to retrieve the CPU speed from file [%s]", cpuInfoFreqFileName), e);
            }
        }
        return 0L;
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
            final String lscpuOutput = getLscpuOutput();
            if (this.cpuSpeed == 0) {
                this.cpuSpeed = getCpuSpeed(lscpuOutput, capabilities, hosts);
            } else {
                LOGGER.debug(String.format("Using existing configured CPU frequency %s", this.cpuSpeed));
            }
            this.cpuMaxSpeed = getCpuMaxSpeedFromLscpuOutput(lscpuOutput);

            /*
             * Some CPUs report a single socket and multiple NUMA cells.
             * We need to multiply them to get the correct socket count.
             */
            this.cpusockets = hosts.sockets;
            if (hosts.nodes > 0) {
                this.cpusockets = hosts.sockets * hosts.nodes;
            }
            this.totalCpus = hosts.cpus;
            this.cpuArch = getCPUArchFromCommand();

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
        } catch (final LibvirtException e) {
            LOGGER.error("Caught libvirt exception while fetching host information", e);
        }
    }

    private String getCPUArchFromCommand() {
        LOGGER.info("Fetching host CPU arch");
        return Script.runSimpleBashScript(Script.getExecutableAbsolutePath(cpuArchRetrieveExecutable));
    }
}
