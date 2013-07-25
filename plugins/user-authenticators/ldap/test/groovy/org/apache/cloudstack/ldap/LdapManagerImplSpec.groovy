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

import javax.naming.NamingException
import javax.naming.ldap.InitialLdapContext

import org.apache.cloudstack.api.command.LdapListConfigurationCmd
import org.apache.cloudstack.ldap.*
import org.apache.cloudstack.ldap.dao.LdapConfigurationDaoImpl

import com.cloud.exception.InvalidParameterValueException
import com.cloud.utils.Pair

class LdapManagerImplSpec extends spock.lang.Specification {
    def "Test that addConfiguration fails when a duplicate configuration exists"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapConfigurationDao.findByHostname(_) >> new LdapConfigurationVO("localhost", 389)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        ldapManager.addConfiguration("localhost", 389)
        then:
        thrown InvalidParameterValueException
    }

    def "Test that addConfiguration fails when a binding fails"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext(_) >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        ldapManager.addConfiguration("localhost", 389)
        then:
        thrown InvalidParameterValueException
    }

    def "Test successfully addConfiguration"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext(_) >> null
        ldapConfigurationDao.persist(_) >> null
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.addConfiguration("localhost", 389)
        then:
        result.hostname == "localhost"
        result.port == 389
    }

    def "Test successful failed result from deleteConfiguration due to configuration not existing"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapConfigurationDao.findByHostname(_) >> null
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        ldapManager.deleteConfiguration("localhost")
        then:
        thrown InvalidParameterValueException
    }

    def "Test successful result from deleteConfiguration"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapConfigurationDao.findByHostname(_) >> {
            def configuration = new LdapConfigurationVO("localhost", 389)
            configuration.setId(0);
            return configuration;
        }
        ldapConfigurationDao.remove(_) >> null
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.deleteConfiguration("localhost")
        then:
        result.hostname == "localhost"
        result.port == 389
    }

    def "Test successful failed result from canAuthenticate due to user not found"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = Spy(LdapManagerImpl, constructorArgs: [ldapConfigurationDao, ldapContextFactory, ldapUserManager])
        ldapManager.getUser(_) >> { throw new NamingException() }
        when:
        def result = ldapManager.canAuthenticate("rmurphy", "password")
        then:
        result == false
    }

    def "Test successful failed result from canAuthenticate due to bad password"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        ldapContextFactory.createUserContext(_, _) >> { throw new NamingException() }
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = Spy(LdapManagerImpl, constructorArgs: [ldapConfigurationDao, ldapContextFactory, ldapUserManager])
        ldapManager.getUser(_) >> { new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org") }
        when:
        def result = ldapManager.canAuthenticate("rmurphy", "password")
        then:
        result == false
    }

    def "Test successful result from canAuthenticate"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        ldapContextFactory.createUserContext(_, _) >> null
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = Spy(LdapManagerImpl, constructorArgs: [ldapConfigurationDao, ldapContextFactory, ldapUserManager])
        ldapManager.getUser(_) >> { new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org") }
        when:
        def result = ldapManager.canAuthenticate("rmurphy", "password")
        then:
        result == true
    }

    def "Test successful closing of context"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def context = Mock(InitialLdapContext)
        ldapManager.closeContext(context)
        then:
        context.defaultInitCtx == null
    }

    def "Test successful failing to close of context"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def context = Mock(InitialLdapContext)
        context.close() >> { throw new NamingException() }
        ldapManager.closeContext(context)
        then:
        context.defaultInitCtx == null
    }

    def "Test LdapConfigurationResponse generation"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.createLdapConfigurationResponse(new LdapConfigurationVO("localhost", 389))
        then:
        result.hostname == "localhost"
        result.port == 389
    }

    def "Test LdapUserResponse generation"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.createLdapUserResponse(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org"))
        then:
        result.username == "rmurphy"
        result.email == "rmurphy@test.com"
        result.firstname == "Ryan"
        result.lastname == "Murphy"
        result.principal == "cn=rmurphy,dc=cloudstack,dc=org"
    }

    def "Test that getCommands isn't empty"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.getCommands()
        then:
        result.size() > 0
    }

    def "Test failing of getUser due to bind issue"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext() >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        ldapManager.getUser("rmurphy")
        then:
        thrown NamingException
    }

    def "Test success of getUser"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext() >> null
        ldapUserManager.getUser(_, _) >> new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org")
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.getUser("rmurphy")
        then:
        result.username == "rmurphy"
        result.email == "rmurphy@test.com"
        result.firstname == "Ryan"
        result.lastname == "Murphy"
        result.principal == "cn=rmurphy,dc=cloudstack,dc=org"
    }

    def "Test failing of getUsers due to bind issue"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext() >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        ldapManager.getUsers()
        then:
        thrown NoLdapUserMatchingQueryException
    }

    def "Test success getUsers"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext() >> null
        List<LdapUser> users = new ArrayList<>();
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org"))
        ldapUserManager.getUsers(_) >> users;
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.getUsers()
        then:
        result.size() > 0;
    }

    def "Testing of listConfigurations"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        ldapConfigurationList.add(new LdapConfigurationVO("localhost", 389))
        Pair<List<LdapConfigurationVO>, Integer> configurations = new Pair<List<LdapConfigurationVO>, Integer>();
        configurations.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapConfigurationDao.searchConfigurations(_, _) >> configurations
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.listConfigurations(new LdapListConfigurationCmd())
        then:
        result.second() > 0
    }

    def "Test failing of searchUsers due to a failure to bind"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext() >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        ldapManager.searchUsers("rmurphy")
        then:
        thrown NoLdapUserMatchingQueryException
    }

    def "Test successful result from searchUsers"() {
        given:
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        ldapContextFactory.createBindContext() >> null;

        List<LdapUser> users = new ArrayList<LdapUser>();
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org"))
        ldapUserManager.getUsers(_, _) >> users;

        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManager)
        when:
        def result = ldapManager.searchUsers("rmurphy");
        then:
        result.size() > 0;
    }
}
