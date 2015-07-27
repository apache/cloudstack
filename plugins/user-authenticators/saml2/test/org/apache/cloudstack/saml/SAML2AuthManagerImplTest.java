
package org.apache.cloudstack.saml;

import com.cloud.user.DomainManager;
import com.cloud.user.dao.UserDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SAML2AuthManagerImplTest {

    @Mock
    private KeystoreDao _ksDao;

    @Mock
    private SAMLTokenDao _samlTokenDao;

    @Mock
    private UserDao _userDao;

    @Mock
    DomainManager _domainMgr;

    @InjectMocks
    @Spy
    SAML2AuthManagerImpl saml2AuthManager = new SAML2AuthManagerImpl();

    @Before
    public void setUp() {
        doReturn(true).when(saml2AuthManager).isSAMLPluginEnabled();
        doReturn(true).when(saml2AuthManager).initSP();
    }

    @Test
    public void testStart() {
        when(saml2AuthManager.getSAMLIdentityProviderMetadataURL()).thenReturn("file://does/not/exist");
        boolean started = saml2AuthManager.start();
        assertFalse("saml2authmanager should not start as the file doesnt exist", started);

        when(saml2AuthManager.getSAMLIdentityProviderMetadataURL()).thenReturn(" ");
        started = saml2AuthManager.start();
        assertFalse("saml2authmanager should not start as the file doesnt exist", started);

        when(saml2AuthManager.getSAMLIdentityProviderMetadataURL()).thenReturn("");
        started = saml2AuthManager.start();
        assertFalse("saml2authmanager should not start as the file doesnt exist", started);

    }
}