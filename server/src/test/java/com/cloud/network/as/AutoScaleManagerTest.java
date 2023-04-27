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
package com.cloud.network.as;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AutoScaleManagerTest {

    @Test
    public void testAutoScaleStatsInterval() {
        AutoScaleManager asManager = Mockito.mock(AutoScaleManager.class);

        ConfigKey config = asManager.AutoScaleStatsInterval;
        Assert.assertEquals("Advanced", config.category());
        Assert.assertEquals(Integer.class, config.type());
        Assert.assertEquals("autoscale.stats.interval", config.key());
        Assert.assertEquals("60", config.defaultValue());
        Assert.assertFalse(config.isDynamic());
    }

    @Test
    public void testAutoScaleStatsCleanupDelay() {
        AutoScaleManager asManager = Mockito.mock(AutoScaleManager.class);

        ConfigKey config = asManager.AutoScaleStatsCleanupDelay;
        Assert.assertEquals("Advanced", config.category());
        Assert.assertEquals(Integer.class, config.type());
        Assert.assertEquals("autoscale.stats.cleanup.delay", config.key());
        Assert.assertEquals("7200", config.defaultValue());
        Assert.assertFalse(config.isDynamic());
    }

    @Test
    public void testAutoScaleStatsWorker() {
        AutoScaleManager asManager = Mockito.mock(AutoScaleManager.class);

        ConfigKey config = asManager.AutoScaleStatsWorker;
        Assert.assertEquals("Advanced", config.category());
        Assert.assertEquals(Integer.class, config.type());
        Assert.assertEquals("autoscale.stats.worker", config.key());
        Assert.assertEquals("10", config.defaultValue());
        Assert.assertFalse(config.isDynamic());
    }
}
