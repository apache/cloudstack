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
package org.apache.cloudstack.managed.context.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.cloudstack.managed.context.ManagedContextListener;
import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.junit.Before;
import org.junit.Test;

public class DefaultManagedContextTest {

    DefaultManagedContext context;
    
    @Before
    public void init() {
        ManagedThreadLocal.setValidateInContext(false);
        
        context = new DefaultManagedContext();
    }
    
    @Test
    public void testCallable() throws Exception {
        assertEquals(5, context.callWithContext(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 5;
            }
        }).intValue());
    }

    @Test
    public void testRunnable() throws Exception {
        final List<Object> touch = new ArrayList<Object>();
        
        context.runWithContext(new Runnable() {
            @Override
            public void run() {
                touch.add(new Object());
            }
        });
        
        assertEquals(1, touch.size());
    }
    
    @Test
    public void testGoodListeners() throws Exception {
        final List<Object> touch = new ArrayList<Object>();

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter");
                return "hi";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave");
                assertEquals("hi", data);
            }
        });

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter1");
                return "hi";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave1");
                assertEquals("hi", data);
            }
        });
        
        assertEquals(5, context.callWithContext(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 5;
            }
        }).intValue());
        
        assertEquals("enter", touch.get(0));
        assertEquals("enter1", touch.get(1));
        assertEquals("leave1", touch.get(2));
        assertEquals("leave", touch.get(3));
    }
    
    @Test
    public void testBadListeners() throws Exception {
        final List<Object> touch = new ArrayList<Object>();

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter");
                throw new RuntimeException("I'm a failure");
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave");
                assertNull(data);
            }
        });

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter1");
                return "hi";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave1");
                assertEquals("hi", data);
            }
        });
        
        try {
            context.callWithContext(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return 5;
                }
            }).intValue();
            
            fail();
        } catch ( Throwable t ) {
            assertTrue(t instanceof RuntimeException);
            assertEquals("I'm a failure", t.getMessage());
        }
        
        assertEquals("enter", touch.get(0));
        assertEquals("enter1", touch.get(1));
        assertEquals("leave1", touch.get(2));
        assertEquals("leave", touch.get(3));
    }
    
    @Test
    public void testBadInvocation() throws Exception {
        final List<Object> touch = new ArrayList<Object>();

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter");
                return "hi";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave");
                assertEquals("hi", data);
            }
        });

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter1");
                return "hi1";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave1");
                assertEquals("hi1", data);
            }
        });
        
        try {
            context.callWithContext(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    throw new RuntimeException("I'm a failure");
                }
            }).intValue();
            
            fail();
        } catch ( Throwable t ) {
            assertTrue(t.getMessage(), t instanceof RuntimeException);
            assertEquals("I'm a failure", t.getMessage());
        }
        
        assertEquals("enter", touch.get(0));
        assertEquals("enter1", touch.get(1));
        assertEquals("leave1", touch.get(2));
        assertEquals("leave", touch.get(3));
    }
    
    @Test
    public void testBadListernInExit() throws Exception {
        final List<Object> touch = new ArrayList<Object>();

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter");
                return "hi";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave");
                assertEquals("hi", data);
                
                throw new RuntimeException("I'm a failure");
            }
        });

        context.registerListener(new ManagedContextListener<Object>() {
            @Override
            public Object onEnterContext(boolean reentry) {
                touch.add("enter1");
                return "hi1";
            }

            @Override
            public void onLeaveContext(Object data, boolean reentry) {
                touch.add("leave1");
                assertEquals("hi1", data);
            }
        });
        
        try {
            context.callWithContext(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return 5;
                }
            }).intValue();
            
            fail();
        } catch ( Throwable t ) {
            assertTrue(t.getMessage(), t instanceof RuntimeException);
            assertEquals("I'm a failure", t.getMessage());
        }
        
        assertEquals("enter", touch.get(0));
        assertEquals("enter1", touch.get(1));
        assertEquals("leave1", touch.get(2));
        assertEquals("leave", touch.get(3));
    }
}
