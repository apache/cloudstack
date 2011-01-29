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

/*
 */
package org.apache.log4j.rule;

import org.apache.log4j.spi.LoggingEvent;

import java.beans.PropertyChangeListener;


/**
 * A Rule evaluates to true of false given a LoggingEvent object, and can notify
 * listeners when the underlying implementation of this Rule has it's
 * criteria changed by using the standard PropertyChangeListener infrastructure.
 *
 * @author Paul Smith (psmith@apache.org)
 * @author Scott Deboy (sdeboy@apache.org)
 */
public interface Rule {
  /**
   * Returns true if this implementation of the rule accepts the LoggingEvent,
   * or false if not.
   *
   * <p>What True/False means can be client-specific.
   *
   * @param e LoggingEvent this instance will evaluate
   * @return true if this Rule instance accepts the event, otherwise false.
   */
  boolean evaluate(LoggingEvent e);

  /**
   * Adds a PropertyChangeListener to this instance, which is notified when
   * underlying Rule information has changed.
   * (there are no specific property name events).
   * @param listener listener
   */
  void addPropertyChangeListener(PropertyChangeListener listener);

  /**
   * Removes a known PropertyChangeListener from this Rule.
   * @param listener listener
   */
  void removePropertyChangeListener(PropertyChangeListener listener);
}
