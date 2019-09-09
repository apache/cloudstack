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
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.server.StatsCollector.ExternalStatsProtocol;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(DataProviderRunner.class)
@PrepareForTest({InfluxDBFactory.class, BatchPoints.class})
public class StatsCollectorTest {
    private StatsCollector statsCollector = Mockito.spy(new StatsCollector());

    private static final int GRAPHITE_DEFAULT_PORT = 2003;
    private static final int INFLUXDB_DEFAULT_PORT = 8086;
    private static final String HOST_ADDRESS = "192.168.16.10";
    private static final String URL = String.format("http://%s:%s/", HOST_ADDRESS, INFLUXDB_DEFAULT_PORT);

    private static final String DEFAULT_DATABASE_NAME = "cloudstack";

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
    public void isCurrentVmDiskStatsDifferentFromPreviousTestBothNull() {
        boolean result = statsCollector.isCurrentVmDiskStatsDifferentFromPrevious(null, null);
        Assert.assertFalse(result);
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
}
