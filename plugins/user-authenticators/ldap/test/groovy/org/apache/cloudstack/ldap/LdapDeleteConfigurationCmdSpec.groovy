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

import com.cloud.exception.InvalidParameterValueException
import org.apache.cloudstack.api.ServerApiException
import org.apache.cloudstack.api.command.LdapDeleteConfigurationCmd
import org.apache.cloudstack.api.response.LdapConfigurationResponse
import org.apache.cloudstack.ldap.LdapManager

class LdapDeleteConfigurationCmdSpec extends spock.lang.Specification {

    def "Test successful response from execute"() {
        given:
        def ldapManager = Mock(LdapManager)
        ldapManager.deleteConfiguration(_) >> new LdapConfigurationResponse("localhost")
        def ldapDeleteConfigurationCmd = new LdapDeleteConfigurationCmd(ldapManager)
        when:
        ldapDeleteConfigurationCmd.execute()
        then:
        ldapDeleteConfigurationCmd.responseObject.hostname == "localhost"
    }

    def "Test failed response from execute"() {
        given:
        def ldapManager = Mock(LdapManager)
        ldapManager.deleteConfiguration(_) >> { throw new InvalidParameterValueException() }
        def ldapDeleteConfigurationCmd = new LdapDeleteConfigurationCmd(ldapManager)
        when:
        ldapDeleteConfigurationCmd.execute()
        then:
        thrown ServerApiException
    }

    def "Test getEntityOwnerId is 1"() {
        given:
        def ldapManager = Mock(LdapManager)
        def ldapDeleteConfigurationCmd = new LdapDeleteConfigurationCmd(ldapManager)
        when:
        long ownerId = ldapDeleteConfigurationCmd.getEntityOwnerId()
        then:
        ownerId == 1
    }

    def "Test successful return of getCommandName"() {
        given:
        def ldapManager = Mock(LdapManager)
        def ldapDeleteConfigurationCmd = new LdapDeleteConfigurationCmd(ldapManager)
        when:
        String commandName = ldapDeleteConfigurationCmd.getCommandName()
        then:
        commandName == "ldapconfigurationresponse"
    }
}
