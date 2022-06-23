package com.cloud.network.dao;

import com.cloud.network.Networks;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

@RunWith(PowerMockRunner.class)
public class NetworkDaoImplTest {

    @Mock
    SearchBuilder<NetworkVO> searchBuilderNetworkVoMock;

    @Mock
    SearchCriteria<NetworkVO> searchCriteriaNetworkVoMock;

    @Mock
    List<NetworkVO> listNetworkVoMock;

    @Test
    public void listByPhysicalNetworkTrafficTypeTestSetParametersValidation() throws Exception {
        NetworkDaoImpl networkDaoImplSpy = PowerMockito.spy(new NetworkDaoImpl());

        networkDaoImplSpy.AllFieldsSearch = searchBuilderNetworkVoMock;
        Mockito.doReturn(searchCriteriaNetworkVoMock).when(searchBuilderNetworkVoMock).create();
        Mockito.doNothing().when(searchCriteriaNetworkVoMock).setParameters(Mockito.anyString(), Mockito.any());
        PowerMockito.doReturn(listNetworkVoMock).when(networkDaoImplSpy, "listBy", Mockito.any(SearchCriteria.class));

        long expectedPhysicalNetwork = 2513l;

        for (Networks.TrafficType trafficType : Networks.TrafficType.values()) {
            List<NetworkVO> result = networkDaoImplSpy.listByPhysicalNetworkTrafficType(expectedPhysicalNetwork, trafficType);
            Assert.assertEquals(listNetworkVoMock, result);
            Mockito.verify(searchCriteriaNetworkVoMock).setParameters("trafficType", trafficType);
        }

        Mockito.verify(searchCriteriaNetworkVoMock, Mockito.times(Networks.TrafficType.values().length)).setParameters("physicalNetwork", expectedPhysicalNetwork);
    }
}
