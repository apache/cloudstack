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

package org.apache.cloudstack.resourcealert;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AlertConditionTest {

    @Test
    public void testGtFiresAbove() {
        assertTrue(AlertCondition.GT.evaluate(81.0, 80.0));
    }

    @Test
    public void testGtSilentAtBoundary() {
        assertFalse(AlertCondition.GT.evaluate(80.0, 80.0));
    }

    @Test
    public void testGtSilentBelow() {
        assertFalse(AlertCondition.GT.evaluate(79.0, 80.0));
    }

    @Test
    public void testGteFiresAbove() {
        assertTrue(AlertCondition.GTE.evaluate(81.0, 80.0));
    }

    @Test
    public void testGteFiresAtBoundary() {
        assertTrue(AlertCondition.GTE.evaluate(80.0, 80.0));
    }

    @Test
    public void testGteSilentBelow() {
        assertFalse(AlertCondition.GTE.evaluate(79.0, 80.0));
    }

    @Test
    public void testLtFiresBelow() {
        assertTrue(AlertCondition.LT.evaluate(10.0, 20.0));
    }

    @Test
    public void testLtSilentAtBoundary() {
        assertFalse(AlertCondition.LT.evaluate(20.0, 20.0));
    }

    @Test
    public void testLtSilentAbove() {
        assertFalse(AlertCondition.LT.evaluate(21.0, 20.0));
    }

    @Test
    public void testLteFiresAtBoundary() {
        assertTrue(AlertCondition.LTE.evaluate(20.0, 20.0));
    }

    @Test
    public void testLteFiresBelow() {
        assertTrue(AlertCondition.LTE.evaluate(19.0, 20.0));
    }

    @Test
    public void testLteSilentAbove() {
        assertFalse(AlertCondition.LTE.evaluate(21.0, 20.0));
    }

    @Test
    public void testEqFiresOnExactMatch() {
        assertTrue(AlertCondition.EQ.evaluate(75.0, 75.0));
    }

    @Test
    public void testEqSilentOnMismatch() {
        assertFalse(AlertCondition.EQ.evaluate(75.001, 75.0));
    }
}
