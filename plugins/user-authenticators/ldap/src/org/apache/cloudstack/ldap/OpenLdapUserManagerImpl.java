// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class OpenLdapUserManagerImpl implements LdapUserManager {
    private static final Logger s_logger = Logger.getLogger(OpenLdapUserManagerImpl.class.getName());

    @Inject
    protected LdapConfiguration _ldapConfiguration;

    public OpenLdapUserManagerImpl() {
    }

    public OpenLdapUserManagerImpl(final LdapConfiguration ldapConfiguration) {
        _ldapConfiguration = ldapConfiguration;
    }

    protected LdapUser createUser(final SearchResult result) throws NamingException {
        final Attributes attributes = result.getAttributes();

        final String username = LdapUtils.getAttributeValue(attributes, _ldapConfiguration.getUsernameAttribute());
        final String email = LdapUtils.getAttributeValue(attributes, _ldapConfiguration.getEmailAttribute());
        final String firstname = LdapUtils.getAttributeValue(attributes, _ldapConfiguration.getFirstnameAttribute());
        final String lastname = LdapUtils.getAttributeValue(attributes, _ldapConfiguration.getLastnameAttribute());
        final String principal = result.getNameInNamespace();

        String domain = principal.replace("cn=" + LdapUtils.getAttributeValue(attributes, _ldapConfiguration.getCommonNameAttribute()) + ",", "");
        domain = domain.replace("," + _ldapConfiguration.getBaseDn(), "");
        domain = domain.replace("ou=", "");

        return new LdapUser(username, email, firstname, lastname, principal, domain);
    }

    private String generateSearchFilter(final String username) {
        final StringBuilder userObjectFilter = new StringBuilder();
        userObjectFilter.append("(objectClass=");
        userObjectFilter.append(_ldapConfiguration.getUserObject());
        userObjectFilter.append(")");

        final StringBuilder usernameFilter = new StringBuilder();
        usernameFilter.append("(");
        usernameFilter.append(_ldapConfiguration.getUsernameAttribute());
        usernameFilter.append("=");
        usernameFilter.append((username == null ? "*" : username));
        usernameFilter.append(")");

        final StringBuilder memberOfFilter = new StringBuilder();
        if (_ldapConfiguration.getSearchGroupPrinciple() != null) {
            memberOfFilter.append("(memberof=");
            memberOfFilter.append(_ldapConfiguration.getSearchGroupPrinciple());
            memberOfFilter.append(")");
        }

        final StringBuilder result = new StringBuilder();
        result.append("(&");
        result.append(userObjectFilter);
        result.append(usernameFilter);
        result.append(memberOfFilter);
        result.append(")");

        return result.toString();
    }

    private String generateGroupSearchFilter(final String groupName) {
        final StringBuilder groupObjectFilter = new StringBuilder();
        groupObjectFilter.append("(objectClass=");
        groupObjectFilter.append(_ldapConfiguration.getGroupObject());
        groupObjectFilter.append(")");

        final StringBuilder groupNameFilter = new StringBuilder();
        groupNameFilter.append("(");
        groupNameFilter.append(_ldapConfiguration.getCommonNameAttribute());
        groupNameFilter.append("=");
        groupNameFilter.append((groupName == null ? "*" : groupName));
        groupNameFilter.append(")");

        final StringBuilder result = new StringBuilder();
        result.append("(&");
        result.append(groupObjectFilter);
        result.append(groupNameFilter);
        result.append(")");

        return result.toString();
    }

    @Override
    public LdapUser getUser(final String username, final LdapContext context) throws NamingException, IOException {
        List<LdapUser> result = searchUsers(username, context);
        if (result!= null && result.size() == 1) {
            return result.get(0);
        } else {
            throw new NamingException("No user found for username " + username);
        }
    }

    @Override
    public List<LdapUser> getUsers(final LdapContext context) throws NamingException, IOException {
        return getUsers(null, context);
    }

    @Override
    public List<LdapUser> getUsers(final String username, final LdapContext context) throws NamingException, IOException {
        List<LdapUser> users = searchUsers(username, context);

        if (CollectionUtils.isNotEmpty(users)) {
            Collections.sort(users);
        }
        return users;
    }

    @Override
    public List<LdapUser> getUsersInGroup(String groupName, LdapContext context) throws NamingException {
        String attributeName = _ldapConfiguration.getGroupUniqueMemeberAttribute();
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(_ldapConfiguration.getScope());
        controls.setReturningAttributes(new String[] {attributeName});

        NamingEnumeration<SearchResult> result = context.search(_ldapConfiguration.getBaseDn(), generateGroupSearchFilter(groupName), controls);

        final List<LdapUser> users = new ArrayList<LdapUser>();
        //Expecting only one result which has all the users
        if (result.hasMoreElements()) {
            Attribute attribute = result.nextElement().getAttributes().get(attributeName);
            NamingEnumeration<?> values = attribute.getAll();

            while (values.hasMoreElements()) {
                String userdn = String.valueOf(values.nextElement());
                try{
                    users.add(getUserForDn(userdn, context));
                } catch (NamingException e){
                    s_logger.info("Userdn: " + userdn + " Not Found:: Exception message: " + e.getMessage());
                }
            }
        }

        Collections.sort(users);

        return users;
    }

    private LdapUser getUserForDn(String userdn, LdapContext context) throws NamingException {
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(_ldapConfiguration.getScope());
        controls.setReturningAttributes(_ldapConfiguration.getReturnAttributes());

        NamingEnumeration<SearchResult> result = context.search(userdn, "(objectClass=" + _ldapConfiguration.getUserObject() + ")", controls);
        if (result.hasMoreElements()) {
            return createUser(result.nextElement());
        } else {
            throw new NamingException("No user found for dn " + userdn);
        }
    }

    @Override
    public List<LdapUser> searchUsers(final LdapContext context) throws NamingException, IOException {
        return searchUsers(null, context);
    }

    @Override
    public List<LdapUser> searchUsers(final String username, final LdapContext context) throws NamingException, IOException {

        final SearchControls searchControls = new SearchControls();

        searchControls.setSearchScope(_ldapConfiguration.getScope());
        searchControls.setReturningAttributes(_ldapConfiguration.getReturnAttributes());

        String basedn = _ldapConfiguration.getBaseDn();
        if (StringUtils.isBlank(basedn)) {
            throw new IllegalArgumentException("ldap basedn is not configured");
        }
        byte[] cookie = null;
        int pageSize = _ldapConfiguration.getLdapPageSize();
        context.setRequestControls(new Control[]{new PagedResultsControl(pageSize, Control.NONCRITICAL)});
        final List<LdapUser> users = new ArrayList<LdapUser>();
        NamingEnumeration<SearchResult> results;
        do {
            results = context.search(basedn, generateSearchFilter(username), searchControls);
            while (results.hasMoreElements()) {
                final SearchResult result = results.nextElement();
                users.add(createUser(result));
            }
            Control[] contextControls = context.getResponseControls();
            if (contextControls != null) {
                for (Control control : contextControls) {
                    if (control instanceof PagedResultsResponseControl) {
                        PagedResultsResponseControl prrc = (PagedResultsResponseControl) control;
                        cookie = prrc.getCookie();
                    }
                }
            } else {
                s_logger.info("No controls were sent from the ldap server");
            }
            context.setRequestControls(new Control[] {new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
        } while (cookie != null);

        return users;
    }
}