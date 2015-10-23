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

import org.apache.cloudstack.api.response.LdapUserResponse


class LdapUserResponseSpec extends spock.lang.Specification {
    def "Testing successful setting of LdapUserResponse email"() {
		given: "We have an LdapResponse"
        LdapUserResponse response = new LdapUserResponse();
		when: "An email address is set"
        response.setEmail("rmurphy@test.com");
		then: "Get email should return that set email"
		response.getEmail() == "rmurphy@test.com";
    }

    def "Testing successful setting of LdapUserResponse firstname"() {
		given: "We have an LdapUserResponse"
		LdapUserResponse response = new LdapUserResponse()
		when: "A firstname is set"
        response.setFirstname("Ryan")
		then: "gGet Firstname returns the set value"
        response.getFirstname() == "Ryan"
    }

    def "Testing successful setting of LdapUserResponse lastname"() {
		given: "We have an LdapUserResponse"
        LdapUserResponse response = new LdapUserResponse()
		when: "A lastname is set"
        response.setLastname("Murphy")
		then: "Get lastname is returned"
		response.getLastname() == "Murphy"
    }

    def "Testing successful setting of LdapUserResponse principal"() {
		given: "We have an LdapResponse"
		LdapUserResponse response = new LdapUserResponse()
		when: "A principal is set"
		response.setPrincipal("dc=cloudstack,dc=org")
		then: "Get principled returns the set value"
		response.getPrincipal() == "dc=cloudstack,dc=org"
    }

    def "Testing successful setting of LdapUserResponse username"() {
		given: "We have an LdapUserResponse"
		LdapUserResponse response = new LdapUserResponse()
		when: "A username is set"
		response.setUsername("rmurphy")
		then: "Get username returns the set value."
		response.getUsername() == "rmurphy"
    }

    def "Testing successful setting of LdapUserResponse domain"() {
	given: "We have an LdapUserResponse"
	LdapUserResponse response = new LdapUserResponse()
	when: "A domain is set"
	response.setDomain("engineering")
	then: "Get domain returns the set value."
	response.getDomain() == "engineering"
    }

    def "Testing setting of LdapUserResponse domain to null"() {
	given: "We have an LdapUserResponse"
	LdapUserResponse response = new LdapUserResponse()
	when: "A domain is set"
	response.setDomain(null)
	then: "Get domain returns the set value."
	response.getDomain() == null
    }
}
