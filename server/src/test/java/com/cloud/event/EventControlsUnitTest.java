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
package com.cloud.event;

import com.cloud.event.dao.EventDao;
import com.cloud.server.ManagementServerImpl;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import junit.framework.TestCase;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class EventControlsUnitTest extends TestCase {
    private Logger logger = LogManager.getLogger(EventControlsUnitTest.class);

    @Spy
    ManagementServerImpl _mgmtServer = new ManagementServerImpl();
    @Mock
    AccountManager _accountMgr;
    @Mock
    EventDao _eventDao;
    List<EventVO> _events = null;
    private AutoCloseable closeable;

    @Override
    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        _mgmtServer._eventDao = _eventDao;
        _mgmtServer._accountMgr = _accountMgr;
        doNothing().when(_accountMgr).checkAccess(any(Account.class), any(AccessType.class), any(Boolean.class), any(ControlledEntity.class));
        when(_eventDao.listToArchiveOrDeleteEvents(anyList(), anyString(), any(Date.class), any(Date.class), anyList())).thenReturn(_events);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testInjected() throws Exception {
        logger.info("Starting test to archive and delete events");
        archiveEvents();
        deleteEvents();
        logger.info("archive/delete events: TEST PASSED");
    }

    protected void archiveEvents() {
        // archive alerts
        doNothing().when(_eventDao).archiveEvents(_events);
    }

    protected void deleteEvents() {
        // delete alerts
    }
}
