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
package org.apache.log4j.filter;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Unit tests for DenyAllFilter.
 */
public class DenyAllFilterTest extends TestCase {

    /**
     * Create new test instance.
     *
     * @param name test name.
     */
    public DenyAllFilterTest(final String name) {
        super(name);
    }

    /**
     * Check that DenyAllFilter.decide() returns Filter.DENY.
     */
    public void test1() {
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(DenyAllFilterTest.class),
                System.currentTimeMillis(), Level.INFO, "Hello, World", null);
        Filter filter = new DenyAllFilter();
        filter.activateOptions();
        assertEquals(Filter.DENY, filter.decide(event));
    }

}

