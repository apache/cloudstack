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
import spock.lang.Shared

import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.LdapContext

class LdapUserManagerSpec extends spock.lang.Specification {

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

    def setupSpec() {
        ldapConfiguration = Mock(LdapConfiguration)

        ldapConfiguration.getScope() >> SearchControls.SUBTREE_SCOPE
        ldapConfiguration.getReturnAttributes() >> ["uid", "mail", "cn"]
        ldapConfiguration.getUsernameAttribute() >> "uid"
        ldapConfiguration.getEmailAttribute() >> "mail"
        ldapConfiguration.getFirstnameAttribute() >> "givenname"
        ldapConfiguration.getLastnameAttribute() >> "sn"
        ldapConfiguration.getBaseDn() >> "dc=cloudstack,dc=org"

        username = "rmurphy"
        email = "rmurphy@test.com"
        firstname = "Ryan"
        lastname = "Murphy"
        principal = "cn=" + username + "," + ldapConfiguration.getBaseDn()
    }

    def "Test that a newly created Ldap User Manager is not null"() {
        given: "You have created a new Ldap user manager object"
        def result = new LdapUserManager();
        expect: "The result is not null"
        result != null
    }

    def "Test successfully creating an Ldap User from Search result"() {
        given:
        def attributes = createUserAttributes(username, email, firstname, lastname)
        def search = createSearchResult(attributes)
        def userManager = new LdapUserManager(ldapConfiguration)
        def result = userManager.createUser(search)

        expect:

        result.username == username
        result.email == email
        result.firstname == firstname
        result.lastname == lastname
        result.principal == principal
    }

    def "Test successfully returning an Ldap user from a get user request"() {
        given:

        def userManager = new LdapUserManager(ldapConfiguration)

        when:
        def result = userManager.getUser(username, createContext())

        then:
        result.username == username
        result.email == email
        result.firstname == firstname
        result.lastname == lastname
        result.principal == principal
    }

    def "Test successfully returning a list from get users"() {
        given:

        def userManager = new LdapUserManager(ldapConfiguration)

        when:
        def result = userManager.getUsers(username, createContext())

        then:
        result.size() == 1
    }

    def "Test successfully returning a list from get users when no username is given"() {
        given:

        def userManager = new LdapUserManager(ldapConfiguration)

        when:
        def result = userManager.getUsers(createContext())

        then:
        result.size() == 1
    }

    def "Test successfully throwing an exception when no users are found with getUser"() {
        given:

        def searchUsersResults = new BasicNamingEnumerationImpl()

        def context = Mock(LdapContext)
        context.search(_, _, _) >> searchUsersResults;

        def userManager = new LdapUserManager(ldapConfiguration)

        when:
        def result = userManager.getUser(username, context)

        then:
        thrown NamingException
    }

    def "Test successfully returning a NamingEnumeration from searchUsers"() {
        given:
        def userManager = new LdapUserManager(ldapConfiguration)

        when:
        def result = userManager.searchUsers(createContext())

        then:
        result.next().getName() + "," + ldapConfiguration.getBaseDn() == principal
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
}
