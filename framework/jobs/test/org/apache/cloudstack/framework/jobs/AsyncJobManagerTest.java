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
package org.apache.cloudstack.framework.jobs;

/*
 * This integration test requires real DB setup, it is not meant to run at per-build
 * basis, it can only be opened in developer's run
 *
 *

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/AsyncJobManagerTestContext.xml")
public class AsyncJobManagerTest extends TestCase {
    private static final Logger s_logger =
            Logger.getLogger(AsyncJobManagerTest.class);

    @Inject
    AsyncJobManager _jobMgr;

    @Inject
    AsyncJobTestDashboard _testDashboard;

    @Override
    @Before
    public void setUp() throws Exception {
        try {
            ComponentContext.initComponentsLifeCycle();
        } catch (Exception ex) {
            ex.printStackTrace();
            s_logger.error(ex.getMessage());
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    public void testWaitBehave() {

        final Object me = this;
        new Thread(new Runnable() {

            @Override
            public void run() {
                s_logger.info("Sleeping...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    s_logger.debug("[ignored] .");
                }

                s_logger.info("wakeup");
                synchronized (me) {
                    me.notifyAll();
                }
            }

        }).start();

        s_logger.info("First wait");
        synchronized (me) {
            try {
                wait(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        s_logger.info("First wait done");

        s_logger.info("Second wait");
        synchronized (me) {
            try {
                wait(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        s_logger.info("Second wait done");
    }

    @Test
    public void test() {
        final int TOTAL_JOBS_PER_QUEUE = 5;
        final int TOTAL_QUEUES = 100;

        for (int i = 0; i < TOTAL_QUEUES; i++) {
            for (int j = 0; j < TOTAL_JOBS_PER_QUEUE; j++) {
                AsyncJobVO job = new AsyncJobVO();
                job.setCmd("TestCmd");
                job.setDispatcher("TestJobDispatcher");
                job.setCmdInfo("TestCmd info");

                _jobMgr.submitAsyncJob(job, "fakequeue", i);

                s_logger.info("Job submitted. job " + job.getId() + ", queue: " + i);
            }
        }

        while (true) {
            if (_testDashboard.getCompletedJobCount() == TOTAL_JOBS_PER_QUEUE * TOTAL_QUEUES)
                break;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] .");
            }
        }

        s_logger.info("Test done with " + _testDashboard.getCompletedJobCount() + " job executed");
    }
}

*/
