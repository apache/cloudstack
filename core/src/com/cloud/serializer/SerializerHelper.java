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

package com.cloud.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;

/**
 * Note: toPairList and appendPairList only support simple POJO objects currently
 */
public class SerializerHelper {
    public static final Logger s_logger = Logger.getLogger(SerializerHelper.class.getName());
    public static final String token = "/";

    public static String toSerializedStringOld(Object result) {
        if (result != null) {
            Class<?> clz = result.getClass();
            Gson gson = GsonHelper.getGson();
            return clz.getName() + token + gson.toJson(result);
        }
        return null;
    }

    public static Object fromSerializedString(String result) {
        try {
            if (result != null && !result.isEmpty()) {

                String[] serializedParts = result.split(token);

                if (serializedParts.length < 2) {
                    return null;
                }
                String clzName = serializedParts[0];
                String nameField = null;
                String content = null;
                if (serializedParts.length == 2) {
                    content = serializedParts[1];
                } else {
                    nameField = serializedParts[1];
                    int index = result.indexOf(token + nameField + token);
                    content = result.substring(index + nameField.length() + 2);
                }

                Class<?> clz;
                try {
                    clz = Class.forName(clzName);
                } catch (ClassNotFoundException e) {
                    return null;
                }

                Gson gson = GsonHelper.getGson();
                Object obj = gson.fromJson(content, clz);
                return obj;
            }
            return null;
        } catch (RuntimeException e) {
            s_logger.error("Caught runtime exception when doing GSON deserialization on: " + result);
            throw e;
        }
    }

    public static List<Pair<String, Object>> toPairList(Object o, String name) {
        List<Pair<String, Object>> l = new ArrayList<Pair<String, Object>>();
        return appendPairList(l, o, name);
    }

    public static List<Pair<String, Object>> appendPairList(List<Pair<String, Object>> l, Object o, String name) {
        if (o != null) {
            Class<?> clz = o.getClass();

            if (clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                l.add(new Pair<String, Object>(name, o.toString()));
                return l;
            }

            for (Field f : clz.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                Param param = f.getAnnotation(Param.class);
                if (param == null) {
                    continue;
                }

                String propName = f.getName();
                if (!param.propName().isEmpty()) {
                    propName = param.propName();
                }

                String paramName = param.name();
                if (paramName.isEmpty()) {
                    paramName = propName;
                }

                Method method = getGetMethod(o, propName);
                if (method != null) {
                    try {
                        Object fieldValue = method.invoke(o);
                        if (fieldValue != null) {
                            if (f.getType() == Date.class) {
                                l.add(new Pair<String, Object>(paramName, DateUtil.getOutputString((Date)fieldValue)));
                            } else {
                                l.add(new Pair<String, Object>(paramName, fieldValue.toString()));
                            }
                        }
                        //else
                        //    l.add(new Pair<String, Object>(paramName, ""));
                    } catch (IllegalArgumentException e) {
                        s_logger.error("Illegal argument exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);

                    } catch (IllegalAccessException e) {
                        s_logger.error("Illegal access exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
                    } catch (InvocationTargetException e) {
                        s_logger.error("Invocation target exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
                    }
                }
            }
        }
        return l;
    }

    private static Method getGetMethod(Object o, String propName) {
        Method method = null;
        String methodName = getGetMethodName("get", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting POJO " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("POJO " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName +
                    ", will check is-prefixed method to see if it is boolean property");
            }
        }

        if (method != null) {
            return method;
        }

        methodName = getGetMethodName("is", propName);
        try {
            method = o.getClass().getMethod(methodName);
        } catch (SecurityException e1) {
            s_logger.error("Security exception in getting POJO " + o.getClass().getName() + " get method for property: " + propName);
        } catch (NoSuchMethodException e1) {
            s_logger.warn("POJO " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName);
        }
        return method;
    }

    private static String getGetMethodName(String prefix, String fieldName) {
        StringBuffer sb = new StringBuffer(prefix);

        if (fieldName.length() >= prefix.length() && fieldName.substring(0, prefix.length()).equals(prefix)) {
            return fieldName;
        } else {
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
        }

        return sb.toString();
    }
}
