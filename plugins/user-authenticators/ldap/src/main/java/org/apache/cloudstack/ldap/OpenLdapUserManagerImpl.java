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

import org.apache.cloudstack.ldap.dao.LdapTrustMapDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class OpenLdapUserManagerImpl implements LdapUserManager {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    protected LdapConfiguration _ldapConfiguration;

    @Inject
    LdapTrustMapDao _ldapTrustMapDao;

    public OpenLdapUserManagerImpl() {
    }

    public OpenLdapUserManagerImpl(final LdapConfiguration ldapConfiguration) {
        _ldapConfiguration = ldapConfiguration;
    }

    protected LdapUser createUser(final SearchResult result, Long domainId) throws NamingException {
        final Attributes attributes = result.getAttributes();

        final String username = LdapUtils.getAttributeValue(attributes, LdapConfiguration.getUsernameAttribute(domainId));
        final String email = LdapUtils.getAttributeValue(attributes, LdapConfiguration.getEmailAttribute(domainId));
        final String firstname = LdapUtils.getAttributeValue(attributes, LdapConfiguration.getFirstnameAttribute(domainId));
        final String lastname = LdapUtils.getAttributeValue(attributes, LdapConfiguration.getLastnameAttribute(domainId));
        final String principal = result.getNameInNamespace();
        final List<String> memberships = LdapUtils.getAttributeValues(attributes, getMemberOfAttribute(domainId));

        String domain = principal.replace("cn=" + LdapUtils.getAttributeValue(attributes, _ldapConfiguration.getCommonNameAttribute()) + ",", "");
        domain = domain.replace("," + LdapConfiguration.getBaseDn(domainId), "");
        domain = domain.replace("ou=", "");

        boolean disabled = isUserDisabled(result);

        return new LdapUser(username, email, firstname, lastname, principal, domain, disabled, memberships);
    }

    private String generateSearchFilter(final String username, Long domainId) {
        String userObjectFilter = "(objectClass=" +
                LdapConfiguration.getUserObject(domainId) +
                ")";

        String usernameFilter = "(" +
                LdapConfiguration.getUsernameAttribute(domainId) +
                "=" +
                (username == null ? "*" : LdapUtils.escapeLDAPSearchFilter(username)) +
                ")";

        String memberOfAttribute = getMemberOfAttribute(domainId);
        StringBuilder ldapGroupsFilter = new StringBuilder();
        // this should get the trustmaps for this domain
        List<String> ldapGroups = getMappedLdapGroups(domainId);
        if (CollectionUtils.isNotEmpty(ldapGroups)) {
            ldapGroupsFilter.append("(|");
            for (String ldapGroup : ldapGroups) {
                ldapGroupsFilter.append(getMemberOfGroupString(ldapGroup, memberOfAttribute));
            }
            ldapGroupsFilter.append(')');
        }
        // make sure only users in the principle group are retrieved
        String pricipleGroup = _ldapConfiguration.getSearchGroupPrinciple(domainId);
        final StringBuilder principleGroupFilter = new StringBuilder();
        if (null != pricipleGroup) {
            principleGroupFilter.append(getMemberOfGroupString(pricipleGroup, memberOfAttribute));
        }

        String returnString = "(&" +
                userObjectFilter +
                usernameFilter +
                ldapGroupsFilter +
                principleGroupFilter +
                ")";
        logger.trace("constructed ldap query: {}", returnString);
        return returnString;
    }

    private List<String> getMappedLdapGroups(Long domainId) {
        List <String> ldapGroups = new ArrayList<>();
        // first get the trustmaps
        if (null != domainId) {
            for (LdapTrustMapVO trustMap : _ldapTrustMapDao.searchByDomainId(domainId)) {
                // then retrieve the string from it
                ldapGroups.add(trustMap.getName());
            }
        }
        return ldapGroups;
    }

    private String getMemberOfGroupString(String group, String memberOfAttribute) {
        final StringBuilder memberOfFilter = new StringBuilder();
        if (null != group) {
            if(logger.isDebugEnabled()) {
                logger.debug("adding search filter for '{}', using '{}'", group, memberOfAttribute);
            }
            memberOfFilter.append("(").append(memberOfAttribute).append("=");
            memberOfFilter.append(group);
            memberOfFilter.append(")");
        }
        return memberOfFilter.toString();
    }

    private String generateGroupSearchFilter(final String groupName, Long domainId) {
        String groupObjectFilter = "(objectClass=" +
                LdapConfiguration.getGroupObject(domainId) +
                ")";

        String groupNameFilter = "(" +
                _ldapConfiguration.getCommonNameAttribute() +
                "=" +
                (groupName == null ? "*" : LdapUtils.escapeLDAPSearchFilter(groupName)) +
                ")";

        return "(&" +
                groupObjectFilter +
                groupNameFilter +
                ")";
    }

    @Override
    public LdapUser getUser(final String username, final LdapContext context, Long domainId) throws NamingException, IOException {
        List<LdapUser> result = searchUsers(username, context, domainId);
        if (result!= null && result.size() == 1) {
            return result.get(0);
        } else {
            throw new NamingException("No user found for username " + username);
        }
    }

    @Override
    public LdapUser getUser(final String username, final String type, final String name, final LdapContext context, Long domainId) throws NamingException, IOException {
        String basedn;
        if("OU".equals(type)) {
            basedn = name;
        } else {
            basedn = LdapConfiguration.getBaseDn(domainId);
        }

        String userObjectFilter = "(objectClass=" +
                LdapConfiguration.getUserObject(domainId) +
                ")";

        String usernameFilter = "(" +
                LdapConfiguration.getUsernameAttribute(domainId) +
                "=" +
                (username == null ? "*" : LdapUtils.escapeLDAPSearchFilter(username)) +
                ")";

        final StringBuilder memberOfFilter = new StringBuilder();
        if ("GROUP".equals(type)) {
            memberOfFilter.append("(").append(getMemberOfAttribute(domainId)).append("=");
            memberOfFilter.append(name);
            memberOfFilter.append(")");
        }

        String searchQuery = "(&" +
                userObjectFilter +
                usernameFilter +
                memberOfFilter +
                ")";

        return searchUser(basedn, searchQuery, context, domainId);
    }

    protected String getMemberOfAttribute(final Long domainId) {
        return LdapConfiguration.getUserMemberOfAttribute(domainId);
    }

    @Override
    public List<LdapUser> getUsers(final LdapContext context, Long domainId) throws NamingException, IOException {
        return getUsers(null, context, domainId);
    }

    @Override
    public List<LdapUser> getUsers(final String username, final LdapContext context, Long domainId) throws NamingException, IOException {
        List<LdapUser> users = searchUsers(username, context, domainId);

        if (CollectionUtils.isNotEmpty(users)) {
            Collections.sort(users);
        }
        return users;
    }

    @Override
    public List<LdapUser> getUsersInGroup(String groupName, LdapContext context, Long domainId) throws NamingException {
        String attributeName = LdapConfiguration.getGroupUniqueMemberAttribute(domainId);
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(_ldapConfiguration.getScope());
        controls.setReturningAttributes(new String[] {attributeName});

        NamingEnumeration<SearchResult> result = context.search(LdapConfiguration.getBaseDn(domainId), generateGroupSearchFilter(groupName, domainId), controls);

        final List<LdapUser> users = new ArrayList<>();
        //Expecting only one result which has all the users
        if (result.hasMoreElements()) {
            Attribute attribute = result.nextElement().getAttributes().get(attributeName);
            NamingEnumeration<?> values = attribute.getAll();

            while (values.hasMoreElements()) {
                String userdn = String.valueOf(values.nextElement());
                try{
                    users.add(getUserForDn(userdn, context, domainId));
                } catch (NamingException e){
                    logger.info("Userdn: {} Not Found:: Exception message: {}", userdn, e.getMessage());
                }
            }
        }

        Collections.sort(users);

        return users;
    }

    private LdapUser getUserForDn(String userdn, LdapContext context, Long domainId) throws NamingException {
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(_ldapConfiguration.getScope());
        controls.setReturningAttributes(_ldapConfiguration.getReturnAttributes(domainId));

        NamingEnumeration<SearchResult> result = context.search(userdn, "(objectClass=" + LdapConfiguration.getUserObject(domainId) + ")", controls);
        if (result.hasMoreElements()) {
            return createUser(result.nextElement(), domainId);
        } else {
            throw new NamingException("No user found for dn " + userdn);
        }
    }

    @Override
    public List<LdapUser> searchUsers(final LdapContext context, Long domainId) throws NamingException, IOException {
        return searchUsers(null, context, domainId);
    }

    protected boolean isUserDisabled(SearchResult result) throws NamingException {
        return false;
    }

    public LdapUser searchUser(final String basedn, final String searchString, final LdapContext context, Long domainId) throws NamingException {
        final SearchControls searchControls = new SearchControls();

        searchControls.setSearchScope(_ldapConfiguration.getScope());
        searchControls.setReturningAttributes(_ldapConfiguration.getReturnAttributes(domainId));

        NamingEnumeration<SearchResult> results = context.search(basedn, searchString, searchControls);
        logger.debug("searching user(s) with filter: \"{}\"", searchString);
        final List<LdapUser> users = new ArrayList<>();
        while (results.hasMoreElements()) {
            final SearchResult result = results.nextElement();
                users.add(createUser(result, domainId));
        }

        if (users.size() == 1) {
            return users.get(0);
        } else {
            throw new NamingException("No user found for basedn " + basedn + " and searchString " + searchString);
        }
    }

    @Override
    public List<LdapUser> searchUsers(final String username, final LdapContext context, Long domainId) throws NamingException, IOException {

        final SearchControls searchControls = new SearchControls();

        searchControls.setSearchScope(_ldapConfiguration.getScope());
        searchControls.setReturningAttributes(_ldapConfiguration.getReturnAttributes(domainId));

        String basedn = LdapConfiguration.getBaseDn(domainId);
        if (StringUtils.isBlank(basedn)) {
            throw new IllegalArgumentException(String.format("ldap basedn is not configured (for domain: %s)", domainId));
        }
        byte[] cookie = null;
        int pageSize = LdapConfiguration.getLdapPageSize(domainId);
        context.setRequestControls(new Control[]{new PagedResultsControl(pageSize, Control.NONCRITICAL)});
        final List<LdapUser> users = new ArrayList<>();
        NamingEnumeration<SearchResult> results;
        do {
            results = context.search(basedn, generateSearchFilter(username, domainId), searchControls);
            while (results.hasMoreElements()) {
                final SearchResult result = results.nextElement();
                if (!isUserDisabled(result)) {
                    users.add(createUser(result, domainId));
                }
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
                logger.info("No controls were sent from the ldap server");
            }
            context.setRequestControls(new Control[] {new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
        } while (cookie != null);

        return users;
    }
}
