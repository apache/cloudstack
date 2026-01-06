/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.util;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.cloudstack.storage.datastore.util.NexentaNmsClient.NmsResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.annotations.SerializedName;

public class NexentaStorAppliance {
    protected Logger logger = LogManager.getLogger(getClass());

    protected NexentaNmsClient client;
    protected NexentaUtil.NexentaPluginParameters parameters;

    public NexentaStorAppliance(NexentaUtil.NexentaPluginParameters parameters) {
        client = new NexentaNmsClient(parameters.getNmsUrl());
        this.parameters = parameters;
    }

    NexentaStorAppliance(NexentaNmsClient client, NexentaUtil.NexentaPluginParameters parameters) {
        this.client = client;
        this.parameters = parameters;
    }

    String getVolumeName(String volumeName) {
        if (volumeName.startsWith("/")) {
            return String.format("%s%s", parameters.getVolume(), volumeName);
        }
        return String.format("%s/%s", parameters.getVolume(), volumeName);
    }

    static String getTargetName(String volumeName) {
        return NexentaUtil.ISCSI_TARGET_NAME_PREFIX + volumeName;
    }

    static String getTargetGroupName(String volumeName) {
        return NexentaUtil.ISCSI_TARGET_GROUP_PREFIX + volumeName;
    }

    @SuppressWarnings("unused")
    static final class IntegerNmsResponse extends NmsResponse {
        Integer result;

        IntegerNmsResponse(int result) {
            this.result = Integer.valueOf(result);
        }

        public Integer getResult() {
            return result;
        }
    }

    @SuppressWarnings("unused")
    static final class IscsiTarget {
        protected String status;
        protected String protocol;
        protected String name;
        protected String sessions;
        protected String alias;
        protected String provider;

        IscsiTarget(String status, String protocol, String name, String sessions, String alias, String provider) {
            this.status = status;
            this.protocol = protocol;
            this.name = name;
            this.sessions = sessions;
            this.alias = alias;
            this.provider = provider;
        }
    }

    @SuppressWarnings("unused")
    static final class ListOfIscsiTargetsNmsResponse extends NmsResponse {
        protected HashMap<String, IscsiTarget> result;

        ListOfIscsiTargetsNmsResponse() {}

        ListOfIscsiTargetsNmsResponse(HashMap<String, IscsiTarget> result) {
            this.result = result;
        }

        public HashMap<String, IscsiTarget> getResult() {
            return result;
        }
    }

    /**
     * Checks if iSCSI target exists.
     * @param targetName iSCSI target name
     * @return true if iSCSI target exists, else false
     */
    boolean isIscsiTargetExists(String targetName) {
        ListOfIscsiTargetsNmsResponse response = (ListOfIscsiTargetsNmsResponse) client.execute(ListOfIscsiTargetsNmsResponse.class, "stmf", "list_targets");
        if (response == null) {
            return false;
        }
        HashMap<String, IscsiTarget> result = response.getResult();
        return result != null && result.keySet().contains(targetName);
    }

    @SuppressWarnings("unused")
    static final class CreateIscsiTargetRequestParams {
        @SerializedName("target_name") String targetName;

        CreateIscsiTargetRequestParams(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CreateIscsiTargetRequestParams && targetName.equals(((CreateIscsiTargetRequestParams) other).targetName);
        }
    }

    /**
     * Creates iSCSI target on NexentaStor Appliance.
     * @param targetName iSCSI target name
     */
    void createIscsiTarget(String targetName) {
        try {
            client.execute(NmsResponse.class, "iscsitarget", "create_target", new CreateIscsiTargetRequestParams(targetName));
        } catch (CloudRuntimeException ex) {
            if (!ex.getMessage().contains("already configured")) {
                throw ex;
            }
            logger.debug("Ignored target creation error: " + ex);
        }
    }

    @SuppressWarnings("unused")
    static final class ListOfStringsNmsResponse extends NmsResponse {
        LinkedList<String> result;

        ListOfStringsNmsResponse() {}

        ListOfStringsNmsResponse(LinkedList<String> result) {
            this.result = result;
        }

        public LinkedList<String> getResult() {
            return result;
        }
    }

    /**
     * Checks if iSCSI target group already exists on NexentaStor Appliance.
     * @param targetGroupName iSCSI target group name
     * @return true if iSCSI target group already exists, else false
     */
    boolean isIscsiTargetGroupExists(String targetGroupName) {
        ListOfStringsNmsResponse response = (ListOfStringsNmsResponse) client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroups");
        if (response == null) {
            return false;
        }
        LinkedList<String> result = response.getResult();
        return result != null && result.contains(targetGroupName);
    }

    /**
     * Creates iSCSI target group on NexentaStor Appliance.
     * @param targetGroupName iSCSI target group name
     */
    void createIscsiTargetGroup(String targetGroupName) {
        try {
            client.execute(NmsResponse.class, "stmf", "create_targetgroup", targetGroupName);
        } catch (CloudRuntimeException ex) {
            if (!ex.getMessage().contains("already exists") && !ex.getMessage().contains("target must be offline")) {
                throw ex;
            }
            logger.info("Ignored target group creation error: " + ex);
        }
    }

    /**
     * Checks if iSCSI target is member of target group.
     * @param targetGroupName iSCSI target group name
     * @param targetName iSCSI target name
     * @return true if target is member of iSCSI target group, else false
     */
    boolean isTargetMemberOfTargetGroup(String targetGroupName, String targetName) {
        ListOfStringsNmsResponse response = (ListOfStringsNmsResponse) client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroup_members", targetGroupName);
        if (response == null) {
            return false;
        }
        LinkedList<String> result = response.getResult();
        return result != null && result.contains(targetName);
    }

    /**
     * Adds iSCSI target to target group.
     * @param targetGroupName iSCSI target group name
     * @param targetName iSCSI target name
     */
    void addTargetGroupMember(String targetGroupName, String targetName) {
        try {
            client.execute(NmsResponse.class, "stmf", "add_targetgroup_member", targetGroupName, targetName);
        } catch (CloudRuntimeException ex) {
            if (!ex.getMessage().contains("already exists") && !ex.getMessage().contains("target must be offline")) {
                throw ex;
            }
            logger.debug("Ignored target group member addition error: " + ex);
        }
    }

    /**
     * Checks if LU already exists on NexentaStor appliance.
     * @param luName LU name
     * @return true if LU already exists, else false
     */
    boolean isLuExists(String luName) {
        IntegerNmsResponse response;
        try {
            response = (IntegerNmsResponse) client.execute(IntegerNmsResponse.class, "scsidisk", "lu_exists", luName);
        } catch (CloudRuntimeException ex) {
            if (ex.getMessage().contains("does not exist")) {
                return false;
            }
            throw ex;
        }
        return response!= null && response.getResult() > 0;
    }

    @SuppressWarnings("unused")
    static final class LuParams {
        @Override
        public boolean equals(Object other) {
            return other instanceof LuParams;
        }
    }

    /**
     * Creates LU for volume.
     * @param volumeName volume name
     */
    void createLu(String volumeName) {
        try {
            client.execute(NmsResponse.class, "scsidisk", "create_lu", volumeName, new LuParams());
        } catch (CloudRuntimeException ex) {
            if (!ex.getMessage().contains("in use")) {
                throw ex;
            }
            logger.info("Ignored LU creation error: " + ex);
        }
    }

    /**
     * Checks if LU shared on NexentaStor appliance.
     * @param luName LU name
     * @return true if LU was already shared, else false
     */
    boolean isLuShared(String luName) {
        IntegerNmsResponse response;
        try {
            response = (IntegerNmsResponse) client.execute(IntegerNmsResponse.class, "scsidisk", "lu_shared", luName);
        } catch (CloudRuntimeException ex) {
            if (ex.getMessage().contains("does not exist")) {
                return false;
            }
            throw ex;
        }
        return response != null && response.getResult() > 0;
    }

    @SuppressWarnings("unused")
    static final class MappingEntry {
        @SerializedName("target_group") String targetGroup;
        String lun;
        String zvol;
        @SerializedName("host_group") String hostGroup;
        @SerializedName("entry_number") String entryNumber;

        MappingEntry(String targetGroup, String lun) {
            this.targetGroup = targetGroup;
            this.lun = lun;
        }

        static boolean isEquals(Object a, Object b) {
            return (a == null && b == null) || (a != null && a.equals(b));
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof MappingEntry) {
                MappingEntry o = (MappingEntry) other;
                return isEquals(targetGroup, o.targetGroup) && isEquals(lun, o.lun) && isEquals(zvol, o.zvol) &&
                        isEquals(hostGroup, o.hostGroup) && isEquals(entryNumber, o.entryNumber);
            }
            return false;
        }
    }

    @SuppressWarnings("unused")
    static final class AddMappingEntryNmsResponse extends NmsResponse {
        MappingEntry result;
    }

    /**
     * Adds LU mapping entry to iSCSI target group.
     * @param luName LU name
     * @param targetGroupName iSCSI target group name
     */
    void addLuMappingEntry(String luName, String targetGroupName) {
        MappingEntry mappingEntry = new MappingEntry(targetGroupName, "0");
        try {
            client.execute(AddMappingEntryNmsResponse.class, "scsidisk", "add_lun_mapping_entry", luName, mappingEntry);
        } catch (CloudRuntimeException ex) {
            if (!ex.getMessage().contains("view already exists")) {
                throw ex;
            }
            logger.debug("Ignored LU mapping entry addition error " + ex);
        }
    }

    NexentaStorZvol createIscsiVolume(String volumeName, Long volumeSize) {
        final String zvolName = getVolumeName(volumeName);
        String volumeSizeString = String.format("%dB", volumeSize);

        client.execute(NmsResponse.class, "zvol", "create", zvolName, volumeSizeString, parameters.getVolumeBlockSize(), parameters.isSparseVolumes());

        final String targetName = getTargetName(volumeName);
        final String targetGroupName = getTargetGroupName(volumeName);

        if (!isIscsiTargetExists(targetName)) {
            createIscsiTarget(targetName);
        }

        if (!isIscsiTargetGroupExists(targetGroupName)) {
            createIscsiTargetGroup(targetGroupName);
        }

        if (!isTargetMemberOfTargetGroup(targetGroupName, targetName)) {
            addTargetGroupMember(targetGroupName, targetName);
        }

        if (!isLuExists(zvolName)) {
            createLu(zvolName);
        }

        if (!isLuShared(zvolName)) {
            addLuMappingEntry(zvolName, targetGroupName);
        }

        return new NexentaStorZvol(zvolName, targetName);
    }

    static abstract class NexentaStorVolume {
        protected String name;

        NexentaStorVolume(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class NexentaStorZvol extends NexentaStorVolume {
        protected String iqn;

        public NexentaStorZvol(String name, String iqn) {
            super(name);
            this.iqn = iqn;
        }

        public String getIqn() {
            return iqn;
        }
    }

    public void deleteIscsiVolume(String volumeName) {
        try {
            NmsResponse response = client.execute(NmsResponse.class, "zvol", "destroy", volumeName, "");
        } catch (CloudRuntimeException ex) {
            if (!ex.getMessage().contains("does not exist")) {
                throw ex;
            }
            logger.debug(String.format(
                    "Volume %s does not exist, it seems it was already " +
                            "deleted.", volumeName));
        }
    }

    public NexentaStorVolume createVolume(String volumeName, Long volumeSize) {
        return createIscsiVolume(volumeName, volumeSize);
    }

    public void deleteVolume(String volumeName) {
        deleteIscsiVolume(volumeName);
    }
}
