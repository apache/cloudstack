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
package org.apache.cloudstack.ha.provider.host;

import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.ha.provider.HARecoveryException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.alert.AlertManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;

@RunWith(MockitoJUnitRunner.class)
public class HAAbstractHostProviderTest {

    @Mock
    AlertManager alertManager;

    @Mock
    HostVO host;

    private HAAbstractHostProvider provider;

    private static final class TestHAHostProvider extends HAAbstractHostProvider {
        @Override
        public boolean isEligible(Host r) {
            return true;
        }

        @Override
        public boolean isHealthy(Host r) throws HACheckerException {
            return true;
        }

        @Override
        public boolean hasActivity(Host r, DateTime afterThis) throws HACheckerException {
            return true;
        }

        @Override
        public boolean recover(Host r) throws HARecoveryException {
            return true;
        }

        @Override
        public boolean fence(Host r) throws HAFenceException {
            return true;
        }

        @Override
        public Object getConfigValue(HAProviderConfig name, Host r) {
            return null;
        }
    }

    @Before
    public void setup() {
        provider = new TestHAHostProvider();
        ReflectionTestUtils.setField(provider, "alertManager", alertManager);

        Mockito.when(host.getDataCenterId()).thenReturn(1L);
        Mockito.when(host.getPodId()).thenReturn(2L);
        Mockito.when(host.toString()).thenReturn("Host {id=5, name=cs-kvm06}");
    }

    @Test
    public void sendAlertForFencingStateDescribesHostAndOperation() {
        provider.sendAlert(host, HAConfig.HAState.Fencing);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alertManager).sendAlert(Mockito.eq(AlertService.AlertType.ALERT_TYPE_HA_ACTION), Mockito.eq(1L), Mockito.eq(2L),
                subjectCaptor.capture(), bodyCaptor.capture());
        assertTrue(subjectCaptor.getValue().contains("HA Fencing"));
        assertTrue(subjectCaptor.getValue().contains("Host {id=5, name=cs-kvm06}"));
        assertTrue(bodyCaptor.getValue().contains("HA Fencing has been performed"));
        assertTrue(bodyCaptor.getValue().contains("Host {id=5, name=cs-kvm06}"));
    }

    @Test
    public void sendAlertForRecoveringStateDescribesHostAndOperation() {
        provider.sendAlert(host, HAConfig.HAState.Recovering);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alertManager).sendAlert(Mockito.eq(AlertService.AlertType.ALERT_TYPE_HA_ACTION), Mockito.eq(1L), Mockito.eq(2L),
                subjectCaptor.capture(), bodyCaptor.capture());
        assertTrue(subjectCaptor.getValue().contains("HA Recovery"));
        assertTrue(subjectCaptor.getValue().contains("Host {id=5, name=cs-kvm06}"));
        assertTrue(bodyCaptor.getValue().contains("HA Recovery has been performed"));
        assertTrue(bodyCaptor.getValue().contains("Host {id=5, name=cs-kvm06}"));
    }

    @Test
    public void sendAlertForOtherStatesUsesGenericSubjectAndBody() {
        provider.sendAlert(host, HAConfig.HAState.Available);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alertManager).sendAlert(Mockito.eq(AlertService.AlertType.ALERT_TYPE_HA_ACTION), Mockito.eq(1L), Mockito.eq(2L),
                subjectCaptor.capture(), bodyCaptor.capture());
        assertTrue(subjectCaptor.getValue().equals("HA operation performed for host"));
        assertTrue(bodyCaptor.getValue().equals("HA operation performed for host"));
    }
}
