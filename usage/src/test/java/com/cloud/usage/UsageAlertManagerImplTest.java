// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage;

import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.alert.AlertManager;
import com.cloud.alert.AlertVO;
import com.cloud.alert.dao.AlertDao;

@RunWith(MockitoJUnitRunner.class)
public class UsageAlertManagerImplTest {

    @Spy
    @InjectMocks
    UsageAlertManagerImpl usageAlertManagerImplMock;

    @Mock
    AlertDao alertDaoMock;

    @Mock
    AlertVO alertVOMock;

    @Mock
    Logger loggerMock;

    @Mock
    SMTPMailSender mailSenderMock;

    @Test
    public void sendAlertTestSendMail() {
        Mockito.doReturn(null).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(alertDaoMock).persist(Mockito.any());
        usageAlertManagerImplMock.recipients = new String [] {""};

        usageAlertManagerImplMock.sendAlert(AlertManager.AlertType.ALERT_TYPE_CPU, 0, 1l, "", "");

        Mockito.verify(usageAlertManagerImplMock.mailSender).sendMail(Mockito.any());
    }

    @Test
    public void sendAlertTestDebugLogging() {
        Mockito.doReturn(0).when(alertVOMock).getSentCount();
        Mockito.doReturn(alertVOMock).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(), Mockito.anyLong());

        usageAlertManagerImplMock.sendAlert(AlertManager.AlertType.ALERT_TYPE_CPU, 0, 1l, "", "");

        Mockito.verify(usageAlertManagerImplMock.logger).debug(Mockito.anyString());
        Mockito.verify(usageAlertManagerImplMock.mailSender, Mockito.never()).sendMail(Mockito.any());
    }

    @Test
    public void sendAlertTestWarnLogging() {
        Mockito.doReturn(null).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(alertDaoMock).persist(Mockito.any());
        usageAlertManagerImplMock.recipients = null;

        usageAlertManagerImplMock.sendAlert(AlertManager.AlertType.ALERT_TYPE_CPU, 0, 1l, "", "");

        Mockito.verify(usageAlertManagerImplMock.logger).warn(Mockito.anyString());
        Mockito.verify(usageAlertManagerImplMock.mailSender, Mockito.never()).sendMail(Mockito.any());
    }
}
