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
 * A set of permissions associated with a subject
 *
 * @author Citrix Systems, Inc.
 */
public class Role extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    Role(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a Role, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof Role)
        {
            Role other = (Role) obj;
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
     * Represents all the fields in a Role
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "subroles", this.subroles);
            return writer.toString();
        }

        /**
         * Convert a role.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("subroles", this.subroles == null ? new LinkedHashSet<Role>() : this.subroles);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * a short user-friendly name for the role
         */
        public String nameLabel;
        /**
         * what this role is for
         */
        public String nameDescription;
        /**
         * a list of pointers to other roles or permissions
         */
        public Set<Role> subroles;
    }

    /**
     * Get a record containing the current state of the given role.
     *
     * @return all fields from the object
     */
    public Role.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toRoleRecord(result);
    }

    /**
     * Get a reference to the role instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Role getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toRole(result);
    }

    /**
     * Get all the role instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<Role> getByNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfRole(result);
    }

    /**
     * Get the uuid field of the given role.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/label field of the given role.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/description field of the given role.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the subroles field of the given role.
     *
     * @return value of the field
     */
    public Set<Role> getSubroles(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_subroles";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfRole(result);
    }

    /**
     * This call returns a list of permissions given a role
     *
     * @return a list of permissions
     */
    public Set<Role> getPermissions(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_permissions";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfRole(result);
    }

    /**
     * This call returns a list of permission names given a role
     *
     * @return a list of permission names
     */
    public Set<String> getPermissionsNameLabel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_permissions_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * This call returns a list of roles given a permission
     *
     * @return a list of references to roles
     */
    public Set<Role> getByPermission(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_by_permission";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfRole(result);
    }

    /**
     * This call returns a list of roles given a permission name
     *
     * @param label The short friendly name of the role
     * @return a list of references to roles
     */
    public static Set<Role> getByPermissionNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_by_permission_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfRole(result);
    }

    /**
     * Return a list of all the roles known to the system.
     *
     * @return references to all objects
     */
    public static Set<Role> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfRole(result);
    }

    /**
     * Return a map of role references to role records for all roles known to the system.
     *
     * @return records of all objects
     */
    public static Map<Role, Role.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "role.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfRoleRoleRecord(result);
    }

}