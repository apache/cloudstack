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
import com.cloud.user.DomainService
import com.cloud.user.UserAccount
import com.cloud.user.UserAccountVO
import org.apache.cloudstack.api.command.LdapImportUsersCmd
import org.apache.cloudstack.api.response.LdapUserResponse
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
	users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	ldapManager.getUsers() >> users
	LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
	LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
	ldapManager.createLdapUserResponse(_) >>>[response1, response2]


	Domain domain = new DomainVO("engineering", 1L, 1L, "engineering", UUID.randomUUID().toString())
	domainService.getDomainByName("engineering", 1L) >>> [null, domain]
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
	users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	ldapManager.getUsersInGroup("TestGroup") >> users
	LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
	LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
	ldapManager.createLdapUserResponse(_) >>>[response1, response2]


	Domain domain = new DomainVO("TestGroup", 1L, 1L, "TestGroup", UUID.randomUUID().toString())
	domainService.getDomainByName("TestGroup", 1L) >>> [null, domain]
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
	users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	ldapManager.getUsersInGroup("TestGroup") >> users
	LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
	LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
	ldapManager.createLdapUserResponse(_) >>>[response1, response2]


	Domain domain = new DomainVO("TestDomain", 1L, 1L, "TestDomain", UUID.randomUUID().toString())
	domainService.getDomainByName("TestDomain", 1L) >>> [null, domain]
	1 * domainService.createDomain("TestDomain", 1L, "TestDomain", _) >> domain

	def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
	ldapImportUsersCmd.accountType = 2;
	ldapImportUsersCmd.groupName = "TestGroup";
	ldapImportUsersCmd.domainName = "TestDomain";

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
	users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	users.add(new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering"))
	ldapManager.getUsers() >> users
	LdapUserResponse response1 = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
	LdapUserResponse response2 = new LdapUserResponse("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering")
	ldapManager.createLdapUserResponse(_) >>>[response1, response2]


	Domain domain = new DomainVO("TestDomain", 1L, 1L, "TestDomain", UUID.randomUUID().toString())
	domainService.getDomainByName("TestDomain", 1L) >>> [null, domain]
	1 * domainService.createDomain("TestDomain", 1L, "TestDomain", _) >> domain

	def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
	ldapImportUsersCmd.accountType = 2;
	ldapImportUsersCmd.domainName = "TestDomain";

	when: "LdapListUsersCmd is executed"
	ldapImportUsersCmd.execute()
	then: "a list of size 2 is returned"
	ldapImportUsersCmd.responseObject.getResponses().size() == 2
    }

    def "Test getDomain with no domain or group name specified specified"() {
	given: "We have an LdapManager, DomainService, two users and a LdapImportUsersCmd"
	def ldapManager = Mock(LdapManager)
	def domainService = Mock(DomainService)
	def accountService = Mock(AccountService)
	def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService, accountService)
	ldapImportUsersCmd.domainName = varDomainName
	ldapImportUsersCmd.groupName = varGroupName

	def ldapUser1 = new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering")
	def ldapUser2 = new LdapUser("bob", "bob@test.com", "Robert", "Young", "cn=bob,ou=engineering,dc=cloudstack,dc=org", "engineering");

	Domain domain = new DomainVO(expectedDomainName, 1L, 1L, expectedDomainName, UUID.randomUUID().toString())
	2 * domainService.getDomainByName(expectedDomainName, 1L) >>> [null, domain]
	1 * domainService.createDomain(expectedDomainName, 1L, expectedDomainName, _) >> domain

	def result1 = ldapImportUsersCmd.getDomain(ldapUser1)
	def result2 = ldapImportUsersCmd.getDomain(ldapUser2)
	expect: "engineering domain is returned"
	result1 == domain
	result2 == domain
	where: "The domain and group are set to the following values"
	varDomainName | varGroupName | expectedDomainName
	null | null | "engineering"
	"TestDomain" | null | "TestDomain"
	"TestDomain" | "TestGroup" | "TestDomain"
	null | "TestGroup" | "TestGroup"

    }

}
