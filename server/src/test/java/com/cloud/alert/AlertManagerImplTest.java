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
package com.cloud.alert;

import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;

import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.alert.dao.AlertDao;

@RunWith(MockitoJUnitRunner.class)
public class AlertManagerImplTest {

    @Spy
    @InjectMocks
    AlertManagerImpl alertManagerImplMock;

    @Mock
    AlertDao alertDaoMock;

    @Mock
    AlertVO alertVOMock;

    @Mock
    Logger loggerMock;

    @Mock
    SMTPMailSender mailSenderMock;

    private void sendMessage (){
        try {
            alertManagerImplMock.sendAlert(AlertManager.AlertType.ALERT_TYPE_CPU, 0, 1l, 1l, "", "");
        } catch (UnsupportedEncodingException | MessagingException e) {
            Assert.fail();
        }
    }

    @Test
    public void sendAlertTestSendMail() {
        Mockito.doReturn(null).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(alertDaoMock).persist(Mockito.any());
        alertManagerImplMock.recipients = new String [] {""};

        sendMessage();

        Mockito.verify(alertManagerImplMock).sendMessage(Mockito.any());
    }

    @Test
    public void sendAlertTestDebugLogging() {
        Mockito.doReturn(0).when(alertVOMock).getSentCount();
        Mockito.doReturn(alertVOMock).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyLong());

        sendMessage();

        Mockito.verify(alertManagerImplMock.logger).debug(Mockito.anyString());
        Mockito.verify(alertManagerImplMock, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    public void sendAlertTestWarnLogging() {
        Mockito.doReturn(null).when(alertDaoMock).getLastAlert(Mockito.anyShort(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(alertDaoMock).persist(Mockito.any());
        alertManagerImplMock.recipients = null;

        sendMessage();

        Mockito.verify(alertManagerImplMock.logger, Mockito.times(2)).warn(Mockito.anyString());
        Mockito.verify(alertManagerImplMock, Mockito.never()).sendMessage(Mockito.any());
    }
}
