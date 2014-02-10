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

import org.apache.cloudstack.ldap.LdapConfigurationVO


class LdapConfigurationVOSpec extends spock.lang.Specification {
    def "Testing that the ID hostname and port is correctly set within the LDAP configuration VO"() {
		given: "You have created an LDAP Configuration VO"
		def configuration = new LdapConfigurationVO(hostname, port)
		configuration.setId(id)
		expect: "The id hostname and port is equal to the given data source"
		configuration.getId() == id
		configuration.getHostname() == hostname
		configuration.getPort() == port
		where: "The id, hostname and port is set to "
		hostname << ["", null, "localhost"]
		id << [0, 1000, -1000]
		port << [0, 1000, -1000]
    }
}
