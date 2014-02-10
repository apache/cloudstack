// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.log;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.log4j.spi.ThrowableRenderer;

/**
 * This renderer removes all the Cglib generated methods from the call
 *
 * Unfortunately, I had to copy out the EnhancedThrowableRenderer from
 * the apach libraries because EnhancedThrowableRenderer is a final class.
 * simply override doRender. Not sure what the developers are thinking there
 * making it final.
 *
 * <throwableRenderer class="com.cloud.utils.log.CglibThrowableRenderer"/>
 * into log4j.xml.
 *
 */
public class CglibThrowableRenderer implements ThrowableRenderer {
    /**
     * Construct new instance.
     */
    public CglibThrowableRenderer() {
        super();
    }

    @Override
    public String[] doRender(final Throwable th) {
        try {
            ArrayList<String> lines = new ArrayList<String>();
            Throwable throwable = th;
            lines.add(throwable.toString());
            int start = 0;
            do {
                StackTraceElement[] elements = throwable.getStackTrace();
                for (int i = 0; i < elements.length - start; i++) {
                    StackTraceElement element = elements[i];
                    String filename = element.getFileName();
                    String method = element.getMethodName();
                    if ((filename != null && filename.equals("<generated>")) || (method != null && method.equals("invokeSuper"))) {
                        continue;
                    }
                    lines.add("\tat " + element.toString());
                }
                if (start != 0) {
                    lines.add("\t... " + start + " more");
                }
                throwable = throwable.getCause();
                if (throwable != null) {
                    lines.add("Caused by: " + throwable.toString());
                    start = elements.length - 1;
                }
            } while (throwable != null);
            return lines.toArray(new String[lines.size()]);
        } catch (Exception ex) {
            PrintWriter pw = new PrintWriter(System.err);
            ex.printStackTrace(pw);
            pw = new PrintWriter(System.out);
            ex.printStackTrace(pw);
            ex.printStackTrace();
            return null;
        }
    }
}
