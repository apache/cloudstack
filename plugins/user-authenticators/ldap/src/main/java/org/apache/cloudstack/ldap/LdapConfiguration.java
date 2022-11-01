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

import java.util.List;

import javax.inject.Inject;
import javax.naming.directory.SearchControls;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.utils.Pair;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;

public class LdapConfiguration implements Configurable{
    private final static String factory = "com.sun.jndi.ldap.LdapCtxFactory";

    private static final ConfigKey<Long> ldapReadTimeout = new ConfigKey<Long>(
            Long.class,
            "ldap.read.timeout",
            "Advanced",
            "1000",
            "LDAP connection Timeout in milli sec",
            true,
            ConfigKey.Scope.Domain,
            1l);

    private static final ConfigKey<Integer> ldapPageSize = new ConfigKey<Integer>(
            Integer.class,
            "ldap.request.page.size",
            "Advanced",
            "1000",
            "page size sent to ldap server on each request to get user",
            true,
            ConfigKey.Scope.Domain,
            1);

    private static final ConfigKey<Boolean> ldapEnableNestedGroups = new ConfigKey<Boolean>(
            "Advanced",
            Boolean.class,
            "ldap.nested.groups.enable",
            "true",
            "if true, nested groups will also be queried",
            true,
            ConfigKey.Scope.Domain);

    private static final ConfigKey<String> ldapMemberOfAttribute = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.user.memberof.attribute",
            "memberof",
            "the reverse membership attribute for group members",
            true,
            ConfigKey.Scope.Domain);

    private static final ConfigKey<String> ldapProvider = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.provider",
            "openldap",
            "ldap provider ex:openldap, microsoftad",
            true,
            ConfigKey.Scope.Domain);

    private static final ConfigKey<String> ldapBaseDn = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.basedn",
            null,
            "Sets the basedn for LDAP",
            true,
            ConfigKey.Scope.Domain);

    private static final ConfigKey<String> ldapBindPassword = new ConfigKey<String>(
            "Secure",
            String.class,
            "ldap.bind.password",
            null,
            "Sets the bind password for LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapBindPrincipal = new ConfigKey<String>(
            "Secure",
            String.class,
            "ldap.bind.principal",
            null,
            "Sets the bind principal for LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapEmailAttribute = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.email.attribute",
            "mail",
            "Sets the email attribute used within LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapFirstnameAttribute = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.firstname.attribute",
            "givenname",
            "Sets the firstname attribute used within LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapLastnameAttribute = new ConfigKey<String>(
            "Advanced",
            String.class, "ldap.lastname.attribute",
            "sn",
            "Sets the lastname attribute used within LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapUsernameAttribute = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.username.attribute",
            "uid",
            "Sets the username attribute used within LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapUserObject = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.user.object",
            "inetOrgPerson",
            "Sets the object type of users within LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapSearchGroupPrinciple = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.search.group.principle",
            null,
            "Sets the principle of the group that users must be a member of",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapGroupObject = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.group.object",
            "groupOfUniqueNames",
            "Sets the object type of groups within LDAP",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapGroupUniqueMemberAttribute = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.group.user.uniquemember",
            "uniquemember",
            "Sets the attribute for uniquemembers within a group",
            true,
            ConfigKey.Scope.Domain);

    private static final ConfigKey<String> ldapTrustStore = new ConfigKey<String>(
            "Advanced",
            String.class,
            "ldap.truststore",
            null,
            "Sets the path to the truststore to use for SSL",
            true,
            ConfigKey.Scope.Domain);
    private static final ConfigKey<String> ldapTrustStorePassword = new ConfigKey<String>(
            "Secure",
            String.class,
            "ldap.truststore.password",
            null,
            "Sets the password for the truststore",
            true,
            ConfigKey.Scope.Domain);

    private final static int scope = SearchControls.SUBTREE_SCOPE;

    @Inject
    private LdapConfigurationDao _ldapConfigurationDao;

    public LdapConfiguration() {
    }

    public LdapConfiguration(final LdapConfigurationDao ldapConfigurationDao) {
        _ldapConfigurationDao = ldapConfigurationDao;
    }

    @Deprecated
    public LdapConfiguration(final ConfigurationDao configDao, final LdapConfigurationDao ldapConfigurationDao) {
        _ldapConfigurationDao = ldapConfigurationDao;
    }

    public String getAuthentication(final Long domainId) {
        if ((getBindPrincipal(domainId) == null) && (getBindPassword(domainId) == null)) {
            return "none";
        } else {
            return "simple";
        }
    }

    public String getBaseDn(final Long domainId) {
        return ldapBaseDn.valueIn(domainId);
    }

    public String getBindPassword(final Long domainId) {
        return ldapBindPassword.valueIn(domainId);
    }

    public String getBindPrincipal(final Long domainId) {
        return ldapBindPrincipal.valueIn(domainId);
    }

    public String getEmailAttribute(final Long domainId) {
        return ldapEmailAttribute.valueIn(domainId);
    }

    public String getFactory() {
        return factory;
    }

    public String getFirstnameAttribute(final Long domainId) {
        return ldapFirstnameAttribute.valueIn(domainId);
    }

    public String getLastnameAttribute(final Long domainId) {
        return ldapLastnameAttribute.valueIn(domainId);
    }

    public String getProviderUrl(final Long domainId) {
        final String protocol = getSSLStatus(domainId) == true ? "ldaps://" : "ldap://";
        final Pair<List<LdapConfigurationVO>, Integer> result = _ldapConfigurationDao.searchConfigurations(null, 0, domainId);
        final StringBuilder providerUrls = new StringBuilder();
        String delim = "";
        for (final LdapConfigurationVO resource : result.first()) {
            final String providerUrl = protocol + resource.getHostname() + ":" + resource.getPort();
            providerUrls.append(delim).append(providerUrl);
            delim = " ";
        }
        return providerUrls.toString();
    }

    public String[] getReturnAttributes(final Long domainId) {
        return new String[] {
                getUsernameAttribute(domainId),
                getEmailAttribute(domainId),
                getFirstnameAttribute(domainId),
                getLastnameAttribute(domainId),
                getCommonNameAttribute(),
                getUserAccountControlAttribute(),
                getUserMemberOfAttribute(domainId)
        };
    }

    public int getScope() {
        return scope;
    }

    public String getSearchGroupPrinciple(final Long domainId) {
        return ldapSearchGroupPrinciple.valueIn(domainId);
    }

    public boolean getSSLStatus(Long domainId) {
        boolean sslStatus = false;
        if (getTrustStore(domainId) != null && getTrustStorePassword(domainId) != null) {
            sslStatus = true;
        }
        return sslStatus;
    }

    public String getTrustStore(Long domainId) {
        return ldapTrustStore.valueIn(domainId);
    }

    public String getTrustStorePassword(Long domainId) {
        return ldapTrustStorePassword.valueIn(domainId);
    }

    public String getUsernameAttribute(final Long domainId) {
        return ldapUsernameAttribute.valueIn(domainId);
    }

    public String getUserObject(final Long domainId) {
        return ldapUserObject.valueIn(domainId);
    }

    public String getGroupObject(final Long domainId) {
        return ldapGroupObject.valueIn(domainId);
    }

    public String getGroupUniqueMemberAttribute(final Long domainId) {
        return ldapGroupUniqueMemberAttribute.valueIn(domainId);
    }

    // TODO remove hard-coding
    public String getCommonNameAttribute() {
        return "cn";
    }

    // TODO remove hard-coding
    public String getUserAccountControlAttribute() {
        return "userAccountControl";
    }

    public Long getReadTimeout(final Long domainId) {
        return ldapReadTimeout.valueIn(domainId);
    }

    public Integer getLdapPageSize(final Long domainId) {
        return ldapPageSize.valueIn(domainId);
    }

    public LdapUserManager.Provider getLdapProvider(final Long domainId) {
        LdapUserManager.Provider provider;
        try {
            provider = LdapUserManager.Provider.valueOf(ldapProvider.valueIn(domainId).toUpperCase());
        } catch (IllegalArgumentException ex) {
            //openldap is the default
            provider = LdapUserManager.Provider.OPENLDAP;
        }
        return provider;
    }

    public boolean isNestedGroupsEnabled(final Long domainId) {
        return ldapEnableNestedGroups.valueIn(domainId);
    }

    public static String getUserMemberOfAttribute(final Long domainId) {
        return ldapMemberOfAttribute.valueIn(domainId);
    }

    @Override
    public String getConfigComponentName() {
        return LdapConfiguration.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                ldapReadTimeout,
                ldapPageSize,
                ldapProvider,
                ldapEnableNestedGroups,
                ldapBaseDn,
                ldapBindPassword,
                ldapBindPrincipal,
                ldapEmailAttribute,
                ldapFirstnameAttribute,
                ldapLastnameAttribute,
                ldapUsernameAttribute,
                ldapUserObject,
                ldapSearchGroupPrinciple,
                ldapGroupObject,
                ldapGroupUniqueMemberAttribute,
                ldapTrustStore,
                ldapTrustStorePassword,
                ldapMemberOfAttribute
        };
    }
}
