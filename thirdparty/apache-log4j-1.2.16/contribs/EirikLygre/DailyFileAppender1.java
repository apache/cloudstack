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



package org.apache.log4j;

import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.helpers.QuietWriter;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ErrorCode;

/**
   DailyFileAppender extends FileAppender to use filenames formatted with
   date/time information. The filename is recomputed every day at midnight.
   Note that the filename doesn't have to change every day, making it possible
   to have logfiles which are per-week or per-month.

   The appender computes the proper filename using the formats specified in
   <a href="http://java.sun.com/j2se/1.3/docs/api/java/text/SimpleDateFormat.html">
   java.text.SimpleDateFormat</a>. The format requires that most static text is
   enclosed in single quotes, which are removed. The examples below show how
   quotes are used to embed static information in the format.

   Sample filenames:

<code>
     Filename pattern                     Filename
     "'/logs/trace-'yyyy-MM-dd'.log'"     /logs/trace-2000-12-31.log
     "'/logs/trace-'yyyy-ww'.log'"        /logs/trace-2000-52.log
</code>

   @author <a HREF="mailto:eirik.lygre@evita.no">Eirik Lygre</a>
*/
public class DailyFileAppender extends FileAppender {

  /**
     A string constant used in naming the option for setting the
     filename pattern. Current value of this string constant is
     <strong>FileNamePattern</strong>.
   */
  static final public String FILE_NAME_PATTERN_OPTION = "FilePattern";
  
  /**
     The filename pattern
  */
  private String fileNamePattern = null;

  /**
     The actual formatted filename that is currently being written to
  */
  private String currentFileName = null;

  /**
     The timestamp when we shall next recompute the filename
  */
  private long nextFilenameComputingMillis = System.currentTimeMillis () - 1;

  /**
     The default constructor does no longer set a default layout nor a
     default output target.  */
  public
  DailyFileAppender() {
  }

  /**
    Instantiate a RollingFileAppender and open the file designated by
    <code>filename</code>. The opened filename will become the ouput
    destination for this appender.

    <p>If the <code>append</code> parameter is true, the file will be
    appended to. Otherwise, the file desginated by
    <code>filename</code> will be truncated before being opened.
  */
  public DailyFileAppender (Layout layout,String filename,boolean append) throws IOException {
    super(layout, filename, append);
  }

  /**
     Instantiate a FileAppender and open the file designated by
    <code>filename</code>. The opened filename will become the output
    destination for this appender.

    <p>The file will be appended to.  */
  public DailyFileAppender (Layout layout,String filename) throws IOException {
    super(layout, filename);
  }
  
  /**
     Set the current output file.

     The function will compute a new filename, and open a new file only
     when the name has changed.

     The function is automatically called once a day, to allow for
     daily files -- the purpose of this class.
  */

  public
  synchronized
  void setFile(String fileName, boolean append) throws IOException {

    /* Compute filename, but only if fileNamePattern is specified */
    if (fileNamePattern == null) {
      errorHandler.error("Missing file pattern (" + FILE_NAME_PATTERN_OPTION + ") in setFile().");
      return;
    }

    Date now = new Date();

    fileName = new SimpleDateFormat(fileNamePattern).format (now);
    if (fileName.equals(currentFileName))
      return;

    /* Set up next filename checkpoint */
    DailyFileAppenderCalendar c = new DailyFileAppenderCalendar();
    c.rollToNextDay ();
    nextFilenameComputingMillis = c.getTimeInMillis ();

    currentFileName = fileName;

    super.setFile(fileName, append);
  }

  /**
     This method differentiates RollingFileAppender from its super
     class.  

  */
  protected
  void subAppend(LoggingEvent event) {
     
     if (System.currentTimeMillis () >= nextFilenameComputingMillis) {
      try {
        setFile (super.fileName, super.fileAppend);
      }
      catch(IOException e) {
        System.err.println("setFile(null, false) call failed.");
        e.printStackTrace();
      }
    }

    super.subAppend(event);
  } 

  /**
     Retuns the option names for this component, namely {@link
     #FILE_NAME_PATTERN_OPTION} in
     addition to the options of {@link FileAppender#getOptionStrings
     FileAppender}.
  */
  public
  String[] getOptionStrings() {

    return OptionConverter.concatanateArrays(super.getOptionStrings(),
		 new String[] {FILE_NAME_PATTERN_OPTION});
  }

  /**
     Set the options for the appender
  */
  public
  void setOption(String key, String value) {
    super.setOption(key, value);    
    if(key.equalsIgnoreCase(FILE_NAME_PATTERN_OPTION)) {
      fileNamePattern = value;
    }
  }
  
  /**
     If the a value for {@link #FILE_OPTION} is non-null, then {@link
     #setFile} is called with the values of {@link #FILE_OPTION} and
     {@link #APPEND_OPTION}.

     @since 0.8.1 */
  public
  void activateOptions() {
    try {
	   setFile(null, super.fileAppend);
    }
    catch(java.io.IOException e) {
	   errorHandler.error("setFile(null,"+fileAppend+") call failed.",
		  	   e, ErrorCode.FILE_OPEN_FAILURE);
    }
  }
}

/**
   DailyFileAppenderCalendar is a helper class to DailyFileAppender. Using
   this class, it is easy to compute and access the next Millis()
 
   It subclasses the standard
   <a href="http://java.sun.com/j2se/1.3/docs/api/java/text/GregorianCalendar.html">
   java.util.GregorianCalendar</a>-object, to allow access to the protected
   function getTimeInMillis(), which it then exports.

   @author <a HREF="mailto:eirik.lygre@evita.no">Eirik Lygre</a>
*/
class DailyFileAppenderCalendar extends java.util.GregorianCalendar
{
  /**
     Returns the current time in Millis
  */
  public long getTimeInMillis() {
    return super.getTimeInMillis();
  }

  /**
     Roll the date to the next hour, with minute, second and millisecond
     set to zero.
  */
  public void rollToNextDay () {
    this.add(java.util.Calendar.DATE, 0);
    this.add(java.util.Calendar.HOUR, 0);
    this.set(java.util.Calendar.MINUTE, 0);
    this.set(java.util.Calendar.SECOND, 0);
    this.set(java.util.Calendar.MILLISECOND, 0);
  }
}