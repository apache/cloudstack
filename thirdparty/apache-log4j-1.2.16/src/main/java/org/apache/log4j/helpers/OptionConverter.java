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

package org.apache.log4j.helpers;

import java.util.Properties;
import java.net.URL;
import java.io.InterruptedIOException;

import org.apache.log4j.Level;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.PropertyConfigurator;

// Contributors:   Avy Sharell (sharell@online.fr)
//                 Matthieu Verbert (mve@zurich.ibm.com)
//                 Colin Sampaleanu

/**
   A convenience class to convert property values to specific types.

   @author Ceki G&uuml;lc&uuml;
   @author Simon Kitching;
   @author Anders Kristensen
*/
public class OptionConverter {

  static String DELIM_START = "${";
  static char   DELIM_STOP  = '}';
  static int DELIM_START_LEN = 2;
  static int DELIM_STOP_LEN  = 1;

  /** OptionConverter is a static class. */
  private OptionConverter() {}

  public
  static
  String[] concatanateArrays(String[] l, String[] r) {
    int len = l.length + r.length;
    String[] a = new String[len];

    System.arraycopy(l, 0, a, 0, l.length);
    System.arraycopy(r, 0, a, l.length, r.length);

    return a;
  }

  public
  static
  String convertSpecialChars(String s) {
    char c;
    int len = s.length();
    StringBuffer sbuf = new StringBuffer(len);

    int i = 0;
    while(i < len) {
      c = s.charAt(i++);
      if (c == '\\') {
	c =  s.charAt(i++);
	if(c == 'n')      c = '\n';
	else if(c == 'r') c = '\r';
	else if(c == 't') c = '\t';
	else if(c == 'f') c = '\f';
	else if(c == '\b') c = '\b';
	else if(c == '\"') c = '\"';
	else if(c == '\'') c = '\'';
	else if(c == '\\') c = '\\';
      }
      sbuf.append(c);
    }
    return sbuf.toString();
  }


  /**
     Very similar to <code>System.getProperty</code> except
     that the {@link SecurityException} is hidden.

     @param key The key to search for.
     @param def The default value to return.
     @return the string value of the system property, or the default
     value if there is no property with that key.

     @since 1.1 */
  public
  static
  String getSystemProperty(String key, String def) {
    try {
      return System.getProperty(key, def);
    } catch(Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
      LogLog.debug("Was not allowed to read system property \""+key+"\".");
      return def;
    }
  }


  public
  static
  Object instantiateByKey(Properties props, String key, Class superClass,
				Object defaultValue) {

    // Get the value of the property in string form
    String className = findAndSubst(key, props);
    if(className == null) {
      LogLog.error("Could not find value for key " + key);
      return defaultValue;
    }
    // Trim className to avoid trailing spaces that cause problems.
    return OptionConverter.instantiateByClassName(className.trim(), superClass,
						  defaultValue);
  }

  /**
     If <code>value</code> is "true", then <code>true</code> is
     returned. If <code>value</code> is "false", then
     <code>true</code> is returned. Otherwise, <code>default</code> is
     returned.

     <p>Case of value is unimportant.  */
  public
  static
  boolean toBoolean(String value, boolean dEfault) {
    if(value == null)
      return dEfault;
    String trimmedVal = value.trim();
    if("true".equalsIgnoreCase(trimmedVal))
      return true;
    if("false".equalsIgnoreCase(trimmedVal))
      return false;
    return dEfault;
  }

  public
  static
  int toInt(String value, int dEfault) {
    if(value != null) {
      String s = value.trim();
      try {
	return Integer.valueOf(s).intValue();
      }
      catch (NumberFormatException e) {
	 LogLog.error("[" + s + "] is not in proper int form.");
	e.printStackTrace();
      }
    }
    return dEfault;
  }

  /**
     Converts a standard or custom priority level to a Level
     object.  <p> If <code>value</code> is of form
     "level#classname", then the specified class' toLevel method
     is called to process the specified level string; if no '#'
     character is present, then the default {@link org.apache.log4j.Level}
     class is used to process the level value.

     <p>As a special case, if the <code>value</code> parameter is
     equal to the string "NULL", then the value <code>null</code> will
     be returned.

     <p> If any error occurs while converting the value to a level,
     the <code>defaultValue</code> parameter, which may be
     <code>null</code>, is returned.

     <p> Case of <code>value</code> is insignificant for the level level, but is
     significant for the class name part, if present.

     @since 1.1 */
  public
  static
  Level toLevel(String value, Level defaultValue) {
    if(value == null)
      return defaultValue;
      
    value = value.trim();

    int hashIndex = value.indexOf('#');
    if (hashIndex == -1) {
      if("NULL".equalsIgnoreCase(value)) {
	return null;
      } else {
	// no class name specified : use standard Level class
	return(Level) Level.toLevel(value, defaultValue);
      }
    }

    Level result = defaultValue;

    String clazz = value.substring(hashIndex+1);
    String levelName = value.substring(0, hashIndex);

    // This is degenerate case but you never know.
    if("NULL".equalsIgnoreCase(levelName)) {
	return null;
    }

    LogLog.debug("toLevel" + ":class=[" + clazz + "]"
		 + ":pri=[" + levelName + "]");

    try {
      Class customLevel = Loader.loadClass(clazz);

      // get a ref to the specified class' static method
      // toLevel(String, org.apache.log4j.Level)
      Class[] paramTypes = new Class[] { String.class,
					 org.apache.log4j.Level.class
                                       };
      java.lang.reflect.Method toLevelMethod =
                      customLevel.getMethod("toLevel", paramTypes);

      // now call the toLevel method, passing level string + default
      Object[] params = new Object[] {levelName, defaultValue};
      Object o = toLevelMethod.invoke(null, params);

      result = (Level) o;
    } catch(ClassNotFoundException e) {
      LogLog.warn("custom level class [" + clazz + "] not found.");
    } catch(NoSuchMethodException e) {
      LogLog.warn("custom level class [" + clazz + "]"
        + " does not have a class function toLevel(String, Level)", e);
    } catch(java.lang.reflect.InvocationTargetException e) {
        if (e.getTargetException() instanceof InterruptedException
                || e.getTargetException() instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
        }
      LogLog.warn("custom level class [" + clazz + "]"
		   + " could not be instantiated", e);
    } catch(ClassCastException e) {
      LogLog.warn("class [" + clazz
        + "] is not a subclass of org.apache.log4j.Level", e);
    } catch(IllegalAccessException e) {
      LogLog.warn("class ["+clazz+
		   "] cannot be instantiated due to access restrictions", e);
    } catch(RuntimeException e) {
      LogLog.warn("class ["+clazz+"], level ["+levelName+
		   "] conversion failed.", e);
    }
    return result;
   }

  public
  static
  long toFileSize(String value, long dEfault) {
    if(value == null)
      return dEfault;

    String s = value.trim().toUpperCase();
    long multiplier = 1;
    int index;

    if((index = s.indexOf("KB")) != -1) {
      multiplier = 1024;
      s = s.substring(0, index);
    }
    else if((index = s.indexOf("MB")) != -1) {
      multiplier = 1024*1024;
      s = s.substring(0, index);
    }
    else if((index = s.indexOf("GB")) != -1) {
      multiplier = 1024*1024*1024;
      s = s.substring(0, index);
    }
    if(s != null) {
      try {
	return Long.valueOf(s).longValue() * multiplier;
      }
      catch (NumberFormatException e) {
	LogLog.error("[" + s + "] is not in proper int form.");
	LogLog.error("[" + value + "] not in expected format.", e);
      }
    }
    return dEfault;
  }

  /**
     Find the value corresponding to <code>key</code> in
     <code>props</code>. Then perform variable substitution on the
     found value.

 */
  public
  static
  String findAndSubst(String key, Properties props) {
    String value = props.getProperty(key);
    if(value == null)
      return null;

    try {
      return substVars(value, props);
    } catch(IllegalArgumentException e) {
      LogLog.error("Bad option value ["+value+"].", e);
      return value;
    }
  }

  /**
     Instantiate an object given a class name. Check that the
     <code>className</code> is a subclass of
     <code>superClass</code>. If that test fails or the object could
     not be instantiated, then <code>defaultValue</code> is returned.

     @param className The fully qualified class name of the object to instantiate.
     @param superClass The class to which the new object should belong.
     @param defaultValue The object to return in case of non-fulfillment
   */
  public
  static
  Object instantiateByClassName(String className, Class superClass,
				Object defaultValue) {
    if(className != null) {
      try {
	Class classObj = Loader.loadClass(className);
	if(!superClass.isAssignableFrom(classObj)) {
	  LogLog.error("A \""+className+"\" object is not assignable to a \""+
		       superClass.getName() + "\" variable.");
	  LogLog.error("The class \""+ superClass.getName()+"\" was loaded by ");
	  LogLog.error("["+superClass.getClassLoader()+"] whereas object of type ");
	  LogLog.error("\"" +classObj.getName()+"\" was loaded by ["
		       +classObj.getClassLoader()+"].");
	  return defaultValue;
	}
	return classObj.newInstance();
      } catch (ClassNotFoundException e) {
	    LogLog.error("Could not instantiate class [" + className + "].", e);
      } catch (IllegalAccessException e) {
	    LogLog.error("Could not instantiate class [" + className + "].", e);
      } catch (InstantiationException e) {
        LogLog.error("Could not instantiate class [" + className + "].", e);
      } catch (RuntimeException e) {
	    LogLog.error("Could not instantiate class [" + className + "].", e);
      }
    }
    return defaultValue;
  }


  /**
     Perform variable substitution in string <code>val</code> from the
     values of keys found in the system propeties.

     <p>The variable substitution delimeters are <b>${</b> and <b>}</b>.

     <p>For example, if the System properties contains "key=value", then
     the call
     <pre>
     String s = OptionConverter.substituteVars("Value of key is ${key}.");
     </pre>

     will set the variable <code>s</code> to "Value of key is value.".

     <p>If no value could be found for the specified key, then the
     <code>props</code> parameter is searched, if the value could not
     be found there, then substitution defaults to the empty string.

     <p>For example, if system propeties contains no value for the key
     "inexistentKey", then the call

     <pre>
     String s = OptionConverter.subsVars("Value of inexistentKey is [${inexistentKey}]");
     </pre>
     will set <code>s</code> to "Value of inexistentKey is []"

     <p>An {@link java.lang.IllegalArgumentException} is thrown if
     <code>val</code> contains a start delimeter "${" which is not
     balanced by a stop delimeter "}". </p>

     <p><b>Author</b> Avy Sharell</a></p>

     @param val The string on which variable substitution is performed.
     @throws IllegalArgumentException if <code>val</code> is malformed.

  */
  public static
  String substVars(String val, Properties props) throws
                        IllegalArgumentException {

    StringBuffer sbuf = new StringBuffer();

    int i = 0;
    int j, k;

    while(true) {
      j=val.indexOf(DELIM_START, i);
      if(j == -1) {
	// no more variables
	if(i==0) { // this is a simple string
	  return val;
	} else { // add the tail string which contails no variables and return the result.
	  sbuf.append(val.substring(i, val.length()));
	  return sbuf.toString();
	}
      } else {
	sbuf.append(val.substring(i, j));
	k = val.indexOf(DELIM_STOP, j);
	if(k == -1) {
	  throw new IllegalArgumentException('"'+val+
		      "\" has no closing brace. Opening brace at position " + j
					     + '.');
	} else {
	  j += DELIM_START_LEN;
	  String key = val.substring(j, k);
	  // first try in System properties
	  String replacement = getSystemProperty(key, null);
	  // then try props parameter
	  if(replacement == null && props != null) {
	    replacement =  props.getProperty(key);
	  }

	  if(replacement != null) {
	    // Do variable substitution on the replacement string
	    // such that we can solve "Hello ${x2}" as "Hello p1" 
            // the where the properties are
	    // x1=p1
            // x2=${x1}
	    String recursiveReplacement = substVars(replacement, props);
	    sbuf.append(recursiveReplacement);
	  }
	  i = k + DELIM_STOP_LEN;
	}
      }
    }
  }


  /**
     Configure log4j given a URL.

     <p>The url must point to a file or resource which will be interpreted by
     a new instance of a log4j configurator.

     <p>All configurations steps are taken on the
     <code>hierarchy</code> passed as a parameter.

     <p>
     @param url The location of the configuration file or resource.
     @param clazz The classname, of the log4j configurator which will parse
     the file or resource at <code>url</code>. This must be a subclass of
     {@link Configurator}, or null. If this value is null then a default
     configurator of {@link PropertyConfigurator} is used, unless the
     filename pointed to by <code>url</code> ends in '.xml', in which case
     {@link org.apache.log4j.xml.DOMConfigurator} is used.
     @param hierarchy The {@link org.apache.log4j.Hierarchy} to act on.

     @since 1.1.4 */

  static
  public
  void selectAndConfigure(URL url, String clazz, LoggerRepository hierarchy) {
   Configurator configurator = null;
   String filename = url.getFile();

   if(clazz == null && filename != null && filename.endsWith(".xml")) {
     clazz = "org.apache.log4j.xml.DOMConfigurator";
   }

   if(clazz != null) {
     LogLog.debug("Preferred configurator class: " + clazz);
     configurator = (Configurator) instantiateByClassName(clazz,
							  Configurator.class,
							  null);
     if(configurator == null) {
   	  LogLog.error("Could not instantiate configurator ["+clazz+"].");
   	  return;
     }
   } else {
     configurator = new PropertyConfigurator();
   }

   configurator.doConfigure(url, hierarchy);
  }
}
