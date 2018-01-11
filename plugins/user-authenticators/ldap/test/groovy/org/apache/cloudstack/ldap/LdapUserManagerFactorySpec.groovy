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
import org.apache.cloudstack.ldap.LdapUserManager
import org.apache.cloudstack.ldap.LdapUserManagerFactory
import org.apache.cloudstack.ldap.OpenLdapUserManagerImpl
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import spock.lang.Shared

class LdapUserManagerFactorySpec extends spock.lang.Specification {

    @Shared
    def LdapUserManagerFactory ldapUserManagerFactory;

    def setupSpec() {
        ldapUserManagerFactory = new LdapUserManagerFactory();
        ApplicationContext applicationContext = Mock(ApplicationContext);
        AutowireCapableBeanFactory autowireCapableBeanFactory = Mock(AutowireCapableBeanFactory);
        applicationContext.getAutowireCapableBeanFactory() >> autowireCapableBeanFactory;
        ldapUserManagerFactory.setApplicationContext(applicationContext);
    }

    def "Test getInstance() from factory"() {
        def result = ldapUserManagerFactory.getInstance(id);

        def expected;
        if(id == LdapUserManager.Provider.MICROSOFTAD) {
            expected = ADLdapUserManagerImpl.class;
        } else {
            expected = OpenLdapUserManagerImpl.class;
        }

        expect:
        assert result.class.is(expected)
        where:
        id << [LdapUserManager.Provider.MICROSOFTAD, LdapUserManager.Provider.OPENLDAP, null]
    }
}
