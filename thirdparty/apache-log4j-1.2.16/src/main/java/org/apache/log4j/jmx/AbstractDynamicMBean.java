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

package org.apache.log4j.jmx;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import org.apache.log4j.Logger;
import org.apache.log4j.Appender;

public abstract class AbstractDynamicMBean implements DynamicMBean,
                                                      MBeanRegistration {

  String dClassName;
  MBeanServer server;
  private final Vector mbeanList = new Vector();

    /**
     * Get MBean name.
     * @param appender appender, may not be null.
     * @return name.
     * @since 1.2.16
     */
  static protected String getAppenderName(final Appender appender){
      String name = appender.getName();
      if (name == null || name.trim().length() == 0) {
          // try to get some form of a name, because null is not allowed (exception), and empty string certainly isn't useful in JMX..
          name = appender.toString();
      }
      return name;
  }
      

  /**
   * Enables the to get the values of several attributes of the Dynamic MBean.
   */
  public
  AttributeList getAttributes(String[] attributeNames) {

    // Check attributeNames is not null to avoid NullPointerException later on
    if (attributeNames == null) {
      throw new RuntimeOperationsException(
			   new IllegalArgumentException("attributeNames[] cannot be null"),
			   "Cannot invoke a getter of " + dClassName);
    }

    AttributeList resultList = new AttributeList();

    // if attributeNames is empty, return an empty result list
    if (attributeNames.length == 0)
      return resultList;

    // build the result attribute list
    for (int i=0 ; i<attributeNames.length ; i++){
      try {
	Object value = getAttribute((String) attributeNames[i]);
	resultList.add(new Attribute(attributeNames[i],value));
      } catch (JMException e) {
	     e.printStackTrace();
      } catch (RuntimeException e) {
	     e.printStackTrace();
      }
    }
    return(resultList);
  }

  /**
   * Sets the values of several attributes of the Dynamic MBean, and returns the
   * list of attributes that have been set.
   */
  public AttributeList setAttributes(AttributeList attributes) {

    // Check attributes is not null to avoid NullPointerException later on
    if (attributes == null) {
      throw new RuntimeOperationsException(
                    new IllegalArgumentException("AttributeList attributes cannot be null"),
		    "Cannot invoke a setter of " + dClassName);
    }
    AttributeList resultList = new AttributeList();

    // if attributeNames is empty, nothing more to do
    if (attributes.isEmpty())
      return resultList;

    // for each attribute, try to set it and add to the result list if successfull
    for (Iterator i = attributes.iterator(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      try {
	setAttribute(attr);
	String name = attr.getName();
	Object value = getAttribute(name);
	resultList.add(new Attribute(name,value));
      } catch(JMException e) {
	    e.printStackTrace();
      } catch(RuntimeException e) {
	    e.printStackTrace();
      }
    }
    return(resultList);
  }

  protected
  abstract
  Logger getLogger();

  public
  void postDeregister() {
    getLogger().debug("postDeregister is called.");
  }

  public
  void postRegister(java.lang.Boolean registrationDone) {
  }



  public
  ObjectName preRegister(MBeanServer server, ObjectName name) {
    getLogger().debug("preRegister called. Server="+server+ ", name="+name);
    this.server = server;
    return name;
  }
  /**
   * Registers MBean instance in the attached server. Must <em>NOT</em>
   * be called before registration of this instance.
   */
  protected
  void registerMBean(Object mbean, ObjectName objectName)
  throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException {
    server.registerMBean(mbean, objectName);
    mbeanList.add(objectName);
  }

  /**
   * Performs cleanup for deregistering this MBean. Default implementation
   * unregisters MBean instances which are registered using 
   * {@link #registerMBean(Object mbean, ObjectName objectName)}.
   */
   public
   void preDeregister() {
     getLogger().debug("preDeregister called.");
     
    Enumeration iterator = mbeanList.elements();
    while (iterator.hasMoreElements()) {
      ObjectName name = (ObjectName) iterator.nextElement();
      try {
        server.unregisterMBean(name);
      } catch (InstanceNotFoundException e) {
   getLogger().warn("Missing MBean " + name.getCanonicalName());
      } catch (MBeanRegistrationException e) {
   getLogger().warn("Failed unregistering " + name.getCanonicalName());
      }
    }
   }


}
