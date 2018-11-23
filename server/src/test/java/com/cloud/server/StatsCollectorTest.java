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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.BatchPoints.Builder;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.HostStatsEntry;
import com.cloud.host.HostVO;
import com.cloud.server.StatsCollector.ExternalStatsProtocol;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InfluxDBFactory.class, BatchPoints.class})
public class StatsCollectorTest {
    private StatsCollector statsCollector = Mockito.spy(new StatsCollector());

    private static final int GRAPHITE_DEFAULT_PORT = 2003;
    private static final int INFLUXDB_DEFAULT_PORT = 8086;
    private static final String HOST_ADDRESS = "192.168.16.10";
    private static final String URL = String.format("http://%s:%s/", HOST_ADDRESS, INFLUXDB_DEFAULT_PORT);

    private static final String DEFAULT_DATABASE_NAME = "cloudstack";
    private static final String INFLUXDB_HOST_MEASUREMENT = "host_stats";
    private static final String INFLUXDB_VM_MEASUREMENT = "vm_stats";

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
        InfluxDB influxDbConnection = Mockito.mock(InfluxDB.class);
        Mockito.when(influxDbConnection.databaseExists(DEFAULT_DATABASE_NAME)).thenReturn(databaseExists);
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
        Mockito.when(builder.build()).thenReturn(batchPoints);
        Map<String, String> tagsToAdd = new HashMap<>();
        tagsToAdd.put("hostId", "1");
        Map<String, Object> fieldsToAdd = new HashMap<>();
        fieldsToAdd.put("total_memory_kbs", 10000000);
        Point point = Point.measurement("measure").tag(tagsToAdd).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS).fields(fieldsToAdd).build();
        List<Point> points = new ArrayList<>();
        points.add(point);
        Mockito.when(batchPoints.point(point)).thenReturn(batchPoints);

        statsCollector.writeBatches(influxDbConnection, DEFAULT_DATABASE_NAME, points);

        Mockito.verify(influxDbConnection).write(batchPoints);
    }

    @Test
    public void sendVmMetricsToExternalStatsCollectorTestVmStatsMetricsEmpty() {
        configureAndTestSendVmMetricsToExternalStatsCollector(new HashMap<>(), ExternalStatsProtocol.INFLUXDB, 0, 0);
    }

    @Test
    public void sendVmMetricsToExternalStatsCollectorTestVmStatsMetricsInfluxdb() {
        Map<Object, Object> metrics = new HashMap<>();
        metrics.put(0l, 0l);
        configureAndTestSendVmMetricsToExternalStatsCollector(metrics, ExternalStatsProtocol.INFLUXDB, 0, 1);
    }

    @Test
    public void sendVmMetricsToExternalStatsCollectorTestVmStatsMetricsGraphit() {
        Map<Object, Object> metrics = new HashMap<>();
        metrics.put(0l, 0l);
        configureAndTestSendVmMetricsToExternalStatsCollector(metrics, ExternalStatsProtocol.GRAPHITE, 1, 0);
    }

    private void configureAndTestSendVmMetricsToExternalStatsCollector(Map<Object, Object> metrics, ExternalStatsProtocol externalStatsType, int timesGraphite, int timesInflux) {
        HostVO host = new HostVO("guid");
        statsCollector.externalStatsType = externalStatsType;
        Mockito.doNothing().when(statsCollector).sendVmMetricsToGraphiteHost(Mockito.anyMap(), Mockito.any());
//        Mockito.doNothing().when(statsCollector).sendMetricsToInfluxdb(Mockito.anyMap(), Mockito.anyString());

//        statsCollector.sendVmMetricsToExternalStatsCollector(metrics, host); TODO review

        Mockito.verify(statsCollector, Mockito.times(timesGraphite)).sendVmMetricsToGraphiteHost(metrics, host);
//        Mockito.verify(statsCollector, Mockito.times(timesInflux)).sendMetricsToInfluxdb(metrics, "abc");
    }

    @Test
    public void configureExternalStatsPortTestGraphitePort() throws URISyntaxException {
        URI uri = new URI(HOST_ADDRESS);
        statsCollector.externalStatsType = ExternalStatsProtocol.GRAPHITE;
        int port = statsCollector.configureExternalStatsPort(uri);
        Assert.assertEquals(GRAPHITE_DEFAULT_PORT, port);
    }

    @Test
    public void configureExternalStatsPortTestInfluxdbPort() throws URISyntaxException {
        URI uri = new URI(HOST_ADDRESS);
        statsCollector.externalStatsType = ExternalStatsProtocol.INFLUXDB;
        int port = statsCollector.configureExternalStatsPort(uri);
        Assert.assertEquals(INFLUXDB_DEFAULT_PORT, port);
    }

    @Test(expected = CloudRuntimeException.class)
    public void configureExternalStatsPortTestExpectException() throws URISyntaxException {
        statsCollector.externalStatsType = ExternalStatsProtocol.NONE;
        URI uri = new URI(HOST_ADDRESS);
        statsCollector.configureExternalStatsPort(uri);
    }

    @Test
    public void configureExternalStatsPortTestCustomized() throws URISyntaxException {
        URI uri = new URI("test://" + HOST_ADDRESS + ":1234");
        int port = statsCollector.configureExternalStatsPort(uri);
        Assert.assertEquals(1234, port);
    }

    @Test
    public void sendMetricsToInfluxdbTestHostMeasure() {
        configureTestAndVerifySendMetricsToInfluxdb(2, 0, INFLUXDB_HOST_MEASUREMENT);
    }

    @Test
    public void sendMetricsToInfluxdbTestVmMeasure() {
        configureTestAndVerifySendMetricsToInfluxdb(0, 2, INFLUXDB_VM_MEASUREMENT);
    }

    private void configureTestAndVerifySendMetricsToInfluxdb(int timesPointForHost, int timesPointForVm, String measure) {
        HostStatsEntry hostStatsEntry = Mockito.mock(HostStatsEntry.class);
        Point point = Mockito.mock(Point.class);
        List<Point> points = new ArrayList<>();
        points.add(point);
        points.add(point);
        InfluxDB influxDbConnection = Mockito.mock(InfluxDB.class);
        Map<Object, Object> metrics = new HashMap<>();
        metrics.put(0l, hostStatsEntry);
        metrics.put(1l, hostStatsEntry);

        Mockito.doReturn(point).when(statsCollector).createInfluxDbPointForHostMetrics(Mockito.any());
        Mockito.doReturn(point).when(statsCollector).createInfluxDbPointForVmMetrics(Mockito.any());
        Mockito.doReturn(influxDbConnection).when(statsCollector).createInfluxDbConnection();
        Mockito.doNothing().when(statsCollector).writeBatches(influxDbConnection, DEFAULT_DATABASE_NAME, points);

//        statsCollector.sendMetricsToInfluxdb(metrics, measure); TODO review

        InOrder inOrder = Mockito.inOrder(statsCollector);
        inOrder.verify(statsCollector, Mockito.times(1)).createInfluxDbConnection();
        inOrder.verify(statsCollector, Mockito.times(timesPointForHost)).createInfluxDbPointForHostMetrics(Mockito.any());
        inOrder.verify(statsCollector, Mockito.times(timesPointForVm)).createInfluxDbPointForVmMetrics(Mockito.any());
        inOrder.verify(statsCollector, Mockito.times(1)).writeBatches(influxDbConnection, DEFAULT_DATABASE_NAME, points);
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
}
