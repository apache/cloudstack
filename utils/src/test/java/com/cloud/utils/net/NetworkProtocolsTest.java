//
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
//

package com.cloud.utils.net;

import org.junit.Assert;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class NetworkProtocolsTest {

    @Test
    public void validateIcmpTypeAndCode() {
        validateIcmpTypeAndCodeInternal(null, null, true);
        validateIcmpTypeAndCodeInternal(null, -1, true);
        validateIcmpTypeAndCodeInternal(-1, -1, true);
        validateIcmpTypeAndCodeInternal(3, -1, true);
        validateIcmpTypeAndCodeInternal(3, 15, true);
        validateIcmpTypeAndCodeInternal(4, -1, false);
        validateIcmpTypeAndCodeInternal(5, 10, false);
    }

    private void validateIcmpTypeAndCodeInternal(Integer type, Integer code, boolean expected) {
        boolean actual = NetworkProtocols.validateIcmpTypeAndCode(type, code);
        Assert.assertEquals(expected, actual);
    }
}
