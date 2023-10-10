package org.apache.cloudstack.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.ReservationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxElementTest {

    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    NsxServiceImpl nsxService;
    @Mock
    AccountManager accountManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    ResourceManager resourceManager;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    Vpc vpc;
    @Mock
    DataCenterVO dataCenterVO;
    @Mock
    Account account;

    NsxElement nsxElement;
    ReservationContext reservationContext;
    DeployDestination deployDestination;

    @Before
    public void setup() {
        nsxElement = new NsxElement();

        nsxElement.dataCenterDao = dataCenterDao;
        nsxElement.nsxService = nsxService;
        nsxElement.accountMgr = accountManager;
        nsxElement.networkDao = networkDao;
        nsxElement.resourceManager = resourceManager;
        nsxElement.physicalNetworkDao = physicalNetworkDao;
        nsxElement.networkModel = networkModel;
        reservationContext = mock(ReservationContext.class);
        deployDestination = mock(DeployDestination.class);

        when(vpc.getZoneId()).thenReturn(1L);
        when(vpc.getAccountId()).thenReturn(2L);
        when(dataCenterVO.getId()).thenReturn(1L);
        when(dataCenterVO.getName()).thenReturn("zone-NSX");
        when(account.getAccountId()).thenReturn(1L);
        when(account.getName()).thenReturn("testAcc");
        when(vpc.getName()).thenReturn("VPC01");
        when(accountManager.getAccount(2L)).thenReturn(account);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);

        PhysicalNetworkVO physicalNetworkVO = new PhysicalNetworkVO();
        physicalNetworkVO.setIsolationMethods(List.of("NSX"));
        List<PhysicalNetworkVO> physicalNetworkVOList = List.of(physicalNetworkVO);

        when(physicalNetworkDao.listByZoneAndTrafficType(1L, Networks.TrafficType.Guest)).thenReturn(physicalNetworkVOList);
    }

    @Test
    public void testImplementVpc() throws ResourceUnavailableException, InsufficientCapacityException {
        when(nsxService.createVpcNetwork(anyLong(), anyString(), anyLong(), anyString(), anyString())).thenReturn(true);

        assertTrue(nsxElement.implementVpc(vpc, deployDestination, reservationContext));
    }

    @Test
    public void testShutdownVpc() {
        when(nsxService.deleteVpcNetwork(anyLong(), anyString(), anyLong(), anyString(), anyString())).thenReturn(true);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));
    }



}
