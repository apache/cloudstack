/*
 * Copyright 1999,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.rolling.helper;

import java.io.IOException;


/**
 * Abstract base class for implementations of Action.
 *
 * @author Curt Arnold
 */
public abstract class ActionBase implements Action {
  /**
   * Is action complete.
   */
  private boolean complete = false;

  /**
   * Is action interrupted.
   */
  private boolean interrupted = false;

  /**
   * Constructor.
   */
  protected ActionBase() {
  }

  /**
   * Perform action.
   *
   * @throws IOException if IO error.
   * @return true if successful.
   */
  public abstract boolean execute() throws IOException;

  /**
   * {@inheritDoc}
   */
  public synchronized void run() {
    if (!interrupted) {
      try {
        execute();
      } catch (IOException ex) {
        reportException(ex);
      }

      complete = true;
      interrupted = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void close() {
    interrupted = true;
  }

  /**
   * Tests if the action is complete.
   * @return true if action is complete.
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Capture exception.
   *
   * @param ex exception.
   */
  protected void reportException(final Exception ex) {
  }
}
