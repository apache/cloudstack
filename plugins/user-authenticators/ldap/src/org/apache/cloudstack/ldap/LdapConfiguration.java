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

    private static final ConfigKey<Long> ldapReadTimeout = new ConfigKey<Long>(Long.class, "ldap.read.timeout", "Advanced", "1000",
        "LDAP connection Timeout in milli sec", true, ConfigKey.Scope.Global, 1l);

    private static final ConfigKey<Integer> ldapPageSize = new ConfigKey<Integer>(Integer.class, "ldap.request.page.size", "Advanced", "1000",
                                                                               "page size sent to ldap server on each request to get user", true, ConfigKey.Scope.Global, 1);
    private static final ConfigKey<String> ldapProvider = new ConfigKey<String>(String.class, "ldap.provider", "Advanced", "openldap", "ldap provider ex:openldap, microsoftad",
                                                                                true, ConfigKey.Scope.Global, null);

    private final static int scope = SearchControls.SUBTREE_SCOPE;

    @Inject
    private ConfigurationDao _configDao;

    @Inject
    private LdapConfigurationDao _ldapConfigurationDao;

    public LdapConfiguration() {
    }

    public LdapConfiguration(final ConfigurationDao configDao, final LdapConfigurationDao ldapConfigurationDao) {
        _configDao = configDao;
        _ldapConfigurationDao = ldapConfigurationDao;
    }

    public String getAuthentication() {
        if ((getBindPrincipal() == null) && (getBindPassword() == null)) {
            return "none";
        } else {
            return "simple";
        }
    }

    public String getBaseDn() {
        return _configDao.getValue("ldap.basedn");
    }

    public String getBindPassword() {
        return _configDao.getValue("ldap.bind.password");
    }

    public String getBindPrincipal() {
        return _configDao.getValue("ldap.bind.principal");
    }

    public String getEmailAttribute() {
        final String emailAttribute = _configDao.getValue("ldap.email.attribute");
        return emailAttribute == null ? "mail" : emailAttribute;
    }

    public String getFactory() {
        return factory;
    }

    public String getFirstnameAttribute() {
        final String firstnameAttribute = _configDao.getValue("ldap.firstname.attribute");
        return firstnameAttribute == null ? "givenname" : firstnameAttribute;
    }

    public String getLastnameAttribute() {
        final String lastnameAttribute = _configDao.getValue("ldap.lastname.attribute");
        return lastnameAttribute == null ? "sn" : lastnameAttribute;
    }

    public String getProviderUrl() {
        final String protocol = getSSLStatus() == true ? "ldaps://" : "ldap://";
        final Pair<List<LdapConfigurationVO>, Integer> result = _ldapConfigurationDao.searchConfigurations(null, 0);
        final StringBuilder providerUrls = new StringBuilder();
        String delim = "";
        for (final LdapConfigurationVO resource : result.first()) {
            final String providerUrl = protocol + resource.getHostname() + ":" + resource.getPort();
            providerUrls.append(delim).append(providerUrl);
            delim = " ";
        }
        return providerUrls.toString();
    }

    public String[] getReturnAttributes() {
        return new String[] {getUsernameAttribute(), getEmailAttribute(), getFirstnameAttribute(), getLastnameAttribute(), getCommonNameAttribute()};
    }

    public int getScope() {
        return scope;
    }

    public String getSearchGroupPrinciple() {
        return _configDao.getValue("ldap.search.group.principle");
    }

    public boolean getSSLStatus() {
        boolean sslStatus = false;
        if (getTrustStore() != null && getTrustStorePassword() != null) {
            sslStatus = true;
        }
        return sslStatus;
    }

    public String getTrustStore() {
        return _configDao.getValue("ldap.truststore");
    }

    public String getTrustStorePassword() {
        return _configDao.getValue("ldap.truststore.password");
    }

    public String getUsernameAttribute() {
        final String usernameAttribute = _configDao.getValue("ldap.username.attribute");
        return usernameAttribute == null ? "uid" : usernameAttribute;
    }

    public String getUserObject() {
        final String userObject = _configDao.getValue("ldap.user.object");
        return userObject == null ? "inetOrgPerson" : userObject;
    }

    public String getGroupObject() {
        final String groupObject = _configDao.getValue("ldap.group.object");
        return groupObject == null ? "groupOfUniqueNames" : groupObject;
    }

    public String getGroupUniqueMemeberAttribute() {
        final String uniqueMemberAttribute = _configDao.getValue("ldap.group.user.uniquemember");
        return uniqueMemberAttribute == null ? "uniquemember" : uniqueMemberAttribute;
    }

    public String getCommonNameAttribute() {
        return "cn";
    }

    public Long getReadTimeout() {
        return ldapReadTimeout.value();
    }

    public Integer getLdapPageSize() {
        return ldapPageSize.value();
    }

    public LdapUserManager.Provider getLdapProvider() {
        LdapUserManager.Provider provider;
        try {
            provider = LdapUserManager.Provider.valueOf(ldapProvider.value().toUpperCase());
        } catch (IllegalArgumentException ex) {
            //openldap is the default
            provider = LdapUserManager.Provider.OPENLDAP;
        }
        return provider;
    }

    @Override
    public String getConfigComponentName() {
        return LdapConfiguration.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {ldapReadTimeout, ldapPageSize, ldapProvider};
    }
}