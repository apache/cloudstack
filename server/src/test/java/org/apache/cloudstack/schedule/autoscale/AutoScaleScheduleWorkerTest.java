/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.schedule.autoscale;

import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmGroupCmd;
import org.apache.cloudstack.schedule.ResourceScheduledJobVO;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDao;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDetailsDao;
import org.apache.cloudstack.schedule.dao.ResourceScheduledJobDao;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class AutoScaleScheduleWorkerTest {
    @Spy
    @InjectMocks
    private AutoScaleScheduleWorker worker = new AutoScaleScheduleWorker();

    @Mock
    private AutoScaleVmGroupDao autoScaleVmGroupDao;
    @Mock
    private ResourceScheduleDetailsDao resourceScheduleDetailsDao;
    @Mock
    private ResourceScheduleDao resourceScheduleDao;
    @Mock
    private ResourceScheduledJobDao resourceScheduledJobDao;
    @Mock
    private AsyncJobManager asyncJobManager;
    @Mock
    private AutoScaleManager autoScaleManager;

    private AutoCloseable closeable;
    private MockedStatic<ActionEventUtils> actionEventUtilsMocked;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class);
        Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn(1L);
    }

    @After
    public void tearDown() throws Exception {
        actionEventUtilsMocked.close();
        closeable.close();
    }

    @Test
    public void testProcessJobWithValidDetailsSubmitsUpdateAutoScaleVmGroupCmd() {
        ResourceScheduledJobVO job = Mockito.mock(ResourceScheduledJobVO.class);
        AutoScaleVmGroupVO group = Mockito.mock(AutoScaleVmGroupVO.class);
        Map<String, String> details = new HashMap<>();
        details.put("minmembers", "2");
        details.put("maxmembers", "5");

        Mockito.when(job.getResourceId()).thenReturn(1L);
        Mockito.when(job.getScheduleId()).thenReturn(10L);
        Mockito.when(job.getActionName()).thenReturn(AutoScaleScheduleAction.UPDATE.name());
        Mockito.when(autoScaleVmGroupDao.findById(1L)).thenReturn(group);
        Mockito.when(group.getState()).thenReturn(AutoScaleVmGroup.State.ENABLED);
        Mockito.when(group.getAccountId()).thenReturn(3L);
        Mockito.when(group.getId()).thenReturn(1L);
        Mockito.when(group.getUuid()).thenReturn("asg-uuid");
        Mockito.when(resourceScheduleDetailsDao.listDetailsKeyPairs(10L, true)).thenReturn(details);
        Mockito.doReturn(11L).when(worker).submitAsyncJob(
                Mockito.eq(UpdateAutoScaleVmGroupCmd.class), Mockito.eq(3L), Mockito.eq(1L), Mockito.eq(1L), Mockito.anyMap());

        Long asyncJobId = worker.processJob(job);

        Assert.assertEquals(Long.valueOf(11L), asyncJobId);
        Mockito.verify(worker).submitAsyncJob(Mockito.eq(UpdateAutoScaleVmGroupCmd.class), Mockito.eq(3L), Mockito.eq(1L), Mockito.eq(1L),
                Mockito.argThat(map -> "2".equals(map.get("minmembers")) && "5".equals(map.get("maxmembers"))));
    }

    @Test
    public void testProcessJobWithMissingGroupSkipsExecution() {
        ResourceScheduledJobVO job = Mockito.mock(ResourceScheduledJobVO.class);
        Mockito.when(job.getResourceId()).thenReturn(1L);
        Mockito.when(autoScaleVmGroupDao.findById(1L)).thenReturn(null);

        Long asyncJobId = worker.processJob(job);
        Assert.assertNull(asyncJobId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateDetailsMissingRequiredKeys() {
        Map<String, String> details = new HashMap<>();
        details.put("minmembers", "1");
        worker.validateDetails(AutoScaleScheduleAction.UPDATE, details);
    }
}
