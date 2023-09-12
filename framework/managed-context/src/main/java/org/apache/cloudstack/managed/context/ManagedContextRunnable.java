/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.managed.context;

import org.apache.log4j.Logger;

import org.apache.cloudstack.managed.context.impl.DefaultManagedContext;

public abstract class ManagedContextRunnable implements Runnable {

    private static final int SLEEP_COUNT = 120;

    private static final Logger log = Logger.getLogger(ManagedContextRunnable.class);
    private static final ManagedContext DEFAULT_MANAGED_CONTEXT = new DefaultManagedContext();
    private static ManagedContext context;
    private static boolean managedContext = false;

    /* This is slightly dirty, but the idea is that we only save the ManagedContext
     * in a static global.  Any ManagedContextListener can be a fully managed object
     * and not have to rely on global statics
     */
    public static ManagedContext initializeGlobalContext(ManagedContext context) {
        setManagedContext(true);
        return ManagedContextRunnable.context = context;
    }

    @Override
    public void run() {
        getContext().runWithContext(new Runnable() {
            @Override
            public void run() {
                runInContext();
            }
        });
    }

    protected abstract void runInContext();

    protected ManagedContext getContext() {
        if (!managedContext)
            return DEFAULT_MANAGED_CONTEXT;

        for (int i = 0; i < SLEEP_COUNT; i++) {
            if (context == null) {
                try {
                    Thread.sleep(1000);

                    if (context == null)
                        log.info("Sleeping until ManagedContext becomes available");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return context;
            }
        }

        throw new RuntimeException("Failed to obtain ManagedContext");
    }

    public static boolean isManagedContext() {
        return managedContext;
    }

    public static void setManagedContext(boolean managedContext) {
        ManagedContextRunnable.managedContext = managedContext;
    }

}
