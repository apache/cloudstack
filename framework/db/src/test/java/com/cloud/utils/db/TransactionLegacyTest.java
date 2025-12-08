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
package com.cloud.utils.db;

import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class TransactionLegacyTest {

    Properties properties;

    @Before
    public void setup(){
        properties = new Properties();
        properties.setProperty("db.cloud.host", "host");
        properties.setProperty("db.cloud.port", "5555");
        properties.setProperty("db.cloud.name", "name");
        properties.setProperty("db.cloud.autoReconnect", "false");
        properties.setProperty("db.cloud.url.params", "someParams");
        TransactionLegacy.s_dbHAEnabled = false;
    }
    @Test
    public void getConnectionUriAndDriverTestWithoutUri() {
        properties.setProperty("db.cloud.uri", "");
        properties.setProperty("db.cloud.driver", "driver");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("driver://host:5555/name?autoReconnect=false&someParams", result.first());
        Assert.assertEquals("driver", result.second());
    }

    @Test
    public void getConnectionUriAndDriverTestWithUri() {
        properties.setProperty("db.cloud.uri", "jdbc:driver:myFavoriteUri");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("jdbc:driver:myFavoriteUri", result.first());
        Assert.assertEquals("jdbc:driver", result.second());
    }

    @Test
    public void getPropertiesAndBuildConnectionUriTestDbHaDisabled() {
        String result = TransactionLegacy.getPropertiesAndBuildConnectionUri(properties, "strat", "driver", true, "cloud");

        Assert.assertEquals("driver://host:5555/name?autoReconnect=false&someParams&useSSL=true", result);
    }

    @Test
    public void getPropertiesAndBuildConnectionUriTestDbHaEnabled() {
        TransactionLegacy.s_dbHAEnabled = true;
        properties.setProperty("db.cloud.failOverReadOnly", "true");
        properties.setProperty("db.cloud.reconnectAtTxEnd", "false");
        properties.setProperty("db.cloud.autoReconnectForPools", "true");
        properties.setProperty("db.cloud.secondsBeforeRetrySource", "25");
        properties.setProperty("db.cloud.queriesBeforeRetrySource", "105");
        properties.setProperty("db.cloud.initialTimeout", "1000");
        properties.setProperty("db.cloud.replicas", "second_host");

        String result = TransactionLegacy.getPropertiesAndBuildConnectionUri(properties, "strat", "driver", true, "cloud");

        Assert.assertEquals("driver://host,second_host:5555/name?autoReconnect=false&someParams&useSSL=true&failOverReadOnly=true&reconnectAtTxEnd=false&autoReconnectFor"
                + "Pools=true&secondsBeforeRetrySource=25&queriesBeforeRetrySource=105&initialTimeout=1000&loadBalanceStrategy=strat", result);
    }

    @Test
    public void buildConnectionUriTestDbHaDisabled() {
        String result = TransactionLegacy.buildConnectionUri(null, "driver", false, "host", null, 5555, "cloud", false, null, null);

        Assert.assertEquals("driver://host:5555/cloud?autoReconnect=false", result);
    }

    @Test
    public void buildConnectionUriTestDbHaEnabled() {
        TransactionLegacy.s_dbHAEnabled = true;

        String result = TransactionLegacy.buildConnectionUri("strat", "driver", false, "host", "second_host", 5555, "cloud", false, null, "dbHaParams");

        Assert.assertEquals("driver://host,second_host:5555/cloud?autoReconnect=false&dbHaParams&loadBalanceStrategy=strat", result);
    }

    @Test
    public void buildConnectionUriTestUrlParamsNotNull() {
        String result = TransactionLegacy.buildConnectionUri(null, "driver", false, "host", null, 5555, "cloud", false, "urlParams", null);

        Assert.assertEquals("driver://host:5555/cloud?autoReconnect=false&urlParams", result);
    }

    @Test
    public void buildConnectionUriTestUseSslTrue() {
        String result = TransactionLegacy.buildConnectionUri(null, "driver", true, "host", null, 5555, "cloud", false, null, null);

        Assert.assertEquals("driver://host:5555/cloud?autoReconnect=false&useSSL=true", result);
    }
}
