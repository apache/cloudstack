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
    def "Testing succcessful setting of LdapUserResponse email"() {
        given:
        LdapUserResponse response = new LdapUserResponse();
        when:
        response.setEmail("rmurphy@test.com");
        then:
        response.getEmail() == "rmurphy@test.com";
    }

    def "Testing successful setting of LdapUserResponse principal"() {
        given:
        LdapUserResponse response = new LdapUserResponse()
        when:
        response.setPrincipal("dc=cloudstack,dc=org")
        then:
        response.getPrincipal() == "dc=cloudstack,dc=org"
    }

    def "Testing successful setting of LdapUserResponse username"() {
        given:
        LdapUserResponse response = new LdapUserResponse()
        when:
        response.setUsername("rmurphy")
        then:
        response.getUsername() == "rmurphy"
    }

    def "Testing successful setting of LdapUserResponse firstname"() {
        given:
        LdapUserResponse response = new LdapUserResponse()
        when:
        response.setFirstname("Ryan")
        then:
        response.getFirstname() == "Ryan"
    }

    def "Testing successful setting of LdapUserResponse lastname"() {
        given:
        LdapUserResponse response = new LdapUserResponse()
        when:
        response.setLastname("Murphy")
        then:
        response.getLastname() == "Murphy"
    }
}
