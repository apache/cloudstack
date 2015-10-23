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
package org.apache.cloudstack.spring.module.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import org.apache.cloudstack.spring.module.locator.impl.ClasspathModuleDefinitionLocator;
import org.apache.cloudstack.spring.module.model.ModuleDefinition;
import org.apache.cloudstack.spring.module.model.ModuleDefinitionSet;

public class ModuleBasedContextFactoryTest {

    Collection<ModuleDefinition> defs;

    @Before
    public void setUp() throws IOException {
        InstantiationCounter.count = 0;

        ClasspathModuleDefinitionLocator locator = new ClasspathModuleDefinitionLocator();
        defs = locator.locateModules("testhierarchy");
    }

    @Test
    public void testLoad() throws IOException {

        ModuleBasedContextFactory factory = new ModuleBasedContextFactory();

        ModuleDefinitionSet set = factory.loadModules(defs, "base");

        assertNotNull(set.getApplicationContext("base"));
    }

    @Test
    public void testOverride() throws IOException {

        InitTest.initted = false;

        ModuleBasedContextFactory factory = new ModuleBasedContextFactory();

        ModuleDefinitionSet set = factory.loadModules(defs, "base");

        assertTrue(!InitTest.initted);
        assertEquals("a string", set.getApplicationContext("child1").getBean("override", String.class));
    }

    @Test
    public void testExcluded() throws IOException {
        ModuleBasedContextFactory factory = new ModuleBasedContextFactory();
        ModuleDefinitionSet set = factory.loadModules(defs, "base");

        assertNull(set.getApplicationContext("excluded"));
        assertNull(set.getApplicationContext("excluded2"));
        assertNull(set.getApplicationContext("orphan-of-excluded"));
    }

    @Test
    public void testBeans() throws IOException {
        ModuleBasedContextFactory factory = new ModuleBasedContextFactory();
        ModuleDefinitionSet set = factory.loadModules(defs, "base");

        testBeansInContext(set, "base", 1, new String[] {"base"}, new String[] {"child1", "child2", "child1-1"});
        testBeansInContext(set, "child1", 2, new String[] {"base", "child1"}, new String[] {"child2", "child1-1"});
        testBeansInContext(set, "child2", 4, new String[] {"base", "child2"}, new String[] {"child1", "child1-1"});
        testBeansInContext(set, "child1-1", 3, new String[] {"base", "child1", "child1-1"}, new String[] {"child2"});
    }

    protected void testBeansInContext(ModuleDefinitionSet set, String name, int order, String[] parents, String[] notTheres) {
        ApplicationContext context = set.getApplicationContext(name);

        String nameBean = context.getBean("name", String.class);
        assertEquals(name, nameBean);

        for (String parent : parents) {
            String parentBean = context.getBean(parent, String.class);
            assertEquals(parent, parentBean);
        }

        int notfound = 0;
        for (String notThere : notTheres) {
            try {
                context.getBean(notThere, String.class);
                fail();
            } catch (NoSuchBeanDefinitionException e) {
                notfound++;
            }
        }

        int count = context.getBean("count", InstantiationCounter.class).getCount();

        assertEquals(notTheres.length, notfound);
        assertEquals(order, count);
    }

    public static class InstantiationCounter {
        public static Integer count = 0;

        int myCount;

        public InstantiationCounter() {
            synchronized (count) {
                myCount = count + 1;
                count = myCount;
            }
        }

        public int getCount() {
            return myCount;
        }

    }
}
