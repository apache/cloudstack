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
package com.cloud.agent.api;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GetStorageStatsAnswerTest {

    @Test
    public void testDefaultConstructor() {
        GetStorageStatsAnswer answer = new GetStorageStatsAnswer();

        Assert.assertEquals(0, answer.getByteUsed());
        Assert.assertEquals(0, answer.getCapacityBytes());
        Assert.assertNull(answer.getCapacityIops());
        Assert.assertNull(answer.getUsedIops());
    }

    @Test
    public void testConstructorWithCapacityAndUsedBytes() {
        GetStorageStatsCommand mockCmd = new GetStorageStatsCommand();
        long capacityBytes = 1024L;
        long usedBytes = 512L;

        GetStorageStatsAnswer answer = new GetStorageStatsAnswer(mockCmd, capacityBytes, usedBytes);

        Assert.assertEquals(capacityBytes, answer.getCapacityBytes());
        Assert.assertEquals(usedBytes, answer.getByteUsed());
        Assert.assertNull(answer.getCapacityIops());
        Assert.assertNull(answer.getUsedIops());
    }

    @Test
    public void testConstructorWithIops() {
        GetStorageStatsCommand mockCmd = new GetStorageStatsCommand();
        long capacityBytes = 2048L;
        long usedBytes = 1024L;
        Long capacityIops = 1000L;
        Long usedIops = 500L;

        GetStorageStatsAnswer answer = new GetStorageStatsAnswer(mockCmd, capacityBytes, usedBytes, capacityIops, usedIops);

        Assert.assertEquals(capacityBytes, answer.getCapacityBytes());
        Assert.assertEquals(usedBytes, answer.getByteUsed());
        Assert.assertEquals(capacityIops, answer.getCapacityIops());
        Assert.assertEquals(usedIops, answer.getUsedIops());
    }

    @Test
    public void testErrorConstructor() {
        GetStorageStatsCommand mockCmd = new GetStorageStatsCommand();
        String errorDetails = "An error occurred";

        GetStorageStatsAnswer answer = new GetStorageStatsAnswer(mockCmd, errorDetails);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals(errorDetails, answer.getDetails());
        Assert.assertEquals(0, answer.getCapacityBytes());
        Assert.assertEquals(0, answer.getByteUsed());
        Assert.assertNull(answer.getCapacityIops());
        Assert.assertNull(answer.getUsedIops());
    }
}
