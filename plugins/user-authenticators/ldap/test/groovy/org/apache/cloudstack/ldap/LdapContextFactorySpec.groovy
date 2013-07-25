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

import org.apache.cloudstack.ldap.LdapConfiguration
import org.apache.cloudstack.ldap.LdapContextFactory
import spock.lang.Shared

import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.ldap.LdapContext

class LdapContextFactorySpec extends spock.lang.Specification {
    @Shared
    private def ldapConfiguration

    @Shared
    private def username

    @Shared
    private def principal

    @Shared
    private def password

    def setupSpec() {
        ldapConfiguration = Mock(LdapConfiguration)

        ldapConfiguration.getFactory() >> "com.sun.jndi.ldap.LdapCtxFactory"
        ldapConfiguration.getProviderUrl() >> "ldap://localhost:389"
        ldapConfiguration.getAuthentication() >> "none"
        ldapConfiguration.getScope() >> SearchControls.SUBTREE_SCOPE
        ldapConfiguration.getReturnAttributes() >> ["uid", "mail", "cn"]
        ldapConfiguration.getUsernameAttribute() >> "uid"
        ldapConfiguration.getEmailAttribute() >> "mail"
        ldapConfiguration.getFirstnameAttribute() >> "givenname"
        ldapConfiguration.getLastnameAttribute() >> "sn"
        ldapConfiguration.getBaseDn() >> "dc=cloudstack,dc=org"

        username = "rmurphy"
        principal = "cn=" + username + "," + ldapConfiguration.getBaseDn()
        password = "password"
    }

    def "Test successfully creating a system environment with anon bind"() {
        given:
        def ldapContextFactory = new LdapContextFactory(ldapConfiguration)

        when:
        def result = ldapContextFactory.getEnvironment(principal, password, null, false)

        then:
        result['java.naming.provider.url'] == ldapConfiguration.getProviderUrl()
        result['java.naming.factory.initial'] == ldapConfiguration.getFactory()
        result['java.naming.security.principal'] == principal
        result['java.naming.security.authentication'] == "simple"
        result['java.naming.security.credentials'] == password
    }

    def "Test successfully creating a environment with username and password"() {
        given:
        def ldapContextFactory = new LdapContextFactory(ldapConfiguration)

        when:
        def result = ldapContextFactory.getEnvironment(null, null, null, true)

        then:
        result['java.naming.provider.url'] == ldapConfiguration.getProviderUrl()
        result['java.naming.factory.initial'] == ldapConfiguration.getFactory()
        result['java.naming.security.principal'] == null
        result['java.naming.security.authentication'] == ldapConfiguration.getAuthentication()
        result['java.naming.security.credentials'] == null
    }

    def "Test successfully binding as a user"() {
        given:
        def ldapContextFactory = new LdapContextFactory(ldapConfiguration)
        when:
        ldapContextFactory.createUserContext(principal, password)
        then:
        thrown NamingException
    }

    def "Test successully binding as system"() {
        given:
        def ldapContextFactory = new LdapContextFactory(ldapConfiguration)
        when:
        ldapContextFactory.createBindContext()
        then:
        thrown NamingException
    }

    def "Test succcessfully creating a initial context"() {
        given:
        def ldapContextFactory = new LdapContextFactory(ldapConfiguration)
        when:
        ldapContextFactory.createInitialDirContext(null, null, true)
        then:
        thrown NamingException
    }

    def "Test successful failed connection"() {
        given:
        def ldapContextFactory = Spy(LdapContextFactory, constructorArgs: [ldapConfiguration])
        when:
        ldapContextFactory.testConnection(ldapConfiguration.getProviderUrl())
        then:
        thrown NamingException
    }

    def "Test successful connection"() {
        given:
        def ldapContextFactory = Spy(LdapContextFactory, constructorArgs: [ldapConfiguration])
        ldapContextFactory.createBindContext(_) >> Mock(LdapContext)
        when:
        ldapContextFactory.testConnection(ldapConfiguration.getProviderUrl())
        then:
        notThrown NamingException
    }
}
