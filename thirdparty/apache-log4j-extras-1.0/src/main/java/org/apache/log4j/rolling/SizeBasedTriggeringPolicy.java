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
 * SizeBasedTriggeringPolicy looks at size of the file being
 * currently written to.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Curt Arnold
 *
 */
public final class SizeBasedTriggeringPolicy implements TriggeringPolicy,
  OptionHandler {
  /**
   * Rollover threshold size in bytes.
   */
  private long maxFileSize = 10 * 1024 * 1024; // let 10 MB the default max size

  /**
   * Constructs a new instance.
   */
  public SizeBasedTriggeringPolicy() {
  }

  /**
   * Constructs an new instance.
   * @param maxFileSize rollover threshold size in bytes.
   */
  public SizeBasedTriggeringPolicy(final long maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isTriggeringEvent(
    final Appender appender, final LoggingEvent event, final String file,
    final long fileLength) {
    //System.out.println("Size"+file.length());
    return (fileLength >= maxFileSize);
  }

  /**
   * Gets rollover threshold size in bytes.
   * @return rollover threshold size in bytes.
   */
  public long getMaxFileSize() {
    return maxFileSize;
  }

  /**
   * Sets rollover threshold size in bytes.
   * @param l new value for rollover threshold size.
   */
  public void setMaxFileSize(long l) {
    maxFileSize = l;
  }

  /**
   * Prepares policy for use.
   */
  public void activateOptions() {
  }
}
