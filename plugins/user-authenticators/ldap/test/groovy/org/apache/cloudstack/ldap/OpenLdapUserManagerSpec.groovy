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

import org.apache.cloudstack.ldap.LdapConfiguration
import org.apache.cloudstack.ldap.LdapUserManager
import org.apache.cloudstack.ldap.OpenLdapUserManagerImpl
import spock.lang.Shared

import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

class OpenLdapUserManagerSpec extends spock.lang.Specification {

    @Shared
    private def ldapConfiguration

    @Shared
    private def username

    @Shared
    private def email

    @Shared
    private def firstname

    @Shared
    private def lastname

    @Shared
    private def principal

    private def createGroupSearchContextOneUser() {

        def umSearchResult = Mock(SearchResult)
        umSearchResult.getName() >> principal;
        umSearchResult.getAttributes() >> principal

        def uniqueMembers = new BasicNamingEnumerationImpl()
        uniqueMembers.add(umSearchResult);
        def attributes = Mock(Attributes)
        def uniqueMemberAttribute = Mock(Attribute)
        uniqueMemberAttribute.getId() >> "uniquemember"
        uniqueMemberAttribute.getAll() >> uniqueMembers
        attributes.get("uniquemember") >> uniqueMemberAttribute

        def groupSearchResult = Mock(SearchResult)
        groupSearchResult.getName() >> principal;
        groupSearchResult.getAttributes() >> attributes

        def searchGroupResults = new BasicNamingEnumerationImpl()
        searchGroupResults.add(groupSearchResult);

        attributes = createUserAttributes(username, email, firstname, lastname)
        SearchResult userSearchResult = createSearchResult(attributes)
        def searchUsersResults = new BasicNamingEnumerationImpl()
        searchUsersResults.add(userSearchResult);

        def context = Mock(LdapContext)
        context.search(_, _, _) >>> [searchGroupResults, searchUsersResults, searchGroupResults, new BasicNamingEnumerationImpl()];

        return context
    }

    private def createGroupSearchContextNoUser() {

        def umSearchResult = Mock(SearchResult)
        umSearchResult.getName() >> principal;
        umSearchResult.getAttributes() >> principal

        def uniqueMembers = new BasicNamingEnumerationImpl()
        uniqueMembers.add(umSearchResult);
        def attributes = Mock(Attributes)
        def uniqueMemberAttribute = Mock(Attribute)
        uniqueMemberAttribute.getId() >> "uniquemember"
        uniqueMemberAttribute.getAll() >> uniqueMembers
        attributes.get("uniquemember") >> uniqueMemberAttribute

        def groupSearchResult = Mock(SearchResult)
        groupSearchResult.getName() >> principal;
        groupSearchResult.getAttributes() >> attributes

        def searchGroupResults = new BasicNamingEnumerationImpl()
        searchGroupResults.add(groupSearchResult);

        def context = Mock(LdapContext)
        context.search(_, _, _) >>> [searchGroupResults, new BasicNamingEnumerationImpl()];

        return context
    }

    private def createContext() {
        Attributes attributes = createUserAttributes(username, email, firstname, lastname)
        SearchResult searchResults = createSearchResult(attributes)
        def searchUsersResults = new BasicNamingEnumerationImpl()
        searchUsersResults.add(searchResults);

        def context = Mock(LdapContext)
        context.search(_, _, _) >> searchUsersResults;

        return context
    }

    private SearchResult createSearchResult(attributes) {
        def search = Mock(SearchResult)

        search.getName() >> "cn=" + attributes.getAt("uid").get();

        search.getAttributes() >> attributes
        search.getNameInNamespace() >> principal

        return search
    }

    private Attributes createUserAttributes(String username, String email, String firstname, String lastname) {
        def attributes = Mock(Attributes)

        def nameAttribute = Mock(Attribute)
        nameAttribute.getId() >> "uid"
        nameAttribute.get() >> username
        attributes.get("uid") >> nameAttribute

        def mailAttribute = Mock(Attribute)
        mailAttribute.getId() >> "mail"
        mailAttribute.get() >> email
        attributes.get("mail") >> mailAttribute

        def givennameAttribute = Mock(Attribute)
        givennameAttribute.getId() >> "givenname"
        givennameAttribute.get() >> firstname
        attributes.get("givenname") >> givennameAttribute

        def snAttribute = Mock(Attribute)
        snAttribute.getId() >> "sn"
        snAttribute.get() >> lastname
        attributes.get("sn") >> snAttribute

        return attributes
    }

    def setupSpec() {
        ldapConfiguration = Mock(LdapConfiguration)

        ldapConfiguration.getScope() >> SearchControls.SUBTREE_SCOPE
        ldapConfiguration.getReturnAttributes() >> ["uid", "mail", "cn"]
        ldapConfiguration.getUsernameAttribute() >> "uid"
        ldapConfiguration.getEmailAttribute() >> "mail"
        ldapConfiguration.getFirstnameAttribute() >> "givenname"
        ldapConfiguration.getLastnameAttribute() >> "sn"
        ldapConfiguration.getBaseDn() >> "dc=cloudstack,dc=org"
        ldapConfiguration.getCommonNameAttribute() >> "cn"
        ldapConfiguration.getGroupObject() >> "groupOfUniqueNames"
        ldapConfiguration.getGroupUniqueMemeberAttribute() >> "uniquemember"
        ldapConfiguration.getLdapPageSize() >> 1
        ldapConfiguration.getReadTimeout() >> 1000

        username = "rmurphy"
        email = "rmurphy@test.com"
        firstname = "Ryan"
        lastname = "Murphy"
        principal = "cn=" + username + "," + ldapConfiguration.getBaseDn()
    }

    def "Test successfully creating an Ldap User from Search result"() {
        given: "We have attributes, a search and a user manager"
        def attributes = createUserAttributes(username, email, firstname, lastname)
        def search = createSearchResult(attributes)
        def userManager = new OpenLdapUserManagerImpl(ldapConfiguration)
        def result = userManager.createUser(search)

        expect: "The crated user the data supplied from LDAP"

        result.username == username
        result.email == email
        result.firstname == firstname
        result.lastname == lastname
        result.principal == principal
    }

    def "Test successfully returning a list from get users"() {
        given: "We have a LdapUserManager"

        def userManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "A request for users is made"
        def result = userManager.getUsers(username, createContext())

        then: "A list of users is returned"
        result.size() == 1
    }

    def "Test successfully returning a list from get users when no username is given"() {
        given: "We have a LdapUserManager"

        def userManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "Get users is called without a username"
        def result = userManager.getUsers(createContext())

        then: "All users are returned"
        result.size() == 1
    }

    def "Test successfully returning a ldap user from searchUsers"() {
        given: "We have a LdapUserManager"
        def userManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "We search for users"
        def result = userManager.searchUsers(createContext())

        then: "A list of users are returned."
        result.first().getPrincipal() == principal
    }

    def "Test successfully returning an Ldap user from a get user request"() {
        given: "We have a LdapUserMaanger"

        def userManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "A request for a user is made"
        def result = userManager.getUser(username, createContext())

        then: "The user is returned"
        result.username == username
        result.email == email
        result.firstname == firstname
        result.lastname == lastname
        result.principal == principal
    }

    def "Test successfully throwing an exception when no users are found with getUser"() {
        given: "We have a seachResult of users and a User Manager"

        def searchUsersResults = new BasicNamingEnumerationImpl()

        def context = Mock(LdapContext)
        context.search(_, _, _) >> searchUsersResults;

        def userManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "a get user request is made and no user is found"
        def result = userManager.getUser(username, context)

        then: "An exception is thrown."
        thrown NamingException
    }

    def "Test that a newly created Ldap User Manager is not null"() {
        given: "You have created a new Ldap user manager object"
        def result = new OpenLdapUserManagerImpl();
        expect: "The result is not null"
        result != null
    }

    def "test successful generateGroupSearchFilter"() {
        given: "ldap user manager and ldap config"
        def ldapUserManager = new OpenLdapUserManagerImpl(ldapConfiguration)
        def groupName = varGroupName == null ? "*" : varGroupName
        def expectedResult = "(&(objectClass=groupOfUniqueNames)(cn=" + groupName + "))";

        def result = ldapUserManager.generateGroupSearchFilter(varGroupName)
        expect:
        result == expectedResult
        where: "The group name passed is set to "
        varGroupName << ["", null, "Murphy"]
    }

    def "test successful getUsersInGroup one user"() {
        given: "ldap user manager and ldap config"
        def ldapUserManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "A request for users is made"
        def result = ldapUserManager.getUsersInGroup("engineering", createGroupSearchContextOneUser())
        then: "one user is returned"
        result.size() == 1
    }

    def "test successful getUsersInGroup no user"() {
        given: "ldap user manager and ldap config"
        def ldapUserManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "A request for users is made"
        def result = ldapUserManager.getUsersInGroup("engineering", createGroupSearchContextNoUser())
        then: "no user is returned"
        result.size() == 0
    }

    def "test successful getUserForDn"() {
        given: "ldap user manager and ldap config"
        def ldapUserManager = new OpenLdapUserManagerImpl(ldapConfiguration)

        when: "A request for users is made"
        def result = ldapUserManager.getUserForDn("cn=Ryan Murphy,ou=engineering,dc=cloudstack,dc=org", createContext())
        then: "A list of users is returned"
        result != 1
        result.username == username
        result.email == email
        result.firstname == firstname
        result.lastname == lastname
        result.principal == principal

    }

    def "test searchUsers when ldap basedn in not set"() {
        given: "ldap configuration where basedn is not set"
        def ldapconfig = Mock(LdapConfiguration)
        ldapconfig.getBaseDn() >> null
        def ldapUserManager = new OpenLdapUserManagerImpl(ldapconfig)

        when: "A request for search users is made"
        def result = ldapUserManager.searchUsers(new InitialLdapContext())

        then: "An exception with no basedn defined is returned"
        def e = thrown(IllegalArgumentException)
        e.message == "ldap basedn is not configured"
    }
}
