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

package org.apache.log4j.rolling;

import org.apache.log4j.rolling.helper.Action;


/**
 * Description of actions needed to complete rollover.
 *
 * @author Curt Arnold
 *
 */
public interface RolloverDescription {
  /**
   * Active log file name after rollover.
   * @return active log file name after rollover.
   */
  String getActiveFileName();

  /**
   * Specifies if active file should be opened for appending.
   * @return if true, active file should be opened for appending.
   */
  boolean getAppend();

  /**
   * Action to be completed after close of current active log file
   * before returning control to caller.
   *
   * @return action, may be null.
   */
  Action getSynchronous();

  /**
   * Action to be completed after close of current active log file
   * and before next rollover attempt, may be executed asynchronously.
   *
   * @return action, may be null.
   */
  Action getAsynchronous();
}
