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

import org.apache.cloudstack.framework.config.ConfigKey
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import com.cloud.utils.Pair
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl
import org.apache.cloudstack.framework.config.impl.ConfigurationVO
import org.apache.cloudstack.ldap.LdapConfiguration
import org.apache.cloudstack.ldap.LdapConfigurationVO
import org.apache.cloudstack.ldap.LdapUserManager
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao

import javax.naming.directory.SearchControls

class LdapConfigurationSpec extends spock.lang.Specification {
    def "Test that getAuthentication returns none"() {
        given: "We have a ConfigDao, LdapManager and LdapConfiguration"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(ldapConfigurationDao)
        when: "Get authentication is called"
        String authentication = ldapConfiguration.getAuthentication()
        then: "none should be returned"
        authentication == "none"
    }

    def "Test that getEmailAttribute returns mail"() {
        given: "Given that we have a ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.email.attribute") >> "mail"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get Email Attribute is called"
        String emailAttribute = ldapConfiguration.getEmailAttribute()
        then: "mail should be returned"
        emailAttribute == "mail"
    }

    def "Test that getFactory returns com.sun.jndi.ldap.LdapCtxFactory"() {
        given: "We have a ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get Factory is scalled"
        String factory = ldapConfiguration.getFactory();
        then: "com.sun.jndi.ldap.LdapCtxFactory is returned"
        factory == "com.sun.jndi.ldap.LdapCtxFactory"
    }

    def "Test that getFirstnameAttribute returns givenname"() {
        given: "We have a ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.firstname.attribute") >> "givenname"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get firstname attribute is called"
        String firstname = ldapConfiguration.getFirstnameAttribute()
        then: "givennam should be returned"
        firstname == "givenname"
    }

    def "Test that getLastnameAttribute returns givenname"() {
        given: "We have a ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.lastname.attribute") >> "sn"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get Lastname Attribute is scalled "
        String lastname = ldapConfiguration.getLastnameAttribute()
        then: "sn should be returned"
        lastname == "sn"
    }

    def "Test that getReturnAttributes returns the correct data"() {
        given: "We have a ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.firstname.attribute") >> "givenname"
        configDao.getValue("ldap.lastname.attribute") >> "sn"
        configDao.getValue("ldap.username.attribute") >> "uid"
        configDao.getValue("ldap.email.attribute") >> "mail"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get return attributes is called"
        String[] returnAttributes = ldapConfiguration.getReturnAttributes()
        then: "An array containing uid, mail, givenname, sn and cn is returned"
        returnAttributes == ["uid", "mail", "givenname", "sn", "cn", "userAccountControl"]
    }

    def "Test that getScope returns SearchControls.SUBTREE_SCOPE"() {
        given: "We have ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get scope is called"
        int scope = ldapConfiguration.getScope()
        then: "SearchControls.SUBTRE_SCOPE should be returned"
        scope == SearchControls.SUBTREE_SCOPE;
    }

    def "Test that getUsernameAttribute returns uid"() {
        given: "We have ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.username.attribute") >> "uid"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get Username Attribute is called"
        String usernameAttribute = ldapConfiguration.getUsernameAttribute()
        then: "uid should be returned"
        usernameAttribute == "uid"
    }

    def "Test that getUserObject returns inetOrgPerson"() {
        given: "We have a ConfigDao, LdapManager and LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        configDao.getValue("ldap.user.object") >> "inetOrgPerson"
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        def ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)
        when: "Get user object is called"
        String userObject = ldapConfiguration.getUserObject()
        then: "inetOrgPerson is returned"
        userObject == "inetOrgPerson"
    }

    def "Test that providerUrl successfully returns a URL when a configuration is available"() {
        given: "We have a ConfigDao, LdapManager, LdapConfiguration"
        def configDao = Mock(ConfigurationDao)
        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        ldapConfigurationList.add(new LdapConfigurationVO("localhost", 389))
        Pair<List<LdapConfigurationVO>, Integer> result = new Pair<List<LdapConfigurationVO>, Integer>();
        result.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapConfigurationDao.searchConfigurations(_,_) >> result

        LdapConfiguration ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)

        when: "A request is made to get the providerUrl"
        String providerUrl = ldapConfiguration.getProviderUrl(_)

        then: "The providerUrl should be given."
        providerUrl == "ldap://localhost:389"
    }

    def "Test getReadTimeout"() {
        given: "We have configdao for ldap group object"
        def configDao = Mock(ConfigurationDao)
        ConfigurationVO configurationVo = new ConfigurationVO("ldap.read.timeout", LdapConfiguration.ldapReadTimeout);
        configurationVo.setValue(timeout)
        configDao.findById("ldap.read.timeout") >> configurationVo

        def configDepotImpl = Mock(ConfigDepotImpl)
        configDepotImpl.global() >> configDao
        ConfigKey.init(configDepotImpl)

        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        LdapConfiguration ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)

        def expected = timeout == null ? 1000 : timeout.toLong() //1000 is the default value

        def result = ldapConfiguration.getReadTimeout(null)
        expect:
        result == expected
        where:
        timeout << ["1000000", "1000", null]
    }

    def "Test getLdapProvider()"() {
        given: "We have configdao for ldap group object"
        def configDao = Mock(ConfigurationDao)
        ConfigurationVO configurationVo = new ConfigurationVO("ldap.read.timeout", LdapConfiguration.ldapProvider);
        configurationVo.setValue(provider)
        configDao.findById("ldap.provider") >> configurationVo

        def configDepotImpl = Mock(ConfigDepotImpl)
        configDepotImpl.global() >> configDao
        ConfigKey.init(configDepotImpl)

        def ldapConfigurationDao = Mock(LdapConfigurationDao)
        LdapConfiguration ldapConfiguration = new LdapConfiguration(configDao, ldapConfigurationDao)

        def expected = provider.equalsIgnoreCase("microsoftad") ? LdapUserManager.Provider.MICROSOFTAD : LdapUserManager.Provider.OPENLDAP //"openldap" is the default value

        def result = ldapConfiguration.getLdapProvider(null)
        expect:
        println "asserting for provider configuration: " + provider
        result == expected
        where:
        provider << ["openldap", "microsoftad", "", " ", "xyz", "MicrosoftAd", "OpenLdap", "MicrosoftAD"]
    }

}
