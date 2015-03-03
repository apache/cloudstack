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

public class CacheControlUtil {
    /**
     * Returns a cache-control number based on the user agent.
     * The return value is always zero for browsers other than MS Internet Explorer.
     * In case of internet explorer, the cache behavior can be set not to try to
     * check for newer content.
     */
    public static long getCacheControlWorkaround(final HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        if(userAgent == null
            // old MSIE before v11
            || userAgent.indexOf(" MSIE ") != -1
            // new MSIE from v11 on
            || userAgent.indexOf(" Trident/") != -1) {
            return System.currentTimeMillis();
        } else {
            return 0;
        }
    }
}
