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
package org.apache.log4j.lf5.viewer;

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * An AdjustmentListener which ensures that an Adjustable (e.g. a Scrollbar)
 * will "track" when the Adjustable expands.
 * For example, when a vertical scroll bar is at its bottom anchor,
 * the scrollbar will remain at the bottom.  When the vertical scroll bar
 * is at any other location, then no tracking will happen.
 * An instance of this class should only listen to one Adjustable as
 * it retains state information about the Adjustable it listens to.
 *
 * @author Richard Wan
 */

// Contributed by ThoughtWorks Inc.

public class TrackingAdjustmentListener implements AdjustmentListener {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  protected int _lastMaximum = -1;

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  public void adjustmentValueChanged(AdjustmentEvent e) {
    Adjustable bar = e.getAdjustable();
    int currentMaximum = bar.getMaximum();
    if (bar.getMaximum() == _lastMaximum) {
      return; // nothing to do, the adjustable has not expanded
    }
    int bottom = bar.getValue() + bar.getVisibleAmount();

    if (bottom + bar.getUnitIncrement() >= _lastMaximum) {
      bar.setValue(bar.getMaximum()); // use the most recent maximum
    }
    _lastMaximum = currentMaximum;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces
  //--------------------------------------------------------------------------
}

