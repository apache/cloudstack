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

import com.cloud.user.UserAccountVO
import com.cloud.user.dao.UserAccountDao
import com.cloud.utils.Pair
import org.apache.cloudstack.ldap.LdapAuthenticator
import org.apache.cloudstack.ldap.LdapConfigurationVO
import org.apache.cloudstack.ldap.LdapManager

class LdapAuthenticatorSpec extends spock.lang.Specification {

    def "Test a failed authentication due to user not being found within cloudstack"() {
		given: "We have an LdapManager, userAccountDao and ldapAuthenticator and the user doesn't exist within cloudstack."
        LdapManager ldapManager = Mock(LdapManager)
        UserAccountDao userAccountDao = Mock(UserAccountDao)
        userAccountDao.getUserAccount(_, _) >> null
        def ldapAuthenticator = new LdapAuthenticator(ldapManager, userAccountDao)
		when: "A user authentications"
        def result = ldapAuthenticator.authenticate("rmurphy", "password", 0, null)
		then: "their authentication fails"
		result == false
    }

    def "Test failed authentication due to ldap bind being unsuccessful"() {
		given: "We have an LdapManager, LdapConfiguration, userAccountDao and LdapAuthenticator"
		def ldapManager = Mock(LdapManager)
		ldapManager.isLdapEnabled() >> true
		ldapManager.canAuthenticate(_, _) >> false

		UserAccountDao userAccountDao = Mock(UserAccountDao)
		userAccountDao.getUserAccount(_, _) >> new UserAccountVO()
		def ldapAuthenticator = new LdapAuthenticator(ldapManager, userAccountDao)

		when: "The user authenticates with an incorrect password"
		def result = ldapAuthenticator.authenticate("rmurphy", "password", 0, null)

		then: "their authentication fails"
		result == false
    }

    def "Test failed authentication due to ldap not being configured"() {
		given: "We have an LdapManager, A configured LDAP server, a userAccountDao and LdapAuthenticator"
		def ldapManager = Mock(LdapManager)
		ldapManager.isLdapEnabled() >> false

		UserAccountDao userAccountDao = Mock(UserAccountDao)
		userAccountDao.getUserAccount(_, _) >> new UserAccountVO()

		def ldapAuthenticator = new LdapAuthenticator(ldapManager, userAccountDao)
		when: "The user authenticates"
		def result = ldapAuthenticator.authenticate("rmurphy", "password", 0, null)
		then: "their authentication fails"
		result == false
    }

	def "Test successful authentication"() {
		given: "We have an LdapManager, LdapConfiguration, userAccountDao and LdapAuthenticator"
		def ldapManager = Mock(LdapManager)
		ldapManager.isLdapEnabled() >> true
		ldapManager.canAuthenticate(_, _) >> true

		UserAccountDao userAccountDao = Mock(UserAccountDao)
		userAccountDao.getUserAccount(_, _) >> new UserAccountVO()
		def ldapAuthenticator = new LdapAuthenticator(ldapManager, userAccountDao)

		when: "The user authenticates with an incorrect password"
		def result = ldapAuthenticator.authenticate("rmurphy", "password", 0, null)

		then: "their authentication passes"
		result == true
	}

    def "Test that encode doesn't change the input"() {
		given: "We have an LdapManager, userAccountDao and LdapAuthenticator"
		LdapManager ldapManager = Mock(LdapManager)
	    UserAccountDao userAccountDao = Mock(UserAccountDao)
	    def ldapAuthenticator = new LdapAuthenticator(ldapManager, userAccountDao)
		when: "a users password is encoded"
	    def result = ldapAuthenticator.encode("password")
		then: "it doesn't change"
	    result == "password"
    }
}
