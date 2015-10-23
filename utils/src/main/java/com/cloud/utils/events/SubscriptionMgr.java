//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class SubscriptionMgr {
    protected final static Logger s_logger = Logger.getLogger(SubscriptionMgr.class);

    private static SubscriptionMgr s_instance = new SubscriptionMgr();

    private Map<String, List<SubscriberInfo>> registry;

    private SubscriptionMgr() {
        registry = new HashMap<String, List<SubscriberInfo>>();
    }

    public static SubscriptionMgr getInstance() {
        return s_instance;
    }

    public <T> void subscribe(String subject, T subscriber, String listenerMethod) throws SecurityException, NoSuchMethodException {

        synchronized (this) {
            List<SubscriberInfo> l = getAndSetSubscriberList(subject);

            Class<?> clazz = subscriber.getClass();
            SubscriberInfo subscribeInfo = new SubscriberInfo(clazz, subscriber, listenerMethod);

            if (!l.contains(subscribeInfo))
                l.add(subscribeInfo);
        }
    }

    public <T> void unsubscribe(String subject, T subscriber, String listenerMethod) {
        synchronized (this) {
            List<SubscriberInfo> l = getSubscriberList(subject);
            if (l != null) {
                for (SubscriberInfo info : l) {
                    if (info.isMe(subscriber.getClass(), subscriber, listenerMethod)) {
                        l.remove(info);
                        return;
                    }
                }
            }
        }
    }

    public void notifySubscribers(String subject, Object sender, EventArgs args) {

        List<SubscriberInfo> l = getExecutableSubscriberList(subject);
        if (l != null) {
            for (SubscriberInfo info : l) {
                try {
                    info.execute(sender, args);
                } catch (IllegalArgumentException e) {
                    s_logger.warn("Exception on notifying event subscribers: ", e);
                } catch (IllegalAccessException e) {
                    s_logger.warn("Exception on notifying event subscribers: ", e);
                } catch (InvocationTargetException e) {
                    s_logger.warn("Exception on notifying event subscribers: ", e);
                }
            }
        }
    }

    private List<SubscriberInfo> getAndSetSubscriberList(String subject) {
        List<SubscriberInfo> l = registry.get(subject);
        if (l == null) {
            l = new ArrayList<SubscriberInfo>();
            registry.put(subject, l);
        }

        return l;
    }

    private List<SubscriberInfo> getSubscriberList(String subject) {
        return registry.get(subject);
    }

    private synchronized List<SubscriberInfo> getExecutableSubscriberList(String subject) {
        List<SubscriberInfo> l = registry.get(subject);
        if (l != null) {
            // do a shadow clone
            ArrayList<SubscriberInfo> clonedList = new ArrayList<SubscriberInfo>(l.size());
            for (SubscriberInfo info : l)
                clonedList.add(info);

            return clonedList;
        }
        return null;
    }

    private static class SubscriberInfo {
        private Class<?> clazz;
        private Object subscriber;
        private String methodName;
        private Method method;

        public SubscriberInfo(Class<?> clazz, Object subscriber, String methodName) throws SecurityException, NoSuchMethodException {

            this.clazz = clazz;
            this.subscriber = subscriber;
            this.methodName = methodName;
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes != null && paramTypes.length == 2 && paramTypes[0] == Object.class && EventArgs.class.isAssignableFrom(paramTypes[1])) {
                        this.method = method;

                        break;
                    }
                }
            }
            if (this.method == null)
                throw new NoSuchMethodException();
        }

        public void execute(Object sender, EventArgs args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

            method.invoke(subscriber, sender, args);
        }

        public boolean isMe(Class<?> clazz, Object subscriber, String methodName) {
            return this.clazz == clazz && this.subscriber == subscriber && this.methodName.equals(methodName);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;

            if (o instanceof SubscriberInfo) {
                return this.clazz == ((SubscriberInfo)o).clazz && this.subscriber == ((SubscriberInfo)o).subscriber &&
                    this.methodName.equals(((SubscriberInfo)o).methodName);
            }
            return false;
        }
    }
}
