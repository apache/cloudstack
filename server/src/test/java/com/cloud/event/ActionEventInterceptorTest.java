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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.aopalliance.intercept.MethodInvocation;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.configuration.Config;
import com.cloud.event.dao.EventDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentContext.class)
public class ActionEventInterceptorTest {
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

    @InjectMocks
    private ActionEventInterceptor actionEventInterceptor = new ActionEventInterceptor();

    protected static final String eventType = EventTypes.EVENT_VM_START;
    protected static final String eventDescription = "Starting VM";

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
        PowerMockito.mockStatic(ComponentContext.class);
        Mockito.when(ComponentContext.getComponent(EventBus.class)).thenReturn(eventBus);

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
    }

    @Test
    public void testInvokeSuccess() throws Throwable {
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        MethodInvocation invocation = Mockito.mock(MethodInvocation.class);
        Method m = tester.getClass().getMethod("testMethod");
        Mockito.when(invocation.getMethod()).thenReturn(m);
        Mockito.when(invocation.getThis()).thenReturn(tester);
        Boolean o = tester.testMethod();
        Mockito.when(invocation.proceed()).thenReturn(o);
        Object result = actionEventInterceptor.invoke(invocation);
        Assert.assertEquals(result, o);
    }

    @Test
    public void testInterceptStartNonAsync() throws NoSuchMethodException {
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        Method m = tester.getClass().getMethod("testMethod1");
        Object event = actionEventInterceptor.interceptStart(m, tester);
        Assert.assertNull(event);
    }

    @Test
    public void testInterceptStartAsync() throws NoSuchMethodException {
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        Method m = tester.getClass().getMethod("testMethod");
        Object event = actionEventInterceptor.interceptStart(m, tester);
        Assert.assertNull(event);

        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getType(), EventTypes.EVENT_VM_START);
        Assert.assertEquals(eventVO.getDescription(), "Starting VM");
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Started);
    }

    @Test
    public void testInterceptComplete() throws NoSuchMethodException {
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        Method m = tester.getClass().getMethod("testMethod");
        actionEventInterceptor.interceptComplete(m, tester, null);

        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getType(), eventType);
        Assert.assertTrue(eventVO.getDescription().endsWith(eventDescription));
        Assert.assertEquals(eventVO.getLevel(), EventVO.LEVEL_INFO);
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Completed);
    }

    @Test
    public void testInterceptException() throws NoSuchMethodException {
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        Method m = tester.getClass().getMethod("testMethod");
        actionEventInterceptor.interceptException(m, tester, null);

        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getType(), eventType);
        Assert.assertTrue(eventVO.getDescription().endsWith(eventDescription));
        Assert.assertEquals(eventVO.getLevel(), EventVO.LEVEL_ERROR);
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Completed);
    }

    @Test
    public void testInterceptExceptionResource() throws NoSuchMethodException {
        CallContext.register(user, account);
        Long resourceId = 1L;
        ApiCommandResourceType resourceType = ApiCommandResourceType.VirtualMachine;
        CallContext.current().setEventResourceId(resourceId);
        CallContext.current().setEventResourceType(resourceType);
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        Method m = tester.getClass().getMethod("testMethod");
        actionEventInterceptor.interceptException(m, tester, null);

        Assert.assertEquals(persistedEvents.size(), 1);
        EventVO eventVO = persistedEvents.get(0);
        Assert.assertEquals(eventVO.getType(), eventType);
        Assert.assertTrue(eventVO.getDescription().endsWith(eventDescription));
        Assert.assertEquals(eventVO.getLevel(), EventVO.LEVEL_ERROR);
        Assert.assertEquals(eventVO.getState(), com.cloud.event.Event.State.Completed);
        Assert.assertEquals(eventVO.getResourceId(), resourceId);
        Assert.assertEquals(eventVO.getResourceType(), resourceType.toString());
        CallContext.unregister();
    }

    @Test
    public void testNeedToIntercept() throws NoSuchMethodException {
        TestActionEventManagerImpl tester = new TestActionEventManagerImpl();
        Method m = tester.getClass().getMethod("testMethod");
        Assert.assertTrue(actionEventInterceptor.needToIntercept(m));
        m = tester.getClass().getMethod("noEventMethod");
        Assert.assertFalse(actionEventInterceptor.needToIntercept(m));
    }

    protected class TestActionEventManagerImpl {
        @ActionEvent(eventType = ActionEventInterceptorTest.eventType, eventDescription = ActionEventInterceptorTest.eventDescription, async = true)
        public Boolean testMethod() {
            return true;
        }
        @ActionEvent(eventType = ActionEventInterceptorTest.eventType, eventDescription = ActionEventInterceptorTest.eventDescription)
        public Boolean testMethod1() {
            return false;
        }
        public Boolean noEventMethod() {
            return false;
        }
    }
}
