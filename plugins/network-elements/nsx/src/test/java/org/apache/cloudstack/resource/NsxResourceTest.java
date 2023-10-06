package org.apache.cloudstack.resource;

import com.cloud.network.dao.NetworkVO;
import com.vmware.nsx.TransportZones;
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.infra.Segments;
import com.vmware.nsx_policy.infra.Sites;
import com.vmware.nsx_policy.infra.Tier1s;
import com.vmware.nsx_policy.infra.sites.EnforcementPoints;
import com.vmware.nsx_policy.infra.tier_0s.LocaleServices;
import com.vmware.nsx_policy.model.EnforcementPoint;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.LocaleServicesListResult;
import com.vmware.nsx_policy.model.Segment;
import com.vmware.nsx_policy.model.Site;
import com.vmware.nsx_policy.model.SiteListResult;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.client.ApiClient;
import junit.framework.Assert;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.service.NsxApi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.cloudstack.utils.NsxApiClientUtils.TransportType.OVERLAY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NsxResourceTest {

    @Mock
    NsxApi nsxApi;
    @Mock
    ApiClient apiClient;

    NsxResource nsxResource;
    AutoCloseable closeable;
    @Mock
    private Tier1s tier1s;
    @Mock
    LocaleServices localeServices;
    @Mock
    EnforcementPoints enforcementPoints;
    @Mock
    Sites sites;
    @Mock
    Segments segments;
    @Mock
    TransportZones transportZones;
    @Mock
    com.vmware.nsx_policy.infra.tier_1s.LocaleServices tier1LocaleService;
    @Mock
    LocaleServicesListResult localeServicesListResult;
    @Mock
    EnforcementPointListResult enforcementPointListResult;
    @Mock
    SiteListResult siteListResult;
    @Mock
    TransportZoneListResult transportZoneListResult;
    @Mock
    com.vmware.nsx_policy.model.LocaleServices localeService;

    private Function<Class<? extends Service>, Service> nsxService = svcClass -> nsxApi.getApiClient().createStub(svcClass);

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        nsxResource = new NsxResource();
        nsxResource.nsxApi = nsxApi;
        nsxResource.transportZone = "Overlay";

        when(nsxApi.getApiClient()).thenReturn(apiClient);
        when(apiClient.createStub(Tier1s.class)).thenReturn(tier1s);
        when(apiClient.createStub(LocaleServices.class)).thenReturn(localeServices);
        when(apiClient.createStub(com.vmware.nsx_policy.infra.tier_1s.LocaleServices.class)).thenReturn(tier1LocaleService);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testConfigure() throws ConfigurationException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "nsxController");
        params.put("guid", "5944b356-644f-11ee-b8c2-f37bc1b564ff");
        params.put("zoneId", "1");
        params.put("hostname", "host1");
        params.put("username", "admin");
        params.put("password", "password");
        params.put("tier0Gateway", "Tier0-GW01");
        params.put("edgeCluster", "EdgeCluster");
        params.put("transportZone", "Overlay");
        params.put("port", "443");

        Assert.assertTrue(nsxResource.configure("nsx", params));
    }

    @Test
    public void testConfigure_MissingParameter() throws ConfigurationException {
        Map<String, Object> params = new HashMap<>();

        assertThrows(ConfigurationException.class, () -> nsxResource.configure("nsx", params));
    }

    @Test
    public void testCreateNsxTier1Gateway() {
        NsxCommand command = new CreateNsxTier1GatewayCommand("ZoneA", 1L,
                "testAcc", 1L, "VPC01");

        when(localeServices.list(nullable(String.class), nullable(String.class),
                nullable(Boolean.class), nullable(String.class), nullable(Long.class),
                nullable(Boolean.class), nullable(String.class))).thenReturn(localeServicesListResult);
        when(localeServicesListResult.getResults()).thenReturn(List.of(localeService));
        doNothing().when(tier1LocaleService).patch(anyString(), anyString(), any(com.vmware.nsx_policy.model.LocaleServices.class));

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteTier1Gateway() {
        NsxCommand command = new DeleteNsxTier1GatewayCommand("ZoneA", 1L,
                "testAcc", 1L, "VPC01");

        doNothing().when(tier1LocaleService).delete(anyString(), anyString());
        doNothing().when(tier1s).delete(anyString());

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateNsxSegment() {
        NetworkVO tierNetwork = new NetworkVO();
        tierNetwork.setName("tier1");
        tierNetwork.setCidr("10.0.0.0/8");
        Site site = mock(Site.class);
        List<Site> siteList = List.of(site);
        EnforcementPoint enforcementPoint = mock(EnforcementPoint.class);
        List<EnforcementPoint> enforcementPointList = List.of(enforcementPoint);
        List<TransportZone> transportZoneList = List.of(new TransportZone.Builder().setDisplayName("Overlay").build());

        NsxCommand command = new CreateNsxSegmentCommand("ZoneA", 1L,
                "testAcc", 1L, "VPC01", tierNetwork);

        when(apiClient.createStub(Sites.class)).thenReturn(sites);
        when(sites.list(nullable(String.class), anyBoolean(), nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class))).thenReturn(siteListResult);
        when(siteListResult.getResults()).thenReturn(siteList);
        when(siteList.get(0).getId()).thenReturn("site1");

        when(apiClient.createStub(EnforcementPoints.class)).thenReturn(enforcementPoints);
        when(enforcementPoints.list(anyString(), nullable(String.class), nullable(Boolean.class),
                nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class))).thenReturn(enforcementPointListResult);
        when(enforcementPointListResult.getResults()).thenReturn(enforcementPointList);
        when(enforcementPointList.get(0).getPath()).thenReturn("enforcementPointPath");

        when(apiClient.createStub(TransportZones.class)).thenReturn(transportZones);
        when(transportZones.list(nullable(String.class), nullable(String.class), anyBoolean(),
                nullable(String.class), anyBoolean(), nullable(Long.class), nullable(Boolean.class),
                nullable(String.class), anyString(), nullable(String.class))).thenReturn(transportZoneListResult);
        when(transportZoneListResult.getResults()).thenReturn(transportZoneList);

        when(localeServices.list(nullable(String.class), nullable(String.class),
                nullable(Boolean.class), nullable(String.class), nullable(Long.class),
                nullable(Boolean.class), nullable(String.class))).thenReturn(localeServicesListResult);
        when(localeServicesListResult.getResults()).thenReturn(List.of(localeService));

        when(apiClient.createStub(Segments.class)).thenReturn(segments);
        doNothing().when(segments).patch(anyString(), any(Segment.class));

        doNothing().when(tier1LocaleService).patch(anyString(), anyString(), any(com.vmware.nsx_policy.model.LocaleServices.class));

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        System.out.println(answer.getResult());
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxSegment() {
        NetworkVO tierNetwork = new NetworkVO();
        tierNetwork.setName("tier1");
        DeleteNsxSegmentCommand command = new DeleteNsxSegmentCommand("testAcc", "VPC01", tierNetwork);

        when(apiClient.createStub(Segments.class)).thenReturn(segments);
        doNothing().when(segments).delete(anyString());
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }
}
