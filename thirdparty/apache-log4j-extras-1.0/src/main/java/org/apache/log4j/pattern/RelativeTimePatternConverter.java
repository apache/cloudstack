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

package org.apache.log4j.pattern;

import org.apache.log4j.spi.LoggingEvent;


/**
 * Return the relative time in milliseconds since loading of the LoggingEvent
 * class.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class RelativeTimePatternConverter extends LoggingEventPatternConverter {
  /**
   * Cached formatted timestamp.
   */
  private CachedTimestamp lastTimestamp = new CachedTimestamp(0, "");

  /**
   * Private constructor.
   */
  public RelativeTimePatternConverter() {
    super("Time", "time");
  }

  /**
   * Obtains an instance of RelativeTimePatternConverter.
   * @param options options, currently ignored, may be null.
   * @return instance of RelativeTimePatternConverter.
   */
  public static RelativeTimePatternConverter newInstance(
    final String[] options) {
    return new RelativeTimePatternConverter();
  }

  /**
   * {@inheritDoc}
   */
  public void format(final LoggingEvent event, final StringBuffer toAppendTo) {
    long timestamp = event.timeStamp;

    if (!lastTimestamp.format(timestamp, toAppendTo)) {
      final String formatted =
        Long.toString(timestamp - LoggingEvent.getStartTime());
      toAppendTo.append(formatted);
      lastTimestamp = new CachedTimestamp(timestamp, formatted);
    }
  }

  /**
   * Cached timestamp and formatted value.
   */
  private static final class CachedTimestamp {
    /**
     * Cached timestamp.
     */
    private final long timestamp;

    /**
     * Cached formatted timestamp.
     */
    private final String formatted;

    /**
     * Creates a new instance.
     * @param timestamp timestamp.
     * @param formatted formatted timestamp.
     */
    public CachedTimestamp(long timestamp, final String formatted) {
      this.timestamp = timestamp;
      this.formatted = formatted;
    }

    /**
     * Appends the cached formatted timestamp to the buffer if timestamps match.
     * @param newTimestamp requested timestamp.
     * @param toAppendTo buffer to append formatted timestamp.
     * @return true if requested timestamp matched cached timestamp.
     */
    public boolean format(long newTimestamp, final StringBuffer toAppendTo) {
      if (newTimestamp == timestamp) {
        toAppendTo.append(formatted);

        return true;
      }

      return false;
    }
  }
}
