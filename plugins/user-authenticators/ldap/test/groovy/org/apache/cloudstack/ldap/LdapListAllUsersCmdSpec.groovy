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


class LdapListAllUsersCmdSpec extends spock.lang.Specification {
    def "Test getEntityOwnerId is 1"() {
	given: "We have an LdapManager and LdapListAllUsersCmd"
	def ldapManager = Mock(LdapManager)
	def ldapListAllUsersCmd = new LdapListAllUsersCmd(ldapManager)
	when: "Get entity owner id is called"
	long ownerId = ldapListAllUsersCmd.getEntityOwnerId()
	then: "a 1 should be returned"
	ownerId == 1
    }

    def "Test successful empty response from execute"() {
	given: "We have a LdapManager with no users and a LdapListAllUsersCmd"
        def ldapManager = Mock(LdapManager)
        ldapManager.getUsers() >> {throw new NoLdapUserMatchingQueryException()}
        def ldapListAllUsersCmd = new LdapListAllUsersCmd(ldapManager)
	when: "LdapListAllUsersCmd is executed"
        ldapListAllUsersCmd.execute()
	then: "An array of size 0 is returned"
		ldapListAllUsersCmd.responseObject.getResponses().size() == 0
    }

    def "Test successful response from execute"() {
	given: "We have an LdapManager, one user and a LdapListAllUsersCmd"
	def ldapManager = Mock(LdapManager)
	List<LdapUser> users = new ArrayList()
	users.add(new LdapUser("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org"))
	ldapManager.getUsers() >> users
	LdapUserResponse response = new LdapUserResponse("rmurphy", "rmurphy@test.com", "Ryan", "Murphy", "cn=rmurphy,dc=cloudstack,dc=org")
	ldapManager.createLdapUserResponse(_) >> response
	def ldapListAllUsersCmd = new LdapListAllUsersCmd(ldapManager)
	when: "LdapListAllUsersCmd is executed"
	ldapListAllUsersCmd.execute()
	then: "a list of size not 0 is returned"
	ldapListAllUsersCmd.responseObject.getResponses().size() != 0
    }

    def "Test successful return of getCommandName"() {
	given: "We have an LdapManager and a LdapListAllUsersCmd"
        def ldapManager = Mock(LdapManager)
        def ldapListAllUsersCmd = new LdapListAllUsersCmd(ldapManager)
	when: "Get command name is called"
        String commandName = ldapListAllUsersCmd.getCommandName()
	then: "ldapuserresponse is returned"
        commandName == "ldapuserresponse"
    }
}
