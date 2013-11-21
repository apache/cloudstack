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

import org.apache.cloudstack.api.command.LdapListConfigurationCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.utils.Pair;

public class LdapConfiguration {
    private final static String factory = "com.sun.jndi.ldap.LdapCtxFactory";

    private final static int scope = SearchControls.SUBTREE_SCOPE;

    @Inject
    private ConfigurationDao _configDao;

    @Inject
    private LdapManager _ldapManager;

    public LdapConfiguration() {
    }

    public LdapConfiguration(final ConfigurationDao configDao, final LdapManager ldapManager) {
        _configDao = configDao;
        _ldapManager = ldapManager;
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
        final Pair<List<? extends LdapConfigurationVO>, Integer> result = _ldapManager.listConfigurations(new LdapListConfigurationCmd(_ldapManager));
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
}