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

import org.apache.cloudstack.ldap.LdapUser

class LdapUserSpec extends spock.lang.Specification {

    def "Testing LdapUsers hashCode generation"() {
		given:
		def userA = new LdapUser(usernameA, "", "", "", "", "", false)
		expect:
		userA.hashCode() == usernameA.hashCode()
		where:
		usernameA = "A"
    }

    def "Testing that LdapUser successfully gives the correct result for a compare to"() {
		given: "You have created two LDAP user objects"
		def userA = new LdapUser(usernameA, "", "", "", "", "", false)
		def userB = new LdapUser(usernameB, "", "", "", "", "", false)
		expect: "That when compared the result is less than or equal to 0"
		userA.compareTo(userB) <= 0
		where: "The following values are used"
		usernameA | usernameB
		"A"       | "B"
		"A"       | "A"
    }

    def "Testing that LdapUsers equality"() {
		given:
		def userA = new LdapUser(usernameA, "", "", "", "", "", false)
		def userB = new LdapUser(usernameB, "", "", "", "", "", false)
		expect:
		userA.equals(userA) == true
		userA.equals(new Object()) == false
		userA.equals(userB) == false
		where:
		usernameA | usernameB
		"A"       | "B"
    }

    def "Testing that the username is correctly set with the ldap object"() {
        given: "You have created a LDAP user object with a username"
	def user = new LdapUser(username, "", "", "", "", "", false)
        expect: "The username is equal to the given data source"
        user.getUsername() == username
        where: "The username is set to "
        username << ["", null, "rmurphy"]
    }

    def "Testing the email is correctly set with the ldap object"() {
        given: "You have created a LDAP user object with a email"
	def user = new LdapUser("", email, "", "", "", "", false)
        expect: "The email is equal to the given data source"
        user.getEmail() == email
        where: "The email is set to "
        email << ["", null, "test@test.com"]
    }

    def "Testing the firstname is correctly set with the ldap object"() {
        given: "You have created a LDAP user object with a firstname"
	def user = new LdapUser("", "", firstname, "", "", "", false)
        expect: "The firstname is equal to the given data source"
        user.getFirstname() == firstname
        where: "The firstname is set to "
        firstname << ["", null, "Ryan"]
    }

    def "Testing the lastname is correctly set with the ldap object"() {
        given: "You have created a LDAP user object with a lastname"
	def user = new LdapUser("", "", "", lastname, "", "", false)
        expect: "The lastname is equal to the given data source"
        user.getLastname() == lastname
        where: "The lastname is set to "
        lastname << ["", null, "Murphy"]
    }

    def "Testing the principal is correctly set with the ldap object"() {
        given: "You have created a LDAP user object with a principal"
	def user = new LdapUser("", "", "", "", principal, "", false)
        expect: "The principal is equal to the given data source"
        user.getPrincipal() == principal
	where: "The principal is set to "
        principal << ["", null, "cn=rmurphy,dc=cloudstack,dc=org"]
    }

    def "Testing the domain is correctly set with the ldap object"() {
	given: "You have created a LDAP user object with a principal"
	def user = new LdapUser("", "", "", "", "", domain, false)
	expect: "The principal is equal to the given data source"
	user.getDomain() == domain
	where: "The username is set to "
	domain << ["", null, "engineering"]
    }
}