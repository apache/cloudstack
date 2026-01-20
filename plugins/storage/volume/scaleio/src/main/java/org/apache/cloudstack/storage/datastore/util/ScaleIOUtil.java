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

package org.apache.cloudstack.storage.datastore.util;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.Pair;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScaleIOUtil {
    protected static Logger LOGGER = LogManager.getLogger(ScaleIOUtil.class);

    public static final String PROVIDER_NAME = "PowerFlex";

    // Use prefix for CloudStack resources
    public static final String VOLUME_PREFIX = "vol";
    public static final String TEMPLATE_PREFIX = "tmpl";
    public static final String SNAPSHOT_PREFIX = "snap";
    public static final String VMSNAPSHOT_PREFIX = "vmsnap";

    public static final int IDENTIFIER_LENGTH = 16;
    public static final Long MINIMUM_ALLOWED_IOPS_LIMIT = 10L;

    public static final String DISK_PATH = "/dev/disk/by-id";
    public static final String DISK_NAME_PREFIX = "emc-vol-";
    public static final String DISK_NAME_PREFIX_FILTER = DISK_NAME_PREFIX + "*-";

    private static final String AGENT_PROPERTIES_FILE = "/etc/cloudstack/agent/agent.properties";

    private static final String DEFAULT_SDC_HOME_PATH = "/opt/emc/scaleio/sdc";
    private static final String SDC_HOME_PARAMETER = "powerflex.sdc.home.dir";
    private static final String SDC_HOME_PATH = getSdcHomePath();

    private static final String RESCAN_CMD = "drv_cfg --rescan";

    private static final String SDC_SERVICE_STATUS_CMD = "systemctl status scini";
    private static final String SDC_SERVICE_START_CMD = "systemctl start scini";
    private static final String SDC_SERVICE_STOP_CMD = "systemctl stop scini";
    private static final String SDC_SERVICE_RESTART_CMD = "systemctl restart scini";

    private static final String SDC_SERVICE_IS_ACTIVE_CMD = "systemctl is-active scini";
    private static final String SDC_SERVICE_IS_ENABLED_CMD = "systemctl is-enabled scini";
    private static final String SDC_SERVICE_ENABLE_CMD = "systemctl enable scini";

    public static final String CONNECTED_SDC_COUNT_STAT = "ConnectedSDCCount";

    /**
     * Time (in seconds) to wait after SDC service 'scini' start/restart/stop.<br>
     * Data type: Integer.<br>
     * Default value: <code>3</code>
     */
    public static final AgentProperties.Property<Integer> SDC_SERVICE_ACTION_WAIT = new AgentProperties.Property<>("powerflex.sdc.service.wait", 3);

    /**
     * Cmd for querying volumes in SDC
     * Sample output for cmd {@code drv_cfg --query_vols}:
     * Retrieved 2 volume(s)
     * VOL-ID 6c33633100000009 MDM-ID 218ce1797566a00f
     * VOL-ID 6c3362a30000000a MDM-ID 218ce1797566a00f
     */
    private static final String QUERY_VOLUMES_CMD = "drv_cfg --query_vols";

    /**
     * Cmd for querying guid in SDC
     * Sample output for cmd {@code drv_cfg --query_guid}:
     * B0E3BFB8-C20B-43BF-93C8-13339E85AA50
     */
    private static final String QUERY_GUID_CMD = "drv_cfg --query_guid";

    /**
     * Cmd for querying MDMs in SDC
     * Sample output for cmd {@code drv_cfg --query_mdms}:
     * Retrieved 2 mdm(s)
     * MDM-ID 3ef46cbf2aaf5d0f SDC ID 6b18479c00000003 INSTALLATION ID 68ab55462cbb3ae4 IPs [0]-x.x.x.x [1]-x.x.x.x
     * MDM-ID 2e706b2740ec200f SDC ID 301b852c00000003 INSTALLATION ID 33f8662e7a5c1e6c IPs [0]-x.x.x.x [1]-x.x.x.x
     */
    private static final String QUERY_MDMS_CMD = "drv_cfg --query_mdms";

    private static final String ADD_MDMS_CMD = "drv_cfg --add_mdm";

    private static final String REMOVE_MDM_PARAMETER = "--remove_mdm";

    /**
     * Calls the kernel module to remove MDM.
     */
    private static final String REMOVE_MDMS_CMD = "drv_cfg " + REMOVE_MDM_PARAMETER;

    /**
     * Command to get back "Usage" response. As of now it is just drv_cfg without parameters.
     */
    private static final String USAGE_CMD = "drv_cfg";

    private static final String DRV_CFG_FILE = "/etc/emc/scaleio/drv_cfg.txt";

    /**
     * Sample Command - sed -i '/x.x.x.x\,/d' /etc/emc/scaleio/drv_cfg.txt
     */
    private static final String REMOVE_MDM_CMD_TEMPLATE = "sed -i '/%s\\,/d' %s";

    /**
     * Patterns to parse {@link ScaleIOUtil#DRV_CFG_FILE} and {@code --query_mdms} command output.
     * The format is:
     * MDM-ID {HEX_ID} SDC ID {HEX_ID} INSTALLATION ID {HEX_ID} IPs [{DEC_INC_NUMBER}]-{IP_ADDRESS} [{DEC_INC_NUMBER}]-{IP_ADDRESS} ...
     */
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\\r?\\n");
    /**
     * Pattern to find "IPs" substring in {@link ScaleIOUtil#QUERY_MDMS_CMD} command output.
     * The output format is:
     * MDM-ID {HEX_ID} SDC ID {HEX_ID} INSTALLATION ID {HEX_ID} IPs [{DEC_INC_NUMBER}]-{IP_ADDRESS} [{DEC_INC_NUMBER}]-{IP_ADDRESS} ...
     */
    private static final Pattern USAGE_IPS_LINE_PATTERN = Pattern.compile("IPs\\s*(.*)$");
    /**
     * Pattern to find individual IP address in {@link ScaleIOUtil#QUERY_MDMS_CMD} command output.
     */
    private static final Pattern USAGE_IP_TOKEN_PATTERN = Pattern.compile("(\\s*\\[\\d\\]\\-)([^\\s$]+)");

    /**
     * Pattern to find Volume ID in {@link  ScaleIOUtil#QUERY_VOLUMES_CMD}
     */
    private static final Pattern VOLUME_ID_TOKEN_PATTERN = Pattern.compile("VOL-ID\\s*([^\\s$]+)");
    /**
     * Pattern to find MDM entries line in {@link ScaleIOUtil#DRV_CFG_FILE}.
     */
    private static final Pattern DRV_CFG_MDM_LINE_PATTERN = Pattern.compile("^mdm\\s*([0-9A-F:\\.,]+)$", Pattern.CASE_INSENSITIVE);
    /**
     * Pattern to split comma separated string of IP addresses (space aware).
     */
    private static final Pattern DRV_CFG_MDM_IPS_PATTERN = Pattern.compile("\\s*,\\s*");

    public static boolean addMdms(String... mdmAddresses) {
        if (mdmAddresses.length < 1) {
            return false;
        }
        // Sample Cmd - /opt/emc/scaleio/sdc/bin/drv_cfg --add_mdm --ip x.x.x.x,x.x.x.x --file /etc/emc/scaleio/drv_cfg.txt
        String command = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.ADD_MDMS_CMD;
        command += " --ip " + String.join(",", mdmAddresses);
        command += " --file " + DRV_CFG_FILE;
        return runCmd(command);
    }

    /**
     * Remove MDM via ScaleIO via CLI.
     *
     * @param mdmAddress MDM address to remove
     * @return true if IP address successfully removed
     */
    private static boolean removeMdm(String mdmAddress) {
        // Sample Cmd - /opt/emc/scaleio/sdc/bin/drv_cfg --remove_mdm --ip x.x.x.x,x.x.x.x --file /etc/emc/scaleio/drv_cfg.txt
        String command = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.REMOVE_MDMS_CMD;
        command += " --ip " + String.join(",", mdmAddress);
        command += " --file " + DRV_CFG_FILE;
        return runCmd(command);
    }

    /**
     * Run command, log command result and return {@link Boolean#TRUE} if command succeeded.
     * FIXME: may need to do refactoring and replace static method calls with dynamic.
     */
    private static boolean runCmd(String command) {
        Pair<String, String> result = Script.executeCommand(command);
        String stdOut = result.first();
        String stdErr = result.second();
        boolean succeeded = StringUtils.isEmpty(stdErr);
        if (succeeded) {
            LOGGER.debug(String.format("Successfully executed command '%s': %s", command, stdOut));
        } else {
            LOGGER.warn(String.format("Failed to execute command '%s': %s", command, stdErr));
        }
        return succeeded;
    }

    /**
     * Remove MDMs either via ScaleIO CLI (if supported) or by updating configuration file.
     *
     * @param mdmAddresses MDM addresses
     * @return returns {@link Boolean#TRUE} if changes were applied to the configuration
     */
    public static boolean removeMdms(String... mdmAddresses) {
        if (mdmAddresses.length < 1) {
            return false;
        }

        boolean changesApplied = false;
        boolean removeMdmCliSupported = isRemoveMdmCliSupported();
        boolean restartSDC = false;
        for (String mdmAddress : mdmAddresses) {
            // continue to next address if current MDM is not present in configuration
            if (!isMdmPresent(mdmAddress)) {
                continue;
            }
            // remove MDM via CLI if it is supported
            if (removeMdmCliSupported) {
                if (removeMdm(mdmAddress)) {
                    changesApplied = true;
                }
            } else {
                String command = String.format(REMOVE_MDM_CMD_TEMPLATE, mdmAddress, DRV_CFG_FILE);
                String stdErr = Script.executeCommand(command).second();
                if(StringUtils.isEmpty(stdErr)) {
                    // restart SDC needed only if configuration file modified manually (not by CLI)
                    restartSDC = true;
                    changesApplied = true;
                } else {
                    LOGGER.error(String.format("Failed to remove MDM %s from %s: %s", mdmAddress, DRV_CFG_FILE, stdErr));
                }
            }
        }
        if (restartSDC) {
            restartSDCService();
        }
        return changesApplied;
    }

    /**
     * Returns MDM entries from {@link ScaleIOUtil#DRV_CFG_FILE}.
     */
    public static Collection<String> getMdmsFromConfigFile() {
        List<String> configFileLines;
        try {
            configFileLines = Files.readAllLines(Path.of(DRV_CFG_FILE));
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to read MDMs from %s", DRV_CFG_FILE), e);
            return List.of();
        }
        Set<String> mdms = new LinkedHashSet<>();
        for (String line : configFileLines) {
            Matcher mdmLineMatcher = DRV_CFG_MDM_LINE_PATTERN.matcher(line);
            if(mdmLineMatcher.find() && mdmLineMatcher.groupCount() > 0) {
                String mdmLine = mdmLineMatcher.group(1);
                String[] mdmValues = DRV_CFG_MDM_IPS_PATTERN.split(mdmLine);
                mdms.addAll(Arrays.asList(mdmValues));
            }
        }
        return mdms;
    }
    /**
     * Returns Volume Ids from {@link ScaleIOUtil#DRV_CFG_FILE}.
     */
    public static Collection<String> getVolumeIds() {
        String command = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.QUERY_VOLUMES_CMD;
        Pair<String, String> result = Script.executeCommand(command);
        String stdOut = result.first();

        Set<String> volumeIds = new LinkedHashSet<>();
        String[] stdOutLines = NEW_LINE_PATTERN.split(stdOut);
        for (String line : stdOutLines) {
            Matcher volumeIdMatcher = VOLUME_ID_TOKEN_PATTERN.matcher(line);
            if (volumeIdMatcher.find() && volumeIdMatcher.groupCount() > 0) {
                volumeIds.add(volumeIdMatcher.group(1));
            }
        }
        return volumeIds;
    }

    /**
     * Returns MDM entries from CLI Cmd using {@code --query_mdms}.
     */
    public static Collection<String> getMdmsFromCliCmd() {
        String command = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.QUERY_MDMS_CMD;
        Pair<String, String> result = Script.executeCommand(command);
        String stdOut = result.first();

        Set<String> mdms = new LinkedHashSet<>();
        String[] stdOutLines = NEW_LINE_PATTERN.split(stdOut);
        for (String line : stdOutLines) {
            Matcher ipsLineMatcher = USAGE_IPS_LINE_PATTERN.matcher(line);
            if (ipsLineMatcher.find() && ipsLineMatcher.groupCount() > 0) {
                String ipToken = ipsLineMatcher.group(1);
                Matcher ipMatcher = USAGE_IP_TOKEN_PATTERN.matcher(ipToken);
                while (ipMatcher.find()) {
                    if (ipMatcher.groupCount() > 1) {
                        mdms.add(ipMatcher.group(2));
                    }
                }
            }
        }
        return mdms;
    }

    /**
     * Returns {@link Boolean#TRUE} if ScaleIO CLI tool (drv_cfg) supports MDMs removal.
     */
    public static boolean isRemoveMdmCliSupported() {
        /*
         * New version of drv_cfg supports remove mdm API.
         * Instead of defining supported version and checking it, the logic is to check drv_cfg "Usage" output
         * and see whether remove_mdm command supported.
         * The "Usage" returned if tool executed without parameters or with invalid parameters.
         */
        String command = SDC_HOME_PATH + "/bin/" + USAGE_CMD;

        Pair<String, String> result = Script.executeCommand(command);
        String stdOut = result.first();
        String stdErr = result.second();

        /*
         * Check whether stderr or stdout contains mdm removal "--remove_mdm" parameter.
         *
         * Current version returns "Usage" in stderr, check stdout as well in case this will be changed in the future,
         * as returned "Usage" is not an error.
         */
        return (stdOut + stdErr).toLowerCase().contains(REMOVE_MDM_PARAMETER);
    }

    /**
     * Returns true if provided MDM address is present in configuration.
     */
    public static boolean isMdmPresent(String mdmAddress) {
        //query_mdms outputs "MDM-ID <System/MDM-Id> SDC ID <SDC-Id> INSTALLATION ID <Installation-Id> IPs [0]-x.x.x.x [1]-x.x.x.x" for a MDM with ID: <MDM-Id>
        String queryMdmsCmd = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.QUERY_MDMS_CMD;
        queryMdmsCmd += "|grep " + mdmAddress;
        String result = Script.runSimpleBashScript(queryMdmsCmd);

        return StringUtils.isNotBlank(result) && result.contains(mdmAddress);
    }

    public static String getSdcHomePath() {
        String sdcHomePropertyCmdFormat = "sed -n '/%s/p' '%s' 2>/dev/null  | sed 's/%s=//g' 2>/dev/null";
        String sdcHomeCmd = String.format(sdcHomePropertyCmdFormat, SDC_HOME_PARAMETER, AGENT_PROPERTIES_FILE, SDC_HOME_PARAMETER);
        String result = Script.runSimpleBashScript(sdcHomeCmd);
        String sdcHomePath;
        if (result == null) {
            sdcHomePath = DEFAULT_SDC_HOME_PATH;
            LOGGER.warn(String.format("Failed to get sdc home path from agent.properties, fallback to default path %s", sdcHomePath));
        } else {
            sdcHomePath = result;
        }

        return sdcHomePath;
    }

    public static void rescanForNewVolumes() {
        // Detecting new volumes
        String rescanCmd = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.RESCAN_CMD;

        String result = Script.runSimpleBashScript(rescanCmd);
        if (result == null) {
            LOGGER.warn("Failed to rescan for new volumes");
        }
    }

    public static String getSystemIdForVolume(String volumeId) {
        //query_vols outputs "VOL-ID <VolumeID> MDM-ID <SystemID>" for a volume with ID: <VolumeID>
        String queryDiskCmd = SDC_HOME_PATH + "/bin/" + ScaleIOUtil.QUERY_VOLUMES_CMD;
        queryDiskCmd += "|grep " + volumeId + "|awk '{print $4}'";

        String result = Script.runSimpleBashScript(queryDiskCmd);
        if (result == null) {
            LOGGER.warn("Query volumes failed to get volume: " + volumeId + " details for system id");
            return null;
        }

        if (result.isEmpty()) {
            LOGGER.warn("Query volumes doesn't list volume: " + volumeId + ", probably volume is not mapped yet, or sdc not connected");
            return null;
        }

        return result;
    }

    public static String getSdcGuid() {
        String queryGuidCmd = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.QUERY_GUID_CMD;
        String result = Script.runSimpleBashScript(queryGuidCmd);
        if (result == null) {
            LOGGER.warn("Failed to get SDC guid");
            return null;
        }

        if (result.isEmpty()) {
            LOGGER.warn("No SDC guid retrieved");
            return null;
        }

        if (!UuidUtils.isUuid(result)) {
            LOGGER.warn("Invalid SDC guid: " + result);
            return null;
        }

        return result;
    }

    public static String getSdcId(String mdmId) {
        //query_mdms outputs "MDM-ID <System/MDM-Id> SDC ID <SDC-Id> INSTALLATION ID <Installation-Id> IPs [0]-x.x.x.x [1]-x.x.x.x" for a MDM with ID: <MDM-Id>
        String queryMdmsCmd = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.QUERY_MDMS_CMD;
        queryMdmsCmd += "|grep " + mdmId + "|awk '{print $5}'";
        String result = Script.runSimpleBashScript(queryMdmsCmd);
        if (result == null) {
            LOGGER.warn("Failed to get SDC Id, for the MDM: " + mdmId);
            return null;
        }

        if (result.isEmpty()) {
            LOGGER.warn("No SDC Id retrieved, for the MDM: " + mdmId);
            return null;
        }

        String sdcIdRegEx = "^[0-9a-fA-F]{16}$";
        if (!result.matches(sdcIdRegEx)) {
            LOGGER.warn("Invalid SDC Id: " + result + " retrieved, for the MDM: " + mdmId);
            return null;
        }

        return result;
    }

    public static String getVolumePath(String volumePathWithName) {
        if (StringUtils.isEmpty(volumePathWithName)) {
            return volumePathWithName;
        }

        if (volumePathWithName.contains(":")) {
            return volumePathWithName.substring(0, volumePathWithName.indexOf(':'));
        }

        return volumePathWithName;
    }

    public static String updatedPathWithVolumeName(String volumePath, String volumeName) {
        if (StringUtils.isAnyEmpty(volumePath, volumeName)) {
            return volumePath;
        }

        return String.format("%s:%s", volumePath, volumeName);
    }

    public static boolean isSDCServiceInstalled() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_STATUS_CMD);
        return exitValue != 4;
    }

    public static boolean isSDCServiceActive() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_IS_ACTIVE_CMD);
        return exitValue == 0;
    }

    public static boolean isSDCServiceEnabled() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_IS_ENABLED_CMD);
        return exitValue == 0;
    }

    public static boolean enableSDCService() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_ENABLE_CMD);
        return exitValue == 0;
    }

    public static boolean startSDCService() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_START_CMD);
        if (exitValue != 0) {
            return false;
        }
        waitForSdcServiceActionToComplete();
        return true;
    }

    public static boolean stopSDCService() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_STOP_CMD);
        if (exitValue != 0) {
            return false;
        }
        waitForSdcServiceActionToComplete();
        return true;
    }

    public static boolean restartSDCService() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_RESTART_CMD);
        if (exitValue != 0) {
            return false;
        }
        waitForSdcServiceActionToComplete();
        return true;
    }

    private static void waitForSdcServiceActionToComplete() {
        // Wait for the SDC service to settle after start/restart/stop and reaches a stable state
        int waitTimeInSecs = AgentPropertiesFileHandler.getPropertyValue(SDC_SERVICE_ACTION_WAIT);
        if (waitTimeInSecs < 0) {
            waitTimeInSecs = SDC_SERVICE_ACTION_WAIT.getDefaultValue();
        }
        try {
            LOGGER.debug(String.format("Waiting for %d secs after SDC service action, to reach a stable state", waitTimeInSecs));
            Thread.sleep(waitTimeInSecs * 1000L);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Represents {@link ScaleIOUtil#DRV_CFG_FILE} MDM entry (SDC and Installation Ids are skipped).
     */
    public static class MdmEntry {
        private String mdmId;
        private Collection<String> ips;

        /**
         * MDM entry constructor.
         *
         * @param mdmId MDM Id
         * @param ips   IP Addresses
         */
        public MdmEntry(String mdmId, Collection<String> ips) {
            this.mdmId = mdmId;
            this.ips = ips;
        }

        public String getMdmId() {
            return mdmId;
        }

        public void setMdmId(String mdmId) {
            this.mdmId = mdmId;
        }

        public Collection<String> getIps() {
            return ips;
        }

        public void setIps(Collection<String> ips) {
            this.ips = ips;
        }
    }
}
