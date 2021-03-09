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
package com.cloud.utils.db;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Merovingian2Test extends TestCase {
    static final Logger s_logger = Logger.getLogger(Merovingian2Test.class);
    Merovingian2 _lockController = Merovingian2.createLockController(1234);

    @Override
    @Before
    protected void setUp() throws Exception {
        _lockController.cleanupThisServer();
    }

    @Override
    @After
    protected void tearDown() throws Exception {
        _lockController.cleanupThisServer();
    }

    @Test
    public void testLockAndRelease() {

        s_logger.info("Testing first acquire");
        boolean result = _lockController.acquire("first" + 1234, 5);
        Assert.assertTrue(result);

        s_logger.info("Testing acquire of different lock");
        result = _lockController.acquire("second" + 1234, 5);
        Assert.assertTrue(result);

        s_logger.info("Testing reacquire of the same lock");
        result = _lockController.acquire("first" + 1234, 5);
        Assert.assertTrue(result);

        int count = _lockController.owns("first" + 1234);
        Assert.assertEquals(count, 2);

        count = _lockController.owns("second" + 1234);
        Assert.assertEquals(count, 1);

        s_logger.info("Testing release of the first lock");
        result = _lockController.release("first" + 1234);
        Assert.assertTrue(result);

        count = _lockController.owns("first" + 1234);
        Assert.assertEquals(count, 1);

        s_logger.info("Testing release of the second lock");
        result = _lockController.release("second" + 1234);
        Assert.assertTrue(result);

        result = _lockController.release("first" + 1234);
        Assert.assertTrue(result);
    }

}
