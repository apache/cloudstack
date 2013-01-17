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
 * A virtual block device
 *
 * @author Citrix Systems, Inc.
 */
public class VBD extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    VBD(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a VBD, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof VBD)
        {
            VBD other = (VBD) obj;
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
     * Represents all the fields in a VBD
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "allowedOperations", this.allowedOperations);
            print.printf("%1$20s: %2$s\n", "currentOperations", this.currentOperations);
            print.printf("%1$20s: %2$s\n", "VM", this.VM);
            print.printf("%1$20s: %2$s\n", "VDI", this.VDI);
            print.printf("%1$20s: %2$s\n", "device", this.device);
            print.printf("%1$20s: %2$s\n", "userdevice", this.userdevice);
            print.printf("%1$20s: %2$s\n", "bootable", this.bootable);
            print.printf("%1$20s: %2$s\n", "mode", this.mode);
            print.printf("%1$20s: %2$s\n", "type", this.type);
            print.printf("%1$20s: %2$s\n", "unpluggable", this.unpluggable);
            print.printf("%1$20s: %2$s\n", "storageLock", this.storageLock);
            print.printf("%1$20s: %2$s\n", "empty", this.empty);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "currentlyAttached", this.currentlyAttached);
            print.printf("%1$20s: %2$s\n", "statusCode", this.statusCode);
            print.printf("%1$20s: %2$s\n", "statusDetail", this.statusDetail);
            print.printf("%1$20s: %2$s\n", "runtimeProperties", this.runtimeProperties);
            print.printf("%1$20s: %2$s\n", "qosAlgorithmType", this.qosAlgorithmType);
            print.printf("%1$20s: %2$s\n", "qosAlgorithmParams", this.qosAlgorithmParams);
            print.printf("%1$20s: %2$s\n", "qosSupportedAlgorithms", this.qosSupportedAlgorithms);
            print.printf("%1$20s: %2$s\n", "metrics", this.metrics);
            return writer.toString();
        }

        /**
         * Convert a VBD.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("allowed_operations", this.allowedOperations == null ? new LinkedHashSet<Types.VbdOperations>() : this.allowedOperations);
            map.put("current_operations", this.currentOperations == null ? new HashMap<String, Types.VbdOperations>() : this.currentOperations);
            map.put("VM", this.VM == null ? new VM("OpaqueRef:NULL") : this.VM);
            map.put("VDI", this.VDI == null ? new VDI("OpaqueRef:NULL") : this.VDI);
            map.put("device", this.device == null ? "" : this.device);
            map.put("userdevice", this.userdevice == null ? "" : this.userdevice);
            map.put("bootable", this.bootable == null ? false : this.bootable);
            map.put("mode", this.mode == null ? Types.VbdMode.UNRECOGNIZED : this.mode);
            map.put("type", this.type == null ? Types.VbdType.UNRECOGNIZED : this.type);
            map.put("unpluggable", this.unpluggable == null ? false : this.unpluggable);
            map.put("storage_lock", this.storageLock == null ? false : this.storageLock);
            map.put("empty", this.empty == null ? false : this.empty);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("currently_attached", this.currentlyAttached == null ? false : this.currentlyAttached);
            map.put("status_code", this.statusCode == null ? 0 : this.statusCode);
            map.put("status_detail", this.statusDetail == null ? "" : this.statusDetail);
            map.put("runtime_properties", this.runtimeProperties == null ? new HashMap<String, String>() : this.runtimeProperties);
            map.put("qos_algorithm_type", this.qosAlgorithmType == null ? "" : this.qosAlgorithmType);
            map.put("qos_algorithm_params", this.qosAlgorithmParams == null ? new HashMap<String, String>() : this.qosAlgorithmParams);
            map.put("qos_supported_algorithms", this.qosSupportedAlgorithms == null ? new LinkedHashSet<String>() : this.qosSupportedAlgorithms);
            map.put("metrics", this.metrics == null ? new VBDMetrics("OpaqueRef:NULL") : this.metrics);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * list of the operations allowed in this state. This list is advisory only and the server state may have changed by the time this field is read by a client.
         */
        public Set<Types.VbdOperations> allowedOperations;
        /**
         * links each of the running tasks using this object (by reference) to a current_operation enum which describes the nature of the task.
         */
        public Map<String, Types.VbdOperations> currentOperations;
        /**
         * the virtual machine
         */
        public VM VM;
        /**
         * the virtual disk
         */
        public VDI VDI;
        /**
         * device seen by the guest e.g. hda1
         */
        public String device;
        /**
         * user-friendly device name e.g. 0,1,2,etc.
         */
        public String userdevice;
        /**
         * true if this VBD is bootable
         */
        public Boolean bootable;
        /**
         * the mode the VBD should be mounted with
         */
        public Types.VbdMode mode;
        /**
         * how the VBD will appear to the guest (e.g. disk or CD)
         */
        public Types.VbdType type;
        /**
         * true if this VBD will support hot-unplug
         */
        public Boolean unpluggable;
        /**
         * true if a storage level lock was acquired
         */
        public Boolean storageLock;
        /**
         * if true this represents an empty drive
         */
        public Boolean empty;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * is the device currently attached (erased on reboot)
         */
        public Boolean currentlyAttached;
        /**
         * error/success code associated with last attach-operation (erased on reboot)
         */
        public Long statusCode;
        /**
         * error/success information associated with last attach-operation status (erased on reboot)
         */
        public String statusDetail;
        /**
         * Device runtime properties
         */
        public Map<String, String> runtimeProperties;
        /**
         * QoS algorithm to use
         */
        public String qosAlgorithmType;
        /**
         * parameters for chosen QoS algorithm
         */
        public Map<String, String> qosAlgorithmParams;
        /**
         * supported QoS algorithms for this VBD
         */
        public Set<String> qosSupportedAlgorithms;
        /**
         * metrics associated with this VBD
         */
        public VBDMetrics metrics;
    }

    /**
     * Get a record containing the current state of the given VBD.
     *
     * @return all fields from the object
     */
    public VBD.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVBDRecord(result);
    }

    /**
     * Get a reference to the VBD instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VBD getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVBD(result);
    }

    /**
     * Create a new VBD instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, VBD.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VBD.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VBD instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VBD create(Connection c, VBD.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVBD(result);
    }

    /**
     * Destroy the specified VBD instance.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VBD.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the specified VBD instance.
     *
     */
    public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Get the uuid field of the given VBD.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the allowed_operations field of the given VBD.
     *
     * @return value of the field
     */
    public Set<Types.VbdOperations> getAllowedOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVbdOperations(result);
    }

    /**
     * Get the current_operations field of the given VBD.
     *
     * @return value of the field
     */
    public Map<String, Types.VbdOperations> getCurrentOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_current_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringVbdOperations(result);
    }

    /**
     * Get the VM field of the given VBD.
     *
     * @return value of the field
     */
    public VM getVM(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_VM";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Get the VDI field of the given VBD.
     *
     * @return value of the field
     */
    public VDI getVDI(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_VDI";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Get the device field of the given VBD.
     *
     * @return value of the field
     */
    public String getDevice(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_device";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the userdevice field of the given VBD.
     *
     * @return value of the field
     */
    public String getUserdevice(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_userdevice";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the bootable field of the given VBD.
     *
     * @return value of the field
     */
    public Boolean getBootable(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_bootable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the mode field of the given VBD.
     *
     * @return value of the field
     */
    public Types.VbdMode getMode(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_mode";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVbdMode(result);
    }

    /**
     * Get the type field of the given VBD.
     *
     * @return value of the field
     */
    public Types.VbdType getType(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVbdType(result);
    }

    /**
     * Get the unpluggable field of the given VBD.
     *
     * @return value of the field
     */
    public Boolean getUnpluggable(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_unpluggable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the storage_lock field of the given VBD.
     *
     * @return value of the field
     */
    public Boolean getStorageLock(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_storage_lock";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the empty field of the given VBD.
     *
     * @return value of the field
     */
    public Boolean getEmpty(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_empty";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the other_config field of the given VBD.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the currently_attached field of the given VBD.
     *
     * @return value of the field
     */
    public Boolean getCurrentlyAttached(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_currently_attached";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the status_code field of the given VBD.
     *
     * @return value of the field
     */
    public Long getStatusCode(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_status_code";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the status_detail field of the given VBD.
     *
     * @return value of the field
     */
    public String getStatusDetail(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_status_detail";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the runtime_properties field of the given VBD.
     *
     * @return value of the field
     */
    public Map<String, String> getRuntimeProperties(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_runtime_properties";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the qos/algorithm_type field of the given VBD.
     *
     * @return value of the field
     */
    public String getQosAlgorithmType(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_qos_algorithm_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the qos/algorithm_params field of the given VBD.
     *
     * @return value of the field
     */
    public Map<String, String> getQosAlgorithmParams(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_qos_algorithm_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the qos/supported_algorithms field of the given VBD.
     *
     * @return value of the field
     */
    public Set<String> getQosSupportedAlgorithms(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_qos_supported_algorithms";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the metrics field of the given VBD.
     *
     * @return value of the field
     */
    public VBDMetrics getMetrics(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVBDMetrics(result);
    }

    /**
     * Set the userdevice field of the given VBD.
     *
     * @param userdevice New value to set
     */
    public void setUserdevice(Connection c, String userdevice) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_userdevice";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(userdevice)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the bootable field of the given VBD.
     *
     * @param bootable New value to set
     */
    public void setBootable(Connection c, Boolean bootable) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_bootable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootable)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the mode field of the given VBD.
     *
     * @param mode New value to set
     */
    public void setMode(Connection c, Types.VbdMode mode) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_mode";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(mode)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the type field of the given VBD.
     *
     * @param type New value to set
     */
    public void setType(Connection c, Types.VbdType type) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(type)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the unpluggable field of the given VBD.
     *
     * @param unpluggable New value to set
     */
    public void setUnpluggable(Connection c, Boolean unpluggable) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_unpluggable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(unpluggable)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the other_config field of the given VBD.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given VBD.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given VBD.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the qos/algorithm_type field of the given VBD.
     *
     * @param algorithmType New value to set
     */
    public void setQosAlgorithmType(Connection c, String algorithmType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_qos_algorithm_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(algorithmType)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the qos/algorithm_params field of the given VBD.
     *
     * @param algorithmParams New value to set
     */
    public void setQosAlgorithmParams(Connection c, Map<String, String> algorithmParams) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.set_qos_algorithm_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(algorithmParams)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the qos/algorithm_params field of the given VBD.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToQosAlgorithmParams(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.add_to_qos_algorithm_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the qos/algorithm_params field of the given VBD.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromQosAlgorithmParams(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.remove_from_qos_algorithm_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the media from the device and leave it empty
     *
     * @return Task
     */
    public Task ejectAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VbdNotRemovableMedia,
       Types.VbdIsEmpty {
        String method_call = "Async.VBD.eject";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Remove the media from the device and leave it empty
     *
     */
    public void eject(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VbdNotRemovableMedia,
       Types.VbdIsEmpty {
        String method_call = "VBD.eject";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Insert new media into the device
     *
     * @param vdi The new VDI to 'insert'
     * @return Task
     */
    public Task insertAsync(Connection c, VDI vdi) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VbdNotRemovableMedia,
       Types.VbdNotEmpty {
        String method_call = "Async.VBD.insert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(vdi)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Insert new media into the device
     *
     * @param vdi The new VDI to 'insert'
     */
    public void insert(Connection c, VDI vdi) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VbdNotRemovableMedia,
       Types.VbdNotEmpty {
        String method_call = "VBD.insert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(vdi)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Hotplug the specified VBD, dynamically attaching it to the running VM
     *
     * @return Task
     */
    public Task plugAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VBD.plug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Hotplug the specified VBD, dynamically attaching it to the running VM
     *
     */
    public void plug(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.plug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Hot-unplug the specified VBD, dynamically unattaching it from the running VM
     *
     * @return Task
     */
    public Task unplugAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.DeviceDetachRejected,
       Types.DeviceAlreadyDetached {
        String method_call = "Async.VBD.unplug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Hot-unplug the specified VBD, dynamically unattaching it from the running VM
     *
     */
    public void unplug(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.DeviceDetachRejected,
       Types.DeviceAlreadyDetached {
        String method_call = "VBD.unplug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Forcibly unplug the specified VBD
     *
     * @return Task
     */
    public Task unplugForceAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VBD.unplug_force";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Forcibly unplug the specified VBD
     *
     */
    public void unplugForce(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.unplug_force";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Throws an error if this VBD could not be attached to this VM if the VM were running. Intended for debugging.
     *
     * @return Task
     */
    public Task assertAttachableAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VBD.assert_attachable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Throws an error if this VBD could not be attached to this VM if the VM were running. Intended for debugging.
     *
     */
    public void assertAttachable(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.assert_attachable";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Return a list of all the VBDs known to the system.
     *
     * @return references to all objects
     */
    public static Set<VBD> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVBD(result);
    }

    /**
     * Return a map of VBD references to VBD records for all VBDs known to the system.
     *
     * @return records of all objects
     */
    public static Map<VBD, VBD.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VBD.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVBDVBDRecord(result);
    }

}