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
package com.cloud.consoleproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.dao.ConsoleProxyDao;

@RunWith(MockitoJUnitRunner.class)
public class ConsoleProxyManagerImplTest {
    @InjectMocks
    private ConsoleProxyManagerImpl consoleProxyManager;

    @Mock
    private ConsoleProxyDao consoleProxyDao;

    @Mock
    private AccountManager accountManager;

    @Mock
    private ServiceOffering serviceOffering;
    @Mock
    private VMTemplateVO template;
    @Mock
    private Account systemAccount;
    @Mock
    private User systemUser;

    @Before
    public void setUp() {
        when(accountManager.getSystemUser()).thenReturn(systemUser);
    }

    @Test
    public void testCreateConsoleProxy_New() {
        long dataCenterId = 1L;
        long id = 10L;
        String name = "console1";
        // When creating a new proxy, persist should be called.
        when(consoleProxyDao.persist(any(ConsoleProxyVO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ConsoleProxyVO result = consoleProxyManager.createOrUpdateConsoleProxy(null, dataCenterId, id, name, serviceOffering, template, systemAccount);
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(serviceOffering.getId(), result.getServiceOfferingId());
        assertEquals(name, result.getName());
        assertEquals(template.getId(), result.getTemplateId());
        assertEquals(template.getHypervisorType(), result.getHypervisorType());
        assertEquals(template.getGuestOSId(), result.getGuestOSId());
        assertEquals(dataCenterId, result.getDataCenterId());
        assertEquals(systemAccount.getDomainId(), result.getDomainId());
        assertEquals(systemAccount.getId(), result.getAccountId());
        assertEquals(serviceOffering.isOfferHA(), result.isHaEnabled());
        assertEquals(template.isDynamicallyScalable(), result.isDynamicallyScalable());
        assertEquals(serviceOffering.getLimitCpuUse(), result.limitCpuUse());
        verify(consoleProxyDao).persist(any(ConsoleProxyVO.class));
    }

    @Test
    public void testUpdateConsoleProxy() {
        long dataCenterId = 1L;
        long id = 10L;
        String name = "console1";
        ConsoleProxyVO existing = new ConsoleProxyVO(id, serviceOffering.getId(), name, 999L, Hypervisor.HypervisorType.KVM, 111L,
                dataCenterId, systemAccount.getDomainId(), systemAccount.getId(),
                systemUser.getId(), 0, serviceOffering.isOfferHA());
        existing.setDynamicallyScalable(false);
        ConsoleProxyVO result = consoleProxyManager.createOrUpdateConsoleProxy(existing, dataCenterId, id, name, serviceOffering, template, systemAccount);
        verify(consoleProxyDao).update(existing.getId(), existing);
        assertEquals(template.getId(), result.getTemplateId());
        assertEquals(template.getHypervisorType(), result.getHypervisorType());
        assertEquals(template.getGuestOSId(), result.getGuestOSId());
        assertEquals(template.isDynamicallyScalable(), result.isDynamicallyScalable());
    }
}
