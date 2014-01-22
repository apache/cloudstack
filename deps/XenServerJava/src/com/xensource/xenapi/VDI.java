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
 * A virtual disk image
 *
 * @author Citrix Systems, Inc.
 */
public class VDI extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    VDI(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a VDI, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof VDI)
        {
            VDI other = (VDI) obj;
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
     * Represents all the fields in a VDI
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "allowedOperations", this.allowedOperations);
            print.printf("%1$20s: %2$s\n", "currentOperations", this.currentOperations);
            print.printf("%1$20s: %2$s\n", "SR", this.SR);
            print.printf("%1$20s: %2$s\n", "VBDs", this.VBDs);
            print.printf("%1$20s: %2$s\n", "crashDumps", this.crashDumps);
            print.printf("%1$20s: %2$s\n", "virtualSize", this.virtualSize);
            print.printf("%1$20s: %2$s\n", "physicalUtilisation", this.physicalUtilisation);
            print.printf("%1$20s: %2$s\n", "type", this.type);
            print.printf("%1$20s: %2$s\n", "sharable", this.sharable);
            print.printf("%1$20s: %2$s\n", "readOnly", this.readOnly);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "storageLock", this.storageLock);
            print.printf("%1$20s: %2$s\n", "location", this.location);
            print.printf("%1$20s: %2$s\n", "managed", this.managed);
            print.printf("%1$20s: %2$s\n", "missing", this.missing);
            print.printf("%1$20s: %2$s\n", "parent", this.parent);
            print.printf("%1$20s: %2$s\n", "xenstoreData", this.xenstoreData);
            print.printf("%1$20s: %2$s\n", "smConfig", this.smConfig);
            print.printf("%1$20s: %2$s\n", "isASnapshot", this.isASnapshot);
            print.printf("%1$20s: %2$s\n", "snapshotOf", this.snapshotOf);
            print.printf("%1$20s: %2$s\n", "snapshots", this.snapshots);
            print.printf("%1$20s: %2$s\n", "snapshotTime", this.snapshotTime);
            print.printf("%1$20s: %2$s\n", "tags", this.tags);
            print.printf("%1$20s: %2$s\n", "allowCaching", this.allowCaching);
            print.printf("%1$20s: %2$s\n", "onBoot", this.onBoot);
            print.printf("%1$20s: %2$s\n", "metadataOfPool", this.metadataOfPool);
            print.printf("%1$20s: %2$s\n", "metadataLatest", this.metadataLatest);
            return writer.toString();
        }

        /**
         * Convert a VDI.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("allowed_operations", this.allowedOperations == null ? new LinkedHashSet<Types.VdiOperations>() : this.allowedOperations);
            map.put("current_operations", this.currentOperations == null ? new HashMap<String, Types.VdiOperations>() : this.currentOperations);
            map.put("SR", this.SR == null ? new SR("OpaqueRef:NULL") : this.SR);
            map.put("VBDs", this.VBDs == null ? new LinkedHashSet<VBD>() : this.VBDs);
            map.put("crash_dumps", this.crashDumps == null ? new LinkedHashSet<Crashdump>() : this.crashDumps);
            map.put("virtual_size", this.virtualSize == null ? 0 : this.virtualSize);
            map.put("physical_utilisation", this.physicalUtilisation == null ? 0 : this.physicalUtilisation);
            map.put("type", this.type == null ? Types.VdiType.UNRECOGNIZED : this.type);
            map.put("sharable", this.sharable == null ? false : this.sharable);
            map.put("read_only", this.readOnly == null ? false : this.readOnly);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("storage_lock", this.storageLock == null ? false : this.storageLock);
            map.put("location", this.location == null ? "" : this.location);
            map.put("managed", this.managed == null ? false : this.managed);
            map.put("missing", this.missing == null ? false : this.missing);
            map.put("parent", this.parent == null ? new VDI("OpaqueRef:NULL") : this.parent);
            map.put("xenstore_data", this.xenstoreData == null ? new HashMap<String, String>() : this.xenstoreData);
            map.put("sm_config", this.smConfig == null ? new HashMap<String, String>() : this.smConfig);
            map.put("is_a_snapshot", this.isASnapshot == null ? false : this.isASnapshot);
            map.put("snapshot_of", this.snapshotOf == null ? new VDI("OpaqueRef:NULL") : this.snapshotOf);
            map.put("snapshots", this.snapshots == null ? new LinkedHashSet<VDI>() : this.snapshots);
            map.put("snapshot_time", this.snapshotTime == null ? new Date(0) : this.snapshotTime);
            map.put("tags", this.tags == null ? new LinkedHashSet<String>() : this.tags);
            map.put("allow_caching", this.allowCaching == null ? false : this.allowCaching);
            map.put("on_boot", this.onBoot == null ? Types.OnBoot.UNRECOGNIZED : this.onBoot);
            map.put("metadata_of_pool", this.metadataOfPool == null ? new Pool("OpaqueRef:NULL") : this.metadataOfPool);
            map.put("metadata_latest", this.metadataLatest == null ? false : this.metadataLatest);
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
         * list of the operations allowed in this state. This list is advisory only and the server state may have changed by the time this field is read by a client.
         */
        public Set<Types.VdiOperations> allowedOperations;
        /**
         * links each of the running tasks using this object (by reference) to a current_operation enum which describes the nature of the task.
         */
        public Map<String, Types.VdiOperations> currentOperations;
        /**
         * storage repository in which the VDI resides
         */
        public SR SR;
        /**
         * list of vbds that refer to this disk
         */
        public Set<VBD> VBDs;
        /**
         * list of crash dumps that refer to this disk
         */
        public Set<Crashdump> crashDumps;
        /**
         * size of disk as presented to the guest (in bytes). Note that, depending on storage backend type, requested size may not be respected exactly
         */
        public Long virtualSize;
        /**
         * amount of physical space that the disk image is currently taking up on the storage repository (in bytes)
         */
        public Long physicalUtilisation;
        /**
         * type of the VDI
         */
        public Types.VdiType type;
        /**
         * true if this disk may be shared
         */
        public Boolean sharable;
        /**
         * true if this disk may ONLY be mounted read-only
         */
        public Boolean readOnly;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * true if this disk is locked at the storage level
         */
        public Boolean storageLock;
        /**
         * location information
         */
        public String location;
        /**
         * 
         */
        public Boolean managed;
        /**
         * true if SR scan operation reported this VDI as not present on disk
         */
        public Boolean missing;
        /**
         * References the parent disk, if this VDI is part of a chain
         */
        public VDI parent;
        /**
         * data to be inserted into the xenstore tree (/local/domain/0/backend/vbd/<domid>/<device-id>/sm-data) after the VDI is attached. This is generally set by the SM backends on vdi_attach.
         */
        public Map<String, String> xenstoreData;
        /**
         * SM dependent data
         */
        public Map<String, String> smConfig;
        /**
         * true if this is a snapshot.
         */
        public Boolean isASnapshot;
        /**
         * Ref pointing to the VDI this snapshot is of.
         */
        public VDI snapshotOf;
        /**
         * List pointing to all the VDIs snapshots.
         */
        public Set<VDI> snapshots;
        /**
         * Date/time when this snapshot was created.
         */
        public Date snapshotTime;
        /**
         * user-specified tags for categorization purposes
         */
        public Set<String> tags;
        /**
         * true if this VDI is to be cached in the local cache SR
         */
        public Boolean allowCaching;
        /**
         * The behaviour of this VDI on a VM boot
         */
        public Types.OnBoot onBoot;
        /**
         * The pool whose metadata is contained in this VDI
         */
        public Pool metadataOfPool;
        /**
         * Whether this VDI contains the latest known accessible metadata for the pool
         */
        public Boolean metadataLatest;
    }

    /**
     * Get a record containing the current state of the given VDI.
     *
     * @return all fields from the object
     */
    public VDI.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDIRecord(result);
    }

    /**
     * Get a reference to the VDI instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VDI getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Create a new VDI instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, VDI.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VDI instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VDI create(Connection c, VDI.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Destroy the specified VDI instance.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the specified VDI instance.
     *
     */
    public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Get all the VDI instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<VDI> getByNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVDI(result);
    }

    /**
     * Get the uuid field of the given VDI.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/label field of the given VDI.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/description field of the given VDI.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the allowed_operations field of the given VDI.
     *
     * @return value of the field
     */
    public Set<Types.VdiOperations> getAllowedOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVdiOperations(result);
    }

    /**
     * Get the current_operations field of the given VDI.
     *
     * @return value of the field
     */
    public Map<String, Types.VdiOperations> getCurrentOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_current_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringVdiOperations(result);
    }

    /**
     * Get the SR field of the given VDI.
     *
     * @return value of the field
     */
    public SR getSR(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSR(result);
    }

    /**
     * Get the VBDs field of the given VDI.
     *
     * @return value of the field
     */
    public Set<VBD> getVBDs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_VBDs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVBD(result);
    }

    /**
     * Get the crash_dumps field of the given VDI.
     *
     * @return value of the field
     */
    public Set<Crashdump> getCrashDumps(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_crash_dumps";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfCrashdump(result);
    }

    /**
     * Get the virtual_size field of the given VDI.
     *
     * @return value of the field
     */
    public Long getVirtualSize(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_virtual_size";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the physical_utilisation field of the given VDI.
     *
     * @return value of the field
     */
    public Long getPhysicalUtilisation(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_physical_utilisation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the type field of the given VDI.
     *
     * @return value of the field
     */
    public Types.VdiType getType(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVdiType(result);
    }

    /**
     * Get the sharable field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getSharable(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_sharable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the read_only field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getReadOnly(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_read_only";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the other_config field of the given VDI.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the storage_lock field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getStorageLock(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_storage_lock";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the location field of the given VDI.
     *
     * @return value of the field
     */
    public String getLocation(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_location";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the managed field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getManaged(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_managed";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the missing field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getMissing(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_missing";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the parent field of the given VDI.
     *
     * @return value of the field
     */
    public VDI getParent(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_parent";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Get the xenstore_data field of the given VDI.
     *
     * @return value of the field
     */
    public Map<String, String> getXenstoreData(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the sm_config field of the given VDI.
     *
     * @return value of the field
     */
    public Map<String, String> getSmConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_sm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the is_a_snapshot field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getIsASnapshot(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_is_a_snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the snapshot_of field of the given VDI.
     *
     * @return value of the field
     */
    public VDI getSnapshotOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_snapshot_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Get the snapshots field of the given VDI.
     *
     * @return value of the field
     */
    public Set<VDI> getSnapshots(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_snapshots";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVDI(result);
    }

    /**
     * Get the snapshot_time field of the given VDI.
     *
     * @return value of the field
     */
    public Date getSnapshotTime(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_snapshot_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDate(result);
    }

    /**
     * Get the tags field of the given VDI.
     *
     * @return value of the field
     */
    public Set<String> getTags(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the allow_caching field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getAllowCaching(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_allow_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the on_boot field of the given VDI.
     *
     * @return value of the field
     */
    public Types.OnBoot getOnBoot(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_on_boot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toOnBoot(result);
    }

    /**
     * Get the metadata_of_pool field of the given VDI.
     *
     * @return value of the field
     */
    public Pool getMetadataOfPool(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_metadata_of_pool";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPool(result);
    }

    /**
     * Get the metadata_latest field of the given VDI.
     *
     * @return value of the field
     */
    public Boolean getMetadataLatest(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_metadata_latest";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Set the other_config field of the given VDI.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given VDI.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given VDI.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the xenstore_data field of the given VDI.
     *
     * @param xenstoreData New value to set
     */
    public void setXenstoreData(Connection c, Map<String, String> xenstoreData) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(xenstoreData)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the xenstore_data field of the given VDI.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToXenstoreData(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.add_to_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the xenstore_data field of the given VDI.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromXenstoreData(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.remove_from_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the sm_config field of the given VDI.
     *
     * @param smConfig New value to set
     */
    public void setSmConfig(Connection c, Map<String, String> smConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_sm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(smConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the sm_config field of the given VDI.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToSmConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.add_to_sm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the sm_config field of the given VDI.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromSmConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.remove_from_sm_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the tags field of the given VDI.
     *
     * @param tags New value to set
     */
    public void setTags(Connection c, Set<String> tags) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(tags)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given value to the tags field of the given VDI.  If the value is already in that Set, then do nothing.
     *
     * @param value New value to add
     */
    public void addTags(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.add_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given value from the tags field of the given VDI.  If the value is not in that Set, then do nothing.
     *
     * @param value Value to remove
     */
    public void removeTags(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.remove_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Take a read-only snapshot of the VDI, returning a reference to the snapshot. If any driver_params are specified then these are passed through to the storage-specific substrate driver that takes the snapshot. NB the snapshot lives in the same Storage Repository as its parent.
     *
     * @param driverParams Optional parameters that can be passed through to backend driver in order to specify storage-type-specific snapshot options
     * @return Task
     */
    public Task snapshotAsync(Connection c, Map<String, String> driverParams) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(driverParams)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Take a read-only snapshot of the VDI, returning a reference to the snapshot. If any driver_params are specified then these are passed through to the storage-specific substrate driver that takes the snapshot. NB the snapshot lives in the same Storage Repository as its parent.
     *
     * @param driverParams Optional parameters that can be passed through to backend driver in order to specify storage-type-specific snapshot options
     * @return The ID of the newly created VDI.
     */
    public VDI snapshot(Connection c, Map<String, String> driverParams) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(driverParams)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Take an exact copy of the VDI and return a reference to the new disk. If any driver_params are specified then these are passed through to the storage-specific substrate driver that implements the clone operation. NB the clone lives in the same Storage Repository as its parent.
     *
     * @param driverParams Optional parameters that are passed through to the backend driver in order to specify storage-type-specific clone options
     * @return Task
     */
    public Task createCloneAsync(Connection c, Map<String, String> driverParams) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.clone";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(driverParams)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Take an exact copy of the VDI and return a reference to the new disk. If any driver_params are specified then these are passed through to the storage-specific substrate driver that implements the clone operation. NB the clone lives in the same Storage Repository as its parent.
     *
     * @param driverParams Optional parameters that are passed through to the backend driver in order to specify storage-type-specific clone options
     * @return The ID of the newly created VDI.
     */
    public VDI createClone(Connection c, Map<String, String> driverParams) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.clone";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(driverParams)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Resize the VDI.
     *
     * @param size The new size of the VDI
     * @return Task
     */
    public Task resizeAsync(Connection c, Long size) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.resize";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(size)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Resize the VDI.
     *
     * @param size The new size of the VDI
     */
    public void resize(Connection c, Long size) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.resize";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(size)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Resize the VDI which may or may not be attached to running guests.
     *
     * @param size The new size of the VDI
     * @return Task
     */
    public Task resizeOnlineAsync(Connection c, Long size) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.resize_online";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(size)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Resize the VDI which may or may not be attached to running guests.
     *
     * @param size The new size of the VDI
     */
    public void resizeOnline(Connection c, Long size) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.resize_online";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(size)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Create a new VDI record in the database only
     *
     * @param uuid The uuid of the disk to introduce
     * @param nameLabel The name of the disk record
     * @param nameDescription The description of the disk record
     * @param SR The SR that the VDI is in
     * @param type The type of the VDI
     * @param sharable true if this disk may be shared
     * @param readOnly true if this disk may ONLY be mounted read-only
     * @param otherConfig additional configuration
     * @param location location information
     * @param xenstoreData Data to insert into xenstore
     * @param smConfig Storage-specific config
     * @param managed Storage-specific config
     * @param virtualSize Storage-specific config
     * @param physicalUtilisation Storage-specific config
     * @param metadataOfPool Storage-specific config
     * @param isASnapshot Storage-specific config
     * @param snapshotTime Storage-specific config
     * @param snapshotOf Storage-specific config
     * @return Task
     */
    public static Task introduceAsync(Connection c, String uuid, String nameLabel, String nameDescription, SR SR, Types.VdiType type, Boolean sharable, Boolean readOnly, Map<String, String> otherConfig, String location, Map<String, String> xenstoreData, Map<String, String> smConfig, Boolean managed, Long virtualSize, Long physicalUtilisation, Pool metadataOfPool, Boolean isASnapshot, Date snapshotTime, VDI snapshotOf) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.SrOperationNotSupported {
        String method_call = "Async.VDI.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(SR), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(sharable), Marshalling.toXMLRPC(readOnly), Marshalling.toXMLRPC(otherConfig), Marshalling.toXMLRPC(location), Marshalling.toXMLRPC(xenstoreData), Marshalling.toXMLRPC(smConfig), Marshalling.toXMLRPC(managed), Marshalling.toXMLRPC(virtualSize), Marshalling.toXMLRPC(physicalUtilisation), Marshalling.toXMLRPC(metadataOfPool), Marshalling.toXMLRPC(isASnapshot), Marshalling.toXMLRPC(snapshotTime), Marshalling.toXMLRPC(snapshotOf)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VDI record in the database only
     *
     * @param uuid The uuid of the disk to introduce
     * @param nameLabel The name of the disk record
     * @param nameDescription The description of the disk record
     * @param SR The SR that the VDI is in
     * @param type The type of the VDI
     * @param sharable true if this disk may be shared
     * @param readOnly true if this disk may ONLY be mounted read-only
     * @param otherConfig additional configuration
     * @param location location information
     * @param xenstoreData Data to insert into xenstore
     * @param smConfig Storage-specific config
     * @param managed Storage-specific config
     * @param virtualSize Storage-specific config
     * @param physicalUtilisation Storage-specific config
     * @param metadataOfPool Storage-specific config
     * @param isASnapshot Storage-specific config
     * @param snapshotTime Storage-specific config
     * @param snapshotOf Storage-specific config
     * @return The ref of the newly created VDI record.
     */
    public static VDI introduce(Connection c, String uuid, String nameLabel, String nameDescription, SR SR, Types.VdiType type, Boolean sharable, Boolean readOnly, Map<String, String> otherConfig, String location, Map<String, String> xenstoreData, Map<String, String> smConfig, Boolean managed, Long virtualSize, Long physicalUtilisation, Pool metadataOfPool, Boolean isASnapshot, Date snapshotTime, VDI snapshotOf) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.SrOperationNotSupported {
        String method_call = "VDI.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(SR), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(sharable), Marshalling.toXMLRPC(readOnly), Marshalling.toXMLRPC(otherConfig), Marshalling.toXMLRPC(location), Marshalling.toXMLRPC(xenstoreData), Marshalling.toXMLRPC(smConfig), Marshalling.toXMLRPC(managed), Marshalling.toXMLRPC(virtualSize), Marshalling.toXMLRPC(physicalUtilisation), Marshalling.toXMLRPC(metadataOfPool), Marshalling.toXMLRPC(isASnapshot), Marshalling.toXMLRPC(snapshotTime), Marshalling.toXMLRPC(snapshotOf)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Create a new VDI record in the database only
     *
     * @param uuid The uuid of the disk to introduce
     * @param nameLabel The name of the disk record
     * @param nameDescription The description of the disk record
     * @param SR The SR that the VDI is in
     * @param type The type of the VDI
     * @param sharable true if this disk may be shared
     * @param readOnly true if this disk may ONLY be mounted read-only
     * @param otherConfig additional configuration
     * @param location location information
     * @param xenstoreData Data to insert into xenstore
     * @param smConfig Storage-specific config
     * @param managed Storage-specific config
     * @param virtualSize Storage-specific config
     * @param physicalUtilisation Storage-specific config
     * @param metadataOfPool Storage-specific config
     * @param isASnapshot Storage-specific config
     * @param snapshotTime Storage-specific config
     * @param snapshotOf Storage-specific config
     * @return Task
     */
    public static Task dbIntroduceAsync(Connection c, String uuid, String nameLabel, String nameDescription, SR SR, Types.VdiType type, Boolean sharable, Boolean readOnly, Map<String, String> otherConfig, String location, Map<String, String> xenstoreData, Map<String, String> smConfig, Boolean managed, Long virtualSize, Long physicalUtilisation, Pool metadataOfPool, Boolean isASnapshot, Date snapshotTime, VDI snapshotOf) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.db_introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(SR), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(sharable), Marshalling.toXMLRPC(readOnly), Marshalling.toXMLRPC(otherConfig), Marshalling.toXMLRPC(location), Marshalling.toXMLRPC(xenstoreData), Marshalling.toXMLRPC(smConfig), Marshalling.toXMLRPC(managed), Marshalling.toXMLRPC(virtualSize), Marshalling.toXMLRPC(physicalUtilisation), Marshalling.toXMLRPC(metadataOfPool), Marshalling.toXMLRPC(isASnapshot), Marshalling.toXMLRPC(snapshotTime), Marshalling.toXMLRPC(snapshotOf)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VDI record in the database only
     *
     * @param uuid The uuid of the disk to introduce
     * @param nameLabel The name of the disk record
     * @param nameDescription The description of the disk record
     * @param SR The SR that the VDI is in
     * @param type The type of the VDI
     * @param sharable true if this disk may be shared
     * @param readOnly true if this disk may ONLY be mounted read-only
     * @param otherConfig additional configuration
     * @param location location information
     * @param xenstoreData Data to insert into xenstore
     * @param smConfig Storage-specific config
     * @param managed Storage-specific config
     * @param virtualSize Storage-specific config
     * @param physicalUtilisation Storage-specific config
     * @param metadataOfPool Storage-specific config
     * @param isASnapshot Storage-specific config
     * @param snapshotTime Storage-specific config
     * @param snapshotOf Storage-specific config
     * @return The ref of the newly created VDI record.
     */
    public static VDI dbIntroduce(Connection c, String uuid, String nameLabel, String nameDescription, SR SR, Types.VdiType type, Boolean sharable, Boolean readOnly, Map<String, String> otherConfig, String location, Map<String, String> xenstoreData, Map<String, String> smConfig, Boolean managed, Long virtualSize, Long physicalUtilisation, Pool metadataOfPool, Boolean isASnapshot, Date snapshotTime, VDI snapshotOf) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.db_introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid), Marshalling.toXMLRPC(nameLabel), Marshalling.toXMLRPC(nameDescription), Marshalling.toXMLRPC(SR), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(sharable), Marshalling.toXMLRPC(readOnly), Marshalling.toXMLRPC(otherConfig), Marshalling.toXMLRPC(location), Marshalling.toXMLRPC(xenstoreData), Marshalling.toXMLRPC(smConfig), Marshalling.toXMLRPC(managed), Marshalling.toXMLRPC(virtualSize), Marshalling.toXMLRPC(physicalUtilisation), Marshalling.toXMLRPC(metadataOfPool), Marshalling.toXMLRPC(isASnapshot), Marshalling.toXMLRPC(snapshotTime), Marshalling.toXMLRPC(snapshotOf)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Removes a VDI record from the database
     *
     * @return Task
     */
    public Task dbForgetAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.db_forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Removes a VDI record from the database
     *
     */
    public void dbForget(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.db_forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Ask the storage backend to refresh the fields in the VDI object
     *
     * @return Task
     */
    public Task updateAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.SrOperationNotSupported {
        String method_call = "Async.VDI.update";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Ask the storage backend to refresh the fields in the VDI object
     *
     */
    public void update(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.SrOperationNotSupported {
        String method_call = "VDI.update";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Make a fresh VDI in the specified SR and copy the supplied VDI's data to the new disk
     *
     * @param sr The destination SR
     * @return Task
     */
    public Task copyAsync(Connection c, SR sr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.copy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Make a fresh VDI in the specified SR and copy the supplied VDI's data to the new disk
     *
     * @param sr The destination SR
     * @return The reference of the newly created VDI.
     */
    public VDI copy(Connection c, SR sr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.copy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Sets the VDI's managed field
     *
     * @param value The new value of the VDI's managed field
     */
    public void setManaged(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_managed";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Removes a VDI record from the database
     *
     * @return Task
     */
    public Task forgetAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Removes a VDI record from the database
     *
     */
    public void forget(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the VDI's sharable field
     *
     * @param value The new value of the VDI's sharable field
     */
    public void setSharable(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_sharable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the VDI's read_only field
     *
     * @param value The new value of the VDI's read_only field
     */
    public void setReadOnly(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_read_only";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the VDI's missing field
     *
     * @param value The new value of the VDI's missing field
     */
    public void setMissing(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_missing";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the VDI's virtual_size field
     *
     * @param value The new value of the VDI's virtual size
     */
    public void setVirtualSize(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_virtual_size";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the VDI's physical_utilisation field
     *
     * @param value The new value of the VDI's physical utilisation
     */
    public void setPhysicalUtilisation(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_physical_utilisation";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets whether this VDI is a snapshot
     *
     * @param value The new value indicating whether this VDI is a snapshot
     */
    public void setIsASnapshot(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_is_a_snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the VDI of which this VDI is a snapshot
     *
     * @param value The VDI of which this VDI is a snapshot
     */
    public void setSnapshotOf(Connection c, VDI value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_snapshot_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Sets the snapshot time of this VDI.
     *
     * @param value The snapshot time of this VDI.
     */
    public void setSnapshotTime(Connection c, Date value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_snapshot_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Records the pool whose metadata is contained by this VDI.
     *
     * @param value The pool whose metadata is contained by this VDI
     */
    public void setMetadataOfPool(Connection c, Pool value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_metadata_of_pool";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the name label of the VDI. This can only happen when then its SR is currently attached.
     *
     * @param value The name lable for the VDI
     * @return Task
     */
    public Task setNameLabelAsync(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the name label of the VDI. This can only happen when then its SR is currently attached.
     *
     * @param value The name lable for the VDI
     */
    public void setNameLabel(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the name description of the VDI. This can only happen when its SR is currently attached.
     *
     * @param value The name description for the VDI
     * @return Task
     */
    public Task setNameDescriptionAsync(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the name description of the VDI. This can only happen when its SR is currently attached.
     *
     * @param value The name description for the VDI
     */
    public void setNameDescription(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the on_boot parameter. This value can only be changed when the VDI is not attached to a running VM.
     *
     * @param value The value to set
     * @return Task
     */
    public Task setOnBootAsync(Connection c, Types.OnBoot value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.set_on_boot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the value of the on_boot parameter. This value can only be changed when the VDI is not attached to a running VM.
     *
     * @param value The value to set
     */
    public void setOnBoot(Connection c, Types.OnBoot value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_on_boot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the allow_caching parameter. This value can only be changed when the VDI is not attached to a running VM. The caching behaviour is only affected by this flag for VHD-based VDIs that have one parent and no child VHDs. Moreover, caching only takes place when the host running the VM containing this VDI has a nominated SR for local caching.
     *
     * @param value The value to set
     * @return Task
     */
    public Task setAllowCachingAsync(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.set_allow_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the value of the allow_caching parameter. This value can only be changed when the VDI is not attached to a running VM. The caching behaviour is only affected by this flag for VHD-based VDIs that have one parent and no child VHDs. Moreover, caching only takes place when the host running the VM containing this VDI has a nominated SR for local caching.
     *
     * @param value The value to set
     */
    public void setAllowCaching(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.set_allow_caching";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Load the metadata found on the supplied VDI and return a session reference which can be used in XenAPI calls to query its contents.
     *
     * @return Task
     */
    public Task openDatabaseAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.open_database";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Load the metadata found on the supplied VDI and return a session reference which can be used in XenAPI calls to query its contents.
     *
     * @return A session which can be used to query the database
     */
    public Session openDatabase(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.open_database";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSession(result);
    }

    /**
     * Check the VDI cache for the pool UUID of the database on this VDI.
     *
     * @return Task
     */
    public Task readDatabasePoolUuidAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.read_database_pool_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Check the VDI cache for the pool UUID of the database on this VDI.
     *
     * @return The cached pool UUID of the database on the VDI.
     */
    public String readDatabasePoolUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.read_database_pool_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Migrate a VDI, which may be attached to a running guest, to a different SR. The destination SR must be visible to the guest.
     *
     * @param sr The destination SR
     * @param options Other parameters
     * @return Task
     */
    public Task poolMigrateAsync(Connection c, SR sr, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VDI.pool_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sr), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Migrate a VDI, which may be attached to a running guest, to a different SR. The destination SR must be visible to the guest.
     *
     * @param sr The destination SR
     * @param options Other parameters
     * @return The new reference of the migrated VDI.
     */
    public VDI poolMigrate(Connection c, SR sr, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.pool_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sr), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Return a list of all the VDIs known to the system.
     *
     * @return references to all objects
     */
    public static Set<VDI> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVDI(result);
    }

    /**
     * Return a map of VDI references to VDI records for all VDIs known to the system.
     *
     * @return records of all objects
     */
    public static Map<VDI, VDI.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VDI.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVDIVDIRecord(result);
    }

}