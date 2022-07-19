//
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
//
package com.cloud.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.BatchPoints.Builder;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.server.StatsCollector.ExternalStatsProtocol;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VmStats;
import com.cloud.vm.VmStatsVO;
import com.cloud.vm.dao.VmStatsDao;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(DataProviderRunner.class)
@PrepareForTest({InfluxDBFactory.class, BatchPoints.class})
public class StatsCollectorTest {

    @InjectMocks
    private StatsCollector statsCollector = Mockito.spy(new StatsCollector());

    private static final int GRAPHITE_DEFAULT_PORT = 2003;
    private static final int INFLUXDB_DEFAULT_PORT = 8086;
    private static final String HOST_ADDRESS = "192.168.16.10";
    private static final String URL = String.format("http://%s:%s/", HOST_ADDRESS, INFLUXDB_DEFAULT_PORT);

    private static final String DEFAULT_DATABASE_NAME = "cloudstack";

    @Mock
    VmStatsDao vmStatsDaoMock;

    @Mock
    VmStatsEntry statsForCurrentIterationMock;

    @Captor
    ArgumentCaptor<VmStatsVO> vmStatsVOCaptor;

    @Captor
    ArgumentCaptor<Boolean> booleanCaptor;

    @Mock
    Boolean accumulateMock;

    @Mock
    VmStatsVO vmStatsVoMock1, vmStatsVoMock2;

    @Mock
    VmStatsEntry vmStatsEntryMock;

    @Test
    public void createInfluxDbConnectionTest() {
        configureAndTestCreateInfluxDbConnection(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void createInfluxDbConnectionTestExpectException() {
        configureAndTestCreateInfluxDbConnection(false);
    }

    private void configureAndTestCreateInfluxDbConnection(boolean databaseExists) {
        statsCollector.externalStatsHost = HOST_ADDRESS;
        statsCollector.externalStatsPort = INFLUXDB_DEFAULT_PORT;
        InfluxDB influxDbConnection = Mockito.mock(InfluxDB.class);
        when(influxDbConnection.databaseExists(DEFAULT_DATABASE_NAME)).thenReturn(databaseExists);
        PowerMockito.mockStatic(InfluxDBFactory.class);
        PowerMockito.when(InfluxDBFactory.connect(URL)).thenReturn(influxDbConnection);

        InfluxDB returnedConnection = statsCollector.createInfluxDbConnection();

        Assert.assertEquals(influxDbConnection, returnedConnection);
    }

    @Test
    public void writeBatchesTest() {
        InfluxDB influxDbConnection = Mockito.mock(InfluxDB.class);
        Mockito.doNothing().when(influxDbConnection).write(Mockito.any(Point.class));
        Builder builder = Mockito.mock(Builder.class);
        BatchPoints batchPoints = Mockito.mock(BatchPoints.class);
        PowerMockito.mockStatic(BatchPoints.class);
        PowerMockito.when(BatchPoints.database(DEFAULT_DATABASE_NAME)).thenReturn(builder);
        when(builder.build()).thenReturn(batchPoints);
        Map<String, String> tagsToAdd = new HashMap<>();
        tagsToAdd.put("hostId", "1");
        Map<String, Object> fieldsToAdd = new HashMap<>();
        fieldsToAdd.put("total_memory_kbs", 10000000);
        Point point = Point.measurement("measure").tag(tagsToAdd).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).fields(fieldsToAdd).build();
        List<Point> points = new ArrayList<>();
        points.add(point);
        when(batchPoints.point(point)).thenReturn(batchPoints);

        statsCollector.writeBatches(influxDbConnection, DEFAULT_DATABASE_NAME, points);

        Mockito.verify(influxDbConnection).write(batchPoints);
    }

    @Test
    public void configureExternalStatsPortTestGraphitePort() throws URISyntaxException {
        URI uri = new URI(HOST_ADDRESS);
        statsCollector.externalStatsType = ExternalStatsProtocol.GRAPHITE;
        int port = statsCollector.retrieveExternalStatsPortFromUri(uri);
        Assert.assertEquals(GRAPHITE_DEFAULT_PORT, port);
    }

    @Test
    public void configureExternalStatsPortTestInfluxdbPort() throws URISyntaxException {
        URI uri = new URI(HOST_ADDRESS);
        statsCollector.externalStatsType = ExternalStatsProtocol.INFLUXDB;
        int port = statsCollector.retrieveExternalStatsPortFromUri(uri);
        Assert.assertEquals(INFLUXDB_DEFAULT_PORT, port);
    }

    @Test(expected = URISyntaxException.class)
    public void configureExternalStatsPortTestExpectException() throws URISyntaxException {
        statsCollector.externalStatsType = ExternalStatsProtocol.NONE;
        URI uri = new URI(HOST_ADDRESS);
        statsCollector.retrieveExternalStatsPortFromUri(uri);
    }

    @Test
    public void configureExternalStatsPortTestInfluxDbCustomizedPort() throws URISyntaxException {
        statsCollector.externalStatsType = ExternalStatsProtocol.INFLUXDB;
        URI uri = new URI("test://" + HOST_ADDRESS + ":1234");
        int port = statsCollector.retrieveExternalStatsPortFromUri(uri);
        Assert.assertEquals(1234, port);
    }

    @Test
    public void configureDatabaseNameTestDefaultDbName() throws URISyntaxException {
        URI uri = new URI(URL);
        String dbName = statsCollector.configureDatabaseName(uri);
        Assert.assertEquals(DEFAULT_DATABASE_NAME, dbName);
    }

    @Test
    public void configureDatabaseNameTestCustomDbName() throws URISyntaxException {
        String configuredDbName = "dbName";
        URI uri = new URI(URL + configuredDbName);
        String dbName = statsCollector.configureDatabaseName(uri);
        Assert.assertEquals(configuredDbName, dbName);
    }

    @Test
    public void isCurrentVmDiskStatsDifferentFromPreviousTestNull() {
        VmDiskStatisticsVO currentVmDiskStatisticsVO = new VmDiskStatisticsVO(1l, 1l, 1l, 1l);
        boolean result = statsCollector.isCurrentVmDiskStatsDifferentFromPrevious(null, currentVmDiskStatisticsVO);
        Assert.assertTrue(result);
    }

    @Test
    public void isCurrentVmDiskStatsDifferentFromPreviousTestDifferentIoWrite() {
        configureAndTestisCurrentVmDiskStatsDifferentFromPrevious(123l, 123l, 123l, 12l, true);
    }

    @Test
    public void isCurrentVmDiskStatsDifferentFromPreviousTestDifferentIoRead() {
        configureAndTestisCurrentVmDiskStatsDifferentFromPrevious(123l, 123l, 12l, 123l, true);
    }

    @Test
    public void isCurrentVmDiskStatsDifferentFromPreviousTestDifferentBytesRead() {
        configureAndTestisCurrentVmDiskStatsDifferentFromPrevious(12l, 123l, 123l, 123l, true);
    }

    @Test
    public void isCurrentVmDiskStatsDifferentFromPreviousTestDifferentBytesWrite() {
        configureAndTestisCurrentVmDiskStatsDifferentFromPrevious(123l, 12l, 123l, 123l, true);
    }

    @Test
    public void isCurrentVmDiskStatsDifferentFromPreviousTestAllEqual() {
        configureAndTestisCurrentVmDiskStatsDifferentFromPrevious(123l, 123l, 123l, 123l, false);
    }

    private void configureAndTestisCurrentVmDiskStatsDifferentFromPrevious(long bytesRead, long bytesWrite, long ioRead, long ioWrite, boolean expectedResult) {
        VmDiskStatisticsVO previousVmDiskStatisticsVO = new VmDiskStatisticsVO(1l, 1l, 1l, 1l);
        previousVmDiskStatisticsVO.setCurrentBytesRead(123l);
        previousVmDiskStatisticsVO.setCurrentBytesWrite(123l);
        previousVmDiskStatisticsVO.setCurrentIORead(123l);
        previousVmDiskStatisticsVO.setCurrentIOWrite(123l);

        VmDiskStatisticsVO currentVmDiskStatisticsVO = new VmDiskStatisticsVO(1l, 1l, 1l, 1l);
        currentVmDiskStatisticsVO.setCurrentBytesRead(bytesRead);
        currentVmDiskStatisticsVO.setCurrentBytesWrite(bytesWrite);
        currentVmDiskStatisticsVO.setCurrentIORead(ioRead);
        currentVmDiskStatisticsVO.setCurrentIOWrite(ioWrite);

        boolean result = statsCollector.isCurrentVmDiskStatsDifferentFromPrevious(previousVmDiskStatisticsVO, currentVmDiskStatisticsVO);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @DataProvider({
        "0,0,0,0,true", "1,0,0,0,false", "0,1,0,0,false", "0,0,1,0,false",
        "0,0,0,1,false", "1,0,0,1,false", "1,0,1,0,false", "1,1,0,0,false",
        "0,1,1,0,false", "0,1,0,1,false", "0,0,1,1,false", "0,1,1,1,false",
        "1,1,0,1,false", "1,0,1,1,false", "1,1,1,0,false", "1,1,1,1,false",
    })
    public void configureAndTestCheckIfDiskStatsAreZero(long bytesRead, long bytesWrite, long ioRead, long ioWrite, boolean expected) {
        VmDiskStatsEntry vmDiskStatsEntry = new VmDiskStatsEntry();
        vmDiskStatsEntry.setBytesRead(bytesRead);
        vmDiskStatsEntry.setBytesWrite(bytesWrite);
        vmDiskStatsEntry.setIORead(ioRead);
        vmDiskStatsEntry.setIOWrite(ioWrite);

        boolean result = statsCollector.areAllDiskStatsZero(vmDiskStatsEntry);
        Assert.assertEquals(expected, result);
    }

    private void setVmStatsIncrementMetrics(String value) {
        StatsCollector.vmStatsIncrementMetrics = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.stats.increment.metrics", value,
                "When set to 'true', VM metrics(NetworkReadKBs, NetworkWriteKBs, DiskWriteKBs, DiskReadKBs, DiskReadIOs and DiskWriteIOs) that are collected from the hypervisor are summed before being returned. "
                        + "On the other hand, when set to 'false', the VM metrics API will just display the latest metrics collected.", true);
    }

    private void setVmStatsMaxRetentionTimeValue(String value) {
        StatsCollector.vmStatsMaxRetentionTime = new ConfigKey<Integer>("Advanced", Integer.class, "vm.stats.max.retention.time", value,
                "The maximum time (in minutes) for keeping VM stats records in the database. The VM stats cleanup process will be disabled if this is set to 0 or less than 0.", true);
    }

    @Test
    public void cleanUpVirtualMachineStatsTestIsDisabled() {
        setVmStatsMaxRetentionTimeValue("0");

        statsCollector.cleanUpVirtualMachineStats();

        Mockito.verify(vmStatsDaoMock, Mockito.never()).removeAllByTimestampLessThan(Mockito.any());
    }

    @Test
    public void cleanUpVirtualMachineStatsTestIsEnabled() {
        setVmStatsMaxRetentionTimeValue("1");

        statsCollector.cleanUpVirtualMachineStats();

        Mockito.verify(vmStatsDaoMock).removeAllByTimestampLessThan(Mockito.any());
    }

    @Test
    public void persistVirtualMachineStatsTestPersistsSuccessfully() {
        statsCollector.msId = 1L;
        Date timestamp = new Date();
        VmStatsEntry statsForCurrentIteration = new VmStatsEntry(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, "vm");
        Mockito.doReturn(new VmStatsVO()).when(vmStatsDaoMock).persist(Mockito.any());
        String expectedVmStatsStr = "{\"vmId\":2,\"cpuUtilization\":6.0,\"networkReadKBs\":7.0,\"networkWriteKBs\":8.0,\"diskReadIOs\":12.0,\"diskWriteIOs\":13.0,\"diskReadKBs\":10.0"
                + ",\"diskWriteKBs\":11.0,\"memoryKBs\":3.0,\"intFreeMemoryKBs\":4.0,\"targetMemoryKBs\":5.0,\"numCPUs\":9,\"entityType\":\"vm\"}";

        statsCollector.persistVirtualMachineStats(statsForCurrentIteration, timestamp);

        Mockito.verify(vmStatsDaoMock).persist(vmStatsVOCaptor.capture());
        VmStatsVO actual = vmStatsVOCaptor.getAllValues().get(0);
        Assert.assertEquals(Long.valueOf(2L), actual.getVmId());
        Assert.assertEquals(Long.valueOf(1L), actual.getMgmtServerId());
        Assert.assertEquals(expectedVmStatsStr, actual.getVmStatsData());
        Assert.assertEquals(timestamp, actual.getTimestamp());
    }

    @Test
    public void getVmStatsTestWithAccumulateNotNull() {
        Mockito.doReturn(Arrays.asList(vmStatsVoMock1)).when(vmStatsDaoMock).findByVmIdOrderByTimestampDesc(Mockito.anyLong());
        Mockito.doReturn(true).when(accumulateMock).booleanValue();
        Mockito.doReturn(vmStatsEntryMock).when(statsCollector).getLatestOrAccumulatedVmMetricsStats(Mockito.anyList(), Mockito.anyBoolean());

        VmStats result = statsCollector.getVmStats(1L, accumulateMock);

        Mockito.verify(statsCollector).getLatestOrAccumulatedVmMetricsStats(Mockito.anyList(), booleanCaptor.capture());
        boolean actualArg = booleanCaptor.getValue().booleanValue();
        Assert.assertEquals(false, actualArg);
        Assert.assertEquals(vmStatsEntryMock, result);
    }

    @Test
    public void getVmStatsTestWithNullAccumulate() {
        setVmStatsIncrementMetrics("true");
        Mockito.doReturn(Arrays.asList(vmStatsVoMock1)).when(vmStatsDaoMock).findByVmIdOrderByTimestampDesc(Mockito.anyLong());
        Mockito.doReturn(vmStatsEntryMock).when(statsCollector).getLatestOrAccumulatedVmMetricsStats(Mockito.anyList(), Mockito.anyBoolean());

        VmStats result = statsCollector.getVmStats(1L, null);

        Mockito.verify(statsCollector).getLatestOrAccumulatedVmMetricsStats(Mockito.anyList(), booleanCaptor.capture());
        boolean actualArg = booleanCaptor.getValue().booleanValue();
        Assert.assertEquals(true, actualArg);
        Assert.assertEquals(vmStatsEntryMock, result);
    }

    @Test
    public void getLatestOrAccumulatedVmMetricsStatsTestAccumulate() {
        Mockito.doReturn(null).when(statsCollector).accumulateVmMetricsStats(Mockito.anyList());

        statsCollector.getLatestOrAccumulatedVmMetricsStats(Arrays.asList(vmStatsVoMock1), true);

        Mockito.verify(statsCollector).accumulateVmMetricsStats(Mockito.anyList());
    }

    @Test
    public void getLatestOrAccumulatedVmMetricsStatsTestLatest() {
        statsCollector.getLatestOrAccumulatedVmMetricsStats(Arrays.asList(vmStatsVoMock1), false);

        Mockito.verify(statsCollector, Mockito.never()).accumulateVmMetricsStats(Mockito.anyList());
    }

    @Test
    public void accumulateVmMetricsStatsTest() {
        String fakeStatsData1 = "{\"vmId\":1,\"cpuUtilization\":1.0,\"networkReadKBs\":1.0,"
                + "\"networkWriteKBs\":1.1,\"diskReadIOs\":3.0,\"diskWriteIOs\":3.1,\"diskReadKBs\":2.0,"
                + "\"diskWriteKBs\":2.1,\"memoryKBs\":1.0,\"intFreeMemoryKBs\":1.0,"
                + "\"targetMemoryKBs\":1.0,\"numCPUs\":1,\"entityType\":\"vm\"}";
        String fakeStatsData2 = "{\"vmId\":1,\"cpuUtilization\":10.0,\"networkReadKBs\":1.0,"
                + "\"networkWriteKBs\":1.1,\"diskReadIOs\":3.0,\"diskWriteIOs\":3.1,\"diskReadKBs\":2.0,"
                + "\"diskWriteKBs\":2.1,\"memoryKBs\":1.0,\"intFreeMemoryKBs\":1.0,"
                + "\"targetMemoryKBs\":1.0,\"numCPUs\":1,\"entityType\":\"vm\"}";
        Mockito.doReturn(fakeStatsData1).when(vmStatsVoMock1).getVmStatsData();
        Mockito.doReturn(fakeStatsData2).when(vmStatsVoMock2).getVmStatsData();

        VmStatsEntry result = statsCollector.accumulateVmMetricsStats(new ArrayList<VmStatsVO>(
                Arrays.asList(vmStatsVoMock1, vmStatsVoMock2)));

        Assert.assertEquals("vm", result.getEntityType());
        Assert.assertEquals(1, result.getVmId());
        Assert.assertEquals(1.0, result.getCPUUtilization(), 0);
        Assert.assertEquals(1, result.getNumCPUs());
        Assert.assertEquals(1.0, result.getMemoryKBs(), 0);
        Assert.assertEquals(1.0, result.getIntFreeMemoryKBs(), 0);
        Assert.assertEquals(1.0, result.getTargetMemoryKBs(), 0);
        Assert.assertEquals(2.0, result.getNetworkReadKBs(), 0);
        Assert.assertEquals(2.2, result.getNetworkWriteKBs(), 0);
        Assert.assertEquals(4.0, result.getDiskReadKBs(), 0);
        Assert.assertEquals(4.2, result.getDiskWriteKBs(), 0);
        Assert.assertEquals(6.0, result.getDiskReadIOs(), 0);
        Assert.assertEquals(6.2, result.getDiskWriteIOs(), 0);
    }

    @Test
    public void testIsDbIpv6Local() {
        Properties p = new Properties();
        p.put("db.cloud.host", "::1");
        when(statsCollector.getDbProperties()).thenReturn(p);

        Assert.assertTrue(statsCollector.isDbLocal());
    }
    @Test
    public void testIsDbIpv4Local() {
        Properties p = new Properties();
        p.put("db.cloud.host", "127.0.0.1");
        when(statsCollector.getDbProperties()).thenReturn(p);

        Assert.assertTrue(statsCollector.isDbLocal());
    }
    @Test
    public void testIsDbSymbolicLocal() {
        Properties p = new Properties();
        p.put("db.cloud.host", "localhost");
        when(statsCollector.getDbProperties()).thenReturn(p);

        Assert.assertTrue(statsCollector.isDbLocal());
    }
    @Test
    public void testIsDbOnSameIp() {
        Properties p = new Properties();
        p.put("db.cloud.host", "10.10.10.10");
        p.put("cluster.node.IP", "10.10.10.10");
        when(statsCollector.getDbProperties()).thenReturn(p);

        Assert.assertTrue(statsCollector.isDbLocal());
    }
    @Test
    public void testIsDbNotLocal() {
        Properties p = new Properties();
        p.put("db.cloud.host", "10.10.10.11");
        p.put("cluster.node.IP", "10.10.10.10");
        when(statsCollector.getDbProperties()).thenReturn(p);

        Assert.assertFalse(statsCollector.isDbLocal());
    }
}
