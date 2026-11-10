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
package org.apache.cloudstack.user;

import com.cloud.user.AccountManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetToken;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetTokenExpiryDate;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetTokenIssuedDate;

@RunWith(MockitoJUnitRunner.class)
public class UserPasswordResetManagerImplTest {
    @Spy
    @InjectMocks
    UserPasswordResetManagerImpl passwordReset;

    @Mock
    private UserDetailsDao userDetailsDao;

    @Mock
    private SMTPMailSender mailSender;

    @Mock
    private UserDao userDao;

    @Mock
    private AccountManager accountManager;

    @Test
    public void testGetMessageBody() {
        ConfigKey<String> passwordResetMailTemplate = Mockito.mock(ConfigKey.class);
        UserPasswordResetManagerImpl.PasswordResetMailTemplate = passwordResetMailTemplate;
        Mockito.when(passwordResetMailTemplate.value()).thenReturn("Hello {{username}}!\n" +
                "You have requested to reset your password. Please click the following link to reset your password:\n" +
                "{{{resetLink}}}\n" +
                "If you did not request a password reset, please ignore this email.\n" +
                "\n" +
                "Regards,\n" +
                "The CloudStack Team");

        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getUsername()).thenReturn("test_user");

        String messageBody = passwordReset.getMessageBody(userAccount, "reset_token", "reset_link");
        String expectedMessageBody = "Hello test_user!\n" +
                "You have requested to reset your password. Please click the following link to reset your password:\n" +
                "reset_link\n" +
                "If you did not request a password reset, please ignore this email.\n" +
                "\n" +
                "Regards,\n" +
                "The CloudStack Team";
        Assert.assertEquals("Message body doesn't match", expectedMessageBody, messageBody);
    }

    @Test
    public void testValidateAndResetPassword() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getId()).thenReturn(1L);
        Mockito.when(userAccount.getUsername()).thenReturn("test_user");

        Mockito.doNothing().when(passwordReset).resetPassword(userAccount, "new_password");

        UserDetailVO resetTokenDetail = Mockito.mock(UserDetailVO.class);
        UserDetailVO resetTokenExpiryDate = Mockito.mock(UserDetailVO.class);
        Mockito.when(userDetailsDao.findDetail(1L, PasswordResetToken)).thenReturn(resetTokenDetail);
        Mockito.when(userDetailsDao.findDetail(1L, PasswordResetTokenExpiryDate)).thenReturn(resetTokenExpiryDate);
        Mockito.when(resetTokenExpiryDate.getValue()).thenReturn(String.valueOf(System.currentTimeMillis() - 5 * 60 * 1000));

        try {
            passwordReset.validateAndResetPassword(userAccount, "reset_token", "new_password");
            Assert.fail("Should have thrown exception");
        } catch (ServerApiException e) {
            Assert.assertEquals("No reset token found for user test_user", e.getMessage());
        }

        Mockito.when(resetTokenDetail.getValue()).thenReturn("reset_token_XXX");

        try {
            passwordReset.validateAndResetPassword(userAccount, "reset_token", "new_password");
            Assert.fail("Should have thrown exception");
        } catch (ServerApiException e) {
            Assert.assertEquals("Invalid reset token for user test_user", e.getMessage());
        }

        Mockito.when(resetTokenDetail.getValue()).thenReturn("reset_token");

        try {
            passwordReset.validateAndResetPassword(userAccount, "reset_token", "new_password");
            Assert.fail("Should have thrown exception");
        } catch (ServerApiException e) {
            Assert.assertEquals("Reset token has expired for user test_user", e.getMessage());
        }

        Mockito.when(resetTokenExpiryDate.getValue()).thenReturn(String.valueOf(System.currentTimeMillis() + 5 * 60 * 1000));

        Assert.assertTrue(passwordReset.validateAndResetPassword(userAccount, "reset_token", "new_password"));
        Mockito.verify(passwordReset, Mockito.times(1)).resetPassword(userAccount, "new_password");
    }

    private UserAccount mockUser() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getId()).thenReturn(1L);
        return userAccount;
    }

    private void mockIssuedDate(String value) {
        UserDetailVO issuedDate = Mockito.mock(UserDetailVO.class);
        Mockito.when(issuedDate.getValue()).thenReturn(value);
        Mockito.when(userDetailsDao.findDetail(1L, PasswordResetTokenIssuedDate)).thenReturn(issuedDate);
    }

    @Test
    public void testIsWithinEmailCooldownFirstRequest() {
        UserAccount userAccount = mockUser();
        Mockito.doReturn(60L).when(passwordReset).getEmailCooldownSeconds();
        Mockito.when(userDetailsDao.findDetail(1L, PasswordResetTokenIssuedDate)).thenReturn(null);

        Assert.assertFalse(passwordReset.isWithinEmailCooldown(userAccount));
    }

    @Test
    public void testIsWithinEmailCooldownRecentRequest() {
        UserAccount userAccount = mockUser();
        Mockito.doReturn(60L).when(passwordReset).getEmailCooldownSeconds();
        mockIssuedDate(String.valueOf(System.currentTimeMillis() - 10 * 1000));

        Assert.assertTrue(passwordReset.isWithinEmailCooldown(userAccount));
    }

    @Test
    public void testIsWithinEmailCooldownOldRequest() {
        UserAccount userAccount = mockUser();
        Mockito.doReturn(60L).when(passwordReset).getEmailCooldownSeconds();
        mockIssuedDate(String.valueOf(System.currentTimeMillis() - 5 * 60 * 1000));

        Assert.assertFalse(passwordReset.isWithinEmailCooldown(userAccount));
    }

    @Test
    public void testIsWithinEmailCooldownDisabled() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.doReturn(0L).when(passwordReset).getEmailCooldownSeconds();

        Assert.assertFalse(passwordReset.isWithinEmailCooldown(userAccount));
        Mockito.verify(userDetailsDao, Mockito.never()).findDetail(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testIsWithinEmailCooldownIssueTimeInFuture() {
        UserAccount userAccount = mockUser();
        Mockito.doReturn(60L).when(passwordReset).getEmailCooldownSeconds();
        mockIssuedDate(String.valueOf(System.currentTimeMillis() + 2 * 60 * 1000));

        Assert.assertFalse(passwordReset.isWithinEmailCooldown(userAccount));
    }

    @Test
    public void testIsWithinEmailCooldownLargeValue() {
        UserAccount userAccount = mockUser();
        Mockito.doReturn(Long.MAX_VALUE / 100).when(passwordReset).getEmailCooldownSeconds();
        mockIssuedDate(String.valueOf(System.currentTimeMillis() - 10 * 1000));

        Assert.assertTrue(passwordReset.isWithinEmailCooldown(userAccount));
    }

    @Test
    public void testIsWithinEmailCooldownInvalidIssueTime() {
        UserAccount userAccount = mockUser();
        Mockito.doReturn(60L).when(passwordReset).getEmailCooldownSeconds();
        mockIssuedDate("not-a-number");

        Assert.assertFalse(passwordReset.isWithinEmailCooldown(userAccount));
    }

    @Test
    public void testSetResetTokenAndSendWithinCooldown() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.doReturn(true).when(passwordReset).isWithinEmailCooldown(userAccount);

        passwordReset.setResetTokenAndSend(userAccount);

        Mockito.verify(userDetailsDao, Mockito.never()).addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(mailSender, Mockito.never()).trySendMail(Mockito.any());
    }

    private UserAccount mockUserForSend() {
        UserAccount userAccount = mockUser();
        Mockito.when(userAccount.getUsername()).thenReturn("test_user");
        Mockito.when(userAccount.getEmail()).thenReturn("user@example.com");
        Mockito.doReturn(false).when(passwordReset).isWithinEmailCooldown(userAccount);
        Mockito.doReturn("body").when(passwordReset).getMessageBody(Mockito.eq(userAccount), Mockito.anyString(), Mockito.anyString());
        return userAccount;
    }

    @Test
    public void testSetResetTokenAndSendIssuesNewToken() {
        UserAccount userAccount = mockUserForSend();
        Mockito.when(mailSender.trySendMail(Mockito.any())).thenReturn(true);

        passwordReset.setResetTokenAndSend(userAccount);

        // Each detail has all existing rows removed before the new row is written,
        // so duplicates from parallel requests do not survive the next request.
        InOrder order = Mockito.inOrder(userDetailsDao);
        order.verify(userDetailsDao).removeDetail(1L, PasswordResetTokenIssuedDate);
        order.verify(userDetailsDao).addDetail(Mockito.eq(1L), Mockito.eq(PasswordResetTokenIssuedDate), Mockito.anyString(), Mockito.eq(false));
        order.verify(userDetailsDao).removeDetail(1L, PasswordResetToken);
        order.verify(userDetailsDao).addDetail(Mockito.eq(1L), Mockito.eq(PasswordResetToken), Mockito.anyString(), Mockito.eq(false));
        order.verify(userDetailsDao).removeDetail(1L, PasswordResetTokenExpiryDate);
        order.verify(userDetailsDao).addDetail(Mockito.eq(1L), Mockito.eq(PasswordResetTokenExpiryDate), Mockito.anyString(), Mockito.eq(false));
        Mockito.verify(userDetailsDao, Mockito.times(1)).removeDetail(1L, PasswordResetTokenIssuedDate);
    }

    @Test
    public void testSetResetTokenAndSendFailureKeepsExistingTokenAndCooldown() {
        UserAccount userAccount = mockUserForSend();
        Mockito.when(mailSender.trySendMail(Mockito.any())).thenReturn(false);

        passwordReset.setResetTokenAndSend(userAccount);

        Mockito.verify(userDetailsDao, Mockito.never()).addDetail(Mockito.eq(1L), Mockito.eq(PasswordResetToken), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(userDetailsDao, Mockito.never()).addDetail(Mockito.eq(1L), Mockito.eq(PasswordResetTokenExpiryDate), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(userDetailsDao, Mockito.never()).removeDetail(1L, PasswordResetToken);
        Mockito.verify(userDetailsDao).addDetail(Mockito.eq(1L), Mockito.eq(PasswordResetTokenIssuedDate), Mockito.anyString(), Mockito.eq(false));
        // Removed once while claiming the cooldown, and not released after the failure.
        Mockito.verify(userDetailsDao, Mockito.times(1)).removeDetail(1L, PasswordResetTokenIssuedDate);
    }

    @Test
    public void testResetPasswordRemovesResetDetails() {
        UserAccount userAccount = mockUser();
        UserVO user = Mockito.mock(UserVO.class);
        Mockito.when(userDao.getUser(1L)).thenReturn(user);

        passwordReset.resetPassword(userAccount, "new_password");

        Mockito.verify(userDetailsDao).removeDetail(1L, PasswordResetToken);
        Mockito.verify(userDetailsDao).removeDetail(1L, PasswordResetTokenExpiryDate);
        Mockito.verify(userDetailsDao).removeDetail(1L, PasswordResetTokenIssuedDate);
    }
}
