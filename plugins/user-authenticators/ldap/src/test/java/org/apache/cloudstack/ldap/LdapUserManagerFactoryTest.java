package org.apache.cloudstack.ldap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

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
    LdapUserManagerFactory ldapUserManagerFactory = new LdapUserManagerFactory();

    // circumvent springframework for these {code ManagerImpl}
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