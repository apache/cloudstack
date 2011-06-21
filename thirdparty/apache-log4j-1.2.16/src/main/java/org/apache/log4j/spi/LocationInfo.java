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

// Contributors: Mathias Rupprecht <mmathias.rupprecht@fja.com>

package org.apache.log4j.spi;

import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
   The internal representation of caller location information.

   @since 0.8.3
*/
public class LocationInfo implements java.io.Serializable {

  /**
     Caller's line number.
  */
  transient String lineNumber;
  /**
     Caller's file name.
  */
  transient String fileName;
  /**
     Caller's fully qualified class name.
  */
  transient String className;
  /**
     Caller's method name.
  */
  transient String methodName;
  /**
     All available caller information, in the format
     <code>fully.qualified.classname.of.caller.methodName(Filename.java:line)</code>
    */
  public String fullInfo;

  private static StringWriter sw = new StringWriter();
  private static PrintWriter pw = new PrintWriter(sw);

  private static Method getStackTraceMethod;
  private static Method getClassNameMethod;
  private static Method getMethodNameMethod;
  private static Method getFileNameMethod;
  private static Method getLineNumberMethod;


  /**
     When location information is not available the constant
     <code>NA</code> is returned. Current value of this string
     constant is <b>?</b>.  */
  public final static String NA = "?";

  static final long serialVersionUID = -1325822038990805636L;

    /**
     * NA_LOCATION_INFO is provided for compatibility with log4j 1.3.
     * @since 1.2.15
     */
    public static final LocationInfo NA_LOCATION_INFO =
            new LocationInfo(NA, NA, NA, NA);



  // Check if we are running in IBM's visual age.
  static boolean inVisualAge = false;
  static {
    try {
      inVisualAge = Class.forName("com.ibm.uvm.tools.DebugSupport") != null;
      LogLog.debug("Detected IBM VisualAge environment.");
    } catch(Throwable e) {
      // nothing to do
    }
      try {
          Class[] noArgs = null;
          getStackTraceMethod = Throwable.class.getMethod("getStackTrace", noArgs);
          Class stackTraceElementClass = Class.forName("java.lang.StackTraceElement");
          getClassNameMethod = stackTraceElementClass.getMethod("getClassName", noArgs);
          getMethodNameMethod = stackTraceElementClass.getMethod("getMethodName", noArgs);
          getFileNameMethod = stackTraceElementClass.getMethod("getFileName", noArgs);
          getLineNumberMethod = stackTraceElementClass.getMethod("getLineNumber", noArgs);
      } catch(ClassNotFoundException ex) {
          LogLog.debug("LocationInfo will use pre-JDK 1.4 methods to determine location.");
      } catch(NoSuchMethodException ex) {
          LogLog.debug("LocationInfo will use pre-JDK 1.4 methods to determine location.");
      }
  }

  /**
     Instantiate location information based on a Throwable. We
     expect the Throwable <code>t</code>, to be in the format

       <pre>
        java.lang.Throwable
        ...
          at org.apache.log4j.PatternLayout.format(PatternLayout.java:413)
          at org.apache.log4j.FileAppender.doAppend(FileAppender.java:183)
        at org.apache.log4j.Category.callAppenders(Category.java:131)
        at org.apache.log4j.Category.log(Category.java:512)
        at callers.fully.qualified.className.methodName(FileName.java:74)
	...
       </pre>

       <p>However, we can also deal with JIT compilers that "lose" the
       location information, especially between the parentheses.
        @param t throwable used to determine location, may be null.
        @param fqnOfCallingClass class name of first class considered part of
           the logging framework.  Location will be site that calls a method on this class.

    */
    public LocationInfo(Throwable t, String fqnOfCallingClass) {
      if(t == null || fqnOfCallingClass == null)
	return;
      if (getLineNumberMethod != null) {
          try {
              Object[] noArgs = null;
              Object[] elements =  (Object[]) getStackTraceMethod.invoke(t, noArgs);
              String prevClass = NA;
              for(int i = elements.length - 1; i >= 0; i--) {
                  String thisClass = (String) getClassNameMethod.invoke(elements[i], noArgs);
                  if(fqnOfCallingClass.equals(thisClass)) {
                      int caller = i + 1;
                      if (caller < elements.length) {
                          className = prevClass;
                          methodName = (String) getMethodNameMethod.invoke(elements[caller], noArgs);
                          fileName = (String) getFileNameMethod.invoke(elements[caller], noArgs);
                          if (fileName == null) {
                              fileName = NA;
                          }
                          int line = ((Integer) getLineNumberMethod.invoke(elements[caller], noArgs)).intValue();
                          if (line < 0) {
                              lineNumber = NA;
                          } else {
                              lineNumber = String.valueOf(line);
                          }
                          StringBuffer buf = new StringBuffer();
                          buf.append(className);
                          buf.append(".");
                          buf.append(methodName);
                          buf.append("(");
                          buf.append(fileName);
                          buf.append(":");
                          buf.append(lineNumber);
                          buf.append(")");
                          this.fullInfo = buf.toString();
                      }
                      return;
                  }
                  prevClass = thisClass;
              }
              return;
          } catch(IllegalAccessException ex) {
              LogLog.debug("LocationInfo failed using JDK 1.4 methods", ex);
          } catch(InvocationTargetException ex) {
              if (ex.getTargetException() instanceof InterruptedException
                      || ex.getTargetException() instanceof InterruptedIOException) {
                  Thread.currentThread().interrupt();
              }
              LogLog.debug("LocationInfo failed using JDK 1.4 methods", ex);
          } catch(RuntimeException ex) {
              LogLog.debug("LocationInfo failed using JDK 1.4 methods", ex);
          }
      }

      String s;
      // Protect against multiple access to sw.
      synchronized(sw) {
	t.printStackTrace(pw);
	s = sw.toString();
	sw.getBuffer().setLength(0);
      }
      //System.out.println("s is ["+s+"].");
      int ibegin, iend;

      // Given the current structure of the package, the line
      // containing "org.apache.log4j.Category." should be printed just
      // before the caller.

      // This method of searching may not be fastest but it's safer
      // than counting the stack depth which is not guaranteed to be
      // constant across JVM implementations.
      ibegin = s.lastIndexOf(fqnOfCallingClass);
      if(ibegin == -1)
	return;

      //
      //   if the next character after the class name exists
      //       but is not a period, see if the classname is
      //       followed by a period earlier in the trace.
      //       Minimizes mistakeningly matching on a class whose
      //       name is a substring of the desired class.
      //       See bug 44888.
      if (ibegin + fqnOfCallingClass.length() < s.length() &&
              s.charAt(ibegin + fqnOfCallingClass.length()) != '.') {
          int i = s.lastIndexOf(fqnOfCallingClass + ".");
          if (i != -1) {
              ibegin = i;
          }
      }


      ibegin = s.indexOf(Layout.LINE_SEP, ibegin);
      if(ibegin == -1)
	return;
      ibegin+= Layout.LINE_SEP_LEN;

      // determine end of line
      iend = s.indexOf(Layout.LINE_SEP, ibegin);
      if(iend == -1)
	return;

      // VA has a different stack trace format which doesn't
      // need to skip the inital 'at'
      if(!inVisualAge) {
	// back up to first blank character
	ibegin = s.lastIndexOf("at ", iend);
	if(ibegin == -1)
	  return;
	// Add 3 to skip "at ";
	ibegin += 3;
      }
      // everything between is the requested stack item
      this.fullInfo = s.substring(ibegin, iend);
    }

    /**
     *   Appends a location fragment to a buffer to build the 
     *     full location info.
     *    @param buf StringBuffer to receive content.
     *    @param fragment fragment of location (class, method, file, line),
     *        if null the value of NA will be appended.
     *    @since 1.2.15
     */
    private static final void appendFragment(final StringBuffer buf,
                                             final String fragment) {
          if (fragment == null) {
             buf.append(NA);
          } else {
             buf.append(fragment);
          }
    }

    /**
     * Create new instance.
     * @param file source file name
     * @param classname class name
     * @param method method
     * @param line source line number
     *
     * @since 1.2.15
     */
    public LocationInfo(
      final String file,
      final String classname,
      final String method,
      final String line) {
      this.fileName = file;
      this.className = classname;
      this.methodName = method;
      this.lineNumber = line;
      StringBuffer buf = new StringBuffer();
	  appendFragment(buf, classname);
	  buf.append(".");
	  appendFragment(buf, method);
	  buf.append("(");
	  appendFragment(buf, file);
	  buf.append(":");
	  appendFragment(buf, line);
	  buf.append(")");
	  this.fullInfo = buf.toString();
    }

    /**
       Return the fully qualified class name of the caller making the
       logging request.
    */
    public
    String getClassName() {
      if(fullInfo == null) return NA;
      if(className == null) {
	// Starting the search from '(' is safer because there is
	// potentially a dot between the parentheses.
	int iend = fullInfo.lastIndexOf('(');
	if(iend == -1)
	  className = NA;
	else {
	  iend =fullInfo.lastIndexOf('.', iend);

	  // This is because a stack trace in VisualAge looks like:

          //java.lang.RuntimeException
	  //  java.lang.Throwable()
	  //  java.lang.Exception()
	  //  java.lang.RuntimeException()
	  //  void test.test.B.print()
	  //  void test.test.A.printIndirect()
	  //  void test.test.Run.main(java.lang.String [])
          int ibegin = 0;
	  if (inVisualAge) {
	    ibegin = fullInfo.lastIndexOf(' ', iend)+1;
          }

	  if(iend == -1)
	    className = NA;
	  else
	    className = this.fullInfo.substring(ibegin, iend);
	}
      }
      return className;
    }

    /**
       Return the file name of the caller.

       <p>This information is not always available.
    */
    public
    String getFileName() {
      if(fullInfo == null) return NA;

      if(fileName == null) {
	int iend = fullInfo.lastIndexOf(':');
	if(iend == -1)
	  fileName = NA;
	else {
	  int ibegin = fullInfo.lastIndexOf('(', iend - 1);
	  fileName = this.fullInfo.substring(ibegin + 1, iend);
	}
      }
      return fileName;
    }

    /**
       Returns the line number of the caller.

       <p>This information is not always available.
    */
    public
    String getLineNumber() {
      if(fullInfo == null) return NA;

      if(lineNumber == null) {
	int iend = fullInfo.lastIndexOf(')');
	int ibegin = fullInfo.lastIndexOf(':', iend -1);
	if(ibegin == -1)
	  lineNumber = NA;
	else
	  lineNumber = this.fullInfo.substring(ibegin + 1, iend);
      }
      return lineNumber;
    }

    /**
       Returns the method name of the caller.
    */
    public
    String getMethodName() {
      if(fullInfo == null) return NA;
      if(methodName == null) {
	int iend = fullInfo.lastIndexOf('(');
	int ibegin = fullInfo.lastIndexOf('.', iend);
	if(ibegin == -1)
	  methodName = NA;
	else
	  methodName = this.fullInfo.substring(ibegin + 1, iend);
      }
      return methodName;
    }
}
