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

import java.io.File;
import java.util.Date;

import org.apache.log4j.Appender;
import org.apache.log4j.pattern.PatternConverter;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.rolling.helper.FileRenameAction;
import org.apache.log4j.rolling.helper.GZCompressAction;
import org.apache.log4j.rolling.helper.ZipCompressAction;
import org.apache.log4j.spi.LoggingEvent;


/**
 * <code>TimeBasedRollingPolicy</code> is both easy to configure and quite
 * powerful.
 *
 * <p>In order to use  <code>TimeBasedRollingPolicy</code>, the
 * <b>FileNamePattern</b> option must be set. It basically specifies the name of the
 * rolled log files. The value <code>FileNamePattern</code> should consist of
 * the name of the file, plus a suitably placed <code>%d</code> conversion
 * specifier. The <code>%d</code> conversion specifier may contain a date and
 * time pattern as specified by the {@link java.text.SimpleDateFormat} class. If
 * the date and time pattern is ommitted, then the default pattern of
 * "yyyy-MM-dd" is assumed. The following examples should clarify the point.
 *
 * <p>
 * <table cellspacing="5px" border="1">
 *   <tr>
 *     <th><code>FileNamePattern</code> value</th>
 *     <th>Rollover schedule</th>
 *     <th>Example</th>
 *   </tr>
 *   <tr>
 *     <td nowrap="true"><code>/wombat/folder/foo.%d</code></td>
 *     <td>Daily rollover (at midnight).  Due to the omission of the optional
 *         time and date pattern for the %d token specifier, the default pattern
 *         of "yyyy-MM-dd" is assumed, which corresponds to daily rollover.
 *     </td>
 *     <td>During November 23rd, 2004, logging output will go to
 *       the file <code>/wombat/foo.2004-11-23</code>. At midnight and for
 *       the rest of the 24th, logging output will be directed to
 *       <code>/wombat/foo.2004-11-24</code>.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td nowrap="true"><code>/wombat/foo.%d{yyyy-MM}.log</code></td>
 *     <td>Rollover at the beginning of each month.</td>
 *     <td>During the month of October 2004, logging output will go to
 *     <code>/wombat/foo.2004-10.log</code>. After midnight of October 31st
 *     and for the rest of November, logging output will be directed to
 *       <code>/wombat/foo.2004-11.log</code>.
 *     </td>
 *   </tr>
 * </table>
 * <h2>Automatic file compression</h2>
 * <code>TimeBasedRollingPolicy</code> supports automatic file compression.
 * This feature is enabled if the value of the <b>FileNamePattern</b> option
 * ends with <code>.gz</code> or <code>.zip</code>.
 * <p>
 * <table cellspacing="5px" border="1">
 *   <tr>
 *     <th><code>FileNamePattern</code> value</th>
 *     <th>Rollover schedule</th>
 *     <th>Example</th>
 *   </tr>
 *   <tr>
 *     <td nowrap="true"><code>/wombat/foo.%d.gz</code></td>
 *     <td>Daily rollover (at midnight) with automatic GZIP compression of the
 *      archived files.</td>
 *     <td>During November 23rd, 2004, logging output will go to
 *       the file <code>/wombat/foo.2004-11-23</code>. However, at midnight that
 *       file will be compressed to become <code>/wombat/foo.2004-11-23.gz</code>.
 *       For the 24th of November, logging output will be directed to
 *       <code>/wombat/folder/foo.2004-11-24</code> until its rolled over at the
 *       beginning of the next day.
 *     </td>
 *   </tr>
 * </table>
 *
 * <h2>Decoupling the location of the active log file and the archived log files</h2>
 * <p>The <em>active file</em> is defined as the log file for the current period
 * whereas <em>archived files</em> are those files which have been rolled over
 * in previous periods.
 *
 * <p>By setting the <b>ActiveFileName</b> option you can decouple the location
 * of the active log file and the location of the archived log files.
 * <p>
 *  <table cellspacing="5px" border="1">
 *   <tr>
 *     <th><code>FileNamePattern</code> value</th>
 *     <th>ActiveFileName</th>
 *     <th>Rollover schedule</th>
 *     <th>Example</th>
 *   </tr>
 *   <tr>
 *     <td nowrap="true"><code>/wombat/foo.log.%d</code></td>
 *     <td nowrap="true"><code>/wombat/foo.log</code></td>
 *     <td>Daily rollover.</td>
 *
 *     <td>During November 23rd, 2004, logging output will go to
 *       the file <code>/wombat/foo.log</code>. However, at midnight that file
 *       will archived as <code>/wombat/foo.log.2004-11-23</code>. For the 24th
 *       of November, logging output will be directed to
 *       <code>/wombat/folder/foo.log</code> until its archived as
 *       <code>/wombat/foo.log.2004-11-24</code> at the beginning of the next
 *       day.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * If configuring programatically, do not forget to call {@link #activateOptions}
 * method before using this policy. Moreover, {@link #activateOptions} of
 * <code> TimeBasedRollingPolicy</code> must be called <em>before</em> calling
 * the {@link #activateOptions} method of the owning
 * <code>RollingFileAppender</code>.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Curt Arnold
 */
public final class TimeBasedRollingPolicy extends RollingPolicyBase
  implements TriggeringPolicy {

  /**
   * Time for next determination if time for rollover.
   */
  private long nextCheck = 0;

  /**
   * File name at last rollover.
   */
  private String lastFileName = null;

  /**
   * Length of any file type suffix (.gz, .zip).
   */
  private int suffixLength = 0;

  /**
   * Constructs a new instance.
   */
  public TimeBasedRollingPolicy() {
  }

  /**
   * Prepares instance of use.
   */
  public void activateOptions() {
    super.activateOptions();

    PatternConverter dtc = getDatePatternConverter();

    if (dtc == null) {
      throw new IllegalStateException(
        "FileNamePattern [" + getFileNamePattern()
        + "] does not contain a valid date format specifier");
    }

    long n = System.currentTimeMillis();
    StringBuffer buf = new StringBuffer();
    formatFileName(new Date(n), buf);
    lastFileName = buf.toString();

    suffixLength = 0;

    if (lastFileName.endsWith(".gz")) {
      suffixLength = 3;
    } else if (lastFileName.endsWith(".zip")) {
      suffixLength = 4;
    }
  }

  /**
   * {@inheritDoc}
   */
  public RolloverDescription initialize(
    final String currentActiveFile, final boolean append) {
    long n = System.currentTimeMillis();
    nextCheck = ((n / 1000) + 1) * 1000;

    StringBuffer buf = new StringBuffer();
    formatFileName(new Date(n), buf);
    lastFileName = buf.toString();

    //
    //  RollingPolicyBase.activeFileName duplicates RollingFileAppender.file
    //    and should be removed.
    //
    if (activeFileName != null) {
      return new RolloverDescriptionImpl(activeFileName, append, null, null);
    } else if (currentActiveFile != null) {
      return new RolloverDescriptionImpl(
        currentActiveFile, append, null, null);
    } else {
      return new RolloverDescriptionImpl(
        lastFileName.substring(0, lastFileName.length() - suffixLength), append,
        null, null);
    }
  }

  /**
   * {@inheritDoc}
   */
  public RolloverDescription rollover(final String currentActiveFile) {
    long n = System.currentTimeMillis();
    nextCheck = ((n / 1000) + 1) * 1000;

    StringBuffer buf = new StringBuffer();
    formatFileName(new Date(n), buf);

    String newFileName = buf.toString();

    //
    //  if file names haven't changed, no rollover
    //
    if (newFileName.equals(lastFileName)) {
      return null;
    }

    Action renameAction = null;
    Action compressAction = null;
    String lastBaseName =
      lastFileName.substring(0, lastFileName.length() - suffixLength);
    String nextActiveFile =
      newFileName.substring(0, newFileName.length() - suffixLength);

    //
    //   if currentActiveFile is not lastBaseName then
    //        active file name is not following file pattern
    //        and requires a rename plus maintaining the same name
    if (!currentActiveFile.equals(lastBaseName)) {
      renameAction =
        new FileRenameAction(
          new File(currentActiveFile), new File(lastBaseName), true);
      nextActiveFile = currentActiveFile;
    }

    if (suffixLength == 3) {
      compressAction =
        new GZCompressAction(
          new File(lastBaseName), new File(lastFileName), true);
    }

    if (suffixLength == 4) {
      compressAction =
        new ZipCompressAction(
          new File(lastBaseName), new File(lastFileName), true);
    }

    lastFileName = newFileName;

    return new RolloverDescriptionImpl(
      nextActiveFile, false, renameAction, compressAction);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isTriggeringEvent(
    final Appender appender, final LoggingEvent event, final String filename,
    final long fileLength) {
    return System.currentTimeMillis() >= nextCheck;
  }
}
