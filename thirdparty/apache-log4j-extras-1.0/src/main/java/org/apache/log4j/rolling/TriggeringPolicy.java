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

import org.apache.log4j.Appender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;


/**
 * A <code>TriggeringPolicy</code> controls the conditions under which rollover
 * occurs. Such conditions include time of day, file size, an
 * external event, the log request or a combination thereof.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Curt Arnold
 * 
 */
public interface TriggeringPolicy extends OptionHandler {
  /**
   * Determines if a rollover may be appropriate at this time.  If
   * true is returned, RolloverPolicy.rollover will be called but it
   * can determine that a rollover is not warranted.
   *
   * @param appender A reference to the appender.
   * @param event A reference to the currently event.
   * @param filename The filename for the currently active log file.
   * @param fileLength Length of the file in bytes.
   * @return true if a rollover should occur.
   */
  public boolean isTriggeringEvent(
    final Appender appender, final LoggingEvent event, final String filename,
    final long fileLength);
}
