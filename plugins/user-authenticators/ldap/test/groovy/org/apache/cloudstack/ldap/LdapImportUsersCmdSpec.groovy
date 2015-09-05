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

import com.cloud.domain.Domain
import com.cloud.domain.DomainVO
import com.cloud.user.AccountService
import com.cloud.user.AccountVO
import com.cloud.user.DomainService
import com.cloud.user.User
import com.cloud.user.UserAccountVO
import com.cloud.user.UserVO
import org.apache.cloudstack.api.command.LdapCreateAccountCmd
import org.apache.cloudstack.api.command.LdapImportUsersCmd
import org.apache.cloudstack.api.response.LdapUserResponse
import org.apache.cloudstack.context.CallContext
import org.apache.cloudstack.ldap.LdapManager
import org.apache.cloudstack.ldap.LdapUser

class LdapImportUsersCmdSpec extends spock.lang.Specification {


    def "Test successful return of getCommandName"() {
        given: "We have an LdapManager, DomainService and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        def domainService = Mock(DomainService)
        def accountService = Mock(AccountService)
        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        when: "Get command name is called"
        String commandName = ldapImportUsersCmd.getCommandName()
        then: "ldapuserresponse is returned"
        commandName == "ldapuserresponse"
    }

    def "Test successful response from execute"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        def domainService = Mock(DomainService)
        def accountService = Mock(AccountService)

        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsers() >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> [response1, response2]


        Domain domain = new DomainVO("engineering", 1L, 1L, "engineering", UUID.randomUUID().toString())
        2 * domainService.getDomainByName("engineering", 1L) >>> [null, domain]
        1 * domainService.createDomain("engineering", 1L, "engineering", _) >> domain

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountType = 2;

        when: "LdapListUsersCmd is executed"
        ldapImportUsersCmd.execute()
        then: "a list of size 2 is returned"
        ldapImportUsersCmd.responseObject.getResponses().size() == 2
    }

    def "Test successful response from execute with group specified"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        def domainService = Mock(DomainService)
        def accountService = Mock(AccountService)

        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsersInGroup("TestGroup") >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> [response1, response2]


        Domain domain = new DomainVO("TestGroup", 1L, 1L, "TestGroup", UUID.randomUUID().toString())
        1 * domainService.getDomainByName("TestGroup", 1L) >>> null
        1 * domainService.createDomain("TestGroup", 1L, "TestGroup", _) >> domain

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountType = 2;
        ldapImportUsersCmd.groupName = "TestGroup";

        when: "LdapListUsersCmd is executed"
        ldapImportUsersCmd.execute()
        then: "a list of size 2 is returned"
        ldapImportUsersCmd.responseObject.getResponses().size() == 2
    }

    def "Test successful response from execute with group and domain specified"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        def domainService = Mock(DomainService)
        def accountService = Mock(AccountService)

        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsersInGroup("TestGroup") >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> [response1, response2]


        Domain domain = new DomainVO("TestDomain", 1L, 1L, "TestDomain", UUID.randomUUID().toString())
        1 * domainService.getDomain(1L) >> domain;

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountType = 2;
        ldapImportUsersCmd.groupName = "TestGroup";
        ldapImportUsersCmd.domainId = 1L;

        when: "LdapListUsersCmd is executed"
        ldapImportUsersCmd.execute()
        then: "a list of size 2 is returned"
        ldapImportUsersCmd.responseObject.getResponses().size() == 2
    }

    def "Test successful response from execute with domain specified"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        def domainService = Mock(DomainService)
        def accountService = Mock(AccountService)

        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsers() >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> [response1, response2]


        Domain domain = new DomainVO("TestDomain", 1L, 1L, "TestDomain", UUID.randomUUID().toString())
        1 * domainService.getDomain(1L) >> domain;

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountType = 2;
        ldapImportUsersCmd.domainId = 1L;

        when: "LdapListUsersCmd is executed"
        ldapImportUsersCmd.execute()
        then: "a list of size 2 is returned"
        ldapImportUsersCmd.responseObject.getResponses().size() == 2
    }

    def "Test getDomain"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        def domainService = Mock(DomainService)
        def accountService = Mock(AccountService)
        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.domainId = varDomainId
        ldapImportUsersCmd.groupName = varGroupName

        def ldapUser1 = new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false)
        def ldapUser2 = new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering", false);

        Domain domain = new DomainVO(expectedDomainName, 1L, 1L, expectedDomainName, UUID.randomUUID().toString());
        if (varDomainId != null) {
            1 * domainService.getDomain(varDomainId) >> domain;
        } else {
            if(varGroupName != null) {
                1 * domainService.getDomainByName(expectedDomainName, 1L) >> null
            } else {
                domainService.getDomainByName(expectedDomainName, 1L) >>> [null, domain]
            }
            1 * domainService.createDomain(expectedDomainName, 1L, expectedDomainName, _) >> domain
        }

        def result1 = ldapImportUsersCmd.getDomain(ldapUser1)
        def result2 = ldapImportUsersCmd.getDomain(ldapUser2)
        expect: "engineering domain is returned"
        result1 == domain
        result2 == domain
        where: "The domain and group are set to the following values"
        varDomainId | varGroupName | expectedDomainName
        null        | null         | "engineering"
        1L          | null         | "TestDomain"
        1L          | "TestGroup"  | "TestDomain"
        null        | "TestGroup"  | "TestGroup"
        null        | "Test Group"  | "TestGroup"

    }


    def "Test create ldap import account for an already existing cloudstack account"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsers() >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> response1

        def domainService = Mock(DomainService)
        1 * domainService.getDomain(1L) >> new DomainVO("DOMAIN", 1L, 1L, "DOMAIN", UUID.randomUUID().toString());;

        def accountService = Mock(AccountService)
        1 * accountService.getActiveAccountByName('ACCOUNT', 0) >>  Mock(AccountVO)

        1 * accountService.createUser('rmurphy', _ , 'Ryan', 'Murphy', 'rmurphy@test.com', null, 'ACCOUNT', 0, _, User.Source.LDAP) >> Mock(UserVO)
        0 * accountService.createUserAccount('rmurphy', _, 'Ryan', 'Murphy', 'rmurphy@test.com', null, 'ACCOUNT', 2, 0, 'DOMAIN', null, _, _, User.Source.LDAP)
        0 * accountService.updateUser(_,'Ryan', 'Murphy', 'rmurphy@test.com', null, null, null, null, null);

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountName = "ACCOUNT"
        ldapImportUsersCmd.accountType = 2;
        ldapImportUsersCmd.domainId = 1L;

        when: "create account is called"
        ldapImportUsersCmd.execute()
        then: "expect 1 call on accountService createUser and 0 on account service create user account"
    }


    def "Test create ldap import account for an already existing cloudstack user"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsers() >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> response1

        def domainService = Mock(DomainService)
        1 * domainService.getDomain(1L) >> new DomainVO("DOMAIN", 1L, 1L, "DOMAIN", UUID.randomUUID().toString());;

        def accountService = Mock(AccountService)
        1 * accountService.getActiveAccountByName('ACCOUNT', 0) >>  Mock(AccountVO)
        1 * accountService.getActiveUserAccount('rmurphy',0) >> Mock(UserAccountVO)
        0 * accountService.createUser('rmurphy', _ , 'Ryan', 'Murphy', 'rmurphy@test.com', null, 'ACCOUNT', 0, _) >> Mock(UserVO)
        0 * accountService.createUserAccount('rmurphy', _, 'Ryan', 'Murphy', 'rmurphy@test.com', null, 'ACCOUNT', 2, 0, 'DOMAIN', null, _, _)
        1 * accountService.updateUser(_,'Ryan', 'Murphy', 'rmurphy@test.com', null, null, null, null, null);

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountName = "ACCOUNT"
        ldapImportUsersCmd.accountType = 2;
        ldapImportUsersCmd.domainId = 1L;

        when: "create account is called"
        ldapImportUsersCmd.execute()
        then: "expect 1 call on accountService updateUser and 0 on account service create user and create user account"
    }

    def "Test create ldap import account for a new cloudstack account"() {
        given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
        def ldapManager = Mock(LdapManager)
        List<LdapUser> users = new ArrayList()
        users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false))
        ldapManager.getUsers() >> users
        LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
        ldapManager.createLdapUserResponse(_) >>> response1

        def domainService = Mock(DomainService)
        1 * domainService.getDomain(1L) >> new DomainVO("DOMAIN", 1L, 1L, "DOMAIN", UUID.randomUUID().toString());;

        def accountService = Mock(AccountService)
        1 * accountService.getActiveAccountByName('ACCOUNT', 0) >>  null
        0 * accountService.createUser('rmurphy', _ , 'Ryan', 'Murphy', 'rmurphy@test.com', null, 'ACCOUNT', 0, _, User.Source.LDAP)
        1 * accountService.createUserAccount('rmurphy', _, 'Ryan', 'Murphy', 'rmurphy@test.com', null, 'ACCOUNT', 2, 0, 'DOMAIN', null, _, _, User.Source.LDAP)
        0 * accountService.updateUser(_,'Ryan', 'Murphy', 'rmurphy@test.com', null, null, null, null, null);

        def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
        ldapImportUsersCmd.accountName = "ACCOUNT"
        ldapImportUsersCmd.accountType = 2;
        ldapImportUsersCmd.domainId = 1L;

        when: "create account is called"
        ldapImportUsersCmd.execute()
        then: "expect 1 call on accountService createUser and 0 on account service create user account"
    }

}
