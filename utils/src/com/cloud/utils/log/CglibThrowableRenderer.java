/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.utils.log;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

public final class CglibThrowableRenderer {
    /**
     * Throwable.getStackTrace() method.
     */
    private Method getStackTraceMethod;
    /**
     * StackTraceElement.getClassName() method.
     */
    private Method getClassNameMethod;


    /**
     * Construct new instance.
     */
    public CglibThrowableRenderer() {
        try {
            Class[] noArgs = null;
            getStackTraceMethod = Throwable.class.getMethod("getStackTrace", noArgs);
            Class ste = Class.forName("java.lang.StackTraceElement");
            getClassNameMethod = ste.getMethod("getClassName", noArgs);
        } catch(Exception ex) {
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] doRender(final Throwable throwable) {
        try {
            Object[] noArgs = null;
            Object[] elements = (Object[]) getStackTraceMethod.invoke(throwable, noArgs);
            String[] lines = new String[elements.length + 1];
            lines[0] = throwable.toString();
            Map classMap = new HashMap();
            for(int i = 0; i < elements.length; i++) {
                lines[i+1] = formatElement(elements[i], classMap);
            }
            return lines;
        } catch(Exception ex) {
            return null;
        }
    }

    /**
     * Format one element from stack trace.
     * @param element element, may not be null.
     * @param classMap map of class name to location.
     * @return string representation of element.
     */
    private String formatElement(final Object element, final Map classMap) {
        StringBuffer buf = new StringBuffer("\tat ");
        buf.append(element);
        try {
            String className = getClassNameMethod.invoke(element, (Object[]) null).toString();
            Object classDetails = classMap.get(className);
            if (classDetails != null) {
                buf.append(classDetails);
            } else {
                Class cls = findClass(className);
                int detailStart = buf.length();
                buf.append('[');
                try {
                    CodeSource source = cls.getProtectionDomain().getCodeSource();
                    if (source != null) {
                        URL locationURL = source.getLocation();
                        if (locationURL != null) {
                            //
                            //   if a file: URL
                            //
                            if ("file".equals(locationURL.getProtocol())) {
                                String path = locationURL.getPath();
                                if (path != null) {
                                    //
                                    //  find the last file separator character
                                    //
                                    int lastSlash = path.lastIndexOf('/');
                                    int lastBack = path.lastIndexOf(File.separatorChar);
                                    if (lastBack > lastSlash) {
                                        lastSlash = lastBack;
                                    }
                                    //
                                    //  if no separator or ends with separator (a directory)
                                    //     then output the URL, otherwise just the file name.
                                    //
                                    if (lastSlash <= 0 || lastSlash == path.length() - 1) {
                                        buf.append(locationURL);
                                    } else {
                                        buf.append(path.substring(lastSlash + 1));
                                    }
                                }
                            } else {
                                buf.append(locationURL);
                            }
                        }
                    }
                } catch(SecurityException ex) {
                }
                buf.append(':');
                Package pkg = cls.getPackage();
                if (pkg != null) {
                    String implVersion = pkg.getImplementationVersion();
                    if (implVersion != null) {
                        buf.append(implVersion);
                    }
                }
                buf.append(']');
                classMap.put(className, buf.substring(detailStart));
            }
        } catch(Exception ex) {
        }
        return buf.toString();
    }

    /**
     * Find class given class name.
     * @param className class name, may not be null.
     * @return class, will not be null.
     * @throws ClassNotFoundException thrown if class can not be found.
     */
    private Class findClass(final String className) throws ClassNotFoundException {
     try {
       return Thread.currentThread().getContextClassLoader().loadClass(className);
     } catch (ClassNotFoundException e) {
       try {
         return Class.forName(className);
       } catch (ClassNotFoundException e1) {
          return getClass().getClassLoader().loadClass(className);
      }
    }
  }

}
