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
 * A physical GPU (pGPU)
 *
 * @author Citrix Systems, Inc.
 */
public class PGPU extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    PGPU(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a PGPU, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof PGPU)
        {
            PGPU other = (PGPU) obj;
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
     * Represents all the fields in a PGPU
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "PCI", this.PCI);
            print.printf("%1$20s: %2$s\n", "GPUGroup", this.GPUGroup);
            print.printf("%1$20s: %2$s\n", "host", this.host);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "supportedVGPUTypes", this.supportedVGPUTypes);
            print.printf("%1$20s: %2$s\n", "enabledVGPUTypes", this.enabledVGPUTypes);
            print.printf("%1$20s: %2$s\n", "residentVGPUs", this.residentVGPUs);
            print.printf("%1$20s: %2$s\n", "supportedVGPUMaxCapacities", this.supportedVGPUMaxCapacities);
            return writer.toString();
        }

        /**
         * Convert a PGPU.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("PCI", this.PCI == null ? new PCI("OpaqueRef:NULL") : this.PCI);
            map.put("GPU_group", this.GPUGroup == null ? new GPUGroup("OpaqueRef:NULL") : this.GPUGroup);
            map.put("host", this.host == null ? new Host("OpaqueRef:NULL") : this.host);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("supported_VGPU_types", this.supportedVGPUTypes == null ? new LinkedHashSet<VGPUType>() : this.supportedVGPUTypes);
            map.put("enabled_VGPU_types", this.enabledVGPUTypes == null ? new LinkedHashSet<VGPUType>() : this.enabledVGPUTypes);
            map.put("resident_VGPUs", this.residentVGPUs == null ? new LinkedHashSet<VGPU>() : this.residentVGPUs);
            map.put("supported_VGPU_max_capacities", this.supportedVGPUMaxCapacities == null ? new HashMap<VGPUType, Long>() : this.supportedVGPUMaxCapacities);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * Link to underlying PCI device
         */
        public PCI PCI;
        /**
         * GPU group the pGPU is contained in
         */
        public GPUGroup GPUGroup;
        /**
         * Host that own the GPU
         */
        public Host host;
        /**
         * Additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * List of VGPU types supported by the underlying hardware
         */
        public Set<VGPUType> supportedVGPUTypes;
        /**
         * List of VGPU types which have been enabled for this PGPU
         */
        public Set<VGPUType> enabledVGPUTypes;
        /**
         * List of VGPUs running on this PGPU
         */
        public Set<VGPU> residentVGPUs;
        /**
         * A map relating each VGPU type supported on this GPU to the maximum number of VGPUs of that type which can run simultaneously on this GPU
         */
        public Map<VGPUType, Long> supportedVGPUMaxCapacities;
    }

    /**
     * Get a record containing the current state of the given PGPU.
     *
     * @return all fields from the object
     */
    public PGPU.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPGPURecord(result);
    }

    /**
     * Get a reference to the PGPU instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static PGPU getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPGPU(result);
    }

    /**
     * Get the uuid field of the given PGPU.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the PCI field of the given PGPU.
     *
     * @return value of the field
     */
    public PCI getPCI(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_PCI";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPCI(result);
    }

    /**
     * Get the GPU_group field of the given PGPU.
     *
     * @return value of the field
     */
    public GPUGroup getGPUGroup(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_GPU_group";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toGPUGroup(result);
    }

    /**
     * Get the host field of the given PGPU.
     *
     * @return value of the field
     */
    public Host getHost(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_host";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toHost(result);
    }

    /**
     * Get the other_config field of the given PGPU.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the supported_VGPU_types field of the given PGPU.
     *
     * @return value of the field
     */
    public Set<VGPUType> getSupportedVGPUTypes(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_supported_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVGPUType(result);
    }

    /**
     * Get the enabled_VGPU_types field of the given PGPU.
     *
     * @return value of the field
     */
    public Set<VGPUType> getEnabledVGPUTypes(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVGPUType(result);
    }

    /**
     * Get the resident_VGPUs field of the given PGPU.
     *
     * @return value of the field
     */
    public Set<VGPU> getResidentVGPUs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_resident_VGPUs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVGPU(result);
    }

    /**
     * Get the supported_VGPU_max_capacities field of the given PGPU.
     *
     * @return value of the field
     */
    public Map<VGPUType, Long> getSupportedVGPUMaxCapacities(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_supported_VGPU_max_capacities";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVGPUTypeLong(result);
    }

    /**
     * Set the other_config field of the given PGPU.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given PGPU.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given PGPU.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * 
     *
     * @param value The VGPU type to enable
     * @return Task
     */
    public Task addEnabledVGPUTypesAsync(Connection c, VGPUType value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PGPU.add_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * 
     *
     * @param value The VGPU type to enable
     */
    public void addEnabledVGPUTypes(Connection c, VGPUType value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.add_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * 
     *
     * @param value The VGPU type to disable
     * @return Task
     */
    public Task removeEnabledVGPUTypesAsync(Connection c, VGPUType value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PGPU.remove_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * 
     *
     * @param value The VGPU type to disable
     */
    public void removeEnabledVGPUTypes(Connection c, VGPUType value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.remove_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * 
     *
     * @param value The VGPU types to enable
     * @return Task
     */
    public Task setEnabledVGPUTypesAsync(Connection c, Set<VGPUType> value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PGPU.set_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * 
     *
     * @param value The VGPU types to enable
     */
    public void setEnabledVGPUTypes(Connection c, Set<VGPUType> value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.set_enabled_VGPU_types";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * 
     *
     * @param value The group to which the PGPU will be moved
     * @return Task
     */
    public Task setGPUGroupAsync(Connection c, GPUGroup value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PGPU.set_GPU_group";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * 
     *
     * @param value The group to which the PGPU will be moved
     */
    public void setGPUGroup(Connection c, GPUGroup value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.set_GPU_group";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * 
     *
     * @param vgpuType The VGPU type for which we want to find the number of VGPUs which can still be started on this PGPU
     * @return Task
     */
    public Task getRemainingCapacityAsync(Connection c, VGPUType vgpuType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PGPU.get_remaining_capacity";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(vgpuType)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * 
     *
     * @param vgpuType The VGPU type for which we want to find the number of VGPUs which can still be started on this PGPU
     * @return The number of VGPUs of the specified type which can still be started on this PGPU
     */
    public Long getRemainingCapacity(Connection c, VGPUType vgpuType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_remaining_capacity";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(vgpuType)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Return a list of all the PGPUs known to the system.
     *
     * @return references to all objects
     */
    public static Set<PGPU> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPGPU(result);
    }

    /**
     * Return a map of PGPU references to PGPU records for all PGPUs known to the system.
     *
     * @return records of all objects
     */
    public static Map<PGPU, PGPU.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PGPU.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfPGPUPGPURecord(result);
    }

}