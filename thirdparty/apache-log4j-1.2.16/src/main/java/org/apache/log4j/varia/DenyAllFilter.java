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

package org.apache.log4j.varia;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;


/**
   This filter drops all logging events. 

   <p>You can add this filter to the end of a filter chain to
   switch from the default "accept all unless instructed otherwise"
   filtering behaviour to a "deny all unless instructed otherwise"
   behaviour.


   @author Ceki G&uuml;lc&uuml;

   @since 0.9.0 */
public class DenyAllFilter extends Filter {

  /**
     Returns <code>null</code> as there are no options.
     
     @deprecated We now use JavaBeans introspection to configure
     components. Options strings are no longer needed.
  */
  public
  String[] getOptionStrings() {
    return null;
  }

  
  /**
     No options to set.
     
     @deprecated Use the setter method for the option directly instead
     of the generic <code>setOption</code> method. 
  */
  public
  void setOption(String key, String value) {
  }
  
  /**
     Always returns the integer constant {@link Filter#DENY}
     regardless of the {@link LoggingEvent} parameter.

     @param event The LoggingEvent to filter.
     @return Always returns {@link Filter#DENY}.
  */
  public
  int decide(LoggingEvent event) {
    return Filter.DENY;
  }
}

