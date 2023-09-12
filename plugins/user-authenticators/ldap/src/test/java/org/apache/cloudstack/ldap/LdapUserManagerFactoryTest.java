/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The
 * ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class LdapUserManagerFactoryTest {

    @Mock
    ApplicationContext applicationCtx;

    @Mock
    AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Mock
    protected LdapConfiguration _ldapConfiguration;

    @Spy
    @InjectMocks
    static LdapUserManagerFactory ldapUserManagerFactory = new LdapUserManagerFactory();

    /**
     * circumvent springframework for these {code ManagerImpl}
     */
    @BeforeClass
    public static void init()
    {
        ldapUserManagerFactory.ldapUserManagerMap.put(LdapUserManager.Provider.MICROSOFTAD, new ADLdapUserManagerImpl());
        ldapUserManagerFactory.ldapUserManagerMap.put(LdapUserManager.Provider.OPENLDAP, new OpenLdapUserManagerImpl());
    }

    @Before
    public void setup() {

    }
    @Test
    public void getOpenLdapInstance() {
        LdapUserManager userManager = ldapUserManagerFactory.getInstance(LdapUserManager.Provider.OPENLDAP);
        assertTrue("x dude", userManager instanceof OpenLdapUserManagerImpl);
    }

    @Test
    public void getMSADInstance() {
        LdapUserManager userManager = ldapUserManagerFactory.getInstance(LdapUserManager.Provider.MICROSOFTAD);
        assertTrue("wrong dude", userManager instanceof ADLdapUserManagerImpl);
    }
}
