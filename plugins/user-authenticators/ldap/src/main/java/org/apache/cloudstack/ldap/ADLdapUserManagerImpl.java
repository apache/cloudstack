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

import org.apache.commons.lang3.StringUtils;

public class ADLdapUserManagerImpl extends OpenLdapUserManagerImpl implements LdapUserManager {
    private static final String MICROSOFT_AD_NESTED_MEMBERS_FILTER = "memberOf:1.2.840.113556.1.4.1941:";
    private static final String MICROSOFT_AD_MEMBERS_FILTER = "memberOf";

    @Override
    public List<LdapUser> getUsersInGroup(String groupName, LdapContext context, Long domainId) throws NamingException {
        if (StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException("ldap group name cannot be blank");
        }

        String basedn = _ldapConfiguration.getBaseDn(domainId);
        if (StringUtils.isBlank(basedn)) {
            throw new IllegalArgumentException("ldap basedn is not configured");
        }

        final SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(_ldapConfiguration.getScope());
        searchControls.setReturningAttributes(_ldapConfiguration.getReturnAttributes(domainId));

        NamingEnumeration<SearchResult> results = context.search(basedn, generateADGroupSearchFilter(groupName, domainId), searchControls);
        final List<LdapUser> users = new ArrayList<LdapUser>();
        while (results.hasMoreElements()) {
            final SearchResult result = results.nextElement();
            users.add(createUser(result, domainId));
        }
        return users;
    }

    String generateADGroupSearchFilter(String groupName, Long domainId) {
        final StringBuilder userObjectFilter = new StringBuilder();
        userObjectFilter.append("(objectClass=");
        userObjectFilter.append(_ldapConfiguration.getUserObject(domainId));
        userObjectFilter.append(")");

        final StringBuilder memberOfFilter = new StringBuilder();
        String groupCnName =  _ldapConfiguration.getCommonNameAttribute() + "=" +groupName + "," +  _ldapConfiguration.getBaseDn(domainId);
        memberOfFilter.append("(").append(getMemberOfAttribute(domainId)).append("=");
        memberOfFilter.append(groupCnName);
        memberOfFilter.append(")");

        final StringBuilder result = new StringBuilder();
        result.append("(&");
        result.append(userObjectFilter);
        result.append(memberOfFilter);
        result.append(")");

        logger.debug("group search filter = " + result);
        return result.toString();
    }

    protected boolean isUserDisabled(SearchResult result) throws NamingException {
        boolean isDisabledUser = false;
        String userAccountControl = LdapUtils.getAttributeValue(result.getAttributes(), _ldapConfiguration.getUserAccountControlAttribute());
        if (userAccountControl != null) {
            int control = Integer.parseInt(userAccountControl);
            // second bit represents disabled user flag in AD
            if ((control & 2) > 0) {
                isDisabledUser = true;
            }
        }
        return isDisabledUser;
    }

    protected String getMemberOfAttribute(final Long domainId) {
        if(_ldapConfiguration.isNestedGroupsEnabled(domainId)) {
            return MICROSOFT_AD_NESTED_MEMBERS_FILTER;
        } else {
            return MICROSOFT_AD_MEMBERS_FILTER;
        }
    }
}
