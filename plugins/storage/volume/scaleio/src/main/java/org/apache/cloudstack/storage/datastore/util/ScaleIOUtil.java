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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.UuidUtils;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

public class ScaleIOUtil {
    protected static Logger LOGGER = LogManager.getLogger(ScaleIOUtil.class);

    public static final String PROVIDER_NAME = "PowerFlex";

    // Use prefix for CloudStack resources
    public static final String VOLUME_PREFIX = "vol";
    public static final String TEMPLATE_PREFIX = "tmpl";
    public static final String SNAPSHOT_PREFIX = "snap";
    public static final String VMSNAPSHOT_PREFIX = "vmsnap";

    public static final int IDENTIFIER_LENGTH = 16;
    public static final Long MINIMUM_ALLOWED_IOPS_LIMIT = Long.valueOf(10);

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
     * Cmd for querying volumes in SDC
     * Sample output for cmd: drv_cfg --query_vols:
     * Retrieved 2 volume(s)
     * VOL-ID 6c33633100000009 MDM-ID 218ce1797566a00f
     * VOL-ID 6c3362a30000000a MDM-ID 218ce1797566a00f
     */
    private static final String QUERY_VOLUMES_CMD = "drv_cfg --query_vols";

    /**
     * Cmd for querying guid in SDC
     * Sample output for cmd: drv_cfg --query_guid:
     * B0E3BFB8-C20B-43BF-93C8-13339E85AA50
     */
    private static final String QUERY_GUID_CMD = "drv_cfg --query_guid";

    /**
     * Cmd for querying MDMs in SDC
     * Sample output for cmd: drv_cfg --query_mdms:
     * Retrieved 2 mdm(s)
     * MDM-ID 3ef46cbf2aaf5d0f SDC ID 6b18479c00000003 INSTALLATION ID 68ab55462cbb3ae4 IPs [0]-x.x.x.x [1]-x.x.x.x
     * MDM-ID 2e706b2740ec200f SDC ID 301b852c00000003 INSTALLATION ID 33f8662e7a5c1e6c IPs [0]-x.x.x.x [1]-x.x.x.x
     */
    private static final String QUERY_MDMS_CMD = "drv_cfg --query_mdms";

    public static String getSdcHomePath() {
        String sdcHomePath = DEFAULT_SDC_HOME_PATH;
        String sdcHomePropertyCmdFormat = "sed -n '/%s/p' '%s' 2>/dev/null  | sed 's/%s=//g' 2>/dev/null";
        String sdcHomeCmd = String.format(sdcHomePropertyCmdFormat, SDC_HOME_PARAMETER, AGENT_PROPERTIES_FILE, SDC_HOME_PARAMETER);

        String result = Script.runSimpleBashScript(sdcHomeCmd);
        if (result == null) {
            LOGGER.warn("Failed to get sdc home path from agent.properties, fallback to default path");
        } else {
            sdcHomePath = result;
        }

        return sdcHomePath;
    }

    public static final void rescanForNewVolumes() {
        // Detecting new volumes
        String rescanCmd = ScaleIOUtil.SDC_HOME_PATH + "/bin/" + ScaleIOUtil.RESCAN_CMD;

        String result = Script.runSimpleBashScript(rescanCmd);
        if (result == null) {
            LOGGER.warn("Failed to rescan for new volumes");
        }
    }

    public static final String getSystemIdForVolume(String volumeId) {
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

    public static final String getVolumePath(String volumePathWithName) {
        if (StringUtils.isEmpty(volumePathWithName)) {
            return volumePathWithName;
        }

        if (volumePathWithName.contains(":")) {
            return volumePathWithName.substring(0, volumePathWithName.indexOf(':'));
        }

        return volumePathWithName;
    }

    public static final String updatedPathWithVolumeName(String volumePath, String volumeName) {
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
        return exitValue == 0;
    }

    public static boolean stopSDCService() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_STOP_CMD);
        return exitValue == 0;
    }

    public static boolean restartSDCService() {
        int exitValue = Script.runSimpleBashScriptForExitValue(SDC_SERVICE_RESTART_CMD);
        return exitValue == 0;
    }
}
