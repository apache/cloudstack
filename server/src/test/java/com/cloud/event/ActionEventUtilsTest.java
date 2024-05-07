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
package com.cloud.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.configuration.Config;
import com.cloud.event.dao.EventDao;
import com.cloud.network.IpAddress;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(MockitoJUnitRunner.class)
public class ActionEventUtilsTest {
    //Predictable constants used throughout this test.
    public static final long EVENT_ID = 1;
    public static final long USER_ID = 1;
    public static final long ACCOUNT_ID = 1;

    //Keep track of the static field values between tests.
    //A horrid abuse of reflection required due to the strange
    //static/inject pattern found in ActionEventUtils.
    protected Map<String, Object> staticFieldValues = new HashMap<>();

    protected List<EventVO> persistedEvents = new ArrayList<>();
    //List of events published on the event bus. Handled via a mocked method.
    //Cleared on every run.
    protected List<Event> publishedEvents = new ArrayList<>();

    //Mock fields. These are injected into ActionEventUtils by the setup() method.
    @Mock
    protected EventDao eventDao;

    @Mock
    protected AccountDao accountDao;

    @Mock
    protected UserDao userDao;

    @Mock
    protected ProjectDao projectDao;

    @Mock
    protected EntityManager entityMgr;

    @Mock
    protected ConfigurationDao configDao;

    @Mock
    protected EventBus eventBus;

    private AccountVO account;
    private UserVO user;

    private MockedStatic<ComponentContext> componentContextMocked;

    /**
     * This setup method injects the mocked beans into the ActionEventUtils class.
     * Because ActionEventUtils has static methods, we must also remember these fields
     * and restore them later, as otherwise strange behavior can result in other unit
     * tests due to the way the JVM handles static fields.
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        publishedEvents = new ArrayList<>();
        staticFieldValues = new HashMap<>();
        setupCommonMocks();

        ActionEventUtils utils = new ActionEventUtils();

        for (Field field : ActionEventUtils.class.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) != null) {
                field.setAccessible(true);

                try {
                    //Inject the mocked field from this class into the ActionEventUtils
                    //and keep track of its original value.
                    Field mockField = this.getClass().getDeclaredField(field.getName());
                    field.set(utils, mockField.get(this));
                    Field staticField = ActionEventUtils.class.getDeclaredField("s_" + field.getName());
                    staticFieldValues.put(field.getName(), staticField.get(null));
                }
                catch (Exception e) {
                    // ignore missing fields
                }
            }
        }

        utils.init();
    }

    /**
     * Set up the common specialized mocks that are needed to make the ActionEventUtils class behave in a
     * predictable way. This method only mocks things that are common to all the tests. Each individual test
     * also mocks some other methods (e.g. find user/account) by itself.
     */
    public void setupCommonMocks() throws Exception {
        //Some basic mocks.
        Mockito.when(configDao.getValue(Config.PublishActionEvent.key())).thenReturn("true");
        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        componentContextMocked.when(() -> ComponentContext.getComponent(EventBus.class)).thenReturn(eventBus);

        //Needed for persist to actually set an ID that can be returned from the ActionEventUtils
        //methods.
        Mockito.when(eventDao.persist(Mockito.any(EventVO.class))).thenAnswer(new Answer<EventVO>() {
            @Override
            public EventVO answer(InvocationOnMock invocation) throws Throwable {
                EventVO event = (EventVO)invocation.getArguments()[0];
                Field id = event.getClass().getDeclaredField("id");
                id.setAccessible(true);
                id.set(event, EVENT_ID);
                persistedEvents.add(event);
                return event;
            }
        });

        //Needed to record events published on the bus.
        Mockito.doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock invocation) throws Throwable {
                Event event = (Event)invocation.getArguments()[0];
                publishedEvents.add(event);
                return null;
            }

        }).when(eventBus).publish(Mockito.any(Event.class));

        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);

        Mockito.when(accountDao.findById(ACCOUNT_ID)).thenReturn(account);
        Mockito.when(userDao.findById(USER_ID)).thenReturn(user);
    }

    /**
     * This teardown method restores the ActionEventUtils static field values to their original values,
     * keeping the mocked mess inside this class.
     */
    @After
    public void teardown() {
        ActionEventUtils utils = new ActionEventUtils();

        for (String fieldName : staticFieldValues.keySet()) {
            try {
                Field field = ActionEventUtils.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(utils, staticFieldValues.get(fieldName));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        utils.init();

        componentContextMocked.close();
    }

    @Test
    public void testPopulateFirstClassEntities() {
        CallContext.register(user, account);

        //Inject some entity UUIDs into the call context
        String instanceUuid = UUID.randomUUID().toString();
        String ipUuid = UUID.randomUUID().toString();
        CallContext.current().putContextParameter(VirtualMachine.class, instanceUuid);
        CallContext.current().putContextParameter(IpAddress.class, ipUuid);

        ActionEventUtils.onActionEvent(USER_ID, ACCOUNT_ID, account.getDomainId(), "StaticNat", "Test event", null, null);

        //Assertions
        Assert.assertNotEquals(publishedEvents.size(), 0);
        Assert.assertEquals(publishedEvents.size(), 1);

        Event event = publishedEvents.get(0);
        Assert.assertNotNull(event.getDescription());

        JsonObject json = new JsonParser().parse(event.getDescription()).getAsJsonObject();

        Assert.assertTrue(json.has("VirtualMachine"));
        Assert.assertTrue(json.has("IpAddress"));
        Assert.assertEquals(json.get("VirtualMachine").getAsString(), instanceUuid);
        Assert.assertEquals(json.get("IpAddress").getAsString(), ipUuid);

        CallContext.unregister();
    }

    private void checkEventResourceAndUnregisterContext(Long resourceId, String resourceUuid, String resourceType) {
        Assert.assertEquals(publishedEvents.size(), 1);
        Event event = publishedEvents.get(0);
        JsonObject json = new JsonParser().parse(event.getDescription()).getAsJsonObject();
        Assert.assertTrue(json.has("entity"));
        Assert.assertTrue(json.has("entityuuid"));
        Assert.assertEquals(json.get("entity").getAsString(), resourceType);
        Assert.assertEquals(json.get("entityuuid").getAsString(), resourceUuid);

        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getResourceType(), resourceType);
        Assert.assertEquals(eventVO.getResourceId(), resourceId);

        CallContext.unregister();
    }

    @Test
    public void testPublishedEventResource() {
        CallContext.register(user, account);

        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getUuid()).thenReturn(resourceUuid);
        Mockito.when(entityMgr.validEntityType(VirtualMachine.class)).thenReturn(true);
        Mockito.when(entityMgr.findByIdIncludingRemoved(VirtualMachine.class, resourceId)).thenReturn(vm);
        ActionEventUtils.onActionEvent(USER_ID, ACCOUNT_ID, account.getDomainId(), EventTypes.EVENT_VM_START, "Test event", resourceId, resourceType);

        checkEventResourceAndUnregisterContext(resourceId, resourceUuid, resourceType);
    }

    @Test
    public void testPublishedEventResourceWithCallContext() {
        CallContext.register(user, account);

        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(resourceId);
        Mockito.when(entityMgr.validEntityType(VirtualMachine.class)).thenReturn(true);
        Mockito.when(entityMgr.findByUuidIncludingRemoved(VirtualMachine.class, resourceUuid)).thenReturn(vm);
        CallContext.current().putContextParameter(VirtualMachine.class, resourceUuid);
        ActionEventUtils.onActionEvent(USER_ID, ACCOUNT_ID, account.getDomainId(), EventTypes.EVENT_VM_START, "Test event", null, null);

        checkEventResourceAndUnregisterContext(resourceId, resourceUuid, resourceType);
    }

    @Test
    public void testScheduledEvent() {
        CallContext.register(user, account);

        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        CallContext.current().putContextParameter(VirtualMachine.class, resourceUuid);
        ActionEventUtils.onScheduledActionEvent(USER_ID, ACCOUNT_ID, EventTypes.EVENT_VM_START, "Test event", resourceId, resourceType, true, 0L);


        Assert.assertEquals(publishedEvents.size(), 1);
        Event event = publishedEvents.get(0);
        JsonObject json = new JsonParser().parse(event.getDescription()).getAsJsonObject();
        Assert.assertTrue(json.has("status"));
        Assert.assertEquals(json.get("status").getAsString(), com.cloud.event.Event.State.Scheduled.toString());
        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Scheduled);

        CallContext.unregister();
    }

    @Test
    public void testCreatedEvent() {
        CallContext.register(user, account);

        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        CallContext.current().putContextParameter(VirtualMachine.class, resourceUuid);
        ActionEventUtils.onCreatedActionEvent(USER_ID, ACCOUNT_ID, EventVO.LEVEL_INFO, EventTypes.EVENT_VM_START, true, "Test event", resourceId, resourceType);


        Assert.assertEquals(publishedEvents.size(), 1);
        Event event = publishedEvents.get(0);
        JsonObject json = new JsonParser().parse(event.getDescription()).getAsJsonObject();
        Assert.assertTrue(json.has("status"));
        Assert.assertEquals(json.get("status").getAsString(), com.cloud.event.Event.State.Created.toString());
        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Created);

        CallContext.unregister();
    }

    @Test
    public void testNestedEvent() {
        CallContext.register(user, account);

        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        CallContext.current().putContextParameter(VirtualMachine.class, resourceUuid);
        ActionEventUtils.startNestedActionEvent(EventTypes.EVENT_VM_START, "Test event", resourceId, resourceType);

        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Started);

        CallContext.unregister();
    }

    @Test
    public void testSnapshotEventResource() {
        CallContext.register(user, account);

        final Long snapshotResourceId = 100L;
        final String snapshotResourceType = ApiCommandResourceType.Snapshot.toString();
        final String snapshotResourceUuid = UUID.randomUUID().toString();
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        Mockito.when(snapshot.getUuid()).thenReturn(snapshotResourceUuid);
        Mockito.when(entityMgr.validEntityType(Snapshot.class)).thenReturn(true);
        Mockito.when(entityMgr.findByIdIncludingRemoved(Snapshot.class, snapshotResourceId)).thenReturn(snapshot);
        ActionEventUtils.onActionEvent(USER_ID, ACCOUNT_ID, account.getDomainId(), EventTypes.EVENT_SNAPSHOT_CREATE, "Test event", snapshotResourceId, snapshotResourceType);

        checkEventResourceAndUnregisterContext(snapshotResourceId, snapshotResourceUuid, snapshotResourceType);
    }

    @Test
    public void testVmSnapshotEventResource() {
        CallContext.register(user, account);

        final Long vmSnapshotResourceId = 100L;
        final String vmSnapshotResourceType = ApiCommandResourceType.VmSnapshot.toString();
        final String vmSnapshotResourceUuid = UUID.randomUUID().toString();
        final Long resourceId = 1L;
        final String resourceType = ApiCommandResourceType.VirtualMachine.toString();
        final String resourceUuid = UUID.randomUUID().toString();
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getUuid()).thenReturn(vmSnapshotResourceUuid);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(resourceId);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getUuid()).thenReturn(resourceUuid);
        Mockito.when(entityMgr.validEntityType(VMSnapshot.class)).thenReturn(true);
        Mockito.when(entityMgr.findByIdIncludingRemoved(VMSnapshot.class, vmSnapshotResourceId)).thenReturn(vmSnapshot);
        Mockito.when(entityMgr.findByIdIncludingRemoved(VirtualMachine.class, resourceId)).thenReturn(vm);
        ActionEventUtils.onActionEvent(USER_ID, ACCOUNT_ID, account.getDomainId(), EventTypes.EVENT_VM_SNAPSHOT_CREATE, "Test event", vmSnapshotResourceId, vmSnapshotResourceType);

        checkEventResourceAndUnregisterContext(resourceId, resourceUuid, resourceType);
    }
}
