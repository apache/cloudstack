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

package org.apache.log4j.config;

import org.apache.log4j.Priority;
import org.apache.log4j.helpers.LogLog;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
   Used for inferring configuration information for a log4j's component.

   @author  Anders Kristensen
 */
public class PropertyGetter {
  protected static final Object[] NULL_ARG = new Object[] {};
  protected Object obj;
  protected PropertyDescriptor[] props;

  public interface PropertyCallback {
    void foundProperty(Object obj, String prefix, String name, Object value);
  }

  /**
    Create a new PropertyGetter for the specified Object. This is done
    in prepartion for invoking {@link
    #getProperties(PropertyGetter.PropertyCallback, String)} one or
    more times.

    @param obj the object for which to set properties */
  public
  PropertyGetter(Object obj) throws IntrospectionException {
    BeanInfo bi = Introspector.getBeanInfo(obj.getClass());
    props = bi.getPropertyDescriptors();
    this.obj = obj;
  }

  public
  static
  void getProperties(Object obj, PropertyCallback callback, String prefix) {
    try {
      new PropertyGetter(obj).getProperties(callback, prefix);
    } catch (IntrospectionException ex) {
      LogLog.error("Failed to introspect object " + obj, ex);
    }
  }

  public
  void getProperties(PropertyCallback callback, String prefix) {
    for (int i = 0; i < props.length; i++) {
      Method getter = props[i].getReadMethod();
      if (getter == null) continue;
      if (!isHandledType(getter.getReturnType())) {
	//System.err.println("Ignoring " + props[i].getName() +" " + getter.getReturnType());
	continue;
      }
      String name = props[i].getName();
      try {
	Object result = getter.invoke(obj, NULL_ARG);
	//System.err.println("PROP " + name +": " + result);
	if (result != null) {
	  callback.foundProperty(obj, prefix, name, result);
	}
      } catch (IllegalAccessException ex) {
	    LogLog.warn("Failed to get value of property " + name);
      } catch (InvocationTargetException ex) {
        if (ex.getTargetException() instanceof InterruptedException
                || ex.getTargetException() instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
        }
        LogLog.warn("Failed to get value of property " + name);
      } catch (RuntimeException ex) {
	    LogLog.warn("Failed to get value of property " + name);
      }
    }
  }

  protected
  boolean isHandledType(Class type) {
    return String.class.isAssignableFrom(type) ||
      Integer.TYPE.isAssignableFrom(type) ||
      Long.TYPE.isAssignableFrom(type)    ||
      Boolean.TYPE.isAssignableFrom(type) ||
      Priority.class.isAssignableFrom(type);
  }
}
