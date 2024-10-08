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
package com.cloud.utils.backoff.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RangeTimeBackoffTest {

    @Test
    public void testWaitValidValue() {
        RangeTimeBackoff backoff = new RangeTimeBackoff();
        Map<String, Object> map = new HashMap<>();
        int min = 1;
        int max = 3;
        map.put("minSeconds", String.valueOf(min));
        map.put("maxSeconds", String.valueOf(max));
        backoff.configure("RangeTimeBackoff", map);
        long startTime = System.currentTimeMillis();
        backoff.waitBeforeRetry();
        long timeTaken = System.currentTimeMillis() - startTime;
        Assert.assertTrue(timeTaken >= min * 1000L);
        Assert.assertTrue(timeTaken <= max * 1000L);
    }

    @Test
    public void testWaitEmptyValue() {
        RangeTimeBackoff backoff = new RangeTimeBackoff();
        Map<String, Object> map = new HashMap<>();
        map.put("minSeconds", "");
        map.put("maxSeconds", "");
        backoff.configure("RangeTimeBackoff", map);
        long startTime = System.currentTimeMillis();
        backoff.waitBeforeRetry();
        long timeTaken = System.currentTimeMillis() - startTime;
        Assert.assertTrue(timeTaken >= RangeTimeBackoff.DEFAULT_MIN_TIME * 1000L);
    }

    @Test
    public void testWaitNullValue() {
        RangeTimeBackoff backoff = new RangeTimeBackoff();
        Map<String, Object> map = new HashMap<>();
        backoff.configure("RangeTimeBackoff", map);
        long startTime = System.currentTimeMillis();
        backoff.waitBeforeRetry();
        long timeTaken = System.currentTimeMillis() - startTime;
        Assert.assertTrue(timeTaken >= RangeTimeBackoff.DEFAULT_MIN_TIME * 1000L);
    }

    @Test
    public void testWaitVMinHigherThanMax() {
        RangeTimeBackoff backoff = new RangeTimeBackoff();
        Map<String, Object> map = new HashMap<>();
        int min = 3;
        int max = 2;
        map.put("minSeconds", String.valueOf(min));
        map.put("maxSeconds", String.valueOf(max));
        backoff.configure("RangeTimeBackoff", map);
        long startTime = System.currentTimeMillis();
        backoff.waitBeforeRetry();
        long timeTaken = System.currentTimeMillis() - startTime;
        Assert.assertTrue(timeTaken >= min * 1000L);
    }
}
