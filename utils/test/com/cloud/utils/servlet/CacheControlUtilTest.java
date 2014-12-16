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

package com.cloud.utils.servlet;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheControlUtilTest {
    @Mock
    HttpServletRequest request;

    @Test
    public void getCacheControlWorkaroundNull() {
        Mockito.when(request.getHeader("User-Agent")).thenReturn(null);
        // not known user agent, handled as IE
        Assert.assertNotEquals(0,
                CacheControlUtil.getCacheControlWorkaround(request));
    }

    @Test
    public void getCacheControlWorkaroundFirefox() {
        Mockito.when(request.getHeader("User-Agent"))
                .thenReturn(
                        "Mozilla/5.0 (X11; Linux x86_64; rv:28.0) Gecko/20100101 Firefox/28.0");
        Assert.assertEquals(0,
                CacheControlUtil.getCacheControlWorkaround(request));
    }

    @Test
    public void getCacheControlWorkaroundMSIE() {
        // only if it is not 1970 jan 1 :)
        Assume.assumeFalse(System.currentTimeMillis() == 0);
        Mockito.when(request.getHeader("User-Agent")).thenReturn(
                "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US))");
        Assert.assertNotEquals(0,
                CacheControlUtil.getCacheControlWorkaround(request));
    }

    @Test
    public void getCacheControlWorkaroundMSIE11() {
        // only if it is not 1970 jan 1 :)
        Assume.assumeFalse(System.currentTimeMillis() == 0);
        Mockito.when(request.getHeader("User-Agent")).thenReturn(
                "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko");
        Assert.assertNotEquals(0,
                CacheControlUtil.getCacheControlWorkaround(request));
    }


}
