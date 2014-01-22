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
 * Pool-wide information
 *
 * @author Citrix Systems, Inc.
 */
public class Pool extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    Pool(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a Pool, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof Pool)
        {
            Pool other = (Pool) obj;
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
     * Represents all the fields in a Pool
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "master", this.master);
            print.printf("%1$20s: %2$s\n", "defaultSR", this.defaultSR);
            print.printf("%1$20s: %2$s\n", "suspendImageSR", this.suspendImageSR);
            print.printf("%1$20s: %2$s\n", "crashDumpSR", this.crashDumpSR);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "haEnabled", this.haEnabled);
            print.printf("%1$20s: %2$s\n", "haConfiguration", this.haConfiguration);
            print.printf("%1$20s: %2$s\n", "haStatefiles", this.haStatefiles);
            print.printf("%1$20s: %2$s\n", "haHostFailuresToTolerate", this.haHostFailuresToTolerate);
            print.printf("%1$20s: %2$s\n", "haPlanExistsFor", this.haPlanExistsFor);
            print.printf("%1$20s: %2$s\n", "haAllowOvercommit", this.haAllowOvercommit);
            print.printf("%1$20s: %2$s\n", "haOvercommitted", this.haOvercommitted);
            print.printf("%1$20s: %2$s\n", "blobs", this.blobs);
            print.printf("%1$20s: %2$s\n", "tags", this.tags);
            print.printf("%1$20s: %2$s\n", "guiConfig", this.guiConfig);
            print.printf("%1$20s: %2$s\n", "wlbUrl", this.wlbUrl);
            print.printf("%1$20s: %2$s\n", "wlbUsername", this.wlbUsername);
            print.printf("%1$20s: %2$s\n", "wlbEnabled", this.wlbEnabled);
            print.printf("%1$20s: %2$s\n", "wlbVerifyCert", this.wlbVerifyCert);
            print.printf("%1$20s: %2$s\n", "redoLogEnabled", this.redoLogEnabled);
            print.printf("%1$20s: %2$s\n", "redoLogVdi", this.redoLogVdi);
            print.printf("%1$20s: %2$s\n", "vswitchController", this.vswitchController);
            print.printf("%1$20s: %2$s\n", "restrictions", this.restrictions);
            print.printf("%1$20s: %2$s\n", "metadataVDIs", this.metadataVDIs);
            return writer.toString();
        }

        /**
         * Convert a pool.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("master", this.master == null ? new Host("OpaqueRef:NULL") : this.master);
            map.put("default_SR", this.defaultSR == null ? new SR("OpaqueRef:NULL") : this.defaultSR);
            map.put("suspend_image_SR", this.suspendImageSR == null ? new SR("OpaqueRef:NULL") : this.suspendImageSR);
            map.put("crash_dump_SR", this.crashDumpSR == null ? new SR("OpaqueRef:NULL") : this.crashDumpSR);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("ha_enabled", this.haEnabled == null ? false : this.haEnabled);
            map.put("ha_configuration", this.haConfiguration == null ? new HashMap<String, String>() : this.haConfiguration);
            map.put("ha_statefiles", this.haStatefiles == null ? new LinkedHashSet<String>() : this.haStatefiles);
            map.put("ha_host_failures_to_tolerate", this.haHostFailuresToTolerate == null ? 0 : this.haHostFailuresToTolerate);
            map.put("ha_plan_exists_for", this.haPlanExistsFor == null ? 0 : this.haPlanExistsFor);
            map.put("ha_allow_overcommit", this.haAllowOvercommit == null ? false : this.haAllowOvercommit);
            map.put("ha_overcommitted", this.haOvercommitted == null ? false : this.haOvercommitted);
            map.put("blobs", this.blobs == null ? new HashMap<String, Blob>() : this.blobs);
            map.put("tags", this.tags == null ? new LinkedHashSet<String>() : this.tags);
            map.put("gui_config", this.guiConfig == null ? new HashMap<String, String>() : this.guiConfig);
            map.put("wlb_url", this.wlbUrl == null ? "" : this.wlbUrl);
            map.put("wlb_username", this.wlbUsername == null ? "" : this.wlbUsername);
            map.put("wlb_enabled", this.wlbEnabled == null ? false : this.wlbEnabled);
            map.put("wlb_verify_cert", this.wlbVerifyCert == null ? false : this.wlbVerifyCert);
            map.put("redo_log_enabled", this.redoLogEnabled == null ? false : this.redoLogEnabled);
            map.put("redo_log_vdi", this.redoLogVdi == null ? new VDI("OpaqueRef:NULL") : this.redoLogVdi);
            map.put("vswitch_controller", this.vswitchController == null ? "" : this.vswitchController);
            map.put("restrictions", this.restrictions == null ? new HashMap<String, String>() : this.restrictions);
            map.put("metadata_VDIs", this.metadataVDIs == null ? new LinkedHashSet<VDI>() : this.metadataVDIs);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * Short name
         */
        public String nameLabel;
        /**
         * Description
         */
        public String nameDescription;
        /**
         * The host that is pool master
         */
        public Host master;
        /**
         * Default SR for VDIs
         */
        public SR defaultSR;
        /**
         * The SR in which VDIs for suspend images are created
         */
        public SR suspendImageSR;
        /**
         * The SR in which VDIs for crash dumps are created
         */
        public SR crashDumpSR;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * true if HA is enabled on the pool, false otherwise
         */
        public Boolean haEnabled;
        /**
         * The current HA configuration
         */
        public Map<String, String> haConfiguration;
        /**
         * HA statefile VDIs in use
         */
        public Set<String> haStatefiles;
        /**
         * Number of host failures to tolerate before the Pool is declared to be overcommitted
         */
        public Long haHostFailuresToTolerate;
        /**
         * Number of future host failures we have managed to find a plan for. Once this reaches zero any future host failures will cause the failure of protected VMs.
         */
        public Long haPlanExistsFor;
        /**
         * If set to false then operations which would cause the Pool to become overcommitted will be blocked.
         */
        public Boolean haAllowOvercommit;
        /**
         * True if the Pool is considered to be overcommitted i.e. if there exist insufficient physical resources to tolerate the configured number of host failures
         */
        public Boolean haOvercommitted;
        /**
         * Binary blobs associated with this pool
         */
        public Map<String, Blob> blobs;
        /**
         * user-specified tags for categorization purposes
         */
        public Set<String> tags;
        /**
         * gui-specific configuration for pool
         */
        public Map<String, String> guiConfig;
        /**
         * Url for the configured workload balancing host
         */
        public String wlbUrl;
        /**
         * Username for accessing the workload balancing host
         */
        public String wlbUsername;
        /**
         * true if workload balancing is enabled on the pool, false otherwise
         */
        public Boolean wlbEnabled;
        /**
         * true if communication with the WLB server should enforce SSL certificate verification.
         */
        public Boolean wlbVerifyCert;
        /**
         * true a redo-log is to be used other than when HA is enabled, false otherwise
         */
        public Boolean redoLogEnabled;
        /**
         * indicates the VDI to use for the redo-log other than when HA is enabled
         */
        public VDI redoLogVdi;
        /**
         * address of the vswitch controller
         */
        public String vswitchController;
        /**
         * Pool-wide restrictions currently in effect
         */
        public Map<String, String> restrictions;
        /**
         * The set of currently known metadata VDIs for this pool
         */
        public Set<VDI> metadataVDIs;
    }

    /**
     * Get a record containing the current state of the given pool.
     *
     * @return all fields from the object
     */
    public Pool.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPoolRecord(result);
    }

    /**
     * Get a reference to the pool instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Pool getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPool(result);
    }

    /**
     * Get the uuid field of the given pool.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name_label field of the given pool.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name_description field of the given pool.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the master field of the given pool.
     *
     * @return value of the field
     */
    public Host getMaster(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toHost(result);
    }

    /**
     * Get the default_SR field of the given pool.
     *
     * @return value of the field
     */
    public SR getDefaultSR(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_default_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSR(result);
    }

    /**
     * Get the suspend_image_SR field of the given pool.
     *
     * @return value of the field
     */
    public SR getSuspendImageSR(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_suspend_image_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSR(result);
    }

    /**
     * Get the crash_dump_SR field of the given pool.
     *
     * @return value of the field
     */
    public SR getCrashDumpSR(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_crash_dump_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSR(result);
    }

    /**
     * Get the other_config field of the given pool.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the ha_enabled field of the given pool.
     *
     * @return value of the field
     */
    public Boolean getHaEnabled(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the ha_configuration field of the given pool.
     *
     * @return value of the field
     */
    public Map<String, String> getHaConfiguration(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_configuration";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the ha_statefiles field of the given pool.
     *
     * @return value of the field
     */
    public Set<String> getHaStatefiles(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_statefiles";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the ha_host_failures_to_tolerate field of the given pool.
     *
     * @return value of the field
     */
    public Long getHaHostFailuresToTolerate(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_host_failures_to_tolerate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the ha_plan_exists_for field of the given pool.
     *
     * @return value of the field
     */
    public Long getHaPlanExistsFor(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_plan_exists_for";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the ha_allow_overcommit field of the given pool.
     *
     * @return value of the field
     */
    public Boolean getHaAllowOvercommit(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_allow_overcommit";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the ha_overcommitted field of the given pool.
     *
     * @return value of the field
     */
    public Boolean getHaOvercommitted(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_ha_overcommitted";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the blobs field of the given pool.
     *
     * @return value of the field
     */
    public Map<String, Blob> getBlobs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_blobs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringBlob(result);
    }

    /**
     * Get the tags field of the given pool.
     *
     * @return value of the field
     */
    public Set<String> getTags(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the gui_config field of the given pool.
     *
     * @return value of the field
     */
    public Map<String, String> getGuiConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_gui_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the wlb_url field of the given pool.
     *
     * @return value of the field
     */
    public String getWlbUrl(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_wlb_url";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the wlb_username field of the given pool.
     *
     * @return value of the field
     */
    public String getWlbUsername(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_wlb_username";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the wlb_enabled field of the given pool.
     *
     * @return value of the field
     */
    public Boolean getWlbEnabled(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_wlb_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the wlb_verify_cert field of the given pool.
     *
     * @return value of the field
     */
    public Boolean getWlbVerifyCert(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_wlb_verify_cert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the redo_log_enabled field of the given pool.
     *
     * @return value of the field
     */
    public Boolean getRedoLogEnabled(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_redo_log_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the redo_log_vdi field of the given pool.
     *
     * @return value of the field
     */
    public VDI getRedoLogVdi(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_redo_log_vdi";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Get the vswitch_controller field of the given pool.
     *
     * @return value of the field
     */
    public String getVswitchController(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_vswitch_controller";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the restrictions field of the given pool.
     *
     * @return value of the field
     */
    public Map<String, String> getRestrictions(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_restrictions";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the metadata_VDIs field of the given pool.
     *
     * @return value of the field
     */
    public Set<VDI> getMetadataVDIs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_metadata_VDIs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVDI(result);
    }

    /**
     * Set the name_label field of the given pool.
     *
     * @param nameLabel New value to set
     */
    public void setNameLabel(Connection c, String nameLabel) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nameLabel)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the name_description field of the given pool.
     *
     * @param nameDescription New value to set
     */
    public void setNameDescription(Connection c, String nameDescription) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nameDescription)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the default_SR field of the given pool.
     *
     * @param defaultSR New value to set
     */
    public void setDefaultSR(Connection c, SR defaultSR) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_default_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(defaultSR)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the suspend_image_SR field of the given pool.
     *
     * @param suspendImageSR New value to set
     */
    public void setSuspendImageSR(Connection c, SR suspendImageSR) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_suspend_image_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(suspendImageSR)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the crash_dump_SR field of the given pool.
     *
     * @param crashDumpSR New value to set
     */
    public void setCrashDumpSR(Connection c, SR crashDumpSR) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_crash_dump_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(crashDumpSR)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the other_config field of the given pool.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given pool.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given pool.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the ha_allow_overcommit field of the given pool.
     *
     * @param haAllowOvercommit New value to set
     */
    public void setHaAllowOvercommit(Connection c, Boolean haAllowOvercommit) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_ha_allow_overcommit";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(haAllowOvercommit)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the tags field of the given pool.
     *
     * @param tags New value to set
     */
    public void setTags(Connection c, Set<String> tags) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(tags)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given value to the tags field of the given pool.  If the value is already in that Set, then do nothing.
     *
     * @param value New value to add
     */
    public void addTags(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.add_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given value from the tags field of the given pool.  If the value is not in that Set, then do nothing.
     *
     * @param value Value to remove
     */
    public void removeTags(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.remove_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the gui_config field of the given pool.
     *
     * @param guiConfig New value to set
     */
    public void setGuiConfig(Connection c, Map<String, String> guiConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_gui_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(guiConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the gui_config field of the given pool.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToGuiConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.add_to_gui_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the gui_config field of the given pool.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromGuiConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.remove_from_gui_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the wlb_enabled field of the given pool.
     *
     * @param wlbEnabled New value to set
     */
    public void setWlbEnabled(Connection c, Boolean wlbEnabled) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_wlb_enabled";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(wlbEnabled)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the wlb_verify_cert field of the given pool.
     *
     * @param wlbVerifyCert New value to set
     */
    public void setWlbVerifyCert(Connection c, Boolean wlbVerifyCert) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_wlb_verify_cert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(wlbVerifyCert)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     * @return Task
     */
    public static Task joinAsync(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.JoiningHostCannotContainSharedSrs {
        String method_call = "Async.pool.join";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     */
    public static void join(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.JoiningHostCannotContainSharedSrs {
        String method_call = "pool.join";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     * @return Task
     */
    public static Task joinForceAsync(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.join_force";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Instruct host to join a new pool
     *
     * @param masterAddress The hostname of the master of the pool to join
     * @param masterUsername The username of the master (for initial authentication)
     * @param masterPassword The password for the master (for initial authentication)
     */
    public static void joinForce(Connection c, String masterAddress, String masterUsername, String masterPassword) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.join_force";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress), Marshalling.toXMLRPC(masterUsername), Marshalling.toXMLRPC(masterPassword)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Instruct a pool master to eject a host from the pool
     *
     * @param host The host to eject
     * @return Task
     */
    public static Task ejectAsync(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.eject";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Instruct a pool master to eject a host from the pool
     *
     * @param host The host to eject
     */
    public static void eject(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.eject";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Instruct host that's currently a slave to transition to being master
     *
     */
    public static void emergencyTransitionToMaster(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.emergency_transition_to_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Instruct a slave already in a pool that the master has changed
     *
     * @param masterAddress The hostname of the master
     */
    public static void emergencyResetMaster(Connection c, String masterAddress) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.emergency_reset_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(masterAddress)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Instruct a pool master, M, to try and contact its slaves and, if slaves are in emergency mode, reset their master address to M.
     *
     * @return Task
     */
    public static Task recoverSlavesAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.recover_slaves";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Instruct a pool master, M, to try and contact its slaves and, if slaves are in emergency mode, reset their master address to M.
     *
     * @return list of hosts whose master address were succesfully reset
     */
    public static Set<Host> recoverSlaves(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.recover_slaves";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfHost(result);
    }

    /**
     * Create PIFs, mapping a network to the same physical interface/VLAN on each host. This call is deprecated: use Pool.create_VLAN_from_PIF instead.
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param VLAN VLAN tag for the new interface
     * @return Task
     */
    public static Task createVLANAsync(Connection c, String device, Network network, Long VLAN) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "Async.pool.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create PIFs, mapping a network to the same physical interface/VLAN on each host. This call is deprecated: use Pool.create_VLAN_from_PIF instead.
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param VLAN VLAN tag for the new interface
     * @return The references of the created PIF objects
     */
    public static Set<PIF> createVLAN(Connection c, String device, Network network, Long VLAN) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "pool.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPIF(result);
    }

    /**
     * Create a pool-wide VLAN by taking the PIF.
     *
     * @param pif physical interface on any particular host, that identifies the PIF on which to create the (pool-wide) VLAN interface
     * @param network network to which this interface should be connected
     * @param VLAN VLAN tag for the new interface
     * @return Task
     */
    public static Task createVLANFromPIFAsync(Connection c, PIF pif, Network network, Long VLAN) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "Async.pool.create_VLAN_from_PIF";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(pif), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a pool-wide VLAN by taking the PIF.
     *
     * @param pif physical interface on any particular host, that identifies the PIF on which to create the (pool-wide) VLAN interface
     * @param network network to which this interface should be connected
     * @param VLAN VLAN tag for the new interface
     * @return The references of the created PIF objects
     */
    public static Set<PIF> createVLANFromPIF(Connection c, PIF pif, Network network, Long VLAN) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "pool.create_VLAN_from_PIF";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(pif), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPIF(result);
    }

    /**
     * Turn on High Availability mode
     *
     * @param heartbeatSrs Set of SRs to use for storage heartbeating.
     * @param configuration Detailed HA configuration to apply
     * @return Task
     */
    public static Task enableHaAsync(Connection c, Set<SR> heartbeatSrs, Map<String, String> configuration) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.enable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(heartbeatSrs), Marshalling.toXMLRPC(configuration)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Turn on High Availability mode
     *
     * @param heartbeatSrs Set of SRs to use for storage heartbeating.
     * @param configuration Detailed HA configuration to apply
     */
    public static void enableHa(Connection c, Set<SR> heartbeatSrs, Map<String, String> configuration) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.enable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(heartbeatSrs), Marshalling.toXMLRPC(configuration)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Turn off High Availability mode
     *
     * @return Task
     */
    public static Task disableHaAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.disable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Turn off High Availability mode
     *
     */
    public static void disableHa(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.disable_ha";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Forcibly synchronise the database now
     *
     * @return Task
     */
    public static Task syncDatabaseAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.sync_database";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Forcibly synchronise the database now
     *
     */
    public static void syncDatabase(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.sync_database";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Perform an orderly handover of the role of master to the referenced host.
     *
     * @param host The host who should become the new master
     * @return Task
     */
    public static Task designateNewMasterAsync(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.designate_new_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Perform an orderly handover of the role of master to the referenced host.
     *
     * @param host The host who should become the new master
     */
    public static void designateNewMaster(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.designate_new_master";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * When this call returns the VM restart logic will not run for the requested number of seconds. If the argument is zero then the restart thread is immediately unblocked
     *
     * @param seconds The number of seconds to block the restart thread for
     */
    public static void haPreventRestartsFor(Connection c, Long seconds) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.ha_prevent_restarts_for";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(seconds)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Returns true if a VM failover plan exists for up to 'n' host failures
     *
     * @param n The number of host failures to plan for
     * @return true if a failover plan exists for the supplied number of host failures
     */
    public static Boolean haFailoverPlanExists(Connection c, Long n) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.ha_failover_plan_exists";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(n)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Returns the maximum number of host failures we could tolerate before we would be unable to restart configured VMs
     *
     * @return maximum value for ha_host_failures_to_tolerate given current configuration
     */
    public static Long haComputeMaxHostFailuresToTolerate(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.ha_compute_max_host_failures_to_tolerate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Returns the maximum number of host failures we could tolerate before we would be unable to restart the provided VMs
     *
     * @param configuration Map of protected VM reference to restart priority
     * @return maximum value for ha_host_failures_to_tolerate given provided configuration
     */
    public static Long haComputeHypotheticalMaxHostFailuresToTolerate(Connection c, Map<VM, String> configuration) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.ha_compute_hypothetical_max_host_failures_to_tolerate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(configuration)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Return a VM failover plan assuming a given subset of hosts fail
     *
     * @param failedHosts The set of hosts to assume have failed
     * @param failedVms The set of VMs to restart
     * @return VM failover plan: a map of VM to host to restart the host on
     */
    public static Map<VM, Map<String, String>> haComputeVmFailoverPlan(Connection c, Set<Host> failedHosts, Set<VM> failedVms) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.ha_compute_vm_failover_plan";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(failedHosts), Marshalling.toXMLRPC(failedVms)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVMMapOfStringString(result);
    }

    /**
     * Set the maximum number of host failures to consider in the HA VM restart planner
     *
     * @param value New number of host failures to consider
     * @return Task
     */
    public Task setHaHostFailuresToTolerateAsync(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.set_ha_host_failures_to_tolerate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the maximum number of host failures to consider in the HA VM restart planner
     *
     * @param value New number of host failures to consider
     */
    public void setHaHostFailuresToTolerate(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_ha_host_failures_to_tolerate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Create a placeholder for a named binary blob of data that is associated with this pool
     *
     * @param name The name associated with the blob
     * @param mimeType The mime type for the data. Empty string translates to application/octet-stream
     * @param _public True if the blob should be publicly available
     * @return Task
     */
    public Task createNewBlobAsync(Connection c, String name, String mimeType, Boolean _public) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.create_new_blob";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(mimeType), Marshalling.toXMLRPC(_public)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a placeholder for a named binary blob of data that is associated with this pool
     *
     * @param name The name associated with the blob
     * @param mimeType The mime type for the data. Empty string translates to application/octet-stream
     * @param _public True if the blob should be publicly available
     * @return The reference of the blob, needed for populating its data
     */
    public Blob createNewBlob(Connection c, String name, String mimeType, Boolean _public) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.create_new_blob";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(mimeType), Marshalling.toXMLRPC(_public)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBlob(result);
    }

    /**
     * This call enables external authentication on all the hosts of the pool
     *
     * @param config A list of key-values containing the configuration data
     * @param serviceName The name of the service
     * @param authType The type of authentication (e.g. AD for Active Directory)
     */
    public void enableExternalAuth(Connection c, Map<String, String> config, String serviceName, String authType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.enable_external_auth";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(config), Marshalling.toXMLRPC(serviceName), Marshalling.toXMLRPC(authType)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * This call disables external authentication on all the hosts of the pool
     *
     * @param config Optional parameters as a list of key-values containing the configuration data
     */
    public void disableExternalAuth(Connection c, Map<String, String> config) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.disable_external_auth";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(config)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * This call asynchronously detects if the external authentication configuration in any slave is different from that in the master and raises appropriate alerts
     *
     */
    public void detectNonhomogeneousExternalAuth(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.detect_nonhomogeneous_external_auth";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Initializes workload balancing monitoring on this pool with the specified wlb server
     *
     * @param wlbUrl The ip address and port to use when accessing the wlb server
     * @param wlbUsername The username used to authenticate with the wlb server
     * @param wlbPassword The password used to authenticate with the wlb server
     * @param xenserverUsername The usernamed used by the wlb server to authenticate with the xenserver
     * @param xenserverPassword The password used by the wlb server to authenticate with the xenserver
     * @return Task
     */
    public static Task initializeWlbAsync(Connection c, String wlbUrl, String wlbUsername, String wlbPassword, String xenserverUsername, String xenserverPassword) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.initialize_wlb";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(wlbUrl), Marshalling.toXMLRPC(wlbUsername), Marshalling.toXMLRPC(wlbPassword), Marshalling.toXMLRPC(xenserverUsername), Marshalling.toXMLRPC(xenserverPassword)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Initializes workload balancing monitoring on this pool with the specified wlb server
     *
     * @param wlbUrl The ip address and port to use when accessing the wlb server
     * @param wlbUsername The username used to authenticate with the wlb server
     * @param wlbPassword The password used to authenticate with the wlb server
     * @param xenserverUsername The usernamed used by the wlb server to authenticate with the xenserver
     * @param xenserverPassword The password used by the wlb server to authenticate with the xenserver
     */
    public static void initializeWlb(Connection c, String wlbUrl, String wlbUsername, String wlbPassword, String xenserverUsername, String xenserverPassword) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.initialize_wlb";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(wlbUrl), Marshalling.toXMLRPC(wlbUsername), Marshalling.toXMLRPC(wlbPassword), Marshalling.toXMLRPC(xenserverUsername), Marshalling.toXMLRPC(xenserverPassword)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Permanently deconfigures workload balancing monitoring on this pool
     *
     * @return Task
     */
    public static Task deconfigureWlbAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.deconfigure_wlb";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Permanently deconfigures workload balancing monitoring on this pool
     *
     */
    public static void deconfigureWlb(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.deconfigure_wlb";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the pool optimization criteria for the workload balancing server
     *
     * @param config The configuration to use in optimizing this pool
     * @return Task
     */
    public static Task sendWlbConfigurationAsync(Connection c, Map<String, String> config) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.send_wlb_configuration";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(config)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Sets the pool optimization criteria for the workload balancing server
     *
     * @param config The configuration to use in optimizing this pool
     */
    public static void sendWlbConfiguration(Connection c, Map<String, String> config) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.send_wlb_configuration";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(config)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Retrieves the pool optimization criteria from the workload balancing server
     *
     * @return Task
     */
    public static Task retrieveWlbConfigurationAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.retrieve_wlb_configuration";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Retrieves the pool optimization criteria from the workload balancing server
     *
     * @return The configuration used in optimizing this pool
     */
    public static Map<String, String> retrieveWlbConfiguration(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.retrieve_wlb_configuration";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Retrieves vm migrate recommendations for the pool from the workload balancing server
     *
     * @return Task
     */
    public static Task retrieveWlbRecommendationsAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.retrieve_wlb_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Retrieves vm migrate recommendations for the pool from the workload balancing server
     *
     * @return The list of vm migration recommendations
     */
    public static Map<VM, Set<String>> retrieveWlbRecommendations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.retrieve_wlb_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVMSetOfString(result);
    }

    /**
     * Send the given body to the given host and port, using HTTPS, and print the response.  This is used for debugging the SSL layer.
     *
     * @param host 
     * @param port 
     * @param body 
     * @return Task
     */
    public static Task sendTestPostAsync(Connection c, String host, Long port, String body) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.send_test_post";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(port), Marshalling.toXMLRPC(body)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Send the given body to the given host and port, using HTTPS, and print the response.  This is used for debugging the SSL layer.
     *
     * @param host 
     * @param port 
     * @param body 
     * @return The response
     */
    public static String sendTestPost(Connection c, String host, Long port, String body) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.send_test_post";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(port), Marshalling.toXMLRPC(body)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Install an SSL certificate pool-wide.
     *
     * @param name A name to give the certificate
     * @param cert The certificate
     * @return Task
     */
    public static Task certificateInstallAsync(Connection c, String name, String cert) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.certificate_install";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(cert)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Install an SSL certificate pool-wide.
     *
     * @param name A name to give the certificate
     * @param cert The certificate
     */
    public static void certificateInstall(Connection c, String name, String cert) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.certificate_install";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(cert)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove an SSL certificate.
     *
     * @param name The certificate name
     * @return Task
     */
    public static Task certificateUninstallAsync(Connection c, String name) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.certificate_uninstall";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Remove an SSL certificate.
     *
     * @param name The certificate name
     */
    public static void certificateUninstall(Connection c, String name) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.certificate_uninstall";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * List all installed SSL certificates.
     *
     * @return Task
     */
    public static Task certificateListAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.certificate_list";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * List all installed SSL certificates.
     *
     * @return All installed certificates
     */
    public static Set<String> certificateList(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.certificate_list";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Install an SSL certificate revocation list, pool-wide.
     *
     * @param name A name to give the CRL
     * @param cert The CRL
     * @return Task
     */
    public static Task crlInstallAsync(Connection c, String name, String cert) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.crl_install";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(cert)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Install an SSL certificate revocation list, pool-wide.
     *
     * @param name A name to give the CRL
     * @param cert The CRL
     */
    public static void crlInstall(Connection c, String name, String cert) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.crl_install";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(cert)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove an SSL certificate revocation list.
     *
     * @param name The CRL name
     * @return Task
     */
    public static Task crlUninstallAsync(Connection c, String name) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.crl_uninstall";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Remove an SSL certificate revocation list.
     *
     * @param name The CRL name
     */
    public static void crlUninstall(Connection c, String name) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.crl_uninstall";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * List all installed SSL certificate revocation lists.
     *
     * @return Task
     */
    public static Task crlListAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.crl_list";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * List all installed SSL certificate revocation lists.
     *
     * @return All installed CRLs
     */
    public static Set<String> crlList(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.crl_list";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Sync SSL certificates from master to slaves.
     *
     * @return Task
     */
    public static Task certificateSyncAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.certificate_sync";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Sync SSL certificates from master to slaves.
     *
     */
    public static void certificateSync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.certificate_sync";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Enable the redo log on the given SR and start using it, unless HA is enabled.
     *
     * @param sr SR to hold the redo log.
     * @return Task
     */
    public static Task enableRedoLogAsync(Connection c, SR sr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.enable_redo_log";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Enable the redo log on the given SR and start using it, unless HA is enabled.
     *
     * @param sr SR to hold the redo log.
     */
    public static void enableRedoLog(Connection c, SR sr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.enable_redo_log";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Disable the redo log if in use, unless HA is enabled.
     *
     * @return Task
     */
    public static Task disableRedoLogAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.disable_redo_log";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Disable the redo log if in use, unless HA is enabled.
     *
     */
    public static void disableRedoLog(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.disable_redo_log";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the IP address of the vswitch controller.
     *
     * @param address IP address of the vswitch controller.
     * @return Task
     */
    public static Task setVswitchControllerAsync(Connection c, String address) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.set_vswitch_controller";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(address)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the IP address of the vswitch controller.
     *
     * @param address IP address of the vswitch controller.
     */
    public static void setVswitchController(Connection c, String address) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.set_vswitch_controller";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(address)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * This call tests if a location is valid
     *
     * @param config Location config settings to test
     * @return An XMLRPC result
     */
    public String testArchiveTarget(Connection c, Map<String, String> config) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.test_archive_target";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(config)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * This call attempts to enable pool-wide local storage caching
     *
     * @return Task
     */
    public Task enableLocalStorageCachingAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.enable_local_storage_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * This call attempts to enable pool-wide local storage caching
     *
     */
    public void enableLocalStorageCaching(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.enable_local_storage_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * This call disables pool-wide local storage caching
     *
     * @return Task
     */
    public Task disableLocalStorageCachingAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.disable_local_storage_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * This call disables pool-wide local storage caching
     *
     */
    public void disableLocalStorageCaching(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.disable_local_storage_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * This call returns the license state for the pool
     *
     * @return Task
     */
    public Task getLicenseStateAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.get_license_state";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * This call returns the license state for the pool
     *
     * @return The pool's license state
     */
    public Map<String, String> getLicenseState(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_license_state";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Apply an edition to all hosts in the pool
     *
     * @param edition The requested edition
     * @return Task
     */
    public Task applyEditionAsync(Connection c, String edition) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.pool.apply_edition";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(edition)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Apply an edition to all hosts in the pool
     *
     * @param edition The requested edition
     */
    public void applyEdition(Connection c, String edition) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.apply_edition";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(edition)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Return a list of all the pools known to the system.
     *
     * @return references to all objects
     */
    public static Set<Pool> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPool(result);
    }

    /**
     * Return a map of pool references to pool records for all pools known to the system.
     *
     * @return records of all objects
     */
    public static Map<Pool, Pool.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "pool.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfPoolPoolRecord(result);
    }

}