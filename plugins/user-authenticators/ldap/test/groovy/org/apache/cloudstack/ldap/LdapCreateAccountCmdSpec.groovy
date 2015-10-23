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
import org.apache.cloudstack.api.command.LdapAddConfigurationCmd
import org.apache.cloudstack.api.response.LdapConfigurationResponse

import org.apache.cloudstack.ldap.LdapUser;
import org.apache.cloudstack.ldap.LdapManager;

import org.apache.cloudstack.api.command.LdapCreateAccountCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.user.AccountService;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException;

import javax.naming.NamingException

class LdapCreateAccountCmdSpec extends spock.lang.Specification {

    def "Test failure to retrive LDAP user"() {
        given: "We have an LdapManager, AccountService and LdapCreateAccountCmd and LDAP user that doesn't exist"
        LdapManager ldapManager = Mock(LdapManager)
        ldapManager.getUser(_) >> { throw new NoLdapUserMatchingQueryException() }
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = Spy(LdapCreateAccountCmd, constructorArgs: [ldapManager, accountService])
        ldapCreateAccountCmd.getCurrentContext() >> Mock(CallContext)
        CallContext context = ldapCreateAccountCmd.getCurrentContext()
        when: "An an account is created"
        ldapCreateAccountCmd.execute()
        then: "It fails and an exception is thrown"
        thrown ServerApiException
    }

    def "Test failed creation due to a null response from cloudstack account creater"() {
        given: "We have an LdapManager, AccountService and LdapCreateAccountCmd"
        LdapManager ldapManager = Mock(LdapManager)
        ldapManager.getUser(_) >> new LdapUser("rmurphy", "rmurphy@cloudstack.org", "Ryan", "Murphy", "cn=rmurphy,ou=engineering,dc=cloudstack,dc=org", "engineering", false)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = Spy(LdapCreateAccountCmd, constructorArgs: [ldapManager, accountService])
        ldapCreateAccountCmd.getCurrentContext() >> Mock(CallContext)
        ldapCreateAccountCmd.createCloudstackUserAccount(_, _, _) >> null
        when: "Cloudstack fail to create the user"
        ldapCreateAccountCmd.execute()
        then: "An exception is thrown"
        thrown ServerApiException
    }

    def "Test command name"() {
        given: "We have an LdapManager, AccountService and LdapCreateAccountCmd"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService)
        when: "Get command name is called"
        def result = ldapCreateAccountCmd.getCommandName()
        then: "createaccountresponse is returned"
        result == "createaccountresponse"
    }

    def "Test getEntityOwnerId is 1"() {
        given: "We have an LdapManager, AccountService andL dapCreateAccount"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)

        def ldapCreateAccountCmd = Spy(LdapCreateAccountCmd, constructorArgs: [ldapManager, accountService])
        when: "Get entity owner id is called"
        long ownerId = ldapCreateAccountCmd.getEntityOwnerId()
        then: "1 is returned"
        ownerId == 1
    }

    def "Test password generation"() {
        given: "We have an LdapManager, AccountService and LdapCreateAccountCmd"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService)
        when: "A password is generated"
        def result = ldapCreateAccountCmd.generatePassword()
        then: "The result shouldn't be null or empty"
        result != ""
        result != null
    }

    def "Test validate User"() {
        given: "We have an LdapManager, AccountService andL dapCreateAccount"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService);
        when: "a user with an username, email, firstname and lastname is validated"
        def result = ldapCreateAccountCmd.validateUser(new LdapUser("username", "email", "firstname", "lastname", "principal", "domain", false))
        then: "the result is true"
        result == true
    }

    def "Test validate User empty email"() {
        given: "We have an LdapManager, AccountService andL dapCreateAccount"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService)
        when: "A user with no email address attempts to validate"
        ldapCreateAccountCmd.validateUser(new LdapUser("username", null, "firstname", "lastname", "principal", "domain", false))
        then: "An exception is thrown"
        thrown Exception
    }

    def "Test validate User empty firstname"() {
        given: "We have an LdapManager, AccountService andL dapCreateAccount"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService)
        when: "A user with no firstname attempts to validate"
        ldapCreateAccountCmd.validateUser(new LdapUser("username", "email", null, "lastname", "principal", false))
        then: "An exception is thrown"
        thrown Exception
    }

    def "Test validate User empty lastname"() {
        given: "We have an LdapManager, AccountService and LdapCreateAccountCmd"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService)
        when: "A user with no lastname attempts to validate"
        ldapCreateAccountCmd.validateUser(new LdapUser("username", "email", "firstname", null, "principal", "domain", false))
        then: "An exception is thown"
        thrown Exception
    }

    def "Test validation of a user"() {
        given: "We have an LdapManager, AccountService andL dapCreateAccount"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = Spy(LdapCreateAccountCmd, constructorArgs: [ldapManager, accountService])
        when: "Get command name is called"
        def commandName = ldapCreateAccountCmd.getCommandName()
        then: "createaccountresponse is returned"
        commandName == "createaccountresponse"
    }

    def "Test generate password"() {
        given: "We have an LdapManager, AccountService and LdapCreateAccount"
        LdapManager ldapManager = Mock(LdapManager)
        AccountService accountService = Mock(AccountService)
        def ldapCreateAccountCmd = new LdapCreateAccountCmd(ldapManager, accountService)
        when: "A random password is generated for a new account"
        String password = ldapCreateAccountCmd.generatePassword()
        then: "password should not be the array address but an actual encoded string. verifying length > 20 as the byte array size is 20"
        password.length() > 20
    }
}
