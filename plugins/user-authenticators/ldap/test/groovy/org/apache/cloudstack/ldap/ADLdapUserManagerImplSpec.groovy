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

import org.apache.cloudstack.ldap.ADLdapUserManagerImpl
import org.apache.cloudstack.ldap.LdapConfiguration
import spock.lang.Shared

import javax.naming.directory.SearchControls
import javax.naming.ldap.LdapContext

class ADLdapUserManagerImplSpec extends spock.lang.Specification {

    @Shared
    ADLdapUserManagerImpl adLdapUserManager;

    @Shared
    LdapConfiguration ldapConfiguration;

    def setup() {
        adLdapUserManager = new ADLdapUserManagerImpl();
        ldapConfiguration = Mock(LdapConfiguration);
        adLdapUserManager._ldapConfiguration = ldapConfiguration;
    }

    def "test generate AD search filter with nested groups enabled"() {
        ldapConfiguration.getUserObject() >> "user"
        ldapConfiguration.getCommonNameAttribute() >> "CN"
        ldapConfiguration.getBaseDn() >> "DC=cloud,DC=citrix,DC=com"
        ldapConfiguration.isNestedGroupsEnabled() >> true

        def result = adLdapUserManager.generateADGroupSearchFilter(group);
        expect:
            assert result.contains("memberOf:1.2.840.113556.1.4.1941:=")
            result == "(&(objectClass=user)(memberOf:1.2.840.113556.1.4.1941:=CN=" + group + ",DC=cloud,DC=citrix,DC=com))"
        where:
            group << ["dev", "dev-hyd"]
    }

    def "test generate AD search filter with nested groups disabled"() {
        ldapConfiguration.getUserObject() >> "user"
        ldapConfiguration.getCommonNameAttribute() >> "CN"
        ldapConfiguration.getBaseDn() >> "DC=cloud,DC=citrix,DC=com"
        ldapConfiguration.isNestedGroupsEnabled() >> false

        def result = adLdapUserManager.generateADGroupSearchFilter(group);
        expect:
        assert result.contains("memberOf=")
        result == "(&(objectClass=user)(memberOf=CN=" + group + ",DC=cloud,DC=citrix,DC=com))"
        where:
        group << ["dev", "dev-hyd"]
    }

    def "test getUsersInGroup null group"() {
        ldapConfiguration.getScope() >> SearchControls.SUBTREE_SCOPE
        ldapConfiguration.getReturnAttributes() >> ["username", "firstname", "lastname", "email"]
        ldapConfiguration.getBaseDn() >>> [null, null, "DC=cloud,DC=citrix,DC=com"]

        LdapContext context = Mock(LdapContext);

        when:
            def result = adLdapUserManager.getUsersInGroup(group, context)
        then:
            thrown(IllegalArgumentException)
        where:
            group << [null, "group", null]

    }
}
