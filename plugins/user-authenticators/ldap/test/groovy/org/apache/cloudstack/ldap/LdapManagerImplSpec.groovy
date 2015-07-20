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

import org.apache.cloudstack.api.command.LDAPConfigCmd
import org.apache.cloudstack.api.command.LDAPRemoveCmd
import org.apache.cloudstack.api.command.LdapAddConfigurationCmd
import org.apache.cloudstack.api.command.LdapCreateAccountCmd
import org.apache.cloudstack.api.command.LdapDeleteConfigurationCmd
import org.apache.cloudstack.api.command.LdapImportUsersCmd
import org.apache.cloudstack.api.command.LdapListUsersCmd
import org.apache.cloudstack.api.command.LdapUserSearchCmd

import javax.naming.NamingException
import javax.naming.ldap.InitialLdapContext

import org.apache.cloudstack.api.command.LdapListConfigurationCmd
import org.apache.cloudstack.ldap.*
import org.apache.cloudstack.ldap.dao.LdapConfigurationDaoImpl

import com.cloud.exception.InvalidParameterValueException
import com.cloud.utils.Pair

class LdapManagerImplSpec extends spock.lang.Specification {
    def "Test failing of getUser due to bind issue"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext() >> { throw new NoLdapUserMatchingQueryException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for a user but there is a bind issue"
        ldapManager.getUser("rmurphy")
        then: "an exception is thrown"
        thrown NoLdapUserMatchingQueryException
    }

    def "Test failing of getUsers due to bind issue"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext() >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for a group of users but there is a bind issue"
        ldapManager.getUsers()
        then: "An exception is thrown"
        thrown NoLdapUserMatchingQueryException
    }

    def "Test failing of searchUsers due to a failure to bind"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext() >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for users"
        ldapManager.searchUsers("rmurphy")
        then: "An exception is thrown"
        thrown NoLdapUserMatchingQueryException
    }

    def "Test LdapConfigurationResponse generation"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A ldap configuration response is generated"
        def result = ldapManager.createLdapConfigurationResponse(new LdapConfigurationVO("localhost", 389))
        then: "the result of the response should match the given LdapConfigurationVO"
        result.hostname == "localhost"
        result.port == 389
    }

    def "Test LdapUserResponse generation"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A ldap user response is generated"
        def result = ldapManager.createLdapUserResponse(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org",
                "engineering"))
        then: "The result of the response should match the given ldap user"
        result.username == "rmurphy"
        result.email == "rmurphy@test.com"
        result.firstname == "Ryan"
        result.lastname == "Murphy"
        result.principal == "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org"
        result.domain == "engineering"
    }

    def "Test success getUsers"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext() >> null
        List<LdapUser> users = new ArrayList<>();
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null))
        ldapUserManager.getUsers(_) >> users;
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for a group of users"
        def result = ldapManager.getUsers()
        then: "A list greater than 0 is returned"
        result.size() > 0;
    }

    def "Test success of getUser"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext() >> null
        ldapUserManager.getUser(_, _) >> new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for a user"
        def result = ldapManager.getUser("rmurphy")
        then: "The user is returned"
        result.username == "rmurphy"
        result.email == "rmurphy@test.com"
        result.firstname == "Ryan"
        result.lastname == "Murphy"
        result.principal == "cn=rmurphy,dc=cloudstack,dc=org"
    }

    def "Test successful closing of context"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "The context is closed"
        def context = Mock(InitialLdapContext)
        ldapManager.closeContext(context)
        then: "The context is null"
        context.defaultInitCtx == null
    }

    def "Test successful failed result from canAuthenticate due to bad password"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        ldapContextFactory.createUserContext(_, _) >> { throw new NamingException() }
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapConfiguration = Mock(LdapConfiguration)
        def ldapManager = Spy(LdapManagerImpl, constructorArgs: [ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration])
        ldapManager.getUser(_) >> { new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null) }
        when: "The user attempts to authenticate with a bad password"
        def result = ldapManager.canAuthenticate("rmurphy", "password")
        then: "The authentication fails"
        result == false
    }

    def "Test successful failed result from canAuthenticate due to user not found"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapConfiguration = Mock(LdapConfiguration)
        def ldapManager = Spy(LdapManagerImpl, constructorArgs: [ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration])
        ldapManager.getUser(_) >> { throw new NamingException() }
        when: "The user attempts to authenticate and the user is not found"
        def result = ldapManager.canAuthenticate("rmurphy", "password")
        then: "the authentication fails"
        result == false
    }

    def "Test successful failed result from deleteConfiguration due to configuration not existing"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapConfigurationDao.findByHostname(_) >> null
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A ldap configuration that doesn't exist is deleted"
        ldapManager.deleteConfiguration("localhost")
        then: "A exception is thrown"
        thrown InvalidParameterValueException
    }

    def "Test successful failing to close of context"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "The context is closed"
        def context = Mock(InitialLdapContext)
        context.close() >> { throw new NamingException() }
        ldapManager.closeContext(context)
        then: "An exception is thrown"
        context.defaultInitCtx == null
    }

    def "Test successful result from canAuthenticate"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        ldapContextFactory.createUserContext(_, _) >> null
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapManager = Spy(LdapManagerImpl, constructorArgs: [ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration])
        ldapManager.getUser(_) >> { new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", null) }
        when: "A user authenticates"
        def result = ldapManager.canAuthenticate("rmurphy", "password")
        then: "The result is true"
        result == true
    }

    def "Test successful result from deleteConfiguration"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapConfigurationDao.findByHostname(_) >> {
            def configuration = new LdapConfigurationVO("localhost", 389)
            configuration.setId(0);
            return configuration;
        }
        ldapConfigurationDao.remove(_) >> null
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A ldap configuration is deleted"
        def result = ldapManager.deleteConfiguration("localhost")
        then: "The deleted configuration is returned"
        result.hostname == "localhost"
        result.port == 389
    }

    def "Test successful result from searchUsers"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapContextFactory.createBindContext() >> null;

        List<LdapUser> users = new ArrayList<LdapUser>();
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering"))
        ldapUserManager.getUsers(_, _) >> users;

        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for users"
        def result = ldapManager.searchUsers("rmurphy");
        then: "A list of atleast 1 is returned"
        result.size() > 0;
    }

    def "Test successfully addConfiguration"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext(_) >> null
        ldapConfigurationDao.persist(_) >> null
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A ldap configuration is added"
        def result = ldapManager.addConfiguration("localhost", 389)
        then: "the resulting object contain the given hostname and port"
        result.hostname == "localhost"
        result.port == 389
    }

    def "Test that addConfiguration fails when a binding fails"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext(_) >> { throw new NamingException() }
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A configuration is added that can not be binded"
        ldapManager.addConfiguration("localhost", 389)
        then: "An exception is thrown"
        thrown InvalidParameterValueException
    }

    def "Test that addConfiguration fails when a duplicate configuration exists"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapConfigurationDao.findByHostname(_) >> new LdapConfigurationVO("localhost", 389)
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "a configuration that already exists is added"
        ldapManager.addConfiguration("localhost", 389)
        then: "An exception is thrown"
        thrown InvalidParameterValueException
    }

    def supportedLdapCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(LdapUserSearchCmd.class);
        cmdList.add(LdapListUsersCmd.class);
        cmdList.add(LdapAddConfigurationCmd.class);
        cmdList.add(LdapDeleteConfigurationCmd.class);
        cmdList.add(LdapListConfigurationCmd.class);
        cmdList.add(LdapCreateAccountCmd.class);
        cmdList.add(LdapImportUsersCmd.class);
        cmdList.add(LDAPConfigCmd.class);
        cmdList.add(LDAPRemoveCmd.class);
        return cmdList
    }

    def "Test that getCommands isn't empty"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        def ldapConfiguration = Mock(LdapConfiguration)
        final List<Class<?>> cmdList = supportedLdapCommands()
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "Get commands is called"
        def result = ldapManager.getCommands()
        then: "it must return all the commands"
        result.size() > 0
        result == cmdList
    }

    def "Testing of listConfigurations"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        ldapConfigurationList.add(new LdapConfigurationVO("localhost", 389))
        Pair<List<LdapConfigurationVO>, Integer> configurations = new Pair<List<LdapConfigurationVO>, Integer>();
        configurations.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapConfigurationDao.searchConfigurations(_, _) >> configurations
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A request for configurations is made"
        def result = ldapManager.listConfigurations(new LdapListConfigurationCmd())
        then: "Then atleast 1 ldap configuration is returned"
        result.second() > 0
    }

    def "Testing of isLdapEnabled"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        ldapConfigurationList.add(new LdapConfigurationVO("localhost", 389))
        Pair<List<LdapConfigurationVO>, Integer> configurations = new Pair<List<LdapConfigurationVO>, Integer>();
        configurations.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapConfigurationDao.searchConfigurations(_, _) >> configurations
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "A request to find out is ldap enabled"
        def result = ldapManager.isLdapEnabled();
        then: "true is returned because a configuration was found"
        result == true;
    }

    def "Test success getUsersInGroup"() {
        given: "We have an LdapConfigurationDao, LdapContextFactory, LdapUserManager and LdapManager"
        def ldapConfigurationDao = Mock(LdapConfigurationDaoImpl)
        def ldapContextFactory = Mock(LdapContextFactory)
        def ldapUserManager = Mock(LdapUserManager)
        def ldapUserManagerFactory = Mock(LdapUserManagerFactory)
        def ldapConfiguration = Mock(LdapConfiguration)
        ldapUserManagerFactory.getInstance(_) >> ldapUserManager
        ldapContextFactory.createBindContext() >> null
        List<LdapUser> users = new ArrayList<>();
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org", "engineering"))
        ldapUserManager.getUsersInGroup("engineering", _) >> users;
        def ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, ldapUserManagerFactory, ldapConfiguration)
        when: "We search for a group of users"
        def result = ldapManager.getUsersInGroup("engineering")
        then: "A list greater of size one is returned"
        result.size() == 1;
    }
}
