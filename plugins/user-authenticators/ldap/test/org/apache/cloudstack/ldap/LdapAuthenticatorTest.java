package org.apache.cloudstack.ldap;

import com.cloud.server.auth.*;
import com.cloud.user.*;
import com.cloud.user.dao.*;
import com.cloud.utils.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.runners.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticatorTest {

    @Mock
    LdapManager ldapManager;
    @Mock
    UserAccountDao userAccountDao;
    @Mock
    UserAccount user = new UserAccountVO();

    LdapAuthenticator ldapAuthenticator;
    private String username  = "bbanner";
    private String principal = "cd=bbanner";
    private String hardcoded = "password";
    private Long domainId = 1L;

    @Before
    public void setUp() throws Exception {
        ldapAuthenticator = new LdapAuthenticator(ldapManager, userAccountDao);
    }

    @Test
    public void authenticateWithoutAccount() throws Exception {
        LdapUser ldapUser = new LdapUser(username,"a@b","b","banner",principal,"",false,null);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> rc;
        when(ldapManager.getUser(username, domainId)).thenReturn(ldapUser);
        rc = ldapAuthenticator.authenticate(username, "password", domainId, user);
        assertFalse("authentication succeded when it should have failed", rc.first());
        assertEquals("", UserAuthenticator.ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT,rc.second());
    }
    @Test
    public void authenticate() throws Exception {
        LdapUser ldapUser = new LdapUser(username, "a@b", "b", "banner", principal, "", false, null);
        when(ldapManager.getUser(username, domainId)).thenReturn(ldapUser);
        when(ldapManager.canAuthenticate(principal, hardcoded, domainId)).thenReturn(true);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> rc = ldapAuthenticator.authenticate(username, hardcoded, domainId, user);
        assertTrue("authentication failed when it should have succeeded", rc.first());
        assertNull(rc.second());
    }
}