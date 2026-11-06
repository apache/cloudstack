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
package com.cloud.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDaoImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.dao.DataCenterDetailsDaoImpl;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDaoImpl;
import com.cloud.network.dao.NetworkDetailsDaoImpl;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDaoImpl;

public class NetworkRateBackfillTest {

    private static final String SQL_NICS = "default_nic FROM nics";
    private static final String SQL_UPDATE_NIC = "UPDATE nics";
    private static final String SQL_VPC_ROUTER = "FROM domain_router";
    private static final String SQL_ROUTER_GUEST = "FROM nics ni JOIN";
    private static final String SQL_OFFERING_RATE = "nw_rate FROM network_offerings";
    private static final String SQL_NETWORKS = "FROM networks n";
    private static final String SQL_VPCS = "FROM vpc v";

    private static final long NIC_ID = 1L;
    private static final long NETWORK_ID = 2L;
    private static final long VM_ID = 3L;
    private static final long OFFERING_ID = 4L;
    private static final long ZONE_ID = 5L;
    private static final long SERVICE_OFFERING_ID = 6L;

    private Connection conn;
    private NetworkRateBackfill backfill;
    private final Map<String, PreparedStatement> statements = new LinkedHashMap<>();

    private MockedConstruction<VMInstanceDaoImpl> vmDaoConstruction;
    private MockedConstruction<NetworkDaoImpl> networkDaoConstruction;
    private MockedConstruction<NetworkDetailsDaoImpl> networkDetailsDaoConstruction;
    private MockedConstruction<VpcDetailsDaoImpl> vpcDetailsDaoConstruction;
    private MockedConstruction<ServiceOfferingDaoImpl> serviceOfferingDaoConstruction;
    private MockedConstruction<DataCenterDetailsDaoImpl> zoneDetailsDaoConstruction;
    private MockedConstruction<ConfigurationDaoImpl> configDaoConstruction;

    private VMInstanceDaoImpl vmDao;
    private NetworkDaoImpl networkDao;
    private NetworkDetailsDaoImpl networkDetailsDao;
    private VpcDetailsDaoImpl vpcDetailsDao;
    private ServiceOfferingDaoImpl serviceOfferingDao;
    private DataCenterDetailsDaoImpl zoneDetailsDao;
    private ConfigurationDaoImpl configDao;

    @Before
    public void setUp() throws SQLException {
        vmDaoConstruction = Mockito.mockConstruction(VMInstanceDaoImpl.class, (m, c) -> vmDao = m);
        networkDaoConstruction = Mockito.mockConstruction(NetworkDaoImpl.class, (m, c) -> networkDao = m);
        networkDetailsDaoConstruction = Mockito.mockConstruction(NetworkDetailsDaoImpl.class, (m, c) -> networkDetailsDao = m);
        vpcDetailsDaoConstruction = Mockito.mockConstruction(VpcDetailsDaoImpl.class, (m, c) -> vpcDetailsDao = m);
        serviceOfferingDaoConstruction = Mockito.mockConstruction(ServiceOfferingDaoImpl.class, (m, c) -> serviceOfferingDao = m);
        zoneDetailsDaoConstruction = Mockito.mockConstruction(DataCenterDetailsDaoImpl.class, (m, c) -> zoneDetailsDao = m);
        configDaoConstruction = Mockito.mockConstruction(ConfigurationDaoImpl.class, (m, c) -> configDao = m);

        conn = Mockito.mock(Connection.class);
        Mockito.when(conn.prepareStatement(Mockito.anyString())).thenAnswer(inv -> {
            final String sql = inv.getArgument(0);
            for (Map.Entry<String, PreparedStatement> e : statements.entrySet()) {
                if (sql.contains(e.getKey())) {
                    return e.getValue();
                }
            }
            return statement(rows());
        });
        backfill = new NetworkRateBackfill(conn);
    }

    @After
    public void tearDown() {
        vmDaoConstruction.close();
        networkDaoConstruction.close();
        networkDetailsDaoConstruction.close();
        vpcDetailsDaoConstruction.close();
        serviceOfferingDaoConstruction.close();
        zoneDetailsDaoConstruction.close();
        configDaoConstruction.close();
    }

    @SafeVarargs
    private static List<Map<String, Object>> rows(Map<String, Object>... rows) {
        return new ArrayList<>(List.of(rows));
    }

    private static Map<String, Object> row(Object... keyValues) {
        final Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static ResultSet resultSet(List<Map<String, Object>> rows) throws SQLException {
        final ResultSet rs = Mockito.mock(ResultSet.class);
        final int[] position = {-1};
        Mockito.when(rs.next()).thenAnswer(inv -> ++position[0] < rows.size());
        Mockito.when(rs.getLong(Mockito.anyString())).thenAnswer(inv -> ((Number) rows.get(position[0]).get(inv.<String>getArgument(0))).longValue());
        Mockito.when(rs.getBoolean(Mockito.anyString())).thenAnswer(inv -> (Boolean) rows.get(position[0]).get(inv.<String>getArgument(0)));
        Mockito.when(rs.getLong(Mockito.anyInt())).thenAnswer(inv -> ((Number) rows.get(position[0]).get("1")).longValue());
        Mockito.when(rs.getObject(Mockito.anyInt())).thenAnswer(inv -> rows.get(position[0]).get("1"));
        return rs;
    }

    private static PreparedStatement statement(List<Map<String, Object>> rows) throws SQLException {
        final PreparedStatement pstmt = Mockito.mock(PreparedStatement.class);
        Mockito.when(pstmt.executeQuery()).thenAnswer(inv -> resultSet(rows));
        return pstmt;
    }

    private PreparedStatement given(String sqlFragment, List<Map<String, Object>> rows) throws SQLException {
        final PreparedStatement pstmt = statement(rows);
        statements.put(sqlFragment, pstmt);
        return pstmt;
    }

    private void givenFailing(String sqlFragment) throws SQLException {
        final PreparedStatement pstmt = Mockito.mock(PreparedStatement.class);
        Mockito.when(pstmt.executeQuery()).thenThrow(new SQLException("boom"));
        statements.put(sqlFragment, pstmt);
    }

    private PreparedStatement givenNic(boolean defaultNic) throws SQLException {
        given(SQL_NICS, rows(row("id", NIC_ID, "network_id", NETWORK_ID, "instance_id", VM_ID, "default_nic", defaultNic)));
        return given(SQL_UPDATE_NIC, rows());
    }

    private NetworkVO givenNetwork(TrafficType trafficType) {
        final NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getTrafficType()).thenReturn(trafficType);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        Mockito.when(network.getDataCenterId()).thenReturn(ZONE_ID);
        Mockito.when(networkDao.findById(NETWORK_ID)).thenReturn(network);
        return network;
    }

    private VMInstanceVO givenVm(VirtualMachine.Type type) {
        final VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getType()).thenReturn(type);
        Mockito.when(vm.getId()).thenReturn(VM_ID);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(SERVICE_OFFERING_ID);
        Mockito.when(vmDao.findById(VM_ID)).thenReturn(vm);
        return vm;
    }

    private void givenNetworkOfferingRate(Integer rate) throws SQLException {
        given(SQL_OFFERING_RATE, rate == null ? rows(row("1", null)) : rows(row("1", rate)));
    }

    private void givenServiceOffering(Integer rate, String vmType) {
        final ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getRateMbps()).thenReturn(rate);
        Mockito.when(offering.getVmType()).thenReturn(vmType);
        Mockito.when(serviceOfferingDao.findById(SERVICE_OFFERING_ID)).thenReturn(offering);
    }

    private void run() {
        backfill.backfillNetworkRates();
    }

    private void verifyNicRate(PreparedStatement update, int rate) throws SQLException {
        Mockito.verify(update).setInt(1, rate);
        Mockito.verify(update).setLong(2, NIC_ID);
        Mockito.verify(update).executeUpdate();
    }

    private void verifyNoNicUpdate(PreparedStatement update) throws SQLException {
        Mockito.verify(update, Mockito.never()).executeUpdate();
    }

    @Test
    public void userVmDefaultNicUsesComputeOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.User);
        givenServiceOffering(300, "User");

        run();

        verifyNicRate(update, 300);
    }

    @Test
    public void userVmDefaultNicWithoutOfferingRateUsesVmConfigAndNormalizesZero() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.User);
        givenServiceOffering(null, "User");
        Mockito.when(configDao.getValue("vm.network.throttling.rate")).thenReturn("0");

        run();

        verifyNicRate(update, -1);
    }

    @Test
    public void routerOfferingWithoutRateUsesNetworkThrottlingConfig() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.User);
        givenServiceOffering(null, "DomainRouter");
        Mockito.when(configDao.getValue("network.throttling.rate")).thenReturn("120");

        run();

        verifyNicRate(update, 120);
    }

    @Test
    public void zoneDetailOverridesGlobalConfig() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.User);
        givenServiceOffering(null, "User");
        final DataCenterDetailVO detail = Mockito.mock(DataCenterDetailVO.class);
        Mockito.when(detail.getValue()).thenReturn("77");
        Mockito.when(zoneDetailsDao.findDetail(ZONE_ID, "vm.network.throttling.rate")).thenReturn(detail);
        Mockito.when(configDao.getValue("vm.network.throttling.rate")).thenReturn("500");

        run();

        verifyNicRate(update, 77);
    }

    @Test
    public void missingConfigFallsBackToBuiltInDefault() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.User);
        // no service offering found at all

        run();

        verifyNicRate(update, 200);
    }

    @Test
    public void userVmAdditionalNicUsesNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(false);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.User);
        givenNetworkOfferingRate(150);

        run();

        verifyNicRate(update, 150);
    }

    @Test
    public void routerGuestNicUsesNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.DomainRouter);
        givenNetworkOfferingRate(90);

        run();

        verifyNicRate(update, 90);
    }

    @Test
    public void vpcRouterPublicNicIsUnlimited() throws SQLException {
        final PreparedStatement update = givenNic(false);
        givenNetwork(TrafficType.Public);
        givenVm(VirtualMachine.Type.DomainRouter);
        given(SQL_VPC_ROUTER, rows(row("1", 1)));
        givenNetworkOfferingRate(90);

        run();

        verifyNicRate(update, -1);
    }

    @Test
    public void nonVpcRouterPublicNicUsesGuestNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(false);
        givenNetwork(TrafficType.Public);
        givenVm(VirtualMachine.Type.DomainRouter);
        given(SQL_VPC_ROUTER, rows());
        given(SQL_ROUTER_GUEST, rows(row("1", 9L)));
        givenNetworkOfferingRate(60);

        run();

        verifyNicRate(update, 60);
    }

    @Test
    public void nonVpcRouterPublicNicWithoutGuestNicUsesOwnNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(false);
        givenNetwork(TrafficType.Public);
        givenVm(VirtualMachine.Type.DomainRouter);
        given(SQL_VPC_ROUTER, rows());
        given(SQL_ROUTER_GUEST, rows());
        givenNetworkOfferingRate(45);

        run();

        verifyNicRate(update, 45);
    }

    @Test
    public void vpcCheckFailureTreatsRouterAsNonVpc() throws SQLException {
        final PreparedStatement update = givenNic(false);
        givenNetwork(TrafficType.Public);
        givenVm(VirtualMachine.Type.DomainRouter);
        givenFailing(SQL_VPC_ROUTER);
        given(SQL_ROUTER_GUEST, rows(row("1", 9L)));
        givenNetworkOfferingRate(60);

        run();

        verifyNicRate(update, 60);
    }

    @Test
    public void routerGuestLookupFailureFallsBackToOwnNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(false);
        givenNetwork(TrafficType.Public);
        givenVm(VirtualMachine.Type.DomainRouter);
        given(SQL_VPC_ROUTER, rows());
        givenFailing(SQL_ROUTER_GUEST);
        givenNetworkOfferingRate(45);

        run();

        verifyNicRate(update, 45);
    }

    @Test
    public void consoleProxyNicIsUnlimited() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Management);
        givenVm(VirtualMachine.Type.ConsoleProxy);

        run();

        verifyNicRate(update, -1);
    }

    @Test
    public void secondaryStorageVmNicIsUnlimited() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Management);
        givenVm(VirtualMachine.Type.SecondaryStorageVm);

        run();

        verifyNicRate(update, -1);
    }

    @Test
    public void nicOfUnknownVmUsesNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenNetworkOfferingRate(25);

        run();

        verifyNicRate(update, 25);
    }

    @Test
    public void nicOfOtherVmTypeUsesNetworkOfferingRate() throws SQLException {
        final PreparedStatement update = givenNic(true);
        givenNetwork(TrafficType.Guest);
        givenVm(VirtualMachine.Type.Instance);
        givenNetworkOfferingRate(25);

        run();

        verifyNicRate(update, 25);
    }

    @Test
    public void nicOfUnknownNetworkIsSkipped() throws SQLException {
        final PreparedStatement update = givenNic(true);

        run();

        verifyNoNicUpdate(update);
    }

    @Test
    public void failureOnOneNicDoesNotStopTheOthers() throws SQLException {
        given(SQL_NICS, rows(
                row("id", 10L, "network_id", 20L, "instance_id", VM_ID, "default_nic", true),
                row("id", NIC_ID, "network_id", NETWORK_ID, "instance_id", VM_ID, "default_nic", false)));
        final PreparedStatement update = given(SQL_UPDATE_NIC, rows());
        Mockito.when(networkDao.findById(20L)).thenThrow(new RuntimeException("boom"));
        givenNetwork(TrafficType.Guest);
        givenNetworkOfferingRate(150);

        run();

        verifyNicRate(update, 150);
    }

    @Test
    public void nicQueryFailureIsSwallowed() throws SQLException {
        givenFailing(SQL_NICS);

        run();

        Mockito.verifyNoInteractions(networkDao);
    }

    @Test
    public void networkDetailRateIsBackfilledFromOfferingRate() throws SQLException {
        given(SQL_NETWORKS, rows(row("id", NETWORK_ID, "network_offering_id", OFFERING_ID, "data_center_id", ZONE_ID)));
        givenNetworkOfferingRate(150);

        run();

        Mockito.verify(networkDetailsDao).addDetail(NETWORK_ID, "networkrate", "150", true);
    }

    @Test
    public void networkDetailRateFallsBackToZoneConfig() throws SQLException {
        given(SQL_NETWORKS, rows(row("id", NETWORK_ID, "network_offering_id", OFFERING_ID, "data_center_id", ZONE_ID)));
        givenNetworkOfferingRate(null);
        Mockito.when(configDao.getValue("network.throttling.rate")).thenReturn("0");

        run();

        Mockito.verify(networkDetailsDao).addDetail(NETWORK_ID, "networkrate", "-1", true);
    }

    @Test
    public void networkDetailFailureIsSwallowed() throws SQLException {
        given(SQL_NETWORKS, rows(
                row("id", 7L, "network_offering_id", OFFERING_ID, "data_center_id", ZONE_ID),
                row("id", NETWORK_ID, "network_offering_id", OFFERING_ID, "data_center_id", ZONE_ID)));
        givenNetworkOfferingRate(150);
        Mockito.doThrow(new RuntimeException("boom")).when(networkDetailsDao).addDetail(7L, "networkrate", "150", true);

        run();

        Mockito.verify(networkDetailsDao).addDetail(NETWORK_ID, "networkrate", "150", true);
    }

    @Test
    public void networkDetailQueryFailureIsSwallowed() throws SQLException {
        givenFailing(SQL_NETWORKS);

        run();

        Mockito.verifyNoInteractions(networkDetailsDao);
    }

    @Test
    public void offeringRateQueryFailureFallsBackToZoneConfig() throws SQLException {
        given(SQL_NETWORKS, rows(row("id", NETWORK_ID, "network_offering_id", OFFERING_ID, "data_center_id", ZONE_ID)));
        givenFailing(SQL_OFFERING_RATE);
        Mockito.when(configDao.getValue("network.throttling.rate")).thenReturn("33");

        run();

        Mockito.verify(networkDetailsDao).addDetail(NETWORK_ID, "networkrate", "33", true);
    }

    @Test
    public void vpcPublicRateIsBackfilledAsUnlimited() throws SQLException {
        given(SQL_VPCS, rows(row("id", 11L), row("id", 12L)));

        run();

        Mockito.verify(vpcDetailsDao).addDetail(11L, "publicnetworkrate", "-1", true);
        Mockito.verify(vpcDetailsDao).addDetail(12L, "publicnetworkrate", "-1", true);
    }

    @Test
    public void vpcDetailFailureIsSwallowed() throws SQLException {
        given(SQL_VPCS, rows(row("id", 11L), row("id", 12L)));
        Mockito.doThrow(new RuntimeException("boom")).when(vpcDetailsDao).addDetail(11L, "publicnetworkrate", "-1", true);

        run();

        Mockito.verify(vpcDetailsDao).addDetail(12L, "publicnetworkrate", "-1", true);
    }

    @Test
    public void vpcQueryFailureIsSwallowed() throws SQLException {
        givenFailing(SQL_VPCS);

        run();

        Mockito.verifyNoInteractions(vpcDetailsDao);
    }
}
