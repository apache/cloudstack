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
package org.apache.log4j.net;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.helpers.LogLog;

public class ZeroConfSupport {
    private static Object jmDNS = initializeJMDNS();

    Object serviceInfo;
    private static Class jmDNSClass;
    private static Class serviceInfoClass;

    public ZeroConfSupport(String zone, int port, String name, Map properties) {
        //if version 3 is available, use it to constuct a serviceInfo instance, otherwise support the version1 API
        boolean isVersion3 = false;
        try {
            //create method is in version 3, not version 1
            jmDNSClass.getMethod("create", null);
            isVersion3 = true;
        } catch (NoSuchMethodException e) {
            //no-op
        }

        if (isVersion3) {
            LogLog.debug("using JmDNS version 3 to construct serviceInfo instance");
            serviceInfo = buildServiceInfoVersion3(zone, port, name, properties);
        } else {
            LogLog.debug("using JmDNS version 1.0 to construct serviceInfo instance");
            serviceInfo = buildServiceInfoVersion1(zone, port, name, properties);
        }
    }

    public ZeroConfSupport(String zone, int port, String name) {
        this(zone, port, name, new HashMap());
    }

    private static Object createJmDNSVersion1()
    {
        try {
            return jmDNSClass.newInstance();
        } catch (InstantiationException e) {
            LogLog.warn("Unable to instantiate JMDNS", e);
        } catch (IllegalAccessException e) {
            LogLog.warn("Unable to instantiate JMDNS", e);
        }
        return null;
    }

    private static Object createJmDNSVersion3()
    {
        try {
            Method jmDNSCreateMethod = jmDNSClass.getMethod("create", null);
            return jmDNSCreateMethod.invoke(null, null);
        } catch (IllegalAccessException e) {
            LogLog.warn("Unable to instantiate jmdns class", e);
        } catch (NoSuchMethodException e) {
            LogLog.warn("Unable to access constructor", e);
        } catch (InvocationTargetException e) {
                LogLog.warn("Unable to call constructor", e);
        }
        return null;
    }

    private Object buildServiceInfoVersion1(String zone, int port, String name, Map properties) {
        //version 1 uses a hashtable
        Hashtable hashtableProperties = new Hashtable(properties);
        try {
            Class[] args = new Class[6];
            args[0] = String.class;
            args[1] = String.class;
            args[2] = int.class;
            args[3] = int.class; //weight (0)
            args[4] = int.class; //priority (0)
            args[5] = Hashtable.class;
            Constructor constructor  = serviceInfoClass.getConstructor(args);
            Object[] values = new Object[6];
            values[0] = zone;
            values[1] = name;
            values[2] = new Integer(port);
            values[3] = new Integer(0);
            values[4] = new Integer(0);
            values[5] = hashtableProperties;
            Object result = constructor.newInstance(values);
            LogLog.debug("created serviceinfo: " + result);
            return result;
        } catch (IllegalAccessException e) {
            LogLog.warn("Unable to construct ServiceInfo instance", e);
        } catch (NoSuchMethodException e) {
            LogLog.warn("Unable to get ServiceInfo constructor", e);
        } catch (InstantiationException e) {
            LogLog.warn("Unable to construct ServiceInfo instance", e);
        } catch (InvocationTargetException e) {
            LogLog.warn("Unable to construct ServiceInfo instance", e);
        }
        return null;
    }

    private Object buildServiceInfoVersion3(String zone, int port, String name, Map properties) {
        try {
            Class[] args = new Class[6];
            args[0] = String.class; //zone/type
            args[1] = String.class; //display name
            args[2] = int.class; //port
            args[3] = int.class; //weight (0)
            args[4] = int.class; //priority (0)
            args[5] = Map.class;
            Method serviceInfoCreateMethod = serviceInfoClass.getMethod("create", args);
            Object[] values = new Object[6];
            values[0] = zone;
            values[1] = name;
            values[2] = new Integer(port);
            values[3] = new Integer(0);
            values[4] = new Integer(0);
            values[5] = properties;
            Object result = serviceInfoCreateMethod.invoke(null, values);
            LogLog.debug("created serviceinfo: " + result);
            return result;
        } catch (IllegalAccessException e) {
            LogLog.warn("Unable to invoke create method", e);
        } catch (NoSuchMethodException e) {
            LogLog.warn("Unable to find create method", e);
        } catch (InvocationTargetException e) {
                LogLog.warn("Unable to invoke create method", e);
        }
        return null;
    }

    public void advertise() {
        try {
            Method method = jmDNSClass.getMethod("registerService", new Class[]{serviceInfoClass});
            method.invoke(jmDNS, new Object[]{serviceInfo});
            LogLog.debug("registered serviceInfo: " + serviceInfo);
        } catch(IllegalAccessException e) {
            LogLog.warn("Unable to invoke registerService method", e);
        } catch(NoSuchMethodException e) {
            LogLog.warn("No registerService method", e);
        } catch(InvocationTargetException e) {
            LogLog.warn("Unable to invoke registerService method", e);
        }
    }

    public void unadvertise() {
        try {
            Method method = jmDNSClass.getMethod("unregisterService", new Class[]{serviceInfoClass});
            method.invoke(jmDNS, new Object[]{serviceInfo});
            LogLog.debug("unregistered serviceInfo: " + serviceInfo);
        } catch(IllegalAccessException e) {
            LogLog.warn("Unable to invoke unregisterService method", e);
        } catch(NoSuchMethodException e) {
            LogLog.warn("No unregisterService method", e);
        } catch(InvocationTargetException e) {
            LogLog.warn("Unable to invoke unregisterService method", e);
       }
    }

    private static Object initializeJMDNS() {
        try {
            jmDNSClass = Class.forName("javax.jmdns.JmDNS");
            serviceInfoClass = Class.forName("javax.jmdns.ServiceInfo");
        } catch (ClassNotFoundException e) {
            LogLog.warn("JmDNS or serviceInfo class not found", e);
        }

        //if version 3 is available, use it to constuct a serviceInfo instance, otherwise support the version1 API
        boolean isVersion3 = false;
        try {
            //create method is in version 3, not version 1
            jmDNSClass.getMethod("create", null);
            isVersion3 = true;
        } catch (NoSuchMethodException e) {
            //no-op
        }

        if (isVersion3) {
            return createJmDNSVersion3();
        } else {
            return createJmDNSVersion1();
        }
    }

    public static Object getJMDNSInstance() {
        return jmDNS;
    }
}
