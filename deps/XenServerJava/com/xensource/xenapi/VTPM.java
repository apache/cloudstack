/*
 * Copyright (c) 2006-2010 Citrix Systems, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation, with the additional linking exception as
 * follows:
 * 
 *   Linking this library statically or dynamically with other modules is
 *   making a combined work based on this library. Thus, the terms and
 *   conditions of the GNU General Public License cover the whole combination.
 * 
 *   As a special exception, the copyright holders of this library give you
 *   permission to link this library with independent modules to produce an
 *   executable, regardless of the license terms of these independent modules,
 *   and to copy and distribute the resulting executable under terms of your
 *   choice, provided that you also meet, for each linked independent module,
 *   the terms and conditions of the license of that module. An independent
 *   module is a module which is not derived from or based on this library. If
 *   you modify this library, you may extend this exception to your version of
 *   the library, but you are not obligated to do so. If you do not wish to do
 *   so, delete this exception statement from your version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
 * A virtual TPM device
 *
 * @author Citrix Systems, Inc.
 */
public class VTPM extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    VTPM(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a VTPM, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof VTPM)
        {
            VTPM other = (VTPM) obj;
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
     * Represents all the fields in a VTPM
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "VM", this.VM);
            print.printf("%1$20s: %2$s\n", "backend", this.backend);
            return writer.toString();
        }

        /**
         * Convert a VTPM.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("VM", this.VM == null ? new VM("OpaqueRef:NULL") : this.VM);
            map.put("backend", this.backend == null ? new VM("OpaqueRef:NULL") : this.backend);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * the virtual machine
         */
        public VM VM;
        /**
         * the domain where the backend is located
         */
        public VM backend;
    }

    /**
     * Get a record containing the current state of the given VTPM.
     *
     * @return all fields from the object
     */
    public VTPM.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVTPMRecord(result);
    }

    /**
     * Get a reference to the VTPM instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VTPM getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVTPM(result);
    }

    /**
     * Create a new VTPM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, VTPM.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VTPM.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VTPM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VTPM create(Connection c, VTPM.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVTPM(result);
    }

    /**
     * Destroy the specified VTPM instance.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VTPM.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the specified VTPM instance.
     *
     */
    public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Get the uuid field of the given VTPM.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the VM field of the given VTPM.
     *
     * @return value of the field
     */
    public VM getVM(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.get_VM";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Get the backend field of the given VTPM.
     *
     * @return value of the field
     */
    public VM getBackend(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VTPM.get_backend";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

}