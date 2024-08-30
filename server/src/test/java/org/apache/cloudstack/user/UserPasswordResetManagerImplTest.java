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

import com.cloud.user.UserAccount;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetToken;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetTokenExpiryDate;

@RunWith(MockitoJUnitRunner.class)
public class UserPasswordResetManagerImplTest {
    @Spy
    @InjectMocks
    UserPasswordResetManagerImpl passwordReset;

    @Mock
    private UserDetailsDao userDetailsDao;

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

    @Test
    public void testValidateExistingTokenFirstRequest() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getId()).thenReturn(1L);
        Mockito.when(userDetailsDao.listDetailsKeyPairs(1L)).thenReturn(Collections.emptyMap());

        Assert.assertTrue(passwordReset.validateExistingToken(userAccount));
    }

    @Test
    public void testValidateExistingTokenSecondRequestExpired() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getId()).thenReturn(1L);
        Mockito.when(userDetailsDao.listDetailsKeyPairs(1L)).thenReturn(Map.of(
                PasswordResetToken, "reset_token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() - 5 * 60 * 1000)));

        Assert.assertTrue(passwordReset.validateExistingToken(userAccount));
    }


    @Test
    public void testValidateExistingTokenSecondRequestUnexpired() {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(userAccount.getId()).thenReturn(1L);
        Mockito.when(userDetailsDao.listDetailsKeyPairs(1L)).thenReturn(Map.of(
                PasswordResetToken, "reset_token",
                PasswordResetTokenExpiryDate, String.valueOf(System.currentTimeMillis() + 5 * 60 * 1000)));

        Assert.assertFalse(passwordReset.validateExistingToken(userAccount));
    }
}
