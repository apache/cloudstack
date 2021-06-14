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

import org.apache.log4j.Logger;

import com.cloud.utils.script.Script;
import com.google.common.base.Strings;

public class ScaleIOUtil {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOUtil.class);

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
    private static final String QUERY_VOLUMES_CMD = "drv_cfg --query_vols";
    // Sample output for cmd: drv_cfg --query_vols:
    // Retrieved 2 volume(s)
    // VOL-ID 6c33633100000009 MDM-ID 218ce1797566a00f
    // VOL-ID 6c3362a30000000a MDM-ID 218ce1797566a00f

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

    public static final String getVolumePath(String volumePathWithName) {
        if (Strings.isNullOrEmpty(volumePathWithName)) {
            return volumePathWithName;
        }

        if (volumePathWithName.contains(":")) {
            return volumePathWithName.substring(0, volumePathWithName.indexOf(':'));
        }

        return volumePathWithName;
    }

    public static final String updatedPathWithVolumeName(String volumePath, String volumeName) {
        if (Strings.isNullOrEmpty(volumePath) || Strings.isNullOrEmpty(volumeName)) {
            return volumePath;
        }

        return String.format("%s:%s", volumePath, volumeName);
    }
}
