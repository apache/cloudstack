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

package org.apache.cloudstack.poll;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BackgroundPollManagerImplTest {

    private BackgroundPollManagerImpl pollManager;
    private DummyPollTask pollTask;

    private class DummyPollTask extends ManagedContextRunnable implements BackgroundPollTask {
        private boolean didIRun = false;
        private long counter = 0;

        public boolean didItRan() {
            return didIRun;
        }

        public long getCounter() {
            return counter;
        }

        @Override
        protected void runInContext() {
            didIRun = true;
            counter++;
        }

        @Override
        public Long getDelay() {
            return null;
        }

    }

    @Before
    public void setUp() throws Exception {
        pollManager = new BackgroundPollManagerImpl();
        pollTask = new DummyPollTask();
    }

    @After
    public void tearDown() throws Exception {
        pollManager.stop();
    }

    @Test
    public void testSubmitValidTask() throws Exception {
        Assert.assertFalse(pollTask.didItRan());
        Assert.assertTrue(pollTask.getCounter() == 0);

        pollManager.submitTask(pollTask);
        pollManager.start();
        Thread.sleep(pollManager.getInitialDelay()*2);

        Assert.assertTrue(pollTask.didItRan());
        Assert.assertTrue(pollTask.getCounter() > 0);
    }

    @Test(expected = NullPointerException.class)
    public void testSubmitNullTask() throws Exception {
        pollManager.submitTask(null);
    }

}
