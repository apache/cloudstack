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
package groovy.org.apache.cloudstack.ldap

import com.cloud.configuration.dao.ConfigurationDao
import com.cloud.utils.Pair
import org.apache.cloudstack.api.ServerApiException
import org.apache.cloudstack.ldap.LdapConfiguration
import org.apache.cloudstack.ldap.LdapConfigurationVO
import org.apache.cloudstack.ldap.LdapManager

import javax.naming.directory.SearchControls

class LdapConfigurationSpec extends spock.lang.Specification {
    def "Test that providerUrl successfully returns a URL when a configuration is available"() {
        given:
        def configDao = Mock(ConfigurationDao)

        def ldapManager = Mock(LdapManager)
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        ldapConfigurationList.add(new LdapConfigurationVO("localhost", 389))
        Pair<List<LdapConfigurationVO>, Integer> result = new Pair<List<LdapConfigurationVO>, Integer>();
        result.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapManager.listConfigurations(_) >> result

        LdapConfiguration ldapConfiguration = new LdapConfiguration(configDao, ldapManager)

        when:
        String providerUrl = ldapConfiguration.getProviderUrl()

        then:
        providerUrl == "ldap://localhost:389"
    }

    def "Test that getAuthentication returns simple"() {
        given:
        def configDao = Mock(ConfigurationDao)
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        configDao.getValue("ldap.bind.password") >> "password"
        configDao.getValue("ldap.bind.principal") >> "cn=rmurphy,dc=cloudstack,dc=org"
        when:
        String authentication = ldapConfiguration.getAuthentication()
        then:
        authentication == "simple"
    }

    def "Test that getAuthentication returns none"() {
        given:
        def configDao = Mock(ConfigurationDao)
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String authentication = ldapConfiguration.getAuthentication()
        then:
        authentication == "none"
    }

    def "Test that getEmailAttribute returns mail"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.email.attribute") >> "mail"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String emailAttribute = ldapConfiguration.getEmailAttribute()
        then:
        emailAttribute == "mail"
    }

    def "Test that getUsernameAttribute returns uid"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.username.attribute") >> "uid"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String usernameAttribute = ldapConfiguration.getUsernameAttribute()
        then:
        usernameAttribute == "uid"
    }

    def "Test that getFirstnameAttribute returns givenname"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.firstname.attribute") >> "givenname"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String firstname = ldapConfiguration.getFirstnameAttribute()
        then:
        firstname == "givenname"
    }

    def "Test that getLastnameAttribute returns givenname"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.lastname.attribute") >> "sn"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String lastname = ldapConfiguration.getLastnameAttribute()
        then:
        lastname == "sn"
    }

    def "Test that getUserObject returns inetOrgPerson"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.user.object") >> "inetOrgPerson"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String userObject = ldapConfiguration.getUserObject()
        then:
        userObject == "inetOrgPerson"
    }

    def "Test that getReturnAttributes returns the correct data"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.firstname.attribute") >> "givenname"
        configDao.getValue("ldap.lastname.attribute") >> "sn"
        configDao.getValue("ldap.username.attribute") >> "uid"
        configDao.getValue("ldap.email.attribute") >> "mail"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String[] returnAttributes = ldapConfiguration.getReturnAttributes()
        then:
        returnAttributes == ["uid", "mail", "givenname", "sn"]
    }

    def "Test that getScope returns SearchControls.SUBTREE_SCOPE"() {
        given:
        def configDao = Mock(ConfigurationDao)
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        int scope = ldapConfiguration.getScope()
        then:
        scope == SearchControls.SUBTREE_SCOPE;
    }

    def "Test that getBaseDn returns dc=cloudstack,dc=org"() {
        given:
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.basedn") >> "dc=cloudstack,dc=org"
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String baseDn = ldapConfiguration.getBaseDn();
        then:
        baseDn == "dc=cloudstack,dc=org"
    }

    def "Test that getFactory returns com.sun.jndi.ldap.LdapCtxFactory"() {
        given:
        def configDao = Mock(ConfigurationDao)
        def ldapManager = Mock(LdapManager)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapManager)
        when:
        String factory = ldapConfiguration.getFactory();
        then:
        factory == "com.sun.jndi.ldap.LdapCtxFactory"
    }
}
