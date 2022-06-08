package com.cloud.network;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import com.cloud.dc.DataCenter;
import com.cloud.utils.Pair;

public class NetworkModelImplTest {
    final String[] ip4Dns1 = {"5.5.5.5", "6.6.6.6"};
    final String[] ip4Dns2 = {"7.7.7.7", "8.8.8.8"};
    final String[] ip6Dns1 = {"2001:4860:4860::5555", "2001:4860:4860::6666"};
    final String[] ip6Dns2 = {"2001:4860:4860::7777", "2001:4860:4860::8888"};

    @InjectMocks
    private NetworkModelImpl networkModel = new NetworkModelImpl();

    private void prepareMocks(boolean isIp6, Network network, DataCenter zone,
                              String dns1, String dns2, String dns3, String dns4) {
        if (isIp6) {
            Mockito.when(network.getIp6Dns1()).thenReturn(dns1);
            Mockito.when(zone.getIp6Dns1()).thenReturn(dns2);
            Mockito.when(network.getIp6Dns2()).thenReturn(dns3);
            Mockito.when(zone.getIp6Dns2()).thenReturn(dns4);
        } else {
            Mockito.when(network.getDns1()).thenReturn(dns1);
            Mockito.when(zone.getDns1()).thenReturn(dns2);
            Mockito.when(network.getDns2()).thenReturn(dns3);
            Mockito.when(zone.getDns2()).thenReturn(dns4);
        }
    }

    private void testDnsCases(boolean isIp6) {
        String[] dns1 = isIp6 ? ip6Dns1 : ip4Dns1;
        String[] dns2 = isIp6 ? ip6Dns2 : ip4Dns2;
        Network network = Mockito.mock(Network.class);
        DataCenter zone = Mockito.mock(DataCenter.class);
        // Both network and zone have valid dns
        prepareMocks(isIp6, network, zone, dns1[0], dns1[1], dns2[0], dns1[1]);
        Pair<String, String> result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertEquals(dns2[0], result.second());
        // Network has valid dns and zone don't
        prepareMocks(isIp6, network, zone, dns1[0], null, dns2[0], null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertEquals(dns2[0], result.second());
        // Zone has a valid dns and network don't
        prepareMocks(isIp6, network, zone, null, dns1[1],  null, dns2[1]);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[1], result.first());
        Assert.assertEquals(dns2[1], result.second());
        // Zone has a valid dns and network has only first dns
        prepareMocks(isIp6, network, zone, dns1[0], dns1[1],  null, dns2[1]);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertNull(result.second());
        // Both network and zone only have the first dns
        prepareMocks(isIp6, network, zone, dns1[0], dns1[1],  null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertNull(result.second());
        // Both network and zone dns are null
        prepareMocks(isIp6, network, zone, null, null,  null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertNull(result.first());
        Assert.assertNull(result.second());
    }

    @Test
    public void testGetNetworkIp4Dns() {
        testDnsCases(false);
    }

    @Test
    public void testGetNetworkIp6Dns() {
        testDnsCases(true);
    }
}