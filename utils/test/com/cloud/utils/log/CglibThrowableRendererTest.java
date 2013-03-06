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
package com.cloud.utils.log;

import junit.framework.TestCase;

import org.apache.log4j.*;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.spi.ThrowableRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.CharArrayWriter;
import java.io.Writer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/testContext.xml")
public class CglibThrowableRendererTest extends TestCase {
    static Logger another = Logger.getLogger("TEST");

    private final static Logger s_logger = Logger.getLogger(CglibThrowableRendererTest.class);
    public static class TestClass {
    	
    	public TestClass() {
    	}
    	
        @DB
        public void exception1() {
            throw new IllegalArgumentException("What a bad exception");
        }
        public void exception2() {
            try {
                exception1();
            } catch (Exception e) {
                throw new CloudRuntimeException("exception2", e);
            }
        }
        @DB
        public void exception() {
            try {
                exception2();
            } catch (Exception e) {
                throw new CloudRuntimeException("exception", e);
            }
        }
    }

    private Logger getAlternateLogger(Writer writer, ThrowableRenderer renderer) {
        Hierarchy hierarchy = new Hierarchy(new RootLogger(Level.INFO));
        if (renderer != null) {
            hierarchy.setThrowableRenderer(renderer);
        }
        Logger alternateRoot = hierarchy.getRootLogger();
        alternateRoot.addAppender(new WriterAppender(new SimpleLayout(), writer));
        return alternateRoot;
    }

    @Test
    public void testException() {
        Writer w = new CharArrayWriter();
        Logger alt = getAlternateLogger(w, null);

        TestClass test = ComponentContext.inject(TestClass.class);
        try {
            test.exception();
        } catch (Exception e) {
            alt.warn("exception caught", e);
        }
        // first check that we actually have some call traces containing "<generated>"
       // assertTrue(w.toString().contains("<generated>"));

        w = new CharArrayWriter();
        alt = getAlternateLogger(w, new CglibThrowableRenderer());

        try {
            test.exception();
        } catch (Exception e) {
            alt.warn("exception caught", e);
        }
        // then we check that CglibThrowableRenderer indeed remove those occurrences
        assertFalse(w.toString().contains("<generated>"));

    }
}
