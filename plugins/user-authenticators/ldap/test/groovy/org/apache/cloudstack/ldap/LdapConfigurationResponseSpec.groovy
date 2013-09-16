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

import org.apache.cloudstack.api.response.LdapConfigurationResponse

class LdapConfigurationResponseSpec extends spock.lang.Specification {
    def "Testing succcessful setting of LdapConfigurationResponse hostname"() {
		given: "We have a LdapConfigurationResponse"
        LdapConfigurationResponse response = new LdapConfigurationResponse();
		when: "The hostname is set"
        response.setHostname("localhost");
		then: "Get hostname should return the set value"
		response.getHostname() == "localhost";
    }

    def "Testing successful setting of LdapConfigurationResponse hostname and port via constructor"() {
		given: "We have a LdapConfiguration response"
		LdapConfigurationResponse response
		when: "both hostname and port are set by constructor"
        response = new LdapConfigurationResponse("localhost", 389)
		then: "Get hostname and port should return the set values."
        response.getHostname() == "localhost"
        response.getPort() == 389
    }

    def "Testing successful setting of LdapConfigurationResponse port"() {
		given: "We have a LdapConfigurationResponse"
		LdapConfigurationResponse response = new LdapConfigurationResponse()
		when: "The port is set"
		response.setPort(389)
		then: "Get port should return the set value"
		response.getPort() == 389
    }
}
