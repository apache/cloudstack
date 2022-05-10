package com.cloud.offerings.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.offering.NetworkOffering;
import com.cloud.utils.net.NetUtils;

public class NetworkOfferingDaoImplTest {
    @Mock
    NetworkOfferingDetailsDao detailsDao;

    @InjectMocks
    NetworkOfferingDaoImpl networkOfferingDao = new NetworkOfferingDaoImpl();

    final long offeringId = 1L;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetNetworkOfferingInternetProtocol() {
        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn(null);
        NetUtils.InternetProtocol protocol = networkOfferingDao.getNetworkOfferingInternetProtocol(offeringId);
        Assert.assertNull(protocol);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("IPv4");
        protocol = networkOfferingDao.getNetworkOfferingInternetProtocol(offeringId);
        Assert.assertEquals(NetUtils.InternetProtocol.IPv4, protocol);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("IPv6");
        protocol = networkOfferingDao.getNetworkOfferingInternetProtocol(offeringId);
        Assert.assertEquals(NetUtils.InternetProtocol.IPv6, protocol);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("DualStack");
        protocol = networkOfferingDao.getNetworkOfferingInternetProtocol(offeringId);
        Assert.assertEquals(NetUtils.InternetProtocol.DualStack, protocol);
    }

    @Test
    public void testGetNetworkOfferingInternetProtocolWithDefault() {
        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn(null);
        NetUtils.InternetProtocol protocol = networkOfferingDao.getNetworkOfferingInternetProtocol(offeringId, NetUtils.InternetProtocol.IPv4);
        Assert.assertEquals(NetUtils.InternetProtocol.IPv4, protocol);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("IPv6");
        protocol = networkOfferingDao.getNetworkOfferingInternetProtocol(offeringId, NetUtils.InternetProtocol.IPv4);
        Assert.assertEquals(NetUtils.InternetProtocol.IPv6, protocol);
    }

    @Test
    public void testIsIpv6Supported() {
        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("");
        boolean result = networkOfferingDao.isIpv6Supported(offeringId);
        Assert.assertFalse(result);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("IPv4");
        result = networkOfferingDao.isIpv6Supported(offeringId);
        Assert.assertFalse(result);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("IPv6");
        result = networkOfferingDao.isIpv6Supported(offeringId);
        Assert.assertTrue(result);

        Mockito.when(detailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol)).thenReturn("DualStack");
        result = networkOfferingDao.isIpv6Supported(offeringId);
        Assert.assertTrue(result);
    }
}