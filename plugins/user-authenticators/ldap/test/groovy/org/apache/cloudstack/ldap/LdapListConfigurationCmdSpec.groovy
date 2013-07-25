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

import org.apache.cloudstack.api.ServerApiException
import org.apache.cloudstack.api.command.LdapListConfigurationCmd
import org.apache.cloudstack.api.response.LdapConfigurationResponse
import org.apache.cloudstack.ldap.LdapConfigurationVO
import org.apache.cloudstack.ldap.LdapManager

import com.cloud.utils.Pair

class LdapListConfigurationCmdSpec extends spock.lang.Specification {

    def "Test successful response from execute"() {
        given:
        def ldapManager = Mock(LdapManager)
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        ldapConfigurationList.add(new LdapConfigurationVO("localhost", 389))
        Pair<List<LdapConfigurationVO>, Integer> ldapConfigurations = new Pair<List<LdapConfigurationVO>, Integer>();
        ldapConfigurations.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapManager.listConfigurations(_) >> ldapConfigurations
        ldapManager.createLdapConfigurationResponse(_) >> new LdapConfigurationResponse("localhost", 389)
        def ldapListConfigurationCmd = new LdapListConfigurationCmd(ldapManager)
        when:
        ldapListConfigurationCmd.execute()
        then:
        ldapListConfigurationCmd.getResponseObject().getResponses().size() != 0
    }

    def "Test failed response from execute"() {
        given:

        def ldapManager = Mock(LdapManager)
        List<LdapConfigurationVO> ldapConfigurationList = new ArrayList()
        Pair<List<LdapConfigurationVO>, Integer> ldapConfigurations = new Pair<List<LdapConfigurationVO>, Integer>();
        ldapConfigurations.set(ldapConfigurationList, ldapConfigurationList.size())
        ldapManager.listConfigurations(_) >> ldapConfigurations

        def ldapListConfigurationCmd = new LdapListConfigurationCmd(ldapManager)
        when:
        ldapListConfigurationCmd.execute()
        then:
        ldapListConfigurationCmd.getResponseObject().getResponses().size() == 0
    }

    def "Test successful setting of hostname"() {
        given:
        def ldapManager = Mock(LdapManager)
        def ldapListConfigurationCmd = new LdapListConfigurationCmd(ldapManager)
        when:
        ldapListConfigurationCmd.setHostname("localhost")
        then:
        ldapListConfigurationCmd.getHostname() == "localhost"
    }

    def "Test successful setting of Port"() {
        given:
        def ldapManager = Mock(LdapManager)
        def ldapListConfigurationCmd = new LdapListConfigurationCmd(ldapManager)
        when:
        ldapListConfigurationCmd.setPort(389)
        then:
        ldapListConfigurationCmd.getPort() == 389
    }

    def "Test getEntityOwnerId is 0"() {
        given:
        def ldapManager = Mock(LdapManager)
        def ldapListConfigurationCmd = new LdapListConfigurationCmd(ldapManager)
        when:
        long ownerId = ldapListConfigurationCmd.getEntityOwnerId()
        then:
        ownerId == 1
    }

    def "Test successful return of getCommandName"() {
        given:
        def ldapManager = Mock(LdapManager)
        def ldapListConfigurationCmd = new LdapListConfigurationCmd(ldapManager)
        when:
        String commandName = ldapListConfigurationCmd.getCommandName()
        then:
        commandName == "ldapconfigurationresponse"
    }
}
