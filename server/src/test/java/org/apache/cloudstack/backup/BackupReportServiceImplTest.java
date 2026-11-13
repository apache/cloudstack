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
package org.apache.cloudstack.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.cloudstack.backup.dao.BackupReportDaoImpl;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BackupReportServiceImplTest {

    @Mock
    private ConfigKey<Boolean> backupReportTaskEnabledMock;

    @Mock
    private ConfigKey<Integer> backupReportTimeoutMock;

    @Mock
    private BackupReportDaoImpl backupReportDaoMock;

    @Spy
    @InjectMocks
    private BackupReportServiceImpl backupReportServiceSpy;

    @Test
    public void isTaskDisabledTestTaskIsEnabled() {
        Mockito.doReturn(true).when(backupReportTaskEnabledMock).value();

        boolean result = backupReportServiceSpy.isTaskDisabled(null);

        assertFalse(result);
    }

    @Test
    public void isTaskDisabledTestTaskIsDisabledAndLatestReportIsNull() {
        Mockito.doReturn(false).when(backupReportTaskEnabledMock).value();
        Mockito.doReturn(null).when(backupReportDaoMock).persist(any());

        boolean result = backupReportServiceSpy.isTaskDisabled(null);

        assertTrue(result);
        Mockito.verify(backupReportDaoMock).persist(any());
    }

    @Test
    public void isTaskDisabledTestTaskIsDisabledAndLatestReportIsEnabled() {
        Mockito.doReturn(false).when(backupReportTaskEnabledMock).value();
        Mockito.doReturn(null).when(backupReportDaoMock).persist(any());

        boolean result = backupReportServiceSpy.isTaskDisabled(new BackupReportVO(new Date(), true));

        assertTrue(result);
        Mockito.verify(backupReportDaoMock).persist(any());
    }

    @Test
    public void isTaskDisabledTestTaskIsDisabledAndLatestReportIsDisabled() {
        Mockito.doReturn(false).when(backupReportTaskEnabledMock).value();

        boolean result = backupReportServiceSpy.isTaskDisabled(new BackupReportVO(new Date(), false));

        assertTrue(result);
        Mockito.verify(backupReportDaoMock, Mockito.never()).persist(any());
    }

    @Test
    public void getStartTestNoLatestReport() {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar expectedResult = (Calendar)start.clone();
        int period = 10;
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(start);
            Calendar result = backupReportServiceSpy.getStart(null, period);
            expectedResult.add(Calendar.MINUTE, -period);
            assertEquals(expectedResult, result);
        }
    }

    @Test
    public void getStartTestLatestReportWasDisabled() {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar expectedResult = (Calendar)start.clone();
        int period = 10;
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(start);
            Calendar result = backupReportServiceSpy.getStart(new BackupReportVO(new Date(), false), period);
            expectedResult.add(Calendar.MINUTE, -period);
            assertEquals(expectedResult, result);
        }
    }

    @Test
    public void getStartTestLatestReportWasEnabledAndPeriodHasNotElapsed() {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar copy = (Calendar)start.clone();
        int period = 10;
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(start);
            copy.add(Calendar.MINUTE, -period/2);
            Calendar result = backupReportServiceSpy.getStart(new BackupReportVO(copy.getTime(), true), period);

            assertNull(result);
        }
    }

    @Test
    public void getStartTestLatestReportWasEnabledAndPeriodHasElapsed() {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar expectedResult = (Calendar)start.clone();
        int period = 10;
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(start);
            expectedResult.add(Calendar.MINUTE, -(period*2));
            Mockito.doReturn(expectedResult).when(backupReportServiceSpy).getLastReportStart(any());
            Calendar result = backupReportServiceSpy.getStart(new BackupReportVO(null, true), period);

            assertEquals(expectedResult, result);
        }
    }

    @Test
    public void isLastTaskRunningTestLastTaskIsRunning() {
        Calendar timeout = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar latestReportDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(timeout);
            boolean result = backupReportServiceSpy.isLastTaskRunning(new BackupReportVO(latestReportDate.getTime(), true));

            assertTrue(result);
        }
    }

    @Test
    public void isLastTaskRunningTestLastTaskTimedOut() {
        Calendar timeout = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar latestReportDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int period = 10;
        latestReportDate.add(Calendar.MINUTE, -period*2);
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(timeout);
            boolean result = backupReportServiceSpy.isLastTaskRunning(new BackupReportVO(latestReportDate.getTime(), true));

            assertFalse(result);
            Mockito.verify(backupReportDaoMock).remove(anyLong());
        }
    }

    @Test
    public void isLastTaskRunningTestLastTaskIsRemoved() {
        Calendar timeout = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(timeout);
            BackupReportVO lastTask = new BackupReportVO(null, true);
            lastTask.setRemoved(timeout.getTime());
            boolean result = backupReportServiceSpy.isLastTaskRunning(lastTask);

            assertFalse(result);
            Mockito.verify(backupReportDaoMock, Mockito.never()).remove(anyLong());
        }
    }

    @Test
    public void isLastTaskRunningTestLastTaskIsNull() {
        Calendar timeout = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try (MockedStatic<Calendar> calendarMock = Mockito.mockStatic(Calendar.class)) {
            calendarMock.when(() -> Calendar.getInstance((TimeZone)any())).thenReturn(timeout);

            boolean result = backupReportServiceSpy.isLastTaskRunning(null);

            assertFalse(result);
            Mockito.verify(backupReportDaoMock, Mockito.never()).remove(anyLong());
        }
    }

}
