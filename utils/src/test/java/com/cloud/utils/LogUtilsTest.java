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
package com.cloud.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LogUtilsTest {

    @Test
    public void logGsonWithoutExceptionTestLogCorrectlyPrimitives() {
        String expected = "test primitives: int [1], double [1.11], float [1.2222], boolean [true], null [], char [\"c\"].";
        String log = LogUtils.logGsonWithoutException("test primitives: int [%s], double [%s], float [%s], boolean [%s], null [%s], char [%s].",
                1, 1.11d, 1.2222f, true, null, 'c');
        assertEquals(expected, log);
    }

    @Test
    public void logGsonWithoutExceptionTestPassWrongNumberOfArgs() {
        String expected = "Failed to log objects using GSON due to: [Format specifier '%s'].";
        String result = LogUtils.logGsonWithoutException("teste wrong [%s] %s args.", "blablabla");
        assertEquals(expected, result);
    }
}
