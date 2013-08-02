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

import org.apache.cloudstack.api.command.LdapCreateAccount

import com.cloud.user.AccountService;
import com.cloud.user.UserAccountVO;

import javax.naming.NamingException

class LdapCreateAccountCmdSpec extends spock.lang.Specification {
/*
    def "Test failure to retrive LDAP user"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	ldapManager.getUser(_) >> { throw new NamingException() }
	AccountService accountService = Mock(AccountService)

	def ldapCreateAccount = Spy(LdapCreateAccount, constructorArgs: [ldapManager, accountService])
	ldapCreateAccount.updateCallContext() >> System.out.println("Hello World");
	when:
	ldapCreateAccount.execute()
	then:
	thrown ServerApiException
    } */

    def "Test validation of a user"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)

	def ldapCreateAccount = Spy(LdapCreateAccount, constructorArgs: [ldapManager, accountService])

	when:
	def commandName = ldapCreateAccount.getCommandName()

	then:
	commandName == "createaccountresponse"
    }

    def "Test getEntityOwnerId is 1"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)

	def ldapCreateAccount = Spy(LdapCreateAccount, constructorArgs: [ldapManager, accountService])
	when:
	long ownerId = ldapCreateAccount.getEntityOwnerId()
	then:
	ownerId == 1
    }

    def "Test validate User"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService);
	when:
	def result = ldapCreateAccount.validateUser(new LdapUser("username","email","firstname","lastname","principal"))
	then:
	result == true
   }

    def "Test validate User empty email"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService)
	when:
	ldapCreateAccount.validateUser(new LdapUser("username",null,"firstname","lastname","principal"))
	then:
	thrown Exception
   }

    def "Test validate User empty firstname"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService)
	when:
	ldapCreateAccount.validateUser(new LdapUser("username","email",null,"lastname","principal"))
	then:
	thrown Exception
   }

    def "Test validate User empty lastname"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService)
	when:
	ldapCreateAccount.validateUser(new LdapUser("username","email","firstname",null,"principal"))
	then:
	thrown Exception
   }

    def "Test failed password generation"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService)
	when:
	def result = ldapCreateAccount.generatePassword()
	then:
	result != ""
	result != null
    }

    def "Test password generation"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService)
	when:
	def result = ldapCreateAccount.generatePassword()
	then:
	result != ""
	result != null
    }

    def "Test command name"() {
	given:
	LdapManager ldapManager = Mock(LdapManager)
	AccountService accountService = Mock(AccountService)
	def ldapCreateAccount = new LdapCreateAccount(ldapManager, accountService)
	when:
	def result = ldapCreateAccount.getCommandName()
	then:
	result == "createaccountresponse"
    }
}
