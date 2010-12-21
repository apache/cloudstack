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
 * Management of remote authentication services
 *
 * @author Citrix Systems, Inc.
 */
public class Auth extends XenAPIObject {


    public String toWireString() {
        return null;
    }

    /**
     * This call queries the external directory service to obtain the subject_identifier as a string from the human-readable subject_name
     *
     * @param subjectName The human-readable subject_name, such as a username or a groupname
     * @return the subject_identifier obtained from the external directory service
     */
    public static String getSubjectIdentifier(Connection c, String subjectName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "auth.get_subject_identifier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(subjectName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * This call queries the external directory service to obtain the user information (e.g. username, organization etc) from the specified subject_identifier
     *
     * @param subjectIdentifier A string containing the subject_identifier, unique in the external directory service
     * @return key-value pairs containing at least a key called subject_name
     */
    public static Map<String, String> getSubjectInformationFromIdentifier(Connection c, String subjectIdentifier) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "auth.get_subject_information_from_identifier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(subjectIdentifier)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * This calls queries the external directory service to obtain the transitively-closed set of groups that the the subject_identifier is member of.
     *
     * @param subjectIdentifier A string containing the subject_identifier, unique in the external directory service
     * @return set of subject_identifiers that provides the group membership of subject_identifier passed as argument, it contains, recursively, all groups a subject_identifier is member of.
     */
    public static Set<String> getGroupMembership(Connection c, String subjectIdentifier) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "auth.get_group_membership";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(subjectIdentifier)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

}