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
package org.apache.cloudstack.spring.module.util;

import org.apache.cloudstack.spring.module.factory.CloudStackSpringContext;

public class Main {

    long start = System.currentTimeMillis();

    public Main() {

    }

    public void start() throws Exception {
        CloudStackSpringContext context = new CloudStackSpringContext();
        context.registerShutdownHook();

        if (Boolean.getBoolean("force.exit")) {
            System.exit(0);
        }
    }

    public long getTime() {
        return System.currentTimeMillis() - start;
    }

    public static void main(String... args) {
        Main main = new Main();

        try {
            main.start();
            System.out.println("STARTUP COMPLETE [" + main.getTime() + "] ms");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("STARTUP FAILED [" + main.getTime() + "] ms");
            System.err.println("STARTUP FAILED [" + main.getTime() + "] ms");
            System.exit(1);
        }
    }
}
