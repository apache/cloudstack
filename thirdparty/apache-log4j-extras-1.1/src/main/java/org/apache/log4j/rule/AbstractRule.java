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

package org.apache.log4j.rule;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;


/**
 * An abstract Rule class that provides the PropertyChange support plumbing.
 *
 * @author Paul Smith (psmith@apache.org)
 * @author Scott Deboy (sdeboy@apache.org)
 */
public abstract class AbstractRule implements Rule, Serializable {
    /**
     * Serialization id.
     */
  static final long serialVersionUID = -2844288145563025172L;

    /**
     * PropertySupport instance.
     */
  private PropertyChangeSupport propertySupport =
    new PropertyChangeSupport(this);

    /**
     * Add property change listener.
     * @param l listener.
     */
  public void addPropertyChangeListener(final PropertyChangeListener l) {
    propertySupport.addPropertyChangeListener(l);
  }

    /**
     * Remove property change listener.
     * @param l listener.
     */
  public void removePropertyChangeListener(final PropertyChangeListener l) {
    propertySupport.removePropertyChangeListener(l);
  }

    /**
     * Send property change notification to attached listeners.
     * @param propertyName property name.
     * @param oldVal old value.
     * @param newVal new value.
     */
  protected void firePropertyChange(
    final String propertyName,
    final Object oldVal,
    final Object newVal) {
    propertySupport.firePropertyChange(propertyName, oldVal, newVal);
  }

  /**
   * Send property change notification to attached listeners.
   * @param evt property change event.
   */
  public void firePropertyChange(final PropertyChangeEvent evt) {
    propertySupport.firePropertyChange(evt);
  }
}
