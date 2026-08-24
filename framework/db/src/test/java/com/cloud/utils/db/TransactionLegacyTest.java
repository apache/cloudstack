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

        Assert.assertEquals("driver://host:5555/name?autoReconnect=false&someParams&scrollTolerantForwardOnly=true"
                + "&connectionCollation=utf8mb4_general_ci", result.first());
        Assert.assertEquals("driver", result.second());
    }

    @Test
    public void getConnectionUriAndDriverTestWithUri() {
        properties.setProperty("db.cloud.uri", "jdbc:driver:myFavoriteUri");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("jdbc:driver:myFavoriteUri?connectionCollation=utf8mb4_general_ci", result.first());
        Assert.assertEquals("jdbc:driver", result.second());
    }

    @Test
    public void getPropertiesAndBuildConnectionUriTestDbHaDisabled() {
        String result = TransactionLegacy.getPropertiesAndBuildConnectionUri(properties, "strat", "driver", true, "cloud");

        Assert.assertEquals("driver://host:5555/name?autoReconnect=false&someParams&useSSL=true&scrollTolerantForwardOnly=true", result);
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
                + "Pools=true&secondsBeforeRetrySource=25&queriesBeforeRetrySource=105&initialTimeout=1000&loadBalanceStrategy=strat&scrollTolerantForwardOnly=true", result);
    }

    @Test
    public void buildConnectionUriTestDbHaDisabled() {
        String result = TransactionLegacy.buildConnectionUri(null, "driver", false, "host", null, 5555, "cloud", false, null, null);

        Assert.assertEquals("driver://host:5555/cloud?autoReconnect=false&scrollTolerantForwardOnly=true", result);
    }

    @Test
    public void buildConnectionUriTestDbHaEnabled() {
        TransactionLegacy.s_dbHAEnabled = true;

        String result = TransactionLegacy.buildConnectionUri("strat", "driver", false, "host", "second_host", 5555, "cloud", false, null, "dbHaParams");

        Assert.assertEquals("driver://host,second_host:5555/cloud?autoReconnect=false&dbHaParams&loadBalanceStrategy=strat&scrollTolerantForwardOnly=true", result);
    }

    @Test
    public void buildConnectionUriTestUrlParamsNotNull() {
        String result = TransactionLegacy.buildConnectionUri(null, "driver", false, "host", null, 5555, "cloud", false, "urlParams", null);

        Assert.assertEquals("driver://host:5555/cloud?autoReconnect=false&urlParams&scrollTolerantForwardOnly=true", result);
    }

    @Test
    public void buildConnectionUriTestUseSslTrue() {
        String result = TransactionLegacy.buildConnectionUri(null, "driver", true, "host", null, 5555, "cloud", false, null, null);

        Assert.assertEquals("driver://host:5555/cloud?autoReconnect=false&useSSL=true&scrollTolerantForwardOnly=true", result);
    }

    @Test
    public void getConnectionUriAndDriverTestWithoutUriAndUrlParamsDefiningConnectionCollationDoesNotPinTheDefaultOne() {
        properties.setProperty("db.cloud.uri", "");
        properties.setProperty("db.cloud.driver", "driver");
        properties.setProperty("db.cloud.url.params", "connectionCollation=utf8mb4_unicode_ci");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("driver://host:5555/name?autoReconnect=false&connectionCollation=utf8mb4_unicode_ci"
                + "&scrollTolerantForwardOnly=true", result.first());
    }

    @Test
    public void getConnectionUriAndDriverTestWithoutUriAndUrlParamsDefiningCharacterEncodingDoesNotPinTheDefaultCollation() {
        properties.setProperty("db.cloud.uri", "");
        properties.setProperty("db.cloud.driver", "driver");
        properties.setProperty("db.cloud.url.params", "characterEncoding=UTF-8");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("driver://host:5555/name?autoReconnect=false&characterEncoding=UTF-8&scrollTolerantForwardOnly=true",
                result.first());
    }

    @Test
    public void getConnectionUriAndDriverTestWithUriWithoutParametersPinsTheDefaultCollation() {
        properties.setProperty("db.cloud.uri", "jdbc:mysql://host:5555/name");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("jdbc:mysql://host:5555/name?connectionCollation=utf8mb4_general_ci", result.first());
        Assert.assertEquals("jdbc:mysql", result.second());
    }

    @Test
    public void getConnectionUriAndDriverTestWithUriWithParametersPinsTheDefaultCollation() {
        properties.setProperty("db.cloud.uri", "jdbc:mysql://host:5555/name?autoReconnect=false&someParams");

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals("jdbc:mysql://host:5555/name?autoReconnect=false&someParams&connectionCollation=utf8mb4_general_ci",
                result.first());
    }

    @Test
    public void getConnectionUriAndDriverTestWithUriDefiningConnectionCollationKeepsItUntouched() {
        String uri = "jdbc:mysql://host:5555/name?connectionCollation=utf8mb4_unicode_ci";
        properties.setProperty("db.cloud.uri", uri);

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals(uri, result.first());
    }

    @Test
    public void getConnectionUriAndDriverTestWithUriDefiningCharacterEncodingKeepsItUntouched() {
        String uri = "jdbc:mysql://host:5555/name?characterEncoding=UTF-8";
        properties.setProperty("db.cloud.uri", uri);

        Pair<String, String> result = TransactionLegacy.getConnectionUriAndDriver(properties, null, false, "cloud");

        Assert.assertEquals(uri, result.first());
    }

    @Test
    public void shouldPinConnectionCollationTestNullConnectionParamsReturnsTrue() {
        Assert.assertTrue(TransactionLegacy.shouldPinConnectionCollation(null));
    }

    @Test
    public void shouldPinConnectionCollationTestEmptyConnectionParamsReturnsTrue() {
        Assert.assertTrue(TransactionLegacy.shouldPinConnectionCollation(""));
    }

    @Test
    public void shouldPinConnectionCollationTestConnectionParamsWithoutCollationAndEncodingReturnsTrue() {
        Assert.assertTrue(TransactionLegacy.shouldPinConnectionCollation("cachePrepStmts=true&serverTimezone=UTC"));
    }

    @Test
    public void shouldPinConnectionCollationTestConnectionParamsWithConnectionCollationReturnsFalse() {
        Assert.assertFalse(TransactionLegacy.shouldPinConnectionCollation("cachePrepStmts=true&connectionCollation=utf8mb4_unicode_ci"));
    }

    @Test
    public void shouldPinConnectionCollationTestConnectionParamsWithCharacterEncodingReturnsFalse() {
        Assert.assertFalse(TransactionLegacy.shouldPinConnectionCollation("cachePrepStmts=true&characterEncoding=UTF-8"));
    }

    @Test
    public void shouldPinConnectionCollationTestIsCaseInsensitive() {
        Assert.assertFalse(TransactionLegacy.shouldPinConnectionCollation("CONNECTIONCOLLATION=utf8mb4_unicode_ci"));
        Assert.assertFalse(TransactionLegacy.shouldPinConnectionCollation("characterencoding=UTF-8"));
    }

    @Test
    public void addDefaultConnectionCollationTestUriWithoutParametersAddsTheQueryStringSeparator() {
        String result = TransactionLegacy.addDefaultConnectionCollation("jdbc:mysql://host:5555/name", "jdbc:mysql");

        Assert.assertEquals("jdbc:mysql://host:5555/name?connectionCollation=utf8mb4_general_ci", result);
    }

    @Test
    public void addDefaultConnectionCollationTestUriWithParametersAddsTheParameterSeparator() {
        String result = TransactionLegacy.addDefaultConnectionCollation("jdbc:mysql://host:5555/name?someParams", "jdbc:mysql");

        Assert.assertEquals("jdbc:mysql://host:5555/name?someParams&connectionCollation=utf8mb4_general_ci", result);
    }

    @Test
    public void addDefaultConnectionCollationTestUriEndingWithQueryStringSeparatorDoesNotDuplicateIt() {
        String result = TransactionLegacy.addDefaultConnectionCollation("jdbc:mysql://host:5555/name?", "jdbc:mysql");

        Assert.assertEquals("jdbc:mysql://host:5555/name?connectionCollation=utf8mb4_general_ci", result);
    }

    @Test
    public void addDefaultConnectionCollationTestUriEndingWithParameterSeparatorDoesNotDuplicateIt() {
        String result = TransactionLegacy.addDefaultConnectionCollation("jdbc:mysql://host:5555/name?someParams&", "jdbc:mysql");

        Assert.assertEquals("jdbc:mysql://host:5555/name?someParams&connectionCollation=utf8mb4_general_ci", result);
    }

    @Test
    public void addDefaultConnectionCollationTestUriDefiningConnectionCollationKeepsItUntouched() {
        String uri = "jdbc:mysql://host:5555/name?connectionCollation=utf8mb4_unicode_ci";

        Assert.assertEquals(uri, TransactionLegacy.addDefaultConnectionCollation(uri, "jdbc:mysql"));
    }

    @Test
    public void addDefaultConnectionCollationTestUriDefiningCharacterEncodingKeepsItUntouched() {
        String uri = "jdbc:mysql://host:5555/name?characterEncoding=UTF-8";

        Assert.assertEquals(uri, TransactionLegacy.addDefaultConnectionCollation(uri, "jdbc:mysql"));
    }
}
