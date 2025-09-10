/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.framework.messagebus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class MessageDispatcher implements MessageSubscriber {
    protected Logger logger = LogManager.getLogger(getClass());

    private static Map<Class<?>, List<Method>> s_handlerCache = new HashMap<Class<?>, List<Method>>();

    private static Map<Object, MessageDispatcher> s_targetMap = new HashMap<Object, MessageDispatcher>();
    private Object _targetObject;

    public MessageDispatcher(Object targetObject) {
        _targetObject = targetObject;
        buildHandlerMethodCache(targetObject.getClass());
    }

    @Override
    public void onPublishMessage(String senderAddress, String subject, Object args) {
        dispatch(_targetObject, subject, senderAddress, args);
    }

    public static MessageDispatcher getDispatcher(Object targetObject) {
        MessageDispatcher dispatcher;
        synchronized (s_targetMap) {
            dispatcher = s_targetMap.get(targetObject);
            if (dispatcher == null) {
                dispatcher = new MessageDispatcher(targetObject);
                s_targetMap.put(targetObject, dispatcher);
            }
        }
        return dispatcher;
    }

    public static void removeDispatcher(Object targetObject) {
        synchronized (s_targetMap) {
            s_targetMap.remove(targetObject);
        }
    }

    public boolean dispatch(Object target, String subject, String senderAddress, Object args) {
        assert (subject != null);
        assert (target != null);

        Method handler = resolveHandler(target.getClass(), subject);
        if (handler == null)
            return false;

        try {
            handler.invoke(target, subject, senderAddress, args);
        } catch (IllegalArgumentException e) {
            logger.error("Unexpected exception when calling " + target.getClass().getName() + "." + handler.getName(), e);
            throw new RuntimeException("IllegalArgumentException when invoking event handler for subject: " + subject);
        } catch (IllegalAccessException e) {
            logger.error("Unexpected exception when calling " + target.getClass().getName() + "." + handler.getName(), e);
            throw new RuntimeException("IllegalAccessException when invoking event handler for subject: " + subject);
        } catch (InvocationTargetException e) {
            logger.error("Unexpected exception when calling " + target.getClass().getName() + "." + handler.getName(), e);
            throw new RuntimeException("InvocationTargetException when invoking event handler for subject: " + subject);
        }

        return true;
    }

    public Method resolveHandler(Class<?> handlerClz, String subject) {
        synchronized (s_handlerCache) {
            List<Method> handlerList = s_handlerCache.get(handlerClz);
            if (handlerList != null) {
                for (Method method : handlerList) {
                    MessageHandler annotation = method.getAnnotation(MessageHandler.class);
                    assert (annotation != null);

                    if (match(annotation.topic(), subject)) {
                        return method;
                    }
                }
            } else {
                logger.error("Handler class " + handlerClz.getName() + " is not registered");
            }
        }

        return null;
    }

    private static boolean match(String expression, String param) {
        return param.matches(expression);
    }

    private void buildHandlerMethodCache(Class<?> handlerClz) {
        if (logger.isInfoEnabled())
            logger.info("Build message handler cache for " + handlerClz.getName());

        synchronized (s_handlerCache) {
            List<Method> handlerList = s_handlerCache.get(handlerClz);
            if (handlerList == null) {
                handlerList = new ArrayList<Method>();
                s_handlerCache.put(handlerClz, handlerList);

                Class<?> clz = handlerClz;
                while (clz != null && clz != Object.class) {
                    for (Method method : clz.getDeclaredMethods()) {
                        MessageHandler annotation = method.getAnnotation(MessageHandler.class);
                        if (annotation != null) {
                            // allow private member access via reflection
                            method.setAccessible(true);
                            handlerList.add(method);

                            if (logger.isInfoEnabled())
                                logger.info("Add message handler " + handlerClz.getName() + "." + method.getName() + " to cache");
                        }
                    }

                    clz = clz.getSuperclass();
                }
            } else {
                if (logger.isInfoEnabled())
                    logger.info("Message handler for class " + handlerClz.getName() + " is already in cache");
            }
        }

        if (logger.isInfoEnabled())
            logger.info("Done building message handler cache for " + handlerClz.getName());
    }
}
