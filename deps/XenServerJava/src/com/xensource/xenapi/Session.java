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
 * A session
 *
 * @author Citrix Systems, Inc.
 */
public class Session extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    Session(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a Session, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof Session)
        {
            Session other = (Session) obj;
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
     * Represents all the fields in a Session
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "thisHost", this.thisHost);
            print.printf("%1$20s: %2$s\n", "thisUser", this.thisUser);
            print.printf("%1$20s: %2$s\n", "lastActive", this.lastActive);
            print.printf("%1$20s: %2$s\n", "pool", this.pool);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "isLocalSuperuser", this.isLocalSuperuser);
            print.printf("%1$20s: %2$s\n", "subject", this.subject);
            print.printf("%1$20s: %2$s\n", "validationTime", this.validationTime);
            print.printf("%1$20s: %2$s\n", "authUserSid", this.authUserSid);
            print.printf("%1$20s: %2$s\n", "authUserName", this.authUserName);
            print.printf("%1$20s: %2$s\n", "rbacPermissions", this.rbacPermissions);
            print.printf("%1$20s: %2$s\n", "tasks", this.tasks);
            print.printf("%1$20s: %2$s\n", "parent", this.parent);
            return writer.toString();
        }

        /**
         * Convert a session.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("this_host", this.thisHost == null ? new Host("OpaqueRef:NULL") : this.thisHost);
            map.put("this_user", this.thisUser == null ? new User("OpaqueRef:NULL") : this.thisUser);
            map.put("last_active", this.lastActive == null ? new Date(0) : this.lastActive);
            map.put("pool", this.pool == null ? false : this.pool);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("is_local_superuser", this.isLocalSuperuser == null ? false : this.isLocalSuperuser);
            map.put("subject", this.subject == null ? new Subject("OpaqueRef:NULL") : this.subject);
            map.put("validation_time", this.validationTime == null ? new Date(0) : this.validationTime);
            map.put("auth_user_sid", this.authUserSid == null ? "" : this.authUserSid);
            map.put("auth_user_name", this.authUserName == null ? "" : this.authUserName);
            map.put("rbac_permissions", this.rbacPermissions == null ? new LinkedHashSet<String>() : this.rbacPermissions);
            map.put("tasks", this.tasks == null ? new LinkedHashSet<Task>() : this.tasks);
            map.put("parent", this.parent == null ? new Session("OpaqueRef:NULL") : this.parent);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * Currently connected host
         */
        public Host thisHost;
        /**
         * Currently connected user
         */
        public User thisUser;
        /**
         * Timestamp for last time session was active
         */
        public Date lastActive;
        /**
         * True if this session relates to a intra-pool login, false otherwise
         */
        public Boolean pool;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * true iff this session was created using local superuser credentials
         */
        public Boolean isLocalSuperuser;
        /**
         * references the subject instance that created the session. If a session instance has is_local_superuser set, then the value of this field is undefined.
         */
        public Subject subject;
        /**
         * time when session was last validated
         */
        public Date validationTime;
        /**
         * the subject identifier of the user that was externally authenticated. If a session instance has is_local_superuser set, then the value of this field is undefined.
         */
        public String authUserSid;
        /**
         * the subject name of the user that was externally authenticated. If a session instance has is_local_superuser set, then the value of this field is undefined.
         */
        public String authUserName;
        /**
         * list with all RBAC permissions for this session
         */
        public Set<String> rbacPermissions;
        /**
         * list of tasks created using the current session
         */
        public Set<Task> tasks;
        /**
         * references the parent session that created this session
         */
        public Session parent;
    }

    /**
     * Get a record containing the current state of the given session.
     *
     * @return all fields from the object
     */
    public Session.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSessionRecord(result);
    }

    /**
     * Get a reference to the session instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static Session getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSession(result);
    }

    /**
     * Get the uuid field of the given session.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the this_host field of the given session.
     *
     * @return value of the field
     */
    public Host getThisHost(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_this_host";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toHost(result);
    }

    /**
     * Get the this_user field of the given session.
     *
     * @return value of the field
     */
    public User getThisUser(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_this_user";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toUser(result);
    }

    /**
     * Get the last_active field of the given session.
     *
     * @return value of the field
     */
    public Date getLastActive(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_last_active";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDate(result);
    }

    /**
     * Get the pool field of the given session.
     *
     * @return value of the field
     */
    public Boolean getPool(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_pool";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the other_config field of the given session.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the is_local_superuser field of the given session.
     *
     * @return value of the field
     */
    public Boolean getIsLocalSuperuser(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_is_local_superuser";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the subject field of the given session.
     *
     * @return value of the field
     */
    public Subject getSubject(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_subject";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSubject(result);
    }

    /**
     * Get the validation_time field of the given session.
     *
     * @return value of the field
     */
    public Date getValidationTime(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_validation_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDate(result);
    }

    /**
     * Get the auth_user_sid field of the given session.
     *
     * @return value of the field
     */
    public String getAuthUserSid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_auth_user_sid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the auth_user_name field of the given session.
     *
     * @return value of the field
     */
    public String getAuthUserName(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_auth_user_name";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the rbac_permissions field of the given session.
     *
     * @return value of the field
     */
    public Set<String> getRbacPermissions(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_rbac_permissions";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the tasks field of the given session.
     *
     * @return value of the field
     */
    public Set<Task> getTasks(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_tasks";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfTask(result);
    }

    /**
     * Get the parent field of the given session.
     *
     * @return value of the field
     */
    public Session getParent(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_parent";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSession(result);
    }

    /**
     * Set the other_config field of the given session.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given session.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given session.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Attempt to authenticate the user, returning a session reference if successful
     *
     * @param uname Username for login.
     * @param pwd Password for login.
     * @param version Client API version.
     * @return reference of newly created session
     */
    public static Session loginWithPassword(Connection c, String uname, String pwd, String version) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.SessionAuthenticationFailed {
        String method_call = "session.login_with_password";
        Object[] method_params = {Marshalling.toXMLRPC(uname), Marshalling.toXMLRPC(pwd), Marshalling.toXMLRPC(version)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSession(result);
    }

    /**
     * Log out of a session
     *
     */
    public static void logout(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.logout";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Change the account password; if your session is authenticated with root priviledges then the old_pwd is validated and the new_pwd is set regardless
     *
     * @param oldPwd Old password for account
     * @param newPwd New password for account
     */
    public static void changePassword(Connection c, String oldPwd, String newPwd) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.change_password";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(oldPwd), Marshalling.toXMLRPC(newPwd)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Authenticate locally against a slave in emergency mode. Note the resulting sessions are only good for use on this host.
     *
     * @param uname Username for login.
     * @param pwd Password for login.
     * @return ID of newly created session
     */
    public static Session slaveLocalLoginWithPassword(Connection c, String uname, String pwd) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.slave_local_login_with_password";
        Object[] method_params = {Marshalling.toXMLRPC(uname), Marshalling.toXMLRPC(pwd)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSession(result);
    }

    /**
     * Log out of local session.
     *
     */
    public static void localLogout(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.local_logout";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Return a list of all the user subject-identifiers of all existing sessions
     *
     * @return Task
     */
    public static Task getAllSubjectIdentifiersAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.session.get_all_subject_identifiers";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Return a list of all the user subject-identifiers of all existing sessions
     *
     * @return The list of user subject-identifiers of all existing sessions
     */
    public static Set<String> getAllSubjectIdentifiers(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.get_all_subject_identifiers";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Log out all sessions associated to a user subject-identifier, except the session associated with the context calling this function
     *
     * @param subjectIdentifier User subject-identifier of the sessions to be destroyed
     * @return Task
     */
    public static Task logoutSubjectIdentifierAsync(Connection c, String subjectIdentifier) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.session.logout_subject_identifier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(subjectIdentifier)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Log out all sessions associated to a user subject-identifier, except the session associated with the context calling this function
     *
     * @param subjectIdentifier User subject-identifier of the sessions to be destroyed
     */
    public static void logoutSubjectIdentifier(Connection c, String subjectIdentifier) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "session.logout_subject_identifier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(subjectIdentifier)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

}