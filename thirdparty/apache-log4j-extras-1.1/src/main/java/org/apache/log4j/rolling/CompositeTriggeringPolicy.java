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

package org.apache.log4j.rolling;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.xml.UnrecognizedElementHandler;
import org.w3c.dom.Element;


/**
 * CompositeTriggeringPolicy determines if rolling should be triggered
 * by evaluating the current event against a set of triggering policies.
 * 
 * TriggeringPolicy results are OR'd together - if any of the triggering policies report rolling should occur,  
 * a rolling event will be triggered.
 *
 */
public final class CompositeTriggeringPolicy implements TriggeringPolicy, UnrecognizedElementHandler {
  Set triggeringPolicies = new HashSet();

  public CompositeTriggeringPolicy() {
  }

  public boolean isTriggeringEvent(final Appender appender, final LoggingEvent event, final String file, final long fileLength) {
    boolean isTriggered = false;
    for (Iterator iter = triggeringPolicies.iterator();iter.hasNext();) {
        boolean result = ((TriggeringPolicy)iter.next()).isTriggeringEvent(appender, event, file, fileLength);
        isTriggered = isTriggered || result;
    }
    return isTriggered;
  }

  /**
   * Add triggering policy
   * 
   * @param policy
   */
  public void addTriggeringPolicy(final TriggeringPolicy policy) {
    triggeringPolicies.add(policy);
  }

  public void activateOptions() {
    for (Iterator iter = triggeringPolicies.iterator();iter.hasNext();) {
      ((TriggeringPolicy)iter.next()).activateOptions();
    }
  }

  public boolean parseUnrecognizedElement(final Element element, final Properties props) throws Exception {
    final String nodeName = element.getNodeName();
    if ("triggeringPolicy".equals(nodeName)) {
      OptionHandler policy = org.apache.log4j.extras.DOMConfigurator.parseElement(element, props, TriggeringPolicy.class);
      if (policy instanceof TriggeringPolicy) {
        policy.activateOptions();
        addTriggeringPolicy((TriggeringPolicy)policy);
      }
      return true;
    }
    return false;
  }
}
