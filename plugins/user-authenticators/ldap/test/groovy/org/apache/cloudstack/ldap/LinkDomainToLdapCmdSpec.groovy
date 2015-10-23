/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package groovy.org.apache.cloudstack.ldap

import com.cloud.exception.InvalidParameterValueException
import com.cloud.user.Account
import com.cloud.user.AccountService
import com.cloud.user.User
import com.cloud.user.UserAccount
import org.apache.cloudstack.api.ServerApiException
import org.apache.cloudstack.api.command.LinkDomainToLdapCmd
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse
import org.apache.cloudstack.ldap.LdapManager
import org.apache.cloudstack.ldap.LdapUser
import org.apache.cloudstack.ldap.NoLdapUserMatchingQueryException
import spock.lang.Shared
import spock.lang.Specification

class LinkDomainToLdapCmdSpec extends Specification {

    @Shared
    private LdapManager _ldapManager;

    @Shared
    public AccountService _accountService;

    @Shared
    public LinkDomainToLdapCmd linkDomainToLdapCmd;

    def setup() {
        _ldapManager = Mock(LdapManager)
        _accountService = Mock(AccountService)

        linkDomainToLdapCmd = new LinkDomainToLdapCmd()
        linkDomainToLdapCmd._accountService = _accountService
        linkDomainToLdapCmd._ldapManager = _ldapManager
    }

    def "test invalid params"() {
        _ldapManager.linkDomainToLdap(_,_,_,_) >> {throw new InvalidParameterValueException("invalid param")}
        when:
            linkDomainToLdapCmd.execute();
        then:
            thrown(ServerApiException)
    }
    def "test valid params without admin"(){
        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(1, "GROUP", "CN=test,DC=ccp,DC=citrix,DC=com", (short)2)
        _ldapManager.linkDomainToLdap(_,_,_,_) >> response
        when:
            linkDomainToLdapCmd.execute()
        then:
            LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject()
            result.getObjectName() == "LinkDomainToLdap"
            result.getResponseName() == linkDomainToLdapCmd.getCommandName()
    }

    def "test with valid params and with disabled admin"() {
        def domainId = 1;
        def type = "GROUP";
        def name = "CN=test,DC=ccp,DC=Citrix,DC=com"
        def accountType = 2;
        def username = "admin"

        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainId, type, name, (short)accountType)
        _ldapManager.linkDomainToLdap(_,_,_,_) >> response
        _ldapManager.getUser(username, type, name) >> new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", name, "ccp", true)

        linkDomainToLdapCmd.admin = username
        linkDomainToLdapCmd.type = type
        linkDomainToLdapCmd.name = name
        linkDomainToLdapCmd.domainId = domainId

        when:
        linkDomainToLdapCmd.execute()
        then:
        LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject()
        result.getObjectName() == "LinkDomainToLdap"
        result.getResponseName() == linkDomainToLdapCmd.getCommandName()
        result.getDomainId() == domainId
        result.getType() == type
        result.getName() == name
        result.getAdminId() == null
    }

    def "test with valid params and with admin who exist in cloudstack already"() {
        def domainId = 1;
        def type = "GROUP";
        def name = "CN=test,DC=ccp,DC=Citrix,DC=com"
        def accountType = 2;
        def username = "admin"

        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainId, type, name, (short)accountType)
        _ldapManager.linkDomainToLdap(_,_,_,_) >> response
        _ldapManager.getUser(username, type, name) >> new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", name, "ccp", false)

        _accountService.getActiveAccountByName(username, domainId) >> Mock(Account)

        linkDomainToLdapCmd.admin = username
        linkDomainToLdapCmd.type = type
        linkDomainToLdapCmd.name = name
        linkDomainToLdapCmd.domainId = domainId

        when:
        linkDomainToLdapCmd.execute()
        then:
        LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject()
        result.getObjectName() == "LinkDomainToLdap"
        result.getResponseName() == linkDomainToLdapCmd.getCommandName()
        result.getDomainId() == domainId
        result.getType() == type
        result.getName() == name
        result.getAdminId() == null
    }

    def "test with valid params and with admin who doesnt exist in cloudstack"() {
        def domainId = 1;
        def type = "GROUP";
        def name = "CN=test,DC=ccp,DC=Citrix,DC=com"
        def accountType = 2;
        def username = "admin"
        def accountId = 24

        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainId, type, name, (short)accountType)
        _ldapManager.linkDomainToLdap(_,_,_,_) >> response
        _ldapManager.getUser(username, type, name) >> new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", name, "ccp", false)

        _accountService.getActiveAccountByName(username, domainId) >> null
        UserAccount userAccount = Mock(UserAccount)
        userAccount.getAccountId() >> 24
        _accountService.createUserAccount(username, "", "Admin", "Admin", "admin@ccp.citrix.com", null, username, Account.ACCOUNT_TYPE_DOMAIN_ADMIN, domainId,
                username, null, _, _, User.Source.LDAP) >> userAccount

        linkDomainToLdapCmd.admin = username
        linkDomainToLdapCmd.type = type
        linkDomainToLdapCmd.name = name
        linkDomainToLdapCmd.domainId = domainId

        when:
        linkDomainToLdapCmd.execute()
        then:
        LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject()
        result.getObjectName() == "LinkDomainToLdap"
        result.getResponseName() == linkDomainToLdapCmd.getCommandName()
        result.getDomainId() == domainId
        result.getType() == type
        result.getName() == name
        result.getAdminId() == String.valueOf(accountId)
    }

    def "test when admin doesnt exist in ldap"() {
        def domainId = 1;
        def type = "GROUP";
        def name = "CN=test,DC=ccp,DC=Citrix,DC=com"
        def accountType = 2;
        def username = "admin"

        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainId, type, name, (short)accountType)
        _ldapManager.linkDomainToLdap(_,_,_,_) >> response
        _ldapManager.getUser(username, type, name) >> {throw new NoLdapUserMatchingQueryException("get ldap user failed from mock")}

        linkDomainToLdapCmd.admin = username
        linkDomainToLdapCmd.type = type
        linkDomainToLdapCmd.name = name
        linkDomainToLdapCmd.domainId = domainId

        when:
        linkDomainToLdapCmd.execute()
        then:
        LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject()
        result.getObjectName() == "LinkDomainToLdap"
        result.getResponseName() == linkDomainToLdapCmd.getCommandName()
        result.getDomainId() == domainId
        result.getType() == type
        result.getName() == name
        result.getAdminId() == null
    }

    /**
     * api should not fail in this case as link domain to ldap is successful
     */
    def "test when create user account throws a run time exception"() {
        def domainId = 1;
        def type = "GROUP";
        def name = "CN=test,DC=ccp,DC=Citrix,DC=com"
        def accountType = 2;
        def username = "admin"
        def accountId = 24

        LinkDomainToLdapResponse response = new LinkDomainToLdapResponse(domainId, type, name, (short)accountType)
        _ldapManager.linkDomainToLdap(_,_,_,_) >> response
        _ldapManager.getUser(username, type, name) >> new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", name, "ccp", false)

        _accountService.getActiveAccountByName(username, domainId) >> null
        UserAccount userAccount = Mock(UserAccount)
        userAccount.getAccountId() >> 24
        _accountService.createUserAccount(username, "", "Admin", "Admin", "admin@ccp.citrix.com", null, username, Account.ACCOUNT_TYPE_DOMAIN_ADMIN, domainId,
                username, null, _, _, User.Source.LDAP) >> { throw new RuntimeException("created failed from mock") }

        linkDomainToLdapCmd.admin = username
        linkDomainToLdapCmd.type = type
        linkDomainToLdapCmd.name = name
        linkDomainToLdapCmd.domainId = domainId

        when:
        linkDomainToLdapCmd.execute()
        then:
        LinkDomainToLdapResponse result = (LinkDomainToLdapResponse)linkDomainToLdapCmd.getResponseObject()
        result.getObjectName() == "LinkDomainToLdap"
        result.getResponseName() == linkDomainToLdapCmd.getCommandName()
        result.getDomainId() == domainId
        result.getType() == type
        result.getName() == name
        result.getAdminId() == null
    }

}
