package com.cloud.server.auth;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.server.auth.UserAuthenticator.ActionOnFailedAuthentication;
import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class MD5UserAuthenticatorTest {
    @Mock
    UserAccountDao dao;

    @Test
    public void encode() {
        Assert.assertEquals("5f4dcc3b5aa765d61d8327deb882cf99",
                new MD5UserAuthenticator().encode("password"));
    }

    @Test
    public void authenticate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MD5UserAuthenticator authenticator = new MD5UserAuthenticator();
        Field daoField = MD5UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        UserAccountVO account = new UserAccountVO();
        account.setPassword("5f4dcc3b5aa765d61d8327deb882cf99");
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Pair<Boolean, ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertTrue(pair.first());
    }

    @Test
    public void authenticateBadPass() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MD5UserAuthenticator authenticator = new MD5UserAuthenticator();
        Field daoField = MD5UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        UserAccountVO account = new UserAccountVO();
        account.setPassword("surprise");
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Pair<Boolean, ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertFalse(pair.first());
    }

    @Test
    public void authenticateBadUser() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MD5UserAuthenticator authenticator = new MD5UserAuthenticator();
        Field daoField = MD5UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(null);
        Pair<Boolean, ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertFalse(pair.first());
    }
}
