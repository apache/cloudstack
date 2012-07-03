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
 * An message for the attention of the administrator
 *
 * @author Citrix Systems, Inc.
 */
public class Message extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    Message(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a Message, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof Message)
        {
            Message other = (Message) obj;
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
     * Represents all the fields in a Message
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "name", this.name);
            print.printf("%1$20s: %2$s\n", "priority", this.priority);
            print.printf("%1$20s: %2$s\n", "cls", this.cls);
            print.printf("%1$20s: %2$s\n", "objUuid", this.objUuid);
            print.printf("%1$20s: %2$s\n", "timestamp", this.timestamp);
            print.printf("%1$20s: %2$s\n", "body", this.body);
            return writer.toString();
        }

        /**
         * Convert a message.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("name", this.name == null ? "" : this.name);
            map.put("priority", this.priority == null ? 0 : this.priority);
            map.put("cls", this.cls == null ? Types.Cls.UNRECOGNIZED : this.cls);
            map.put("obj_uuid", this.objUuid == null ? "" : this.objUuid);
            map.put("timestamp", this.timestamp == null ? new Date(0) : this.timestamp);
            map.put("body", this.body == null ? "" : this.body);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * The name of the message
         */
        public String name;
        /**
         * The message priority, 0 being low priority
         */
        public Long priority;
        /**
         * The class of the object this message is associated with
         */
        public Types.Cls cls;
        /**
         * The uuid of the object this message is associated with
         */
        public String objUuid;
        /**
         * The time at which the message was created
         */
        public Date timestamp;
        /**
         * The body of the message
         */
        public String body;
    }

    /**
     * 
     *
     * @param name The name of the message
     * @param priority The priority of the message
     * @param cls The class of object this message is associated with
     * @param objUuid The uuid of the object this message is associated with
     * @param body The body of the message
     * @return The reference of the created message
     */
    public static Message create(Connection c, String name, Long priority, Types.Cls cls, String objUuid, String body) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.create";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(priority), Marshalling.toXMLRPC(cls), Marshalling.toXMLRPC(objUuid), Marshalling.toXMLRPC(body)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMessage(result);
    }

    /**
     * 
     *
     */
    public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * 
     *
     * @param cls The class of object
     * @param objUuid The uuid of the object
     * @param since The cutoff time
     * @return The relevant messages
     */
    public static Map<Message, Message.Record> get(Connection c, Types.Cls cls, String objUuid, Date since) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(cls), Marshalling.toXMLRPC(objUuid), Marshalling.toXMLRPC(since)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfMessageMessageRecord(result);
    }

    /**
     * 
     *
     * @return The references to the messages
     */
    public static Set<Message> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfMessage(result);
    }

    /**
     * 
     *
     * @param since The cutoff time
     * @return The relevant messages
     */
    public static Map<Message, Message.Record> getSince(Connection c, Date since) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get_since";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(since)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfMessageMessageRecord(result);
    }

    /**
     * 
     *
     * @return The message record
     */
    public Message.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMessageRecord(result);
    }

    /**
     * 
     *
     * @param uuid The uuid of the message
     * @return The message reference
     */
    public static Message getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMessage(result);
    }

    /**
     * 
     *
     * @return The messages
     */
    public static Map<Message, Message.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfMessageMessageRecord(result);
    }

    /**
     * 
     *
     * @param expr The expression to match (not currently used)
     * @return The messages
     */
    public static Map<Message, Message.Record> getAllRecordsWhere(Connection c, String expr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "message.get_all_records_where";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(expr)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfMessageMessageRecord(result);
    }

}