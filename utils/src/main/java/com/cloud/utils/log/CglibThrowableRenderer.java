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

package com.cloud.utils.log;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.spi.ThrowableRenderer;

/**
 * This renderer removes all the CGLib generated methods from the call
 *
 * Unfortunately, I had to copy out there-write the EnhancedThrowableRenderer from
 * the Apache libraries because EnhancedThrowableRenderer is a final class.
 * simply override doRender. Not sure what the developers are thinking there
 * making it final.
 *
 * <throwableRenderer class="com.cloud.utils.log.CglibThrowableRenderer"/>
 * into log4j.xml.
 *
 */
public class CglibThrowableRenderer implements ThrowableRenderer {

    private final static int MAX_NUMBER_OF_STACK_TRACES_ON_LOG_FOR_CAUSE = 3;
    @Override
    public String[] doRender(Throwable th) {
        List<String> lines = new ArrayList<String>();
        lines.add(th.toString());
        addStackTraceToList(th, lines, 0);
        do {
            th = th.getCause();
            if (th != null) {
                lines.add("Caused by: " + th.toString());
                addStackTraceToList(th, lines, MAX_NUMBER_OF_STACK_TRACES_ON_LOG_FOR_CAUSE);
            }
        } while (th != null);
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * This method adds the stack traces retrieved from {@link Throwable#getStackTrace()}
     * The maxNumberOfStack attribute indicates the number of stacks that will be added,
     * if that value is 0, then all of the stack traces will be added, otherwise the stack traces will be limited to that number
     * @param th
     * @param lines
     * @param maxNumberOfStack
     */
    private void addStackTraceToList(Throwable th, List<String> lines, int maxNumberOfStack) {
        StackTraceElement[] elements = th.getStackTrace();
        if (maxNumberOfStack == 0 || maxNumberOfStack > elements.length) {
            maxNumberOfStack = elements.length;
        }
        for (int i = 0; i < maxNumberOfStack; i++) {
            StackTraceElement element = elements[i];
            if (StringUtils.contains(element.getClassName(), "net.sf.cglib.proxy")) {
                continue;
            }
            lines.add("\tat " + element.toString());
        }
        if (maxNumberOfStack < elements.length) {
            lines.add("\t... " + (elements.length - maxNumberOfStack) + " more");
        }
    }
}
