package org.apache.cloudstack.secondarystorage;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.utils.net.NetUtils;
import com.google.common.net.InetAddresses;

@RunWith(MockitoJUnitRunner.class)
public class SecondaryStorageManagerImplTest {
    private final SecureRandom secureRandom = new SecureRandom();

    @Spy
    @InjectMocks
    private SecondaryStorageManagerImpl secondaryStorageManager;

    private List<DataStore> mockDataStoresForTestAddSecondaryStorageServerAddressToBuffer(List<String> addresses) {
        List<DataStore> dataStores = new ArrayList<>();
        for (String address: addresses) {
            DataStore dataStore = Mockito.mock(DataStore.class);
            DataStoreTO dataStoreTO = Mockito.mock(DataStoreTO.class);
            Mockito.when(dataStoreTO.getUrl()).thenReturn(NetUtils.isValidIp4(address) ? String.format("http://%s", address) : address);
            Mockito.when(dataStore.getTO()).thenReturn(dataStoreTO);
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
        Assert.assertEquals(expected, result);
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
}
