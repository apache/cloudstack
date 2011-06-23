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
package org.apache.log4j.lf5;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * LogRecord.  A LogRecord encapsulates the details of your desired log
 * request.
 *
 * @author Michael J. Sikorsky
 * @author Robert Shaw
 */

// Contributed by ThoughtWorks Inc.

public abstract class LogRecord implements java.io.Serializable {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------
  protected static long _seqCount = 0;

  protected LogLevel _level;
  protected String _message;
  protected long _sequenceNumber;
  protected long _millis;
  protected String _category;
  protected String _thread;
  protected String _thrownStackTrace;
  protected Throwable _thrown;
  protected String _ndc;
  protected String _location;

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------

  public LogRecord() {
    super();

    _millis = System.currentTimeMillis();
    _category = "Debug";
    _message = "";
    _level = LogLevel.INFO;
    _sequenceNumber = getNextId();
    _thread = Thread.currentThread().toString();
    _ndc = "";
    _location = "";
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Get the level of this LogRecord.
   *
   * @return The LogLevel of this record.
   * @see #setLevel(LogLevel)
   * @see LogLevel
   */
  public LogLevel getLevel() {
    return (_level);
  }

  /**
   * Set the level of this LogRecord.
   *
   * @param level The LogLevel for this record.
   * @see #getLevel()
   * @see LogLevel
   */
  public void setLevel(LogLevel level) {
    _level = level;
  }

  /**
   * Abstract method. Must be overridden to indicate what log level
   * to show in red.
   */
  public abstract boolean isSevereLevel();

  /**
   * @return true if getThrown().toString() is a non-empty string.
   */
  public boolean hasThrown() {
    Throwable thrown = getThrown();
    if (thrown == null) {
      return false;
    }
    String thrownString = thrown.toString();
    return thrownString != null && thrownString.trim().length() != 0;
  }

  /**
   * @return true if isSevereLevel() or hasThrown() returns true.
   */
  public boolean isFatal() {
    return isSevereLevel() || hasThrown();
  }

  /**
   * Get the category asscociated with this LogRecord.  For a more detailed
   * description of what a category is see setCategory().
   *
   * @return The category of this record.
   * @see #setCategory(String)
   */
  public String getCategory() {
    return (_category);
  }

  /**
   * Set the category associated with this LogRecord. A category represents
   * a hierarchical dot (".") separated namespace for messages.
   * The definition of a category is application specific, but a common convention
   * is as follows:
   *
   * <p>
   * When logging messages
   * for a particluar class you can use its class name:
   * com.thoughtworks.framework.servlet.ServletServiceBroker.<br><br>
   * Futhermore, to log a message for a particular method in a class
   * add the method name:
   * com.thoughtworks.framework.servlet.ServletServiceBroker.init().
   * </p>
   *
   * @param category The category for this record.
   * @see #getCategory()
   */
  public void setCategory(String category) {
    _category = category;
  }

  /**
   * Get the message asscociated with this LogRecord.
   *
   * @return The message of this record.
   * @see #setMessage(String)
   */
  public String getMessage() {
    return (_message);
  }

  /**
   * Set the message associated with this LogRecord.
   *
   * @param message The message for this record.
   * @see #getMessage()
   */
  public void setMessage(String message) {
    _message = message;
  }

  /**
   * Get the sequence number associated with this LogRecord.  Sequence numbers
   * are generally assigned when a LogRecord is constructed.  Sequence numbers
   * start at 0 and increase with each newly constructed LogRocord.
   *
   * @return The sequence number of this record.
   * @see #setSequenceNumber(long)
   */
  public long getSequenceNumber() {
    return (_sequenceNumber);
  }

  /**
   * Set the sequence number assocsiated with this LogRecord.  A sequence number
   * will automatically be assigned to evey newly constructed LogRecord, however,
   * this method can override the value.
   *
   * @param number The sequence number.
   * @see #getSequenceNumber()
   */
  public void setSequenceNumber(long number) {
    _sequenceNumber = number;
  }

  /**
   * Get the event time of this record in milliseconds from 1970.
   * When a LogRecord is constructed the event time is set but may be
   * overridden by calling setMillis();
   *
   * @return The event time of this record in milliseconds from 1970.
   * @see #setMillis(long)
   */
  public long getMillis() {
    return _millis;
  }

  /**
   * Set the event time of this record.  When a LogRecord is constructed
   * the event time is set but may be overridden by calling this method.
   *
   * @param millis The time in milliseconds from 1970.
   * @see #getMillis()
   */
  public void setMillis(long millis) {
    _millis = millis;
  }

  /**
   * Get the thread description asscociated with this LogRecord.  When a
   * LogRecord is constructed, the thread description is set by calling:
   * Thread.currentThread().toString().  You may supply a thread description
   * of your own by calling the setThreadDescription(String) method.
   *
   * @return The thread description of this record.
   * @see #setThreadDescription(String)
   */
  public String getThreadDescription() {
    return (_thread);
  }

  /**
   * Set the thread description associated with this LogRecord.  When a
   * LogRecord is constructed, the thread description is set by calling:
   * Thread.currentThread().toString().  You may supply a thread description
   * of your own by calling this method.
   *
   * @param threadDescription The description of the thread for this record.
   * @see #getThreadDescription()
   */
  public void setThreadDescription(String threadDescription) {
    _thread = threadDescription;
  }

  /**
   * Get the stack trace in a String-based format for the associated Throwable
   * of this LogRecord.  The stack trace in a String-based format is set
   * when the setThrown(Throwable) method is called.
   *
   * <p>
   * Why do we need this method considering that we
   * have the getThrown() and setThrown() methods?
   * A Throwable object may not be serializable, however, a String representation
   * of it is.  Users of LogRecords should generally call this method over
   * getThrown() for the reasons of serialization.
   * </p>
   *
   * @return The Stack Trace for the asscoiated Throwable of this LogRecord.
   * @see #setThrown(Throwable)
   * @see #getThrown()
   */
  public String getThrownStackTrace() {
    return (_thrownStackTrace);
  }

  /**
   * Set the ThrownStackTrace for the log record.
   *
   * @param trace A String to associate with this LogRecord
   * @see #getThrownStackTrace()
   */
  public void setThrownStackTrace(String trace) {
    _thrownStackTrace = trace;
  }

  /**
   * Get the Throwable associated with this LogRecord.
   *
   * @return The LogLevel of this record.
   * @see #setThrown(Throwable)
   * @see #getThrownStackTrace()
   */
  public Throwable getThrown() {
    return (_thrown);
  }

  /**
   * Set the Throwable associated with this LogRecord.  When this method
   * is called, the stack trace in a String-based format is made
   * available via the getThrownStackTrace() method.
   *
   * @param thrown A Throwable to associate with this LogRecord.
   * @see #getThrown()
   * @see #getThrownStackTrace()
   */
  public void setThrown(Throwable thrown) {
    if (thrown == null) {
      return;
    }
    _thrown = thrown;
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    thrown.printStackTrace(out);
    out.flush();
    _thrownStackTrace = sw.toString();
    try {
      out.close();
      sw.close();
    } catch (IOException e) {
      // Do nothing, this should not happen as it is StringWriter.
    }
    out = null;
    sw = null;
  }

  /**
   * Return a String representation of this LogRecord.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("LogRecord: [" + _level + ", " + _message + "]");
    return (buf.toString());
  }

  /**
   * Get the NDC (nested diagnostic context) for this record.
   *
   * @return The string representing the NDC.
   */
  public String getNDC() {
    return _ndc;
  }

  /**
   * Set the NDC (nested diagnostic context) for this record.
   *
   * @param ndc A string representing the NDC.
   */
  public void setNDC(String ndc) {
    _ndc = ndc;
  }

  /**
   * Get the location in code where this LogRecord originated.
   *
   * @return The string containing the location information.
   */
  public String getLocation() {
    return _location;
  }

  /**
   * Set the location in code where this LogRecord originated.
   *
   * @param location A string containing location information.
   */
  public void setLocation(String location) {
    _location = location;
  }

  /**
   * Resets that sequence number to 0.
   *
   */
  public static synchronized void resetSequenceNumber() {
    _seqCount = 0;
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  protected static synchronized long getNextId() {
    _seqCount++;
    return _seqCount;
  }
  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}



