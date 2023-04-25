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
package com.cloud.network.as;

import org.junit.Assert;
import org.junit.Test;

public class AutoScalePolicyTest {

    @Test
    public void testScaleUpAction() {
        AutoScalePolicy.Action action = AutoScalePolicy.Action.fromValue("scaleup");
        Assert.assertEquals(AutoScalePolicy.Action.SCALEUP, action);
    }

    @Test
    public void testScaleDownAction() {
        AutoScalePolicy.Action action = AutoScalePolicy.Action.fromValue("scaledown");
        Assert.assertEquals(AutoScalePolicy.Action.SCALEDOWN, action);
    }

    @Test
    public void testNullAction() {
        AutoScalePolicy.Action action = AutoScalePolicy.Action.fromValue(null);
        Assert.assertNull(action);
    }

    @Test
    public void testBlankAction() {
        AutoScalePolicy.Action action = AutoScalePolicy.Action.fromValue("");
        Assert.assertNull(action);

        action = AutoScalePolicy.Action.fromValue(" ");
        Assert.assertNull(action);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAction() {
        AutoScalePolicy.Action action = AutoScalePolicy.Action.fromValue("invalid");
    }
}
