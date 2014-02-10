// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.testcase.Log4jEnabledTestCase;

public class TestProfiler extends Log4jEnabledTestCase {
    protected final static Logger s_logger = Logger.getLogger(TestProfiler.class);

    @Test
    public void testProfiler() {
        s_logger.info("testProfiler() started");

        Profiler pf = new Profiler();
        pf.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        pf.stop();

        s_logger.info("Duration : " + pf.getDuration());

        Assert.assertTrue(pf.getDuration() >= 1000);

        s_logger.info("testProfiler() stopped");
    }
}
