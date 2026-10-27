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
package org.apache.cloudstack.framework.jobs;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.crypt.EncryptionSecretKeyChecker;

public class AsyncJobDataHelperTest {

    private static final String RAW_CMD_INFO = "{\"password\":\"secured\",\"id\":\"abcd\"}";
    private static final String RAW_RESULT = "com.cloud.SomeResponseClass/someobject/{\"id\":\"abc\",\"name\":\"test\"}";

    @Test
    public void encryptInfoIfNeededSensitiveEncryptsAndCanBeDecrypted() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class);
             MockedStatic<DBEncryptionUtil> dbEncryptionMock = Mockito.mockStatic(DBEncryptionUtil.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(true);
            dbEncryptionMock.when(() -> DBEncryptionUtil.encrypt(RAW_CMD_INFO)).thenReturn("encrypted-value");
            dbEncryptionMock.when(() -> DBEncryptionUtil.decrypt("encrypted-value")).thenReturn(RAW_CMD_INFO);

            String storedValue = AsyncJobDataHelper.encryptInfoIfNeeded(SensitiveAsyncCmd.class.getName(), RAW_CMD_INFO);
            Assert.assertEquals("encrypted-value", storedValue);

            String unprotectedValue = AsyncJobDataHelper.decryptIfNeeded(SensitiveAsyncCmd.class.getName(), storedValue);
            Assert.assertEquals(RAW_CMD_INFO, unprotectedValue);
        }
    }

    @Test
    public void encryptInfoIfNeededSensitiveWithoutEncryptionReturnsOriginalValue() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(false);

            String storedValue = AsyncJobDataHelper.encryptInfoIfNeeded(SensitiveAsyncCmd.class.getName(), RAW_CMD_INFO);

            Assert.assertEquals(RAW_CMD_INFO, storedValue);
        }
    }

    @Test
    public void encryptInfoIfNeededNonSensitiveReturnsOriginalValue() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(true);

            String storedValue = AsyncJobDataHelper.encryptInfoIfNeeded(NonSensitiveAsyncCmd.class.getName(), RAW_CMD_INFO);

            Assert.assertEquals(RAW_CMD_INFO, storedValue);
        }
    }

    @Test
    public void decryptIfNeededPlainJsonReturnsOriginalValueForSensitiveCommand() {
        String storedValue = AsyncJobDataHelper.decryptIfNeeded(SensitiveAsyncCmd.class.getName(), RAW_CMD_INFO);
        Assert.assertEquals(RAW_CMD_INFO, storedValue);
    }

    @Test
    public void decryptIfNeededPlainJsonReturnsOriginalValueForNonSensitiveCommand() {
        String storedValue = AsyncJobDataHelper.decryptIfNeeded(NonSensitiveAsyncCmd.class.getName(), RAW_CMD_INFO);
        Assert.assertEquals(RAW_CMD_INFO, storedValue);
    }

    @Test
    public void decryptIfNeededBlankReturnsBlank() {
        Assert.assertNull(AsyncJobDataHelper.decryptIfNeeded(SensitiveAsyncCmd.class.getName(), null));
        Assert.assertEquals("", AsyncJobDataHelper.decryptIfNeeded(SensitiveAsyncCmd.class.getName(), ""));
    }

    @Test
    public void decryptIfNeededUnknownClassReturnsOriginalValue() {
        String storedValue = AsyncJobDataHelper.decryptIfNeeded("com.unknown.NonExistentCmd", RAW_CMD_INFO);
        Assert.assertEquals(RAW_CMD_INFO, storedValue);
    }

    // ---- encryptResultIfNeeded ----

    @Test
    public void encryptResultIfNeededSensitiveEncryptsValue() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class);
             MockedStatic<DBEncryptionUtil> dbEncryptionMock = Mockito.mockStatic(DBEncryptionUtil.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(true);
            dbEncryptionMock.when(() -> DBEncryptionUtil.encrypt(RAW_RESULT)).thenReturn("encrypted-result");

            String stored = AsyncJobDataHelper.encryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), RAW_RESULT);
            Assert.assertEquals("encrypted-result", stored);
        }
    }

    @Test
    public void encryptResultIfNeededSensitiveWithoutEncryptionReturnsOriginalValue() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(false);

            String stored = AsyncJobDataHelper.encryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), RAW_RESULT);
            Assert.assertEquals(RAW_RESULT, stored);
        }
    }

    @Test
    public void encryptResultIfNeededNonSensitiveReturnsOriginalValue() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(true);

            String stored = AsyncJobDataHelper.encryptResultIfNeeded(NonSensitiveAsyncCmd.class.getName(), RAW_RESULT);
            Assert.assertEquals(RAW_RESULT, stored);
        }
    }

    // ---- decryptResultIfNeeded ----

    @Test
    public void decryptResultIfNeededValidResultForSensitiveCommandReturnsOriginalValue() {
        // RAW_RESULT contains '{' so isValidResult returns true — no decryption attempted
        String result = AsyncJobDataHelper.decryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), RAW_RESULT);
        Assert.assertEquals(RAW_RESULT, result);
    }

    @Test
    public void decryptResultIfNeededEncryptedResultForSensitiveCommandDecrypts() {
        try (MockedStatic<EncryptionSecretKeyChecker> encryptionKeyCheckerMock = Mockito.mockStatic(EncryptionSecretKeyChecker.class);
             MockedStatic<DBEncryptionUtil> dbEncryptionMock = Mockito.mockStatic(DBEncryptionUtil.class)) {
            encryptionKeyCheckerMock.when(EncryptionSecretKeyChecker::useEncryption).thenReturn(true);
            dbEncryptionMock.when(() -> DBEncryptionUtil.decrypt("encrypted-result")).thenReturn(RAW_RESULT);

            String result = AsyncJobDataHelper.decryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), "encrypted-result");
            Assert.assertEquals(RAW_RESULT, result);
        }
    }

    @Test
    public void decryptResultIfNeededPlainTextErrorForSensitiveCommandReturnsOriginalValue() {
        // Plain-text error messages contain spaces — isValidResult returns true
        String errorMsg = "job cancelled because of management server restart";
        String result = AsyncJobDataHelper.decryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), errorMsg);
        Assert.assertEquals(errorMsg, result);
    }

    @Test
    public void decryptResultIfNeededNonSensitiveReturnsOriginalValue() {
        String result = AsyncJobDataHelper.decryptResultIfNeeded(NonSensitiveAsyncCmd.class.getName(), "encrypted-result");
        Assert.assertEquals("encrypted-result", result);
    }

    @Test
    public void decryptResultIfNeededBlankReturnsBlank() {
        Assert.assertNull(AsyncJobDataHelper.decryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), null));
        Assert.assertEquals("", AsyncJobDataHelper.decryptResultIfNeeded(SensitiveResponseAsyncCmd.class.getName(), ""));
    }

    @APICommand(name = "testSensitiveAsyncCmd", responseObject = SuccessResponse.class,
            requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
    private static class SensitiveAsyncCmd extends BaseAsyncCmd {
        @Override
        public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
                ResourceAllocationException {
        }

        @Override
        public long getEntityOwnerId() {
            return Account.ACCOUNT_ID_SYSTEM;
        }

        @Override
        public String getEventType() {
            return "TEST.SENSITIVE";
        }

        @Override
        public String getEventDescription() {
            return "Test sensitive";
        }
    }

    @APICommand(name = "testSensitiveResponseAsyncCmd", responseObject = SuccessResponse.class,
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
    private static class SensitiveResponseAsyncCmd extends BaseAsyncCmd {
        @Override
        public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
                ResourceAllocationException {
        }

        @Override
        public long getEntityOwnerId() {
            return Account.ACCOUNT_ID_SYSTEM;
        }

        @Override
        public String getEventType() {
            return "TEST.SENSITIVE.RESPONSE";
        }

        @Override
        public String getEventDescription() {
            return "Test sensitive response";
        }
    }

    @APICommand(name = "testNonSensitiveAsyncCmd", responseObject = SuccessResponse.class,
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
    private static class NonSensitiveAsyncCmd extends BaseAsyncCmd {
        @Override
        public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
                ResourceAllocationException {
        }

        @Override
        public long getEntityOwnerId() {
            return Account.ACCOUNT_ID_SYSTEM;
        }

        @Override
        public String getEventType() {
            return "TEST.NONSENSITIVE";
        }

        @Override
        public String getEventDescription() {
            return "Test non-sensitive";
        }
    }
}
