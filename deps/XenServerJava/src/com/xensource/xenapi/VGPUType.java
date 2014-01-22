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
 * A type of virtual GPU
 *
 * @author Citrix Systems, Inc.
 */
public class VGPUType extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    VGPUType(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a VGPUType, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof VGPUType)
        {
            VGPUType other = (VGPUType) obj;
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
     * Represents all the fields in a VGPUType
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "vendorName", this.vendorName);
            print.printf("%1$20s: %2$s\n", "modelName", this.modelName);
            print.printf("%1$20s: %2$s\n", "framebufferSize", this.framebufferSize);
            print.printf("%1$20s: %2$s\n", "maxHeads", this.maxHeads);
            print.printf("%1$20s: %2$s\n", "maxResolutionX", this.maxResolutionX);
            print.printf("%1$20s: %2$s\n", "maxResolutionY", this.maxResolutionY);
            print.printf("%1$20s: %2$s\n", "supportedOnPGPUs", this.supportedOnPGPUs);
            print.printf("%1$20s: %2$s\n", "enabledOnPGPUs", this.enabledOnPGPUs);
            print.printf("%1$20s: %2$s\n", "VGPUs", this.VGPUs);
            print.printf("%1$20s: %2$s\n", "supportedOnGPUGroups", this.supportedOnGPUGroups);
            print.printf("%1$20s: %2$s\n", "enabledOnGPUGroups", this.enabledOnGPUGroups);
            return writer.toString();
        }

        /**
         * Convert a VGPU_type.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("vendor_name", this.vendorName == null ? "" : this.vendorName);
            map.put("model_name", this.modelName == null ? "" : this.modelName);
            map.put("framebuffer_size", this.framebufferSize == null ? 0 : this.framebufferSize);
            map.put("max_heads", this.maxHeads == null ? 0 : this.maxHeads);
            map.put("max_resolution_x", this.maxResolutionX == null ? 0 : this.maxResolutionX);
            map.put("max_resolution_y", this.maxResolutionY == null ? 0 : this.maxResolutionY);
            map.put("supported_on_PGPUs", this.supportedOnPGPUs == null ? new LinkedHashSet<PGPU>() : this.supportedOnPGPUs);
            map.put("enabled_on_PGPUs", this.enabledOnPGPUs == null ? new LinkedHashSet<PGPU>() : this.enabledOnPGPUs);
            map.put("VGPUs", this.VGPUs == null ? new LinkedHashSet<VGPU>() : this.VGPUs);
            map.put("supported_on_GPU_groups", this.supportedOnGPUGroups == null ? new LinkedHashSet<GPUGroup>() : this.supportedOnGPUGroups);
            map.put("enabled_on_GPU_groups", this.enabledOnGPUGroups == null ? new LinkedHashSet<GPUGroup>() : this.enabledOnGPUGroups);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * Name of VGPU vendor
         */
        public String vendorName;
        /**
         * Model name associated with the VGPU type
         */
        public String modelName;
        /**
         * Framebuffer size of the VGPU type, in bytes
         */
        public Long framebufferSize;
        /**
         * Maximum number of displays supported by the VGPU type
         */
        public Long maxHeads;
        /**
         * Maximum resultion (width) supported by the VGPU type
         */
        public Long maxResolutionX;
        /**
         * Maximum resoltion (height) supported by the VGPU type
         */
        public Long maxResolutionY;
        /**
         * List of PGPUs that support this VGPU type
         */
        public Set<PGPU> supportedOnPGPUs;
        /**
         * List of PGPUs that have this VGPU type enabled
         */
        public Set<PGPU> enabledOnPGPUs;
        /**
         * List of VGPUs of this type
         */
        public Set<VGPU> VGPUs;
        /**
         * List of GPU groups in which at least one PGPU supports this VGPU type
         */
        public Set<GPUGroup> supportedOnGPUGroups;
        /**
         * List of GPU groups in which at least one have this VGPU type enabled
         */
        public Set<GPUGroup> enabledOnGPUGroups;
    }

    /**
     * Get a record containing the current state of the given VGPU_type.
     *
     * @return all fields from the object
     */
    public VGPUType.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVGPUTypeRecord(result);
    }

    /**
     * Get a reference to the VGPU_type instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VGPUType getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVGPUType(result);
    }

    /**
     * Get the uuid field of the given VGPU_type.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the vendor_name field of the given VGPU_type.
     *
     * @return value of the field
     */
    public String getVendorName(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_vendor_name";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the model_name field of the given VGPU_type.
     *
     * @return value of the field
     */
    public String getModelName(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_model_name";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the framebuffer_size field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Long getFramebufferSize(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_framebuffer_size";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the max_heads field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Long getMaxHeads(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_max_heads";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the max_resolution_x field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Long getMaxResolutionX(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_max_resolution_x";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the max_resolution_y field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Long getMaxResolutionY(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_max_resolution_y";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the supported_on_PGPUs field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Set<PGPU> getSupportedOnPGPUs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_supported_on_PGPUs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPGPU(result);
    }

    /**
     * Get the enabled_on_PGPUs field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Set<PGPU> getEnabledOnPGPUs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_enabled_on_PGPUs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPGPU(result);
    }

    /**
     * Get the VGPUs field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Set<VGPU> getVGPUs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_VGPUs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVGPU(result);
    }

    /**
     * Get the supported_on_GPU_groups field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Set<GPUGroup> getSupportedOnGPUGroups(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_supported_on_GPU_groups";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfGPUGroup(result);
    }

    /**
     * Get the enabled_on_GPU_groups field of the given VGPU_type.
     *
     * @return value of the field
     */
    public Set<GPUGroup> getEnabledOnGPUGroups(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_enabled_on_GPU_groups";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfGPUGroup(result);
    }

    /**
     * Return a list of all the VGPU_types known to the system.
     *
     * @return references to all objects
     */
    public static Set<VGPUType> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVGPUType(result);
    }

    /**
     * Return a map of VGPU_type references to VGPU_type records for all VGPU_types known to the system.
     *
     * @return records of all objects
     */
    public static Map<VGPUType, VGPUType.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VGPU_type.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVGPUTypeVGPUTypeRecord(result);
    }

}