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