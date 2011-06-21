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

import java.io.Writer;
import java.io.IOException;

/**
 * <p>Extends {@link Writer} with methods that return immediately
 * without doing anything. This class is used to measure the cost of
 * constructing a log message but not actually writing to any device.
 * </p>

 * <p><b> <font color="#FF2222">The
 * <code>org.apache.log4j.performance.NOPWriter</code> class is
 * intended for internal use only.</font> Consequently, it is not
 * included in the <em>log4j.jar</em> file.</b> </p>
 *  
 * @author Ceki G&uuml;lc&uuml; 
 * */
public class NOPWriter extends Writer {

  public void write(char[] cbuf) throws IOException {}

  public void write(char[] cbuf, int off, int len) throws IOException {}


  public void write(int b) throws IOException {}

  public void write(String s) throws IOException {} 

  public void write(String s, int off, int len) throws IOException {} 

  public void flush() throws IOException {
  }

  public void close() throws IOException {
    System.err.println("Close called.");
  }
}
