/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.rolling;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Test CompositeTriggeringPolicy
 * 
 */
public class CompositeTriggeringPolicyTest extends TestCase {
    private CompositeTriggeringPolicy composite;
    private ConsoleAppender appender;
    private LoggingEvent event;

    protected void setUp() throws Exception {
        event = new LoggingEvent("Classname", Logger.getLogger("Logger"), System.currentTimeMillis(), Level.INFO, "msg", null);
        appender = new ConsoleAppender(new PatternLayout("%d %level %c -%m%n"));
        composite = new CompositeTriggeringPolicy();
    }

    protected void tearDown() throws Exception {
    }
    
    public void testNoPolicies() {
        composite.activateOptions();
        Assert.assertFalse(composite.isTriggeringEvent(appender, event, "file", 100));
    }
    
    public void testOneTruePolicy() {
        composite.addTriggeringPolicy(new TestTriggeringPolicy(true));
        composite.activateOptions();
        Assert.assertTrue(composite.isTriggeringEvent(appender, event, "file", 100));
    }
    
    public void testOneFalsePolicy() {
        composite.addTriggeringPolicy(new TestTriggeringPolicy(false));
        composite.activateOptions();
        Assert.assertFalse(composite.isTriggeringEvent(appender, event, "file", 100));
    }

    public void testAllFalsePolicies() {
        composite.addTriggeringPolicy(new TestTriggeringPolicy(false));
        composite.addTriggeringPolicy(new TestTriggeringPolicy(false));
        composite.addTriggeringPolicy(new TestTriggeringPolicy(false));
        composite.activateOptions();
        Assert.assertFalse(composite.isTriggeringEvent(appender, event, "file", 100));
    }
    
    public void testAllTruePolicies() {
        composite.addTriggeringPolicy(new TestTriggeringPolicy(true));
        composite.addTriggeringPolicy(new TestTriggeringPolicy(true));
        composite.addTriggeringPolicy(new TestTriggeringPolicy(true));
        composite.activateOptions();
        Assert.assertTrue(composite.isTriggeringEvent(appender, event, "file", 100));
    }
    
    public void testTrueAndFalsePolicies() {
        composite.addTriggeringPolicy(new TestTriggeringPolicy(false));
        composite.addTriggeringPolicy(new TestTriggeringPolicy(false));
        composite.addTriggeringPolicy(new TestTriggeringPolicy(true));
        composite.activateOptions();
        Assert.assertTrue(composite.isTriggeringEvent(appender, event, "file", 100));
    }
    
    public void testActivateOptionsCalledByCompositeActivateOptions() {
        TestTriggeringPolicy policy1 = new TestTriggeringPolicy(true);
        TestTriggeringPolicy policy2 = new TestTriggeringPolicy(true);
        
        composite.addTriggeringPolicy(policy1);
        composite.addTriggeringPolicy(policy2);
        composite.activateOptions();
        
        Assert.assertTrue(policy1.activateOptionsCalled());
        Assert.assertTrue(policy2.activateOptionsCalled());
    }

    public void testActivateOptionsNotCalledByAddTriggeringPolicy() {
        TestTriggeringPolicy policy1 = new TestTriggeringPolicy(true);
        TestTriggeringPolicy policy2 = new TestTriggeringPolicy(true);
        
        composite.addTriggeringPolicy(policy1);
        composite.addTriggeringPolicy(policy2);
        
        Assert.assertFalse(policy1.activateOptionsCalled());
        Assert.assertFalse(policy2.activateOptionsCalled());
    }

    class TestTriggeringPolicy implements TriggeringPolicy {
        private final boolean result;
        private boolean activateOptionsCalled = false;

        public TestTriggeringPolicy(boolean result) {
            this.result = result;
        }
        
        public boolean activateOptionsCalled() {
            return activateOptionsCalled;
        }

        public boolean isTriggeringEvent(Appender appender, LoggingEvent event,
                String filename, long fileLength) {
            return result;
        }

        public void activateOptions() {
            activateOptionsCalled = true;
        }
    }
}
