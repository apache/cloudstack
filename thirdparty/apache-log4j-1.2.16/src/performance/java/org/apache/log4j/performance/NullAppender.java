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

package org.apache.log4j.performance;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.AppenderSkeleton;

/**
 * A bogus appender which calls the format method of its layout object
 * but does not write the result anywhere.
 *
 * <p><b> <font color="#FF2222">The
 * <code>org.apache.log4j.performance.NullAppender</code> class is
 * intended for internal use only.</font> Consequently, it is not
 * included in the <em>log4j.jar</em> file.</b> </p>
 * */
public class NullAppender extends AppenderSkeleton {

  public static String s;
  public String t;

  public
  NullAppender() {}

  public
  NullAppender(Layout layout) {
    this.layout = layout;
  }

  public
  void close() {}

  public
  void doAppend(LoggingEvent event) {
    if(layout != null) {
      t = layout.format(event);
      s = t;
    }
  }

  public
  void append(LoggingEvent event) {
  }

  /**
     This is a bogus appender but it still uses a layout.
  */
  public
  boolean requiresLayout() {
    return true;
  }
}
