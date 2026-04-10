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
package org.apache.cloudstack.threadcontext;

import org.apache.logging.log4j.ThreadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class, helps to propagate {@link ThreadContext} values from parent to child threads.
 *
 * @author mprokopchuk
 */
public class ThreadContextUtil {
    /**
     * Wrap {@link Runnable} to propagate {@link ThreadContext} values.
     *
     * @param delegate
     * @return
     */
    public static Runnable wrapThreadContext(Runnable delegate) {
        @SuppressWarnings("unchecked")
        Map<String, String> context = ThreadContext.getContext() != null ?
                new HashMap<>(ThreadContext.getContext()) : null;

        return () -> {
            @SuppressWarnings("unchecked")
            Map<String, String> oldContext = ThreadContext.getContext() != null ?
                    new HashMap<>(ThreadContext.getContext()) : null;
            try {
                ThreadContext.clearMap();
                if (context != null) {
                    context.forEach(ThreadContext::put);
                }
                delegate.run();
            } finally {
                ThreadContext.clearMap();
                if (oldContext != null) {
                    oldContext.forEach(ThreadContext::put);
                }
            }
        };
    }
}
