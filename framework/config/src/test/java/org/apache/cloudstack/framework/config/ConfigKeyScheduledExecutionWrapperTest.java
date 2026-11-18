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
package org.apache.cloudstack.framework.config;

import com.cloud.utils.concurrency.NamedThreadFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigKeyScheduledExecutionWrapperTest {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("TestExecutor"));

    @Mock
    ConfigKey<Integer> configKey;

    @Test(expected = IllegalArgumentException.class)
    public void nullExecutorTest() {
        TestRunnable runnable = new TestRunnable();
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(null, runnable, configKey, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCommandTest() {
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, null, configKey, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConfigKeyTest() {
        TestRunnable runnable = new TestRunnable();
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, null, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConfigKeyTest() {
        TestRunnable runnable = new TestRunnable();
        ConfigKey<String> configKey = new ConfigKey<>(String.class, "test", "test", "test", "test", true,
                ConfigKey.Scope.Global, null, null, null, null, null, ConfigKey.Kind.CSV, null);
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, configKey, TimeUnit.SECONDS);
    }

    @Test
    public void scheduleOncePerSecondTest() {
        when(configKey.value()).thenReturn(1);
        TestRunnable runnable = new TestRunnable();
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, configKey, TimeUnit.SECONDS);
        runner.start();

        waitSeconds(3);
        assertThat("Runnable ran once per second", runnable.getRunCount(), isOneOf(2, 3));
    }

    private void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L + 100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void scheduleTwicePerSecondTest() {
        when(configKey.value()).thenReturn(500);
        TestRunnable runnable = new TestRunnable();
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, configKey, TimeUnit.MILLISECONDS);
        runner.start();

        waitSeconds(2);
        assertThat("Runnable ran twice per second", runnable.getRunCount(), isOneOf(3, 4));
    }

    @Test
    public void scheduleDynamicTest() {
        // start with twice per second, then switch to four times per second
        when(configKey.value()).thenReturn(500);
        TestRunnable runnable = new TestRunnable();
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, configKey, TimeUnit.MILLISECONDS);
        runner.start();

        waitSeconds(2);
        assertThat("Runnable ran twice per second", runnable.getRunCount(), isOneOf(3, 4));

        runnable.resetRunCount();
        when(configKey.value()).thenReturn(250);
        waitSeconds(2);
        assertThat("Runnable ran four times per second", runnable.getRunCount(), isOneOf(7, 8));
    }

    @Test
    public void noOverlappingRunsTest() {
        when(configKey.value()).thenReturn(200);
        TestRunnable runnable = new TestRunnable(1);
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, configKey, TimeUnit.MILLISECONDS);
        runner.start();

        waitSeconds(3);
        assertThat("Slow runnable on tight schedule runs without overlap", runnable.getRunCount(), isOneOf(2, 3));
    }

    @Test
    public void temporaryDisableRunsTest() {
        // start with twice per second, then disable, then start again
        when(configKey.value()).thenReturn(500);
        TestRunnable runnable = new TestRunnable();
        ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(executorService, runnable, configKey, 1, TimeUnit.MILLISECONDS);
        runner.start();

        waitSeconds(2);
        assertThat("Runnable ran twice per second", runnable.getRunCount(), isOneOf(3, 4));

        runnable.resetRunCount();
        when(configKey.value()).thenReturn(0);
        waitSeconds(2);
        assertThat("Runnable ran zero times per second", runnable.getRunCount(), is(0));

        runnable.resetRunCount();
        when(configKey.value()).thenReturn(500);
        waitSeconds(2);
        assertThat("Runnable ran twice per second", runnable.getRunCount(), isOneOf(3, 4));
    }

    static class TestRunnable implements Runnable {
        private Integer runCount = 0;
        private int waitSeconds = 0;

        TestRunnable(int waitSeconds) {
            this.waitSeconds = waitSeconds;
        }

        TestRunnable() {
        }

        @Override
        public void run() {
            runCount++;
            if (waitSeconds > 0) {
                try {
                    Thread.sleep(waitSeconds * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public int getRunCount() {
            return this.runCount;
        }

        public void resetRunCount() {
            this.runCount = 0;
        }
    }
}
