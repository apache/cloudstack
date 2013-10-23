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
	def ldapImportUsersCmd = new LdapImportUsersCmd(ldapManager, domainService)
	when: "Get command name is called"
	String commandName = ldapImportUsersCmd.getCommandName()
	then: "ldapuserresponse is returned"
	commandName == "ldapuserresponse"
    }

    def "Test successful response from execute"() {
	given: "We have an LdapManager, DomainService, one user and a LdapImportUsersCmd"
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
}
