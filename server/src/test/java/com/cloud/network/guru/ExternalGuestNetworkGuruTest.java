package com.cloud.network.guru;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentContext.class)
public class ExternalGuestNetworkGuruTest {
    @Mock
    NetworkModel networkModel;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;

    @InjectMocks
    protected ExternalGuestNetworkGuru guru = new ExternalGuestNetworkGuru();


    @Test
    public void testDesignDns() {
        final String[] ip4Dns = {"5.5.5.5", "6.6.6.6"};
        final String[] ip6Dns = {"2001:4860:4860::5555", "2001:4860:4860::6666"};
        Mockito.when(networkModel.areServicesSupportedByNetworkOffering(Mockito.anyLong(), Mockito.any())).thenReturn(false);
        Mockito.when(networkModel.networkIsConfiguredForExternalNetworking(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        Mockito.when(networkOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(networkOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        Mockito.when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        PhysicalNetworkVO physicalNetwork = Mockito.mock(PhysicalNetworkVO.class);
        Mockito.when(physicalNetwork.getId()).thenReturn(1L);
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(physicalNetwork);
        DeploymentPlan plan = Mockito.mock(DeploymentPlan.class);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getDns1()).thenReturn(ip4Dns[0]);
        Mockito.when(network.getDns2()).thenReturn(ip4Dns[1]);
        Mockito.when(network.getIp6Dns1()).thenReturn(ip6Dns[0]);
        Mockito.when(network.getIp6Dns2()).thenReturn(ip6Dns[1]);
        Account owner = Mockito.mock(Account.class);
        Network config = guru.design(networkOffering, plan, network, owner);
        assertNotNull(config);
        assertEquals(ip4Dns[0], config.getDns1());
        assertEquals(ip4Dns[1], config.getDns2());
        assertEquals(ip6Dns[0], config.getIp6Dns1());
        assertEquals(ip6Dns[1], config.getIp6Dns2());
    }
}