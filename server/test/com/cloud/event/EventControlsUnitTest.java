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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.event.dao.EventDao;
import com.cloud.server.ManagementServerImpl;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;

public class EventControlsUnitTest extends TestCase{
    private static final Logger s_logger = Logger.getLogger(EventControlsUnitTest.class);

    @Spy ManagementServerImpl _mgmtServer = new ManagementServerImpl();
    @Mock AccountManager _accountMgr;
    @Mock EventDao _eventDao;
    List<EventVO> _events = null;

    @Override
    @Before
    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        _mgmtServer._eventDao = _eventDao;
        _mgmtServer._accountMgr = _accountMgr;
        doNothing().when(_accountMgr).checkAccess(any(Account.class), any(AccessType.class), any(Boolean.class), any(ControlledEntity.class));
        when(_eventDao.listToArchiveOrDeleteEvents(anyList(), anyString(), any(Date.class), anyLong())).thenReturn(_events);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInjected() throws Exception {
        s_logger.info("Starting test to archive and delete events");
        archiveEvents();
        deleteEvents();
        s_logger.info("archive/delete events: TEST PASSED");
    }

    protected void archiveEvents() {
        // archive alerts
        doNothing().when(_eventDao).archiveEvents(_events);
        }

    protected void deleteEvents() {
        // delete alerts
    }
}
