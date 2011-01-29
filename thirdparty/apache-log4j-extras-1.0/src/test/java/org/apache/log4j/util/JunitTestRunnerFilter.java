/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.util;

import java.util.regex.Pattern;


public class JunitTestRunnerFilter implements Filter {

  private static final String[] PATTERNS = {
          "at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner",
          "at org.apache.tools.ant",
          "at junit.textui.TestRunner",
          "at com.intellij.rt.execution.junit",
          "at java.lang.reflect.Method.invoke",
          "at org.apache.maven.",
          "at org.codehaus.",
		  "at org.junit.internal.runners."
  };
  private final Pattern[] patterns;

  public JunitTestRunnerFilter() {
      patterns = new Pattern[PATTERNS.length];
      for (int i = 0; i < PATTERNS.length; i++) {
          patterns[i] = Pattern.compile(PATTERNS[i]);
      }

  }

  /**
   * Filter out stack trace lines coming from the various JUnit TestRunners.
   */
  public String filter(String in) {
    if (in == null) {
      return null;
    }

      //
      //  restore the one instance of Method.invoke that we actually want
      //
    if (in.indexOf("at junit.framework.TestCase.runTest") != -1) {
        return "\tat java.lang.reflect.Method.invoke(X)\n" + in;
    }

    for (int i = 0; i < patterns.length; i++) {
        if(patterns[i].matcher(in).find()) {
            return null;
        }
    }
    return in;
  }
}
