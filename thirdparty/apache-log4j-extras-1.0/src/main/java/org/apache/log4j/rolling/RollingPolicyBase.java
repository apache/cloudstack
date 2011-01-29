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

import org.apache.log4j.pattern.FormattingInfo;
import org.apache.log4j.pattern.PatternConverter;
import org.apache.log4j.pattern.PatternParser;
import org.apache.log4j.pattern.IntegerPatternConverter;
import org.apache.log4j.pattern.DatePatternConverter;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.OptionHandler;

import java.util.ArrayList;
import java.util.List;


/**
 * Implements methods common to most, it not all, rolling
 * policies. Currently such methods are limited to a compression mode
 * getter/setter.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Curt Arnold
 */
public abstract class RollingPolicyBase
  implements RollingPolicy, OptionHandler {
    /**
     * Error message.
     */
    private static final String FNP_NOT_SET =
      "The FileNamePattern option must be set before using RollingPolicy. ";

    /**
     *   Reference for error message.
     */
    private static final String SEE_FNP_NOT_SET =
      "See also http://logging.apache.org/log4j/codes.html#tbr_fnp_not_set";

  /**
   * File name pattern converters.
   */
  private PatternConverter[] patternConverters;

  /**
   * File name field specifiers.
   */
  private FormattingInfo[] patternFields;

  /**
   * File name pattern.
   */
  private String fileNamePatternStr;

  /**
   * Active file name may be null.
   * Duplicates FileAppender.file and should be removed.
   */
  protected String activeFileName;

  /**
   * {@inheritDoc}
   */
  public void activateOptions() {
      // find out period from the filename pattern
      if (fileNamePatternStr != null) {
        parseFileNamePattern();
      } else {
        LogLog.warn(FNP_NOT_SET);
        LogLog.warn(SEE_FNP_NOT_SET);
        throw new IllegalStateException(FNP_NOT_SET + SEE_FNP_NOT_SET);
      }

  }

  /**
   * Set file name pattern.
   * @param fnp file name pattern.
   */
  public void setFileNamePattern(String fnp) {
    fileNamePatternStr = fnp;
  }

  /**
   * Get file name pattern.
   * @return file name pattern.
   */
  public String getFileNamePattern() {
    return fileNamePatternStr;
  }

  /**
   * ActiveFileName can be left unset, i.e. as null.
   * @param afn active file name.
   * @deprecated Duplicates FileAppender.file and should be removed
   */
  public void setActiveFileName(String afn) {
    activeFileName = afn;
  }

  /**
   * Return the value of the <b>ActiveFile</b> option.
   * @deprecated Duplicates FileAppender.file and should be removed
   * @return active file name.
  */
  public String getActiveFileName() {
    return activeFileName;
  }

  /**
   *   Parse file name pattern.
   */
  protected final void parseFileNamePattern() {
    List converters = new ArrayList();
    List fields = new ArrayList();

    PatternParser.parse(
      fileNamePatternStr, converters, fields, null,
      PatternParser.getFileNamePatternRules());
    patternConverters = new PatternConverter[converters.size()];
    patternConverters =
      (PatternConverter[]) converters.toArray(patternConverters);
    patternFields = new FormattingInfo[converters.size()];
    patternFields = (FormattingInfo[]) fields.toArray(patternFields);
  }

  /**
   * Format file name.
   *
   * @param obj object to be evaluted in formatting, may not be null.
   * @param buf string buffer to which formatted file name is appended, may not be null.
   */
  protected final void formatFileName(
    final Object obj, final StringBuffer buf) {
    for (int i = 0; i < patternConverters.length; i++) {
      int fieldStart = buf.length();
      patternConverters[i].format(obj, buf);

      if (patternFields[i] != null) {
        patternFields[i].format(fieldStart, buf);
      }
    }
  }

  protected final PatternConverter getDatePatternConverter() {
      for (int i = 0; i < patternConverters.length; i++) {
        if (patternConverters[i] instanceof DatePatternConverter) {
          return patternConverters[i];
        }
      }
      return null;

  }

  protected final PatternConverter getIntegerPatternConverter() {
      for (int i = 0; i < patternConverters.length; i++) {
        if (patternConverters[i] instanceof IntegerPatternConverter) {
          return patternConverters[i];
        }
      }
      return null;
  }

}
