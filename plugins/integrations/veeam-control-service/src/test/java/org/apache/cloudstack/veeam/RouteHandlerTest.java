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

package org.apache.cloudstack.veeam;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class RouteHandlerTest {

    @Test
    public void testParseBoolean_TrueValues() {
        for (String value : new String[] {"true", "True", "TRUE", "tRuE", "1", " true ", " 1 "}) {
            assertTrue("Expected true for '" + value + "'", RouteHandler.parseBoolean(value, false));
        }
    }

    @Test
    public void testParseBoolean_FalseValues() {
        for (String value : new String[] {"false", "False", "FALSE", "fAlSe", "0", " false ", " 0 "}) {
            assertFalse("Expected false for '" + value + "'", RouteHandler.parseBoolean(value, true));
        }
    }

    @Test
    public void testParseBoolean_NullEmptyOrUnrecognizedReturnsDefault() {
        for (String value : new String[] {null, "", "   ", "yes", "no", "2", "-1", "truee", "on"}) {
            assertTrue("Expected default for '" + value + "'", RouteHandler.parseBoolean(value, true));
            assertFalse("Expected default for '" + value + "'", RouteHandler.parseBoolean(value, false));
        }
    }

    @Test
    public void testIsRequestAsync_TrueWhenAsyncParamIsTruthyOrEmpty() {
        for (String value : new String[] {"", "true", "TRUE", "1"}) {
            assertTrue("Expected async for '" + value + "'", RouteHandler.isRequestAsync(requestWithAsync(value)));
        }
    }

    @Test
    public void testIsRequestAsync_FalseWhenAsyncParamIsMissingFalsyOrInvalid() {
        for (String value : new String[] {null, "   ", "false", "0", "yes"}) {
            assertFalse("Expected sync for '" + value + "'", RouteHandler.isRequestAsync(requestWithAsync(value)));
        }
    }

    private static HttpServletRequest requestWithAsync(String value) {
        final HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameter("async")).thenReturn(value);
        return req;
    }
}
