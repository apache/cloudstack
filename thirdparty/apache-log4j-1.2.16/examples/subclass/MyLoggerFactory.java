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

package examples.subclass;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

/**
   A factory that makes new {@link MyLogger} objects.

   See <b><a href="doc-files/MyLoggerFactory.java">source
   code</a></b> for more details.

   @author Ceki G&uuml;lc&uuml; */
public class MyLoggerFactory implements LoggerFactory {

  /**
     The constructor should be public as it will be called by
     configurators in different packages.  */
  public
  MyLoggerFactory() {
  }

  public
  Logger makeNewLoggerInstance(String name) {
    return new MyLogger(name);
  }
}
