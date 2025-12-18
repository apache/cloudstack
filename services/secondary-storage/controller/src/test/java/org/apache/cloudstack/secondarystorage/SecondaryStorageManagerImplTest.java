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
package org.apache.cloudstack.secondarystorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.google.common.net.InetAddresses;

@RunWith(MockitoJUnitRunner.class)
public class SecondaryStorageManagerImplTest {
    private final SecureRandom secureRandom = new SecureRandom();

    @Mock
    private SecondaryStorageVmDao secStorageVmDao;

    @Mock
    private AccountManager accountManager;

    @Spy
    @InjectMocks
    private SecondaryStorageManagerImpl secondaryStorageManager;

    @Mock
    private ServiceOffering serviceOffering;
    @Mock
    private VMTemplateVO template;
    @Mock
    private Account systemAccount;
    @Mock
    private User systemUser;

    private List<DataStore> mockDataStoresForTestAddSecondaryStorageServerAddressToBuffer(List<String> addresses) {
        List<DataStore> dataStores = new ArrayList<>();
        for (String address: addresses) {
            DataStore dataStore = Mockito.mock(DataStore.class);
            DataStoreTO dataStoreTO = Mockito.mock(DataStoreTO.class);
            when(dataStoreTO.getUrl()).thenReturn(NetUtils.isValidIp4(address) ? String.format("http://%s", address) : address);
            when(dataStore.getTO()).thenReturn(dataStoreTO);
            dataStores.add(dataStore);
        }
        return dataStores;
    }

    private void runAddSecondaryStorageServerAddressToBufferTest(List<String> addresses, String expected) {
        List<DataStore> dataStores = mockDataStoresForTestAddSecondaryStorageServerAddressToBuffer(addresses);
        StringBuilder builder = new StringBuilder();
        secondaryStorageManager.addSecondaryStorageServerAddressToBuffer(builder, dataStores, "VM");
        String result = builder.toString();
        result = result.contains("=") ? result.split("=")[1] : null;
        assertEquals(expected, result);
    }

    @Test
    public void testAddSecondaryStorageServerAddressToBufferDifferentAddress() {
        String randomIp1 = InetAddresses.fromInteger(secureRandom.nextInt()).getHostAddress();
        String randomIp2 = InetAddresses.fromInteger(secureRandom.nextInt()).getHostAddress();
        List<String> addresses = List.of(randomIp1, randomIp2);
        String expected = StringUtils.join(addresses, ",");
        runAddSecondaryStorageServerAddressToBufferTest(addresses, expected);
    }

    @Test
    public void testAddSecondaryStorageServerAddressToBufferSameAddress() {
        String randomIp1 = InetAddresses.fromInteger(secureRandom.nextInt()).getHostAddress();
        List<String> addresses = List.of(randomIp1, randomIp1);
        runAddSecondaryStorageServerAddressToBufferTest(addresses, randomIp1);
    }

    @Test
    public void testAddSecondaryStorageServerAddressToBufferInvalidAddress() {
        String randomIp1 = InetAddresses.fromInteger(secureRandom.nextInt()).getHostAddress();
        String randomIp2 = InetAddresses.fromInteger(secureRandom.nextInt()).getHostAddress();
        List<String> addresses = List.of(randomIp1, "garbage", randomIp2);
        runAddSecondaryStorageServerAddressToBufferTest(addresses, StringUtils.join(List.of(randomIp1, randomIp2), ","));
    }

    @Test
    public void testCreateSecondaryStorageVm_New() {
        long dataCenterId = 1L;
        long id = 100L;
        String name = "ssvm1";
        SecondaryStorageVm.Role role = SecondaryStorageVm.Role.templateProcessor;
        when(systemUser.getId()).thenReturn(1L);
        when(accountManager.getSystemUser()).thenReturn(systemUser);
        when(secStorageVmDao.persist(any(SecondaryStorageVmVO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SecondaryStorageVmVO result = secondaryStorageManager.createOrUpdateSecondaryStorageVm(
                null, dataCenterId, id, name, serviceOffering, template, systemAccount, role);
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
        assertEquals(role, result.getRole());
        assertEquals(template.isDynamicallyScalable(), result.isDynamicallyScalable());
        assertEquals(serviceOffering.getLimitCpuUse(), result.limitCpuUse());

        verify(secStorageVmDao).persist(any(SecondaryStorageVmVO.class));
    }

    @Test
    public void testUpdateSecondaryStorageVm() {
        long dataCenterId = 1L;
        long id = 100L;
        String name = "ssvm1";
        SecondaryStorageVm.Role role = SecondaryStorageVm.Role.commandExecutor;
        SecondaryStorageVmVO existing = new SecondaryStorageVmVO(id, serviceOffering.getId(), name,
                999L, Hypervisor.HypervisorType.KVM, 888L, dataCenterId, systemAccount.getDomainId(), systemAccount.getId(),
                systemUser.getId(), role, serviceOffering.isOfferHA());
        existing.setDynamicallyScalable(false);

        SecondaryStorageVmVO result = secondaryStorageManager.createOrUpdateSecondaryStorageVm(
                existing, dataCenterId, id, name, serviceOffering, template, systemAccount, role);
        assertNotNull(result);
        assertEquals(template.getId(), result.getTemplateId());
        assertEquals(template.getHypervisorType(), result.getHypervisorType());
        assertEquals(template.getGuestOSId(), result.getGuestOSId());
        assertEquals(template.isDynamicallyScalable(), result.isDynamicallyScalable());

        verify(secStorageVmDao).update(existing.getId(), existing);
    }
}
