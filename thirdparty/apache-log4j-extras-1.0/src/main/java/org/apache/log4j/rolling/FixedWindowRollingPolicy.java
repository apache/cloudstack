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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.pattern.PatternConverter;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.rolling.helper.FileRenameAction;
import org.apache.log4j.rolling.helper.GZCompressAction;
import org.apache.log4j.rolling.helper.ZipCompressAction;
import org.apache.log4j.helpers.LogLog;


/**
 * When rolling over, <code>FixedWindowRollingPolicy</code> renames files
 * according to a fixed window algorithm as described below.
 *
 * <p>The <b>ActiveFileName</b> property, which is required, represents the name
 * of the file where current logging output will be written.
 * The <b>FileNamePattern</b>  option represents the file name pattern for the
 * archived (rolled over) log files. If present, the <b>FileNamePattern</b>
 * option must include an integer token, that is the string "%i" somewhere
 * within the pattern.
 *
 * <p>Let <em>max</em> and <em>min</em> represent the values of respectively
 * the <b>MaxIndex</b> and <b>MinIndex</b> options. Let "foo.log" be the value
 * of the <b>ActiveFile</b> option and "foo.%i.log" the value of
 * <b>FileNamePattern</b>. Then, when rolling over, the file
 * <code>foo.<em>max</em>.log</code> will be deleted, the file
 * <code>foo.<em>max-1</em>.log</code> will be renamed as
 * <code>foo.<em>max</em>.log</code>, the file <code>foo.<em>max-2</em>.log</code>
 * renamed as <code>foo.<em>max-1</em>.log</code>, and so on,
 * the file <code>foo.<em>min+1</em>.log</code> renamed as
 * <code>foo.<em>min+2</em>.log</code>. Lastly, the active file <code>foo.log</code>
 * will be renamed as <code>foo.<em>min</em>.log</code> and a new active file name
 * <code>foo.log</code> will be created.
 *
 * <p>Given that this rollover algorithm requires as many file renaming
 * operations as the window size, large window sizes are discouraged. The
 * current implementation will automatically reduce the window size to 12 when
 * larger values are specified by the user.
 *
 *
 * @author Ceki G&uuml;lc&uuml;
 * */
public final class FixedWindowRollingPolicy extends RollingPolicyBase {

  /**
   * It's almost always a bad idea to have a large window size, say over 12.
   */
  private static final int MAX_WINDOW_SIZE = 12;

  /**
   * Index for oldest retained log file.
   */
  private int maxIndex;

  /**
   * Index for most recent log file.
   */
  private int minIndex;

  /**
   *  if true, then an explicit name for the active file was
   * specified using RollingFileAppender.file or the
   * redundent RollingPolicyBase.setActiveFile
   */
  private boolean explicitActiveFile;

  /**
   * Constructs a new instance.
   */
  public FixedWindowRollingPolicy() {
    minIndex = 1;
    maxIndex = 7;
  }

  /**
   * {@inheritDoc}
   */
  public void activateOptions() {
    super.activateOptions();

    if (maxIndex < minIndex) {
      LogLog.warn(
        "MaxIndex (" + maxIndex + ") cannot be smaller than MinIndex ("
        + minIndex + ").");
      LogLog.warn("Setting maxIndex to equal minIndex.");
      maxIndex = minIndex;
    }

    if ((maxIndex - minIndex) > MAX_WINDOW_SIZE) {
      LogLog.warn("Large window sizes are not allowed.");
      maxIndex = minIndex + MAX_WINDOW_SIZE;
      LogLog.warn("MaxIndex reduced to " + String.valueOf(maxIndex) + ".");
    }

    PatternConverter itc = getIntegerPatternConverter();

    if (itc == null) {
      throw new IllegalStateException(
        "FileNamePattern [" + getFileNamePattern()
        + "] does not contain a valid integer format specifier");
    }
  }

  /**
   * {@inheritDoc}
   */
  public RolloverDescription initialize(
    final String file, final boolean append) {
    String newActiveFile = file;
    explicitActiveFile = false;

    if (activeFileName != null) {
      explicitActiveFile = true;
      newActiveFile = activeFileName;
    }

    if (file != null) {
      explicitActiveFile = true;
      newActiveFile = file;
    }

    if (!explicitActiveFile) {
      StringBuffer buf = new StringBuffer();
      formatFileName(new Integer(minIndex), buf);
      newActiveFile = buf.toString();
    }

    return new RolloverDescriptionImpl(newActiveFile, append, null, null);
  }

  /**
   * {@inheritDoc}
   */
  public RolloverDescription rollover(final String currentFileName) {
    if (maxIndex >= 0) {
      int purgeStart = minIndex;

      if (!explicitActiveFile) {
        purgeStart++;
      }

      if (!purge(purgeStart, maxIndex)) {
        return null;
      }

      StringBuffer buf = new StringBuffer();
      formatFileName(new Integer(purgeStart), buf);

      String renameTo = buf.toString();
      String compressedName = renameTo;
      Action compressAction = null;

      if (renameTo.endsWith(".gz")) {
        renameTo = renameTo.substring(0, renameTo.length() - 3);
        compressAction =
          new GZCompressAction(
            new File(renameTo), new File(compressedName), true);
      } else if (renameTo.endsWith(".zip")) {
        renameTo = renameTo.substring(0, renameTo.length() - 4);
        compressAction =
          new ZipCompressAction(
            new File(renameTo), new File(compressedName), true);
      }

      FileRenameAction renameAction =
        new FileRenameAction(
          new File(currentFileName), new File(renameTo), false);

      return new RolloverDescriptionImpl(
        currentFileName, false, renameAction, compressAction);
    }

    return null;
  }

  /**
   * Get index of oldest log file to be retained.
   * @return index of oldest log file.
   */
  public int getMaxIndex() {
    return maxIndex;
  }

  /**
   * Get index of most recent log file.
   * @return index of oldest log file.
   */
  public int getMinIndex() {
    return minIndex;
  }

  /**
   * Set index of oldest log file to be retained.
   * @param maxIndex index of oldest log file to be retained.
   */
  public void setMaxIndex(int maxIndex) {
    this.maxIndex = maxIndex;
  }

  /**
   * Set index of most recent log file.
   * @param minIndex Index of most recent log file.
   */
  public void setMinIndex(int minIndex) {
    this.minIndex = minIndex;
  }

  /**
   * Purge and rename old log files in preparation for rollover
   * @param lowIndex low index
   * @param highIndex high index.  Log file associated with high
   * index will be deleted if needed.
   * @return true if purge was successful and rollover should be attempted.
   */
  private boolean purge(final int lowIndex, final int highIndex) {
    int suffixLength = 0;

    List renames = new ArrayList();
    StringBuffer buf = new StringBuffer();
    formatFileName(new Integer(lowIndex), buf);

    String lowFilename = buf.toString();

    if (lowFilename.endsWith(".gz")) {
      suffixLength = 3;
    } else if (lowFilename.endsWith(".zip")) {
      suffixLength = 4;
    }

    for (int i = lowIndex; i <= highIndex; i++) {
      File toRename = new File(lowFilename);
      boolean isBase = false;

      if (suffixLength > 0) {
        File toRenameBase =
          new File(
            lowFilename.substring(0, lowFilename.length() - suffixLength));

        if (toRename.exists()) {
          if (toRenameBase.exists()) {
            toRenameBase.delete();
          }
        } else {
          toRename = toRenameBase;
          isBase = true;
        }
      }

      if (toRename.exists()) {
        //
        //    if at upper index then
        //        attempt to delete last file
        //        if that fails then abandon purge
        if (i == highIndex) {
          if (!toRename.delete()) {
            return false;
          }

          break;
        }

        //
        //   if intermediate index
        //     add a rename action to the list
        buf.setLength(0);
        formatFileName(new Integer(i + 1), buf);

        String highFilename = buf.toString();
        String renameTo = highFilename;

        if (isBase) {
          renameTo =
            highFilename.substring(0, highFilename.length() - suffixLength);
        }

        renames.add(new FileRenameAction(toRename, new File(renameTo), true));
        lowFilename = highFilename;
      } else {
        break;
      }
    }

    //
    //   work renames backwards
    //
    for (int i = renames.size() - 1; i >= 0; i--) {
      Action action = (Action) renames.get(i);

      try {
        if (!action.execute()) {
          return false;
        }
      } catch (Exception ex) {
        LogLog.warn("Exception during purge in RollingFileAppender", ex);

        return false;
      }
    }

    return true;
  }
}
