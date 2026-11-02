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
package org.apache.cloudstack.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.network.VirtualNetworkApplianceService.RouterHealthStatus;
import com.cloud.network.dao.RouterHealthCheckResultDao;
import com.cloud.network.dao.RouterHealthCheckResultVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.DomainRouterDao;

@RunWith(MockitoJUnitRunner.class)
public class PrometheusExporterImplVRHealthCheckTest {

    private static final long ZONE_ID = 1L;
    private static final String ZONE_NAME = "zone1";
    private static final String ZONE_UUID = "zone-uuid-1";

    @Mock
    private DomainRouterDao domainRouterDao;
    @Mock
    private RouterHealthCheckResultDao routerHealthCheckResultDao;

    private PrometheusExporterImpl exporter;

    @Before
    public void setUp() {
        exporter = new PrometheusExporterImpl();
        ReflectionTestUtils.setField(exporter, "domainRouterDao", domainRouterDao);
        ReflectionTestUtils.setField(exporter, "routerHealthCheckResultDao", routerHealthCheckResultDao);
    }

    private DomainRouterVO router(long id, String name, VirtualRouter.Role role, State state) {
        DomainRouterVO router = Mockito.mock(DomainRouterVO.class);
        Mockito.lenient().when(router.getId()).thenReturn(id);
        Mockito.lenient().when(router.getInstanceName()).thenReturn(name);
        Mockito.lenient().when(router.getRole()).thenReturn(role);
        Mockito.lenient().when(router.getState()).thenReturn(state);
        return router;
    }

    private RouterHealthCheckResultVO check(long routerId, String name, RouterHealthStatus result, Date lastUpdate) {
        RouterHealthCheckResultVO check = Mockito.mock(RouterHealthCheckResultVO.class);
        Mockito.when(check.getRouterId()).thenReturn(routerId);
        Mockito.lenient().when(check.getCheckName()).thenReturn(name);
        Mockito.lenient().when(check.getCheckType()).thenReturn("basic");
        Mockito.lenient().when(check.getCheckResult()).thenReturn(result);
        Mockito.lenient().when(check.getLastUpdateTime()).thenReturn(lastUpdate);
        return check;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<String> collectMetrics() {
        List metrics = new ArrayList();
        exporter.addVirtualRouterHealthCheckMetrics(metrics, ZONE_ID, ZONE_NAME, ZONE_UUID);
        return ((List<Object>) metrics).stream().map(item -> {
            if (item instanceof PrometheusExporterImpl.ItemVRHealthCheckResult) {
                return ((PrometheusExporterImpl.ItemVRHealthCheckResult) item).toMetricsString();
            }
            return ((PrometheusExporterImpl.ItemVRHealthCheckLastCheck) item).toMetricsString();
        }).collect(Collectors.toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onlyRunningVirtualRoutersAreQueriedInASingleCall() {
        DomainRouterVO running = router(10L, "r-10-VM", VirtualRouter.Role.VIRTUAL_ROUTER, State.Running);
        DomainRouterVO stopped = router(11L, "r-11-VM", VirtualRouter.Role.VIRTUAL_ROUTER, State.Stopped);
        DomainRouterVO internalLb = router(12L, "r-12-VM", VirtualRouter.Role.INTERNAL_LB_VM, State.Running);
        Mockito.when(domainRouterDao.listByDataCenter(ZONE_ID)).thenReturn(List.of(running, stopped, internalLb));
        Mockito.when(routerHealthCheckResultDao.getHealthCheckResults(Mockito.anyList())).thenReturn(List.of());

        collectMetrics();

        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        Mockito.verify(routerHealthCheckResultDao, Mockito.times(1)).getHealthCheckResults(ids.capture());
        Mockito.verify(routerHealthCheckResultDao, Mockito.never()).getHealthCheckResults(Mockito.anyLong());
        assertEquals(List.of(10L), ids.getValue());
    }

    @Test
    public void noRunningRoutersSkipsHealthCheckQuery() {
        DomainRouterVO stopped = router(11L, "r-11-VM", VirtualRouter.Role.VIRTUAL_ROUTER, State.Stopped);
        Mockito.when(domainRouterDao.listByDataCenter(ZONE_ID)).thenReturn(List.of(stopped));

        assertTrue(collectMetrics().isEmpty());
        Mockito.verifyNoInteractions(routerHealthCheckResultDao);
    }

    @Test
    public void resultsAreExportedPerRouterAndCheck() {
        DomainRouterVO r1 = router(10L, "r-10-VM", VirtualRouter.Role.VIRTUAL_ROUTER, State.Running);
        DomainRouterVO r2 = router(20L, "r-20-VM", VirtualRouter.Role.VIRTUAL_ROUTER, State.Running);
        Mockito.when(domainRouterDao.listByDataCenter(ZONE_ID)).thenReturn(List.of(r1, r2));
        List<RouterHealthCheckResultVO> checks = List.of(
                check(10L, "dns_check.py", RouterHealthStatus.SUCCESS, new Date(1735689600000L)),
                check(20L, "disk_space_check.py", RouterHealthStatus.FAILED, null));
        Mockito.when(routerHealthCheckResultDao.getHealthCheckResults(Mockito.anyList())).thenReturn(checks);

        List<String> metrics = collectMetrics();

        assertEquals(List.of(
                "cloudstack_virtualrouter_healthcheck_result{checkname=\"dns_check.py\",checktype=\"basic\",zone=\"zone1\",virtualrouter=\"r-10-VM\"} 1",
                "cloudstack_virtualrouter_healthcheck_lastcheck{checkname=\"dns_check.py\",checktype=\"basic\",zone=\"zone1\",virtualrouter=\"r-10-VM\"} 1735689600",
                "cloudstack_virtualrouter_healthcheck_result{checkname=\"disk_space_check.py\",checktype=\"basic\",zone=\"zone1\",virtualrouter=\"r-20-VM\"} 0"),
                metrics);
    }
}
