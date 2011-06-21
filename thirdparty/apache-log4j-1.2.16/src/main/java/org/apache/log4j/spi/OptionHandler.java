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

package org.apache.log4j.spi;


/**
   A string based interface to configure package components.

   @author Ceki G&uuml;lc&uuml;
   @author Anders Kristensen
   @since 0.8.1
 */
public interface OptionHandler {

  /**
     Activate the options that were previously set with calls to option
     setters.

     <p>This allows to defer activiation of the options until all
     options have been set. This is required for components which have
     related options that remain ambigous until all are set.

     <p>For example, the FileAppender has the {@link
     org.apache.log4j.FileAppender#setFile File} and {@link
     org.apache.log4j.FileAppender#setAppend Append} options both of
     which are ambigous until the other is also set.  */
  void activateOptions();

  /**
     Return list of strings that the OptionHandler instance recognizes.

     @deprecated We now use JavaBeans style getters/setters.
   */
  //  String[] getOptionStrings();

  /**
     Set <code>option</code> to <code>value</code>.

     <p>The handling of each option depends on the OptionHandler
     instance. Some options may become active immediately whereas
     other may be activated only when {@link #activateOptions} is
     called.

     @deprecated We now use JavaBeans style getters/setters.
  */
  //void setOption(String option, String value);
}
