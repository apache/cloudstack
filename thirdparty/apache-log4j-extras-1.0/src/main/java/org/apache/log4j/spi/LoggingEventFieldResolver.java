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

package org.apache.log4j.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Locale;


/**
 * A singleton helper utility which accepts a field name
 * and a LoggingEvent and returns the value of that field.
 *
 * This class defines a grammar used in creation of an expression-based Rule.
 *
 * The only available method is
 * Object getField(String fieldName, LoggingEvent event).
 *
 * Here is a description of the mapping of field names in the grammar
 * to fields on the logging event.  While the getField method returns an Object,
 * the individual types returned per field are described here:
 *
 * Field Name    Field value (String representation        Return type
 * LOGGER        category name (logger)                    String
 * LEVEL         level                                     Level
 * CLASS         locationInformation's class name          String
 * FILE          locationInformation's file name           String
 * LINE          locationInformation's line number         String
 * METHOD        locationInformation's method name         String
 * MSG           message                                   Object
 * NDC           NDC                                       String
 * EXCEPTION     throwable string representation           ThrowableInformation
 * TIMESTAMP     timestamp                                 Long
 * THREAD        thread                                    String
 * PROP.keyName  entry in the Property hashtable           String
 *               mapped to the key [keyName]

 * NOTE:  the values for the 'keyName' portion of the MDC and PROP mappings must
 * be an exact match to the key in the hashTable (case sensitive).
 *
 * If the passed-in field is null or doesn't match an entry
 * in the above-described mapping, an exception is thrown.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 * @author Paul Smith (psmith@apache.org)
 *
 */
public final class LoggingEventFieldResolver {
    /**
     * Keyword list.
     */
  public static final List KEYWORD_LIST = new ArrayList();
    /**
     * LOGGER string literal.
     */
  public static final String LOGGER_FIELD = "LOGGER";
    /**
     * LEVEL string literal.
     */
  public static final String LEVEL_FIELD = "LEVEL";
    /**
     * CLASS string literal.
     */
  public static final String CLASS_FIELD = "CLASS";
    /**
     * FILE string literal.
     */
  public static final String FILE_FIELD = "FILE";
    /**
     * LINE string literal.
     */
  public static final String LINE_FIELD = "LINE";
    /**
     * METHOD string literal.
     */
  public static final String METHOD_FIELD = "METHOD";
    /**
     * MSG string literal.
     */
  public static final String MSG_FIELD = "MSG";
    /**
     * NDC string literal.
     */
  public static final String NDC_FIELD = "NDC";
    /**
     * EXCEPTION string literal.
     */
  public static final String EXCEPTION_FIELD = "EXCEPTION";
    /**
     * TIMESTAMP string literal.
     */
  public static final String TIMESTAMP_FIELD = "TIMESTAMP";
    /**
     * THREAD string literal.
     */
  public static final String THREAD_FIELD = "THREAD";
    /**
     * PROP. string literal.
     */
  public static final String PROP_FIELD = "PROP.";
    /**
     * empty string literal.
     */
  public static final String EMPTY_STRING = "";
    /**
     * LOGGER string literal.
     */
  private static final LoggingEventFieldResolver RESOLVER =
    new LoggingEventFieldResolver();

    /**
     * Create new instance.
     */
  private LoggingEventFieldResolver() {
    super();
    KEYWORD_LIST.add(LOGGER_FIELD);
    KEYWORD_LIST.add(LEVEL_FIELD);
    KEYWORD_LIST.add(CLASS_FIELD);
    KEYWORD_LIST.add(FILE_FIELD);
    KEYWORD_LIST.add(LINE_FIELD);
    KEYWORD_LIST.add(METHOD_FIELD);
    KEYWORD_LIST.add(MSG_FIELD);
    KEYWORD_LIST.add(NDC_FIELD);
    KEYWORD_LIST.add(EXCEPTION_FIELD);
    KEYWORD_LIST.add(TIMESTAMP_FIELD);
    KEYWORD_LIST.add(THREAD_FIELD);
    KEYWORD_LIST.add(PROP_FIELD);
  }

    /**
     * Apply fields.
     * @param replaceText replacement text.
     * @param event logging event.
     * @return evaluted expression
     */
  public String applyFields(final String replaceText,
                            final LoggingEvent event) {
      if (replaceText == null) {
        return null;
      }
      StringTokenizer tokenizer = new StringTokenizer(replaceText);
      StringBuffer result = new StringBuffer();
      boolean found = false;

      while (tokenizer.hasMoreTokens()) {
          String token = tokenizer.nextToken();
          if (isField(token) || token.toUpperCase(Locale.US).startsWith(PROP_FIELD)) {
              result.append(getValue(token, event).toString());
              found = true;
          } else {
              result.append(token);
          }
      }
      if (found) {
        return result.toString();
      }
      return null;
  }

    /**
     * Get singleton instance.
     * @return singleton instance
     */
  public static LoggingEventFieldResolver getInstance() {
    return RESOLVER;
  }

    /**
     * Determines if specified string is a recognized field.
     * @param fieldName field name
     * @return true if recognized field.
     */
  public boolean isField(final String fieldName) {
    if (fieldName != null) {
        return (KEYWORD_LIST.contains(
                fieldName.toUpperCase(Locale.US))
                || fieldName.toUpperCase().startsWith(PROP_FIELD));
    }
    return false;
  }

    /**
     * Get value of field.
     * @param fieldName field
     * @param event event
     * @return value of field
     */
  public Object getValue(final String fieldName,
                         final LoggingEvent event) {
    String upperField = fieldName.toUpperCase(Locale.US);
    if (LOGGER_FIELD.equals(upperField)) {
      return event.getLoggerName();
    } else if (LEVEL_FIELD.equals(upperField)) {
      return event.getLevel();
    } else if (MSG_FIELD.equals(upperField)) {
      return event.getMessage();
    } else if (NDC_FIELD.equals(upperField)) {
      String ndcValue = event.getNDC();
      return ((ndcValue == null) ? EMPTY_STRING : ndcValue);
    } else if (EXCEPTION_FIELD.equals(upperField)) {
        String[] throwableRep = event.getThrowableStrRep();
        if (throwableRep == null) {
            return EMPTY_STRING;
        } else {
            return getExceptionMessage(throwableRep);
        }
    } else if (TIMESTAMP_FIELD.equals(upperField)) {
      return new Long(event.timeStamp);
    } else if (THREAD_FIELD.equals(upperField)) {
      return event.getThreadName();
    } else if (upperField.startsWith(PROP_FIELD)) {
      //note: need to use actual fieldname since case matters
      Object propValue = event.getMDC(fieldName.substring(5));
      return ((propValue == null) ? EMPTY_STRING : propValue.toString());
    } else {
        LocationInfo info = event.getLocationInformation();
        if (CLASS_FIELD.equals(upperField)) {
            return ((info == null) ? EMPTY_STRING : info.getClassName());
        } else if (FILE_FIELD.equals(upperField)) {
            return ((info == null) ? EMPTY_STRING : info.getFileName());
        } else if (LINE_FIELD.equals(upperField)) {
            return ((info == null) ? EMPTY_STRING : info.getLineNumber());
        } else if (METHOD_FIELD.equals(upperField)) {
            return ((info == null) ? EMPTY_STRING : info.getMethodName());
        }
    }

    //there wasn't a match, so throw a runtime exception
    throw new IllegalArgumentException("Unsupported field name: " + fieldName);
  }

    /**
     * Get message from throwable representation.
     * @param exception exception
     * @return message
     */
    private static String getExceptionMessage(final String[] exception) {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < exception.length; i++) {
            buff.append(exception[i]);
        }
        return buff.toString();
    }
}
