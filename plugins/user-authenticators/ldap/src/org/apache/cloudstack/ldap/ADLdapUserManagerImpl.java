/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class ADLdapUserManagerImpl extends OpenLdapUserManagerImpl implements LdapUserManager {
    public static final Logger s_logger = Logger.getLogger(ADLdapUserManagerImpl.class.getName());
    private static final String MICROSOFT_AD_NESTED_MEMBERS_FILTER = "memberOf:1.2.840.113556.1.4.1941";

    @Override
    public List<LdapUser> getUsersInGroup(String groupName, LdapContext context) throws NamingException {
        if (StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException("ldap group name cannot be blank");
        }

        String basedn = _ldapConfiguration.getBaseDn();
        if (StringUtils.isBlank(basedn)) {
            throw new IllegalArgumentException("ldap basedn is not configured");
        }

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(_ldapConfiguration.getScope());
        searchControls.setReturningAttributes(_ldapConfiguration.getReturnAttributes());

        NamingEnumeration<SearchResult> results = context.search(basedn, generateADGroupSearchFilter(groupName), searchControls);
        final List<LdapUser> users = new ArrayList<LdapUser>();
        while (results.hasMoreElements()) {
            final SearchResult result = results.nextElement();
            users.add(createUser(result));
        }
        return users;
    }

    private String generateADGroupSearchFilter(String groupName) {
        final StringBuilder userObjectFilter = new StringBuilder();
        userObjectFilter.append("(objectClass=");
        userObjectFilter.append(_ldapConfiguration.getUserObject());
        userObjectFilter.append(")");

        final StringBuilder memberOfFilter = new StringBuilder();
        String groupCnName =  _ldapConfiguration.getCommonNameAttribute() + "=" +groupName + "," +  _ldapConfiguration.getBaseDn();
        memberOfFilter.append("(" + MICROSOFT_AD_NESTED_MEMBERS_FILTER + ":=");
        memberOfFilter.append(groupCnName);
        memberOfFilter.append(")");

        final StringBuilder result = new StringBuilder();
        result.append("(&");
        result.append(userObjectFilter);
        result.append(memberOfFilter);
        result.append(")");

        s_logger.debug("group search filter = " + result);
        return result.toString();
    }
}
