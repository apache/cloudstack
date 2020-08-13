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

package com.cloud.utils;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class NumbersUtilTest {

    @Test
    public void toReadableSize() {
        Locale.setDefault(Locale.US); // Fixed locale for the test
        assertEquals("1.0000 TB", NumbersUtil.toReadableSize((1024l * 1024l * 1024l * 1024l)));
        assertEquals("1.00 GB", NumbersUtil.toReadableSize(1024L * 1024 * 1024));
        assertEquals("1.00 MB", NumbersUtil.toReadableSize(1024L * 1024));
        assertEquals("1.00 KB", NumbersUtil.toReadableSize((1024L)));
        assertEquals("1023 bytes", NumbersUtil.toReadableSize((1023L)));
    }

    @Test
    public void bytesToLong() {
        assertEquals(0, NumbersUtil.bytesToLong(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}));
        assertEquals(1, NumbersUtil.bytesToLong(new byte[] {0, 0, 0, 0, 0, 0, 0, 1}));
        assertEquals(257, NumbersUtil.bytesToLong(new byte[] {0, 0, 0, 0, 0, 0, 1, 1}));
    }

    @Test
    public void nullToLong() {
        assertEquals("null", NumbersUtil.toReadableSize(null));
    }

}
