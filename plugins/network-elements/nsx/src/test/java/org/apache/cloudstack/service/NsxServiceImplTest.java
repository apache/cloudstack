package org.apache.cloudstack.service;

import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxServiceImplTest {
    @Mock
    private NsxControllerUtils nsxControllerUtils;
    @Mock
    private VpcDao vpcDao;
    NsxServiceImpl nsxService;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        nsxService = new NsxServiceImpl();
        nsxService.nsxControllerUtils = nsxControllerUtils;
        nsxService.vpcDao = vpcDao;
    }

    @After
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateVpcNetwork() {
        NsxAnswer createNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxTier1GatewayCommand.class), anyLong())).thenReturn(createNsxTier1GatewayAnswer);
        when(createNsxTier1GatewayAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.createVpcNetwork(1L, "ZoneNSX", 1L, "testAcc", "VPC01"));
    }

    @Test
    public void testDeleteVpcNetwork() {
        NsxAnswer deleteNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxTier1GatewayCommand.class), anyLong())).thenReturn(deleteNsxTier1GatewayAnswer);
        when(deleteNsxTier1GatewayAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.deleteVpcNetwork(1L, "ZoneNSX", 1L, "testAcc", "VPC01"));
    }

    @Test
    public void testDelete() {
        NetworkVO network = new NetworkVO();
        network.setVpcId(1L);
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(1L)).thenReturn(mock(VpcVO.class));
        NsxAnswer deleteNsxSegmentAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxSegmentCommand.class), anyLong())).thenReturn(deleteNsxSegmentAnswer);
        when(deleteNsxSegmentAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.deleteNetwork("testAcc", network));
    }
}
