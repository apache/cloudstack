/*
 * Copyright (c) Citrix Systems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   1) Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *   2) Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.xensource.xenapi;

import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VersionException;
import com.xensource.xenapi.Types.XenAPIException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;

/**
 * VM Protection Policy
 *
 * @author Citrix Systems, Inc.
 */
public class VMPP extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    VMPP(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a VMPP, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof VMPP)
        {
            VMPP other = (VMPP) obj;
            return other.ref.equals(this.ref);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return ref.hashCode();
    }

    /**
     * Represents all the fields in a VMPP
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "isPolicyEnabled", this.isPolicyEnabled);
            print.printf("%1$20s: %2$s\n", "backupType", this.backupType);
            print.printf("%1$20s: %2$s\n", "backupRetentionValue", this.backupRetentionValue);
            print.printf("%1$20s: %2$s\n", "backupFrequency", this.backupFrequency);
            print.printf("%1$20s: %2$s\n", "backupSchedule", this.backupSchedule);
            print.printf("%1$20s: %2$s\n", "isBackupRunning", this.isBackupRunning);
            print.printf("%1$20s: %2$s\n", "backupLastRunTime", this.backupLastRunTime);
            print.printf("%1$20s: %2$s\n", "archiveTargetType", this.archiveTargetType);
            print.printf("%1$20s: %2$s\n", "archiveTargetConfig", this.archiveTargetConfig);
            print.printf("%1$20s: %2$s\n", "archiveFrequency", this.archiveFrequency);
            print.printf("%1$20s: %2$s\n", "archiveSchedule", this.archiveSchedule);
            print.printf("%1$20s: %2$s\n", "isArchiveRunning", this.isArchiveRunning);
            print.printf("%1$20s: %2$s\n", "archiveLastRunTime", this.archiveLastRunTime);
            print.printf("%1$20s: %2$s\n", "VMs", this.VMs);
            print.printf("%1$20s: %2$s\n", "isAlarmEnabled", this.isAlarmEnabled);
            print.printf("%1$20s: %2$s\n", "alarmConfig", this.alarmConfig);
            print.printf("%1$20s: %2$s\n", "recentAlerts", this.recentAlerts);
            return writer.toString();
        }

        /**
         * Convert a VMPP.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("is_policy_enabled", this.isPolicyEnabled == null ? false : this.isPolicyEnabled);
            map.put("backup_type", this.backupType == null ? Types.VmppBackupType.UNRECOGNIZED : this.backupType);
            map.put("backup_retention_value", this.backupRetentionValue == null ? 0 : this.backupRetentionValue);
            map.put("backup_frequency", this.backupFrequency == null ? Types.VmppBackupFrequency.UNRECOGNIZED : this.backupFrequency);
            map.put("backup_schedule", this.backupSchedule == null ? new HashMap<String, String>() : this.backupSchedule);
            map.put("is_backup_running", this.isBackupRunning == null ? false : this.isBackupRunning);
            map.put("backup_last_run_time", this.backupLastRunTime == null ? new Date(0) : this.backupLastRunTime);
            map.put("archive_target_type", this.archiveTargetType == null ? Types.VmppArchiveTargetType.UNRECOGNIZED : this.archiveTargetType);
            map.put("archive_target_config", this.archiveTargetConfig == null ? new HashMap<String, String>() : this.archiveTargetConfig);
            map.put("archive_frequency", this.archiveFrequency == null ? Types.VmppArchiveFrequency.UNRECOGNIZED : this.archiveFrequency);
            map.put("archive_schedule", this.archiveSchedule == null ? new HashMap<String, String>() : this.archiveSchedule);
            map.put("is_archive_running", this.isArchiveRunning == null ? false : this.isArchiveRunning);
            map.put("archive_last_run_time", this.archiveLastRunTime == null ? new Date(0) : this.archiveLastRunTime);
            map.put("VMs", this.VMs == null ? new LinkedHashSet<VM>() : this.VMs);
            map.put("is_alarm_enabled", this.isAlarmEnabled == null ? false : this.isAlarmEnabled);
            map.put("alarm_config", this.alarmConfig == null ? new HashMap<String, String>() : this.alarmConfig);
            map.put("recent_alerts", this.recentAlerts == null ? new LinkedHashSet<String>() : this.recentAlerts);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * a human-readable name
         */
        public String nameLabel;
        /**
         * a notes field containing human-readable description
         */
        public String nameDescription;
        /**
         * enable or disable this policy
         */
        public Boolean isPolicyEnabled;
        /**
         * type of the backup sub-policy
         */
        public Types.VmppBackupType backupType;
        /**
         * maximum number of backups that should be stored at any time
         */
        public Long backupRetentionValue;
        /**
         * frequency of the backup schedule
         */
        public Types.VmppBackupFrequency backupFrequency;
        /**
         * schedule of the backup containing 'hour', 'min', 'days'. Date/time-related information is in XenServer Local Timezone
         */
        public Map<String, String> backupSchedule;
        /**
         * true if this protection policy's backup is running
         */
        public Boolean isBackupRunning;
        /**
         * time of the last backup
         */
        public Date backupLastRunTime;
        /**
         * type of the archive target config
         */
        public Types.VmppArchiveTargetType archiveTargetType;
        /**
         * configuration for the archive, including its 'location', 'username', 'password'
         */
        public Map<String, String> archiveTargetConfig;
        /**
         * frequency of the archive schedule
         */
        public Types.VmppArchiveFrequency archiveFrequency;
        /**
         * schedule of the archive containing 'hour', 'min', 'days'. Date/time-related information is in XenServer Local Timezone
         */
        public Map<String, String> archiveSchedule;
        /**
         * true if this protection policy's archive is running
         */
        public Boolean isArchiveRunning;
        /**
         * time of the last archive
         */
        public Date archiveLastRunTime;
        /**
         * all VMs attached to this protection policy
         */
        public Set<VM> VMs;
        /**
         * true if alarm is enabled for this policy
         */
        public Boolean isAlarmEnabled;
        /**
         * configuration for the alarm
         */
        public Map<String, String> alarmConfig;
        /**
         * recent alerts
         */
        public Set<String> recentAlerts;
    }

    /**
     * Get a record containing the current state of the given VMPP.
     *
     * @return all fields from the object
     */
    public VMPP.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMPPRecord(result);
    }

    /**
     * Get a reference to the VMPP instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VMPP getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMPP(result);
    }

    /**
     * Create a new VMPP instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, VMPP.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VMPP.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VMPP instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VMPP create(Connection c, VMPP.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMPP(result);
    }

    /**
     * Destroy the specified VMPP instance.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VMPP.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the specified VMPP instance.
     *
     */
    public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Get all the VMPP instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<VMPP> getByNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVMPP(result);
    }

    /**
     * Get the uuid field of the given VMPP.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/label field of the given VMPP.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/description field of the given VMPP.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the is_policy_enabled field of the given VMPP.
     *
     * @return value of the field
     */
    public Boolean getIsPolicyEnabled(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_is_policy_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the backup_type field of the given VMPP.
     *
     * @return value of the field
     */
    public Types.VmppBackupType getBackupType(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_backup_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVmppBackupType(result);
    }

    /**
     * Get the backup_retention_value field of the given VMPP.
     *
     * @return value of the field
     */
    public Long getBackupRetentionValue(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_backup_retention_value";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the backup_frequency field of the given VMPP.
     *
     * @return value of the field
     */
    public Types.VmppBackupFrequency getBackupFrequency(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_backup_frequency";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVmppBackupFrequency(result);
    }

    /**
     * Get the backup_schedule field of the given VMPP.
     *
     * @return value of the field
     */
    public Map<String, String> getBackupSchedule(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_backup_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the is_backup_running field of the given VMPP.
     *
     * @return value of the field
     */
    public Boolean getIsBackupRunning(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_is_backup_running";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the backup_last_run_time field of the given VMPP.
     *
     * @return value of the field
     */
    public Date getBackupLastRunTime(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_backup_last_run_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDate(result);
    }

    /**
     * Get the archive_target_type field of the given VMPP.
     *
     * @return value of the field
     */
    public Types.VmppArchiveTargetType getArchiveTargetType(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_archive_target_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVmppArchiveTargetType(result);
    }

    /**
     * Get the archive_target_config field of the given VMPP.
     *
     * @return value of the field
     */
    public Map<String, String> getArchiveTargetConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_archive_target_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the archive_frequency field of the given VMPP.
     *
     * @return value of the field
     */
    public Types.VmppArchiveFrequency getArchiveFrequency(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_archive_frequency";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVmppArchiveFrequency(result);
    }

    /**
     * Get the archive_schedule field of the given VMPP.
     *
     * @return value of the field
     */
    public Map<String, String> getArchiveSchedule(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_archive_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the is_archive_running field of the given VMPP.
     *
     * @return value of the field
     */
    public Boolean getIsArchiveRunning(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_is_archive_running";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the archive_last_run_time field of the given VMPP.
     *
     * @return value of the field
     */
    public Date getArchiveLastRunTime(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_archive_last_run_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDate(result);
    }

    /**
     * Get the VMs field of the given VMPP.
     *
     * @return value of the field
     */
    public Set<VM> getVMs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_VMs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVM(result);
    }

    /**
     * Get the is_alarm_enabled field of the given VMPP.
     *
     * @return value of the field
     */
    public Boolean getIsAlarmEnabled(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_is_alarm_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the alarm_config field of the given VMPP.
     *
     * @return value of the field
     */
    public Map<String, String> getAlarmConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_alarm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the recent_alerts field of the given VMPP.
     *
     * @return value of the field
     */
    public Set<String> getRecentAlerts(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_recent_alerts";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Set the name/label field of the given VMPP.
     *
     * @param label New value to set
     */
    public void setNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the name/description field of the given VMPP.
     *
     * @param description New value to set
     */
    public void setNameDescription(Connection c, String description) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(description)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the is_policy_enabled field of the given VMPP.
     *
     * @param isPolicyEnabled New value to set
     */
    public void setIsPolicyEnabled(Connection c, Boolean isPolicyEnabled) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_is_policy_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(isPolicyEnabled)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the backup_type field of the given VMPP.
     *
     * @param backupType New value to set
     */
    public void setBackupType(Connection c, Types.VmppBackupType backupType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_backup_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(backupType)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * This call executes the protection policy immediately
     *
     * @return An XMLRPC result
     */
    public String protectNow(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.protect_now";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * This call archives the snapshot provided as a parameter
     *
     * @param snapshot The snapshot to archive
     * @return An XMLRPC result
     */
    public static String archiveNow(Connection c, VM snapshot) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.archive_now";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(snapshot)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * This call fetches a history of alerts for a given protection policy
     *
     * @param hoursFromNow how many hours in the past the oldest record to fetch is
     * @return A list of alerts encoded in xml
     */
    public Set<String> getAlerts(Connection c, Long hoursFromNow) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_alerts";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(hoursFromNow)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setBackupRetentionValue(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_backup_retention_value";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the backup_frequency field
     *
     * @param value the backup frequency
     */
    public void setBackupFrequency(Connection c, Types.VmppBackupFrequency value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_backup_frequency";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setBackupSchedule(Connection c, Map<String, String> value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_backup_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the archive_frequency field
     *
     * @param value the archive frequency
     */
    public void setArchiveFrequency(Connection c, Types.VmppArchiveFrequency value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_archive_frequency";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setArchiveSchedule(Connection c, Map<String, String> value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_archive_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the archive_target_config_type field
     *
     * @param value the archive target config type
     */
    public void setArchiveTargetType(Connection c, Types.VmppArchiveTargetType value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_archive_target_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setArchiveTargetConfig(Connection c, Map<String, String> value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_archive_target_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the is_alarm_enabled field
     *
     * @param value true if alarm is enabled for this policy
     */
    public void setIsAlarmEnabled(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_is_alarm_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setAlarmConfig(Connection c, Map<String, String> value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_alarm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to add
     * @param value the value to add
     */
    public void addToBackupSchedule(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.add_to_backup_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to add
     * @param value the value to add
     */
    public void addToArchiveTargetConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.add_to_archive_target_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to add
     * @param value the value to add
     */
    public void addToArchiveSchedule(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.add_to_archive_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to add
     * @param value the value to add
     */
    public void addToAlarmConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.add_to_alarm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to remove
     */
    public void removeFromBackupSchedule(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.remove_from_backup_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to remove
     */
    public void removeFromArchiveTargetConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.remove_from_archive_target_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to remove
     */
    public void removeFromArchiveSchedule(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.remove_from_archive_schedule";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param key the key to remove
     */
    public void removeFromAlarmConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.remove_from_alarm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setBackupLastRunTime(Connection c, Date value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_backup_last_run_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     *
     *
     * @param value the value to set
     */
    public void setArchiveLastRunTime(Connection c, Date value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.set_archive_last_run_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Return a list of all the VMPPs known to the system.
     *
     * @return references to all objects
     */
    public static Set<VMPP> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVMPP(result);
    }

    /**
     * Return a map of VMPP references to VMPP records for all VMPPs known to the system.
     *
     * @return records of all objects
     */
    public static Map<VMPP, VMPP.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VMPP.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVMPPVMPPRecord(result);
    }

}