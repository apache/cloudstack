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

package org.apache.log4j.helpers;

import java.io.Writer;
import java.io.IOException;

import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.ErrorCode;

/**
   Counts the number of bytes written.

   @author Heinz Richter, heinz.richter@frogdot.com
   @since 0.8.1

   */
public class CountingQuietWriter extends QuietWriter {

  protected long count;

  public
  CountingQuietWriter(Writer writer, ErrorHandler eh) {
    super(writer, eh);
  }

  public
  void write(String string) {
    try {
      out.write(string);
      count += string.length();
    }
    catch(IOException e) {
      errorHandler.error("Write failure.", e, ErrorCode.WRITE_FAILURE);
    }
  }

  public
  long getCount() {
    return count;
  }

  public
  void setCount(long count) {
    this.count = count;
  }

}
