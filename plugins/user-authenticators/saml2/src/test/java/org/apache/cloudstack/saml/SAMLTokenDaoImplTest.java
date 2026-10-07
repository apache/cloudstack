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
package org.apache.cloudstack.saml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.TransactionLegacy;

@RunWith(MockitoJUnitRunner.class)
public class SAMLTokenDaoImplTest {

    @Mock
    private TransactionLegacy transactionMock;

    @Mock
    private PreparedStatement preparedStatementMock;

    private final SAMLTokenDaoImpl samlTokenDao = new SAMLTokenDaoImpl();

    @Test
    public void testExpireTokensUsesBoundGmtCutOff() throws Exception {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        // a JVM/DB local zone ahead of UTC is where NOW() deleted fresh tokens
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
        try (MockedStatic<TransactionLegacy> ignored = Mockito.mockStatic(TransactionLegacy.class)) {
            ignored.when(TransactionLegacy::currentTxn).thenReturn(transactionMock);
            when(transactionMock.prepareAutoCloseStatement(anyString())).thenReturn(preparedStatementMock);

            long before = System.currentTimeMillis();
            samlTokenDao.expireTokens();
            long after = System.currentTimeMillis();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(transactionMock).prepareAutoCloseStatement(sqlCaptor.capture());
            assertFalse("cut-off must not depend on the DB session time zone",
                    sqlCaptor.getValue().toUpperCase().contains("NOW()"));

            ArgumentCaptor<Timestamp> cutOffCaptor = ArgumentCaptor.forClass(Timestamp.class);
            ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
            verify(preparedStatementMock).setTimestamp(eq(1), cutOffCaptor.capture(), calendarCaptor.capture());
            verify(preparedStatementMock).executeUpdate();
            verify(transactionMock).commit();

            long cutOff = cutOffCaptor.getValue().getTime();
            assertTrue(cutOff >= before - SAMLTokenDaoImpl.TOKEN_LIFETIME_MILLIS);
            assertTrue(cutOff <= after - SAMLTokenDaoImpl.TOKEN_LIFETIME_MILLIS);
            assertEquals(0, calendarCaptor.getValue().getTimeZone().getRawOffset());
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }
}
